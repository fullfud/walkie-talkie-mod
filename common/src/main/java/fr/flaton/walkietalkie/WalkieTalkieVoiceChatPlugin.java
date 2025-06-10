package fr.flaton.walkietalkie;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VolumeCategory;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import fr.flaton.walkietalkie.block.entity.SpeakerBlockEntity;
import fr.flaton.walkietalkie.config.ModConfig;
import fr.flaton.walkietalkie.item.WalkieTalkieItem;
import fr.flaton.walkietalkie.ModSoundEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ForgeVoicechatPlugin
public class WalkieTalkieVoiceChatPlugin implements VoicechatPlugin {

    public static final Logger LOGGER = LoggerFactory.getLogger(Constants.MOD_ID);

    public final static String SPEAKER_CATEGORY = "speakers";
    private static final Random random = new Random();
    private static final int SAMPLE_RATE = 48000;

    // --- НОВЫЕ И ИЗМЕНЕННЫЕ КОНСТАНТЫ ДЛЯ НАСТРОЙКИ ---
    // НОВОЕ: Дистанция, в пределах которой качество связи ИДЕАЛЬНОЕ (100%).
    private static final float CLEAR_RECEPTION_DISTANCE = 50.0f;
    // Максимальная дистанция, на которой голос еще можно разобрать
    private static final float MAX_RELIABLE_DISTANCE = 1000.0f;
    // Качество сигнала на максимальной дистанции (0.0 - только шум, 1.0 - идеальный сигнал)
    private static final float MIN_SIGNAL_QUALITY_AT_MAX_DISTANCE = 0.2f;
    // Интенсивность шума, когда рация включена, но никто не говорит
    private static final float IDLE_NOISE_INTENSITY = 0.15f;
    // ------------------------------------

    private final Map<UUID, Boolean> walkiePowerState = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> playerSpeakingState = new ConcurrentHashMap<>();
    private final Map<UUID, AudioProcessingState> playerAudioStates = new ConcurrentHashMap<>();
    // НОВОЕ: Карта для отслеживания, кто кому говорит, чтобы предотвратить фоновый шум во время разговора.
    private final Map<UUID, Boolean> isPlayerReceivingVoice = new ConcurrentHashMap<>();


    @Nullable
    public static VoicechatServerApi api;

    @Override
    public String getPluginId() {
        return Constants.MOD_ID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicPacket);
        registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
    }

    private void onServerStarted(VoicechatServerStartedEvent event) {
        api = event.getVoicechat();
        VolumeCategory speakers = api.volumeCategoryBuilder()
                .setId(SPEAKER_CATEGORY)
                .setName("Speakers")
                .setDescription("The volume of all speakers")
                .setIcon(getIcon("assets/walkietalkie/textures/block/speaker.png"))
                .build();
        api.registerVolumeCategory(speakers);
    }

    // --- ЛОГИКА onServerTick ПОЛНОСТЬЮ ПЕРЕРАБОТАНА ---
    public void onServerTick(MinecraftServer server) {
        if (api == null) {
            return;
        }

        // Сбрасываем статус получения голоса для всех
        isPlayerReceivingVoice.clear();
        // Определяем, кто сейчас получает голос
        for (UUID senderId : playerSpeakingState.keySet()) {
            if (playerSpeakingState.getOrDefault(senderId, false)) {
                ServerPlayerEntity sender = server.getPlayerManager().getPlayer(senderId);
                if (sender != null) {
                    findValidReceivers(sender).forEach(receiver -> isPlayerReceivingVoice.put(receiver.getUuid(), true));
                }
            }
        }

        if (server.getTicks() % 4 != 0) {
            return;
        }

        // Проверяем каждого игрока на сервере, должен ли он слышать фоновый шум
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID playerId = player.getUuid();
            ItemStack walkieStack = Util.getWalkieTalkieActivated(player);

            // Игрок должен слышать шум, если:
            // 1. У него есть активная рация
            // 2. Он не в муте
            // 3. Ему в данный момент никто не говорит по рации
            if (walkieStack != null && !isWalkieTalkieMute(walkieStack) && !isPlayerReceivingVoice.getOrDefault(playerId, false)) {
                AudioProcessingState state = playerAudioStates.computeIfAbsent(playerId, id -> new AudioProcessingState());
                short[] noiseSample = generateRadioNoise(960, IDLE_NOISE_INTENSITY, state);

                Position receiverPosition = api.createPosition(player.getX(), player.getY(), player.getZ());
                LocationalAudioChannel channel = api.createLocationalAudioChannel(UUID.randomUUID(), api.fromServerLevel(player.getWorld()), receiverPosition);

                if (channel != null) {
                    channel.setFilter(p -> p.getUuid().equals(playerId));
                    AudioPlayer audioPlayer = api.createAudioPlayer(channel, api.createEncoder(), noiseSample);
                    audioPlayer.startPlaying();
                }
            }
        }
    }


    public void onMicPacket(MicrophonePacketEvent event) {
        if (api == null || event.getSenderConnection() == null) return;
        if (!(event.getSenderConnection().getPlayer().getPlayer() instanceof ServerPlayerEntity senderPlayer)) return;

        UUID playerId = senderPlayer.getUuid();
        ItemStack senderItemStack = Util.getWalkieTalkieInHand(senderPlayer);

        boolean isPowerOn = senderItemStack != null && isWalkieTalkieActivate(senderItemStack);
        walkiePowerState.put(playerId, isPowerOn);

        if (!isPowerOn) {
            playerSpeakingState.put(playerId, false);
            playerAudioStates.remove(playerId);
            return;
        }

        if (isWalkieTalkieMute(senderItemStack)) {
            playerSpeakingState.put(playerId, false);
            return;
        }

        boolean isCurrentlySpeaking = event.getPacket().getOpusEncodedData().length > 0;
        boolean wasSpeaking = playerSpeakingState.getOrDefault(playerId, false);

        if (isCurrentlySpeaking && !wasSpeaking) {
            playerSpeakingState.put(playerId, true);
            senderPlayer.getWorld().playSound(null, senderPlayer.getBlockPos(), ModSoundEvents.ON_SOUND_EVENT.get(), SoundCategory.PLAYERS, 1.0f, 1.0f);
        } else if (!isCurrentlySpeaking && wasSpeaking) {
            playerSpeakingState.put(playerId, false);
            senderPlayer.getWorld().playSound(null, senderPlayer.getBlockPos(), ModSoundEvents.OFF_SOUND_EVENT.get(), SoundCategory.PLAYERS, 1.0f, 1.0f);
        }

        if (isCurrentlySpeaking) {
            playerAudioStates.computeIfAbsent(playerId, id -> new AudioProcessingState());
            event.cancel();
            processVoice(senderPlayer, senderItemStack, event.getPacket().getOpusEncodedData());
        }
    }

    private void processVoice(ServerPlayerEntity senderPlayer, ItemStack senderItemStack, byte[] opusData) {
        UUID playerId = senderPlayer.getUuid();
        AudioProcessingState senderState = playerAudioStates.get(playerId);
        if (senderState == null) return;

        Set<ServerPlayerEntity> validReceivers = findValidReceivers(senderPlayer);
        OpusDecoder decoder = api.createDecoder();
        short[] rawAudio = decoder.decode(opusData);
        decoder.close();

        // Для спикеров сигнал всегда идеальный (они в зоне действия)
        short[] speakerFinalAudio = applyFullRadioEffect(rawAudio, 1.0f, senderState);
        SpeakerBlockEntity.getSpeakersActivatedInRange(getCanal(senderItemStack), senderPlayer.getWorld(), senderPlayer.getPos(), getRange(senderItemStack))
                .forEach(speakerBlockEntity -> speakerBlockEntity.playSound(api, speakerFinalAudio, senderPlayer));

        for (ServerPlayerEntity receiverPlayer : validReceivers) {
            double distance = senderPlayer.getPos().distanceTo(receiverPlayer.getPos());

            // --- ИЗМЕНЕНА ЛОГИКА РАСЧЕТА КАЧЕСТВА СИГНАЛА ---
            float signalQuality;
            if (distance <= CLEAR_RECEPTION_DISTANCE) {
                signalQuality = 1.0f; // Идеальное качество в зоне чистого приёма
            } else {
                // Рассчитываем расстояние за пределами чистой зоны
                float effectiveDistance = (float)distance - CLEAR_RECEPTION_DISTANCE;
                // Диапазон, на котором происходит ухудшение
                float degradationRange = MAX_RELIABLE_DISTANCE - CLEAR_RECEPTION_DISTANCE;
                // Процент ухудшения
                float falloff = Math.min(effectiveDistance, degradationRange) / degradationRange;
                // Рассчитываем итоговое качество
                signalQuality = 1.0f - falloff * (1.0f - MIN_SIGNAL_QUALITY_AT_MAX_DISTANCE);
            }
            // ----------------------------------------------------

            short[] playerFinalAudio = applyFullRadioEffect(rawAudio, signalQuality, senderState);
            Position receiverPosition = api.createPosition(receiverPlayer.getX(), receiverPlayer.getY(), receiverPlayer.getZ());
            LocationalAudioChannel channel = api.createLocationalAudioChannel(UUID.randomUUID(), api.fromServerLevel(receiverPlayer.getWorld()), receiverPosition);

            if (channel != null) {
                channel.setFilter(player -> player.getUuid().equals(receiverPlayer.getUuid()));
                AudioPlayer player = api.createAudioPlayer(channel, api.createEncoder(), playerFinalAudio);
                player.startPlaying();
            }
        }
    }

    private Set<ServerPlayerEntity> findValidReceivers(ServerPlayerEntity sender) {
        ItemStack senderStack = Util.getWalkieTalkieInHand(sender);
        if (senderStack == null || sender.getServer() == null) {
            return Collections.emptySet();
        }
        int senderCanal = getCanal(senderStack);

        Set<ServerPlayerEntity> validReceivers = new HashSet<>();
        for (PlayerEntity p : sender.getServer().getPlayerManager().getPlayerList()) {
            if (p instanceof ServerPlayerEntity receiverPlayer && !receiverPlayer.getUuid().equals(sender.getUuid())) {
                if (!ModConfig.crossDimensionsEnabled && !receiverPlayer.getWorld().getDimension().equals(sender.getWorld().getDimension())) continue;
                ItemStack receiverStack = Util.getWalkieTalkieActivated(receiverPlayer);
                if (receiverStack == null) continue;
                if (isWalkieTalkieMute(receiverStack)) continue; // ИЗМЕНЕНО: Не отправляем звук тем, у кого рация в муте
                if (!canBroadcastToReceiver(sender, receiverPlayer, getRange(receiverStack)) || getCanal(receiverStack) != senderCanal) continue;
                validReceivers.add(receiverPlayer);
            }
        }
        return validReceivers;
    }

    private short[] applyFullRadioEffect(short[] rawAudio, float signalQuality, AudioProcessingState state) {
        short[] filtered = applyBandpassFilter(rawAudio, state);
        short[] compressed = applyCompression(filtered, state);

        short[] noise = generateRadioNoise(compressed.length, 1.0f, state);
        short[] output = new short[compressed.length];

        // --- ИЗМЕНЕНА ЛОГИКА СМЕШИВАНИЯ ГОЛОСА И ШУМА ДЛЯ ЧИСТОГО ЗВУКА ---
        float signalLevel = 1.0f; // Громкость голоса не меняем
        // Уровень шума растет нелинейно (квадратично) для более резкого появления на больших дистанциях
        float noiseLevel = (1.0f - signalQuality) * (1.0f - signalQuality);
        // ----------------------------------------------------

        for (int i = 0; i < output.length; i++) {
            // Смешиваем чистый сжатый голос с шумом
            float mixed = compressed[i] * signalLevel + noise[i] * noiseLevel;
            // Применяем ослабление громкости, зависящее от качества, ко всему миксу
            // На 100% качества громкость 1.0, на 20% качества громкость ~0.6, чтобы сильный шум не оглушал
            float volumeFalloff = (float) (MIN_SIGNAL_QUALITY_AT_MAX_DISTANCE + (signalQuality * (1.0 - MIN_SIGNAL_QUALITY_AT_MAX_DISTANCE)));
            mixed *= volumeFalloff;
            output[i] = (short)Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, mixed));
        }
        return output;
    }

    private short[] applyBandpassFilter(short[] input, AudioProcessingState state) {
        // ... код без изменений ...
        short[] output = new short[input.length];
        float highpassCutoff = 300.0f / SAMPLE_RATE;
        float lowpassCutoff = 3400.0f / SAMPLE_RATE;
        float highpassRC = (float)(1.0 / (2.0 * Math.PI * highpassCutoff));
        float lowpassRC = (float)(1.0 / (2.0 * Math.PI * lowpassCutoff));
        float highpassAlpha = highpassRC / (highpassRC + 1);
        float lowpassAlpha = lowpassRC / (lowpassRC + 1);

        for (int i = 0; i < input.length; i++) {
            float currentSample = input[i] / 32768.0f;
            state.highpassHistory[0] = highpassAlpha * (state.highpassHistory[0] + currentSample - state.highpassHistory[1]);
            state.highpassHistory[1] = currentSample;
            state.lowpassHistory[0] = state.lowpassHistory[0] + lowpassAlpha * (state.highpassHistory[0] - state.lowpassHistory[0]);
            output[i] = (short)(state.lowpassHistory[0] * 32768.0f);
        }
        return output;
    }

    // --- ИЗМЕНЕНО: Компрессор стал менее агрессивным ---
    private short[] applyCompression(short[] input, AudioProcessingState state) {
        short[] output = new short[input.length];
        float threshold = 0.5f;
        float ratio = 3.0f;
        float attack = 0.002f;
        float release = 0.1f;
        float makeupGain = 1.5f; // Уменьшен гейн, чтобы избежать искажений

        for (int i = 0; i < input.length; i++) {
            float sample = input[i] / 32768.0f;
            float absSample = Math.abs(sample);
            float rate = absSample > state.envelope ? attack : release;
            state.envelope = state.envelope + rate * (absSample - state.envelope);
            float gain = 1.0f;
            if (state.envelope > threshold) {
                float excess = state.envelope - threshold;
                float compressedExcess = excess / ratio;
                gain = (threshold + compressedExcess) / state.envelope;
            }
            float compressedSample = sample * gain * makeupGain;
            // Убрана агрессивная сатурация (tanh), чтобы звук был чище
            output[i] = (short)Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, compressedSample * 32768.0f));
        }
        return output;
    }

    private short[] generateRadioNoise(int length, float intensity, AudioProcessingState state) {
        // ... код без изменений ...
        short[] noise = new short[length];
        for (int i = 0; i < length; i++) {
            float white = (random.nextFloat() * 2f - 1f);
            state.pink = 0.99f * state.pink + 0.01f * white;
            state.brown = 0.998f * state.brown + 0.002f * white;
            float noiseSample = (white * 0.2f + state.pink * 0.5f + state.brown * 0.3f);
            if (random.nextFloat() < 0.001f) {
                noiseSample += (random.nextFloat() - 0.5f) * 6.0f;
            }
            float hum = (float)Math.sin(2 * Math.PI * 60 * i / SAMPLE_RATE) * 0.03f;
            noiseSample += hum;
            state.noiseFilterHistory[0] = state.noiseFilterHistory[0] * 0.9f + noiseSample * 0.1f;
            noise[i] = (short)(state.noiseFilterHistory[0] * intensity * 3000);
        }
        return noise;
    }

    @Nullable
    private int[][] getIcon(String path) {
        // ... код без изменений ...
        try {
            Enumeration<URL> resources = WalkieTalkieVoiceChatPlugin.class.getClassLoader().getResources(path);
            while (resources.hasMoreElements()) {
                BufferedImage bufferedImage = ImageIO.read(resources.nextElement().openStream());
                if (bufferedImage.getWidth() != 16) { continue; }
                if (bufferedImage.getHeight() != 16) { continue; }
                int[][] image = new int[16][16];
                for (int x = 0; x < bufferedImage.getWidth(); x++) {
                    for (int y = 0; y < bufferedImage.getHeight(); y++) {
                        image[x][y] = bufferedImage.getRGB(x, y);
                    }
                }
                return image;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private int getCanal(ItemStack stack) { return Objects.requireNonNull(stack.getNbt()).getInt(WalkieTalkieItem.NBT_KEY_CANAL); }
    private int getRange(ItemStack stack) { WalkieTalkieItem item = (WalkieTalkieItem) Objects.requireNonNull(stack.getItem()); return item.getRange(); }
    private boolean isWalkieTalkieActivate(ItemStack stack) { return Objects.requireNonNull(stack.getNbt()).getBoolean(WalkieTalkieItem.NBT_KEY_ACTIVATE); }
    private boolean isWalkieTalkieMute(ItemStack stack) { return Objects.requireNonNull(stack.getNbt()).getBoolean(WalkieTalkieItem.NBT_KEY_MUTE); }
    private boolean canBroadcastToReceiver(PlayerEntity senderPlayer, PlayerEntity receiverPlayer, int receiverRange) { World senderWorld = senderPlayer.getWorld(); World receiverWorld = receiverPlayer.getWorld(); return Util.canBroadcastToReceiver(senderWorld, receiverWorld, senderPlayer.getPos(), receiverPlayer.getPos(), receiverRange); }

    private static class AudioProcessingState {
        float[] highpassHistory = new float[2];
        float[] lowpassHistory = new float[2];
        float[] noiseFilterHistory = new float[2];
        float envelope = 0.0f;
        float pink = 0;
        float brown = 0;
    }
}