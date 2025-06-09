package fr.flaton.walkietalkie;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VolumeCategory;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import fr.flaton.walkietalkie.block.entity.SpeakerBlockEntity;
import fr.flaton.walkietalkie.config.ModConfig;
import fr.flaton.walkietalkie.item.WalkieTalkieItem;
import fr.flaton.walkietalkie.ModSoundEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ForgeVoicechatPlugin
public class WalkieTalkieVoiceChatPlugin implements VoicechatPlugin {

    public final static String SPEAKER_CATEGORY = "speakers";
    private static final Random random = new Random();
    private static final int SAMPLE_RATE = 48000;

    // Твои системы отслеживания передач
    private final Map<UUID, Long> activeTransmissions = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> transmissionReceivers = new ConcurrentHashMap<>();
    private static final long TRANSMISSION_TIMEOUT = 500;

    private final Map<UUID, AudioProcessingState> transmissionStates = new ConcurrentHashMap<>();

    @Nullable
    public static VoicechatServerApi api;

    // Твой класс для хранения состояния фильтров
    private static class AudioProcessingState {
        float[] highpassHistory = new float[2];
        float[] lowpassHistory = new float[2];
        float[] noiseFilterHistory = new float[2];
        float envelope = 0.0f;
        float pink = 0;
        float brown = 0;
    }

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
        startTransmissionTimeoutChecker();
    }

    // Твой поток для проверки таймаутов
    private void startTransmissionTimeoutChecker() {
        Thread timeoutChecker = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(100);
                    if (api == null) continue;
                    long currentTime = System.currentTimeMillis();

                    activeTransmissions.entrySet().removeIf(entry -> {
                        if (currentTime - entry.getValue() > TRANSMISSION_TIMEOUT) {
                            handleTransmissionEnd(entry.getKey());
                            return true;
                        }
                        return false;
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        timeoutChecker.setDaemon(true);
        timeoutChecker.start();
    }

    // Твоя обработка начала передачи
    private void handleTransmissionStart(ServerPlayerEntity sender, Set<ServerPlayerEntity> receivers) {
        UUID senderId = sender.getUuid();
        if (!activeTransmissions.containsKey(senderId)) {
            transmissionStates.put(senderId, new AudioProcessingState());
            
            // ИСПРАВЛЕНО: Используем .get() для получения SoundEvent
            sender.getWorld().playSound(null, sender.getBlockPos(), ModSoundEvents.ON_SOUND_EVENT.get(), SoundCategory.PLAYERS, 0.5f, 1.0f);
            
            Set<UUID> receiverIds = new HashSet<>();
            for (ServerPlayerEntity receiver : receivers) {
                receiverIds.add(receiver.getUuid());
                receiver.getWorld().playSound(null, receiver.getBlockPos(), ModSoundEvents.ON_SOUND_EVENT.get(), SoundCategory.PLAYERS, 0.5f, 1.0f);
            }
            transmissionReceivers.put(senderId, receiverIds);
        }
        activeTransmissions.put(senderId, System.currentTimeMillis());
    }

    // Твоя обработка конца передачи
    private void handleTransmissionEnd(UUID senderId) {
        transmissionStates.remove(senderId);
        Set<UUID> receiverIds = transmissionReceivers.remove(senderId);

        if (receiverIds != null && api != null) {
            PlayerEntity sender = getPlayerByUuid(senderId);
            if (sender != null) {
                sender.getWorld().playSound(null, sender.getBlockPos(), ModSoundEvents.OFF_SOUND_EVENT.get(), SoundCategory.PLAYERS, 0.5f, 1.0f);
            }
            for (UUID playerId : receiverIds) {
                PlayerEntity player = getPlayerByUuid(playerId);
                if (player != null) {
                    player.getWorld().playSound(null, player.getBlockPos(), ModSoundEvents.OFF_SOUND_EVENT.get(), SoundCategory.PLAYERS, 0.5f, 1.0f);
                }
            }
        }
    }
    
    // Твой вспомогательный метод (ИСПРАВЛЕННЫЙ)
    private PlayerEntity getPlayerByUuid(UUID uuid) {
        if (api == null || api.getServer().isEmpty()) return null;
        return api.getServer().get().getPlayerManager().getPlayer(uuid);
    }

    @Nullable
    private int[][] getIcon(String path) {
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

    // --- ТВОИ ОРИГИНАЛЬНЫЕ МЕТОДЫ ОБРАБОТКИ ЗВУКА ---
    private short[] applyBandpassFilter(short[] input, AudioProcessingState state) {
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

    private short[] applyCompression(short[] input, AudioProcessingState state) {
        short[] output = new short[input.length];
        float threshold = 0.4f;
        float ratio = 4.0f;
        float attack = 0.001f;
        float release = 0.05f;
        float makeupGain = 2.2f;

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
            compressedSample = (float)Math.tanh(compressedSample * 0.7) * 1.4f;
            output[i] = (short)Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, compressedSample * 32768.0f * 0.9f));
        }
        return output;
    }

    private short[] generateRadioNoise(int length, float intensity, AudioProcessingState state) {
        short[] noise = new short[length];
        for (int i = 0; i < length; i++) {
            float white = (random.nextFloat() * 2f - 1f);
            state.pink = 0.99f * state.pink + 0.01f * white;
            state.brown = 0.998f * state.brown + 0.002f * white;
            float noiseSample = (white * 0.2f + state.pink * 0.5f + state.brown * 0.3f);
            if (random.nextFloat() < 0.00005f) {
                noiseSample += (random.nextFloat() - 0.5f) * 5.0f;
            }
            float hum = (float)Math.sin(2 * Math.PI * 60 * i / SAMPLE_RATE) * 0.03f;
            noiseSample += hum;
            state.noiseFilterHistory[0] = state.noiseFilterHistory[0] * 0.9f + noiseSample * 0.1f;
            noise[i] = (short)(state.noiseFilterHistory[0] * intensity * 3000);
        }
        return noise;
    }

    private short[] addRadioDistortion(short[] input, float amount) {
        short[] output = new short[input.length];
        for (int i = 0; i < input.length; i++) {
            float sample = input[i] / 32768.0f;
            float drive = 1.0f + amount * 2.0f;
            float distorted = (float)Math.tanh(sample * drive) / drive;
            distorted = distorted + (distorted * distorted * 0.05f * amount);
            if (distorted > 0) {
                distorted = distorted * (1.0f + amount * 0.1f);
            }
            output[i] = (short)Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, distorted * 32768.0f * 0.95f));
        }
        return output;
    }
    
    private short[] applyFullRadioEffect(short[] rawAudio, float signalQuality, AudioProcessingState state) {
        short[] filtered = applyBandpassFilter(rawAudio, state);
        short[] compressed = applyCompression(filtered, state);
        float distortionAmount = (1.0f - signalQuality) * 0.3f;
        short[] distorted = addRadioDistortion(compressed, distortionAmount);
        short[] noise = generateRadioNoise(distorted.length, 1.0f, state);
        short[] output = new short[distorted.length];
        float signalLevel = 0.6f + signalQuality * 0.4f;
        float noiseLevel = (1.0f - signalQuality) * 0.4f;
        
        for (int i = 0; i < output.length; i++) {
            float mixed = distorted[i] * signalLevel + noise[i] * noiseLevel;
            output[i] = (short)Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, mixed));
        }
        return output;
    }
    
    public void onMicPacket(MicrophonePacketEvent event) {
        if (api == null || event.getSenderConnection() == null) return;
        if (!(event.getSenderConnection().getPlayer().getPlayer() instanceof ServerPlayerEntity senderPlayer)) return;

        ItemStack senderItemStack = Util.getWalkieTalkieInHand(senderPlayer);
        if (senderItemStack == null || !isWalkieTalkieActivate(senderItemStack)) {
            if (activeTransmissions.containsKey(senderPlayer.getUuid())) {
                handleTransmissionEnd(senderPlayer.getUuid());
            }
            return;
        }
        if (isWalkieTalkieMute(senderItemStack)) {
            if (activeTransmissions.containsKey(senderPlayer.getUuid())) {
                handleTransmissionEnd(senderPlayer.getUuid());
            }
            return;
        }
        
        byte[] opusData = event.getPacket().getOpusEncodedData();
        if (opusData.length == 0) {
            // Пакет тишины, обновляем таймер, чтобы передача не прервалась пока зажата кнопка
            activeTransmissions.put(senderPlayer.getUuid(), System.currentTimeMillis());
            return;
        }

        event.cancel();

        OpusDecoder decoder = api.createDecoder();
        short[] rawAudio = decoder.decode(opusData);
        decoder.close();
        
        int senderCanal = getCanal(senderItemStack);
        
        Set<ServerPlayerEntity> validReceivers = new HashSet<>();
        for (PlayerEntity p : Objects.requireNonNull(senderPlayer.getServer()).getPlayerManager().getPlayerList()) {
            if (p instanceof ServerPlayerEntity receiverPlayer && !receiverPlayer.getUuid().equals(senderPlayer.getUuid())) {
                 if (!ModConfig.crossDimensionsEnabled && !receiverPlayer.getWorld().getDimension().equals(senderPlayer.getWorld().getDimension())) continue;
                 ItemStack receiverStack = Util.getWalkieTalkieActivated(receiverPlayer);
                 if (receiverStack == null) continue;
                 if (!canBroadcastToReceiver(senderPlayer, receiverPlayer, getRange(receiverStack)) || getCanal(receiverStack) != senderCanal) continue;
                 validReceivers.add(receiverPlayer);
            }
        }
        
        handleTransmissionStart(senderPlayer, validReceivers);
        
        AudioProcessingState senderState = transmissionStates.get(senderPlayer.getUuid());
        if (senderState == null) {
            senderState = new AudioProcessingState();
            transmissionStates.put(senderPlayer.getUuid(), senderState);
        }

        short[] speakerFinalAudio = applyFullRadioEffect(rawAudio, 0.85f, senderState);
        SpeakerBlockEntity.getSpeakersActivatedInRange(senderCanal, senderPlayer.getWorld(), senderPlayer.getPos(), getRange(senderItemStack))
                .forEach(speakerBlockEntity -> speakerBlockEntity.playSound(api, speakerFinalAudio, senderPlayer));

        for (ServerPlayerEntity receiverPlayer : validReceivers) {
            double distance = senderPlayer.getPos().distanceTo(receiverPlayer.getPos());
            float signalQuality;

            if (distance <= 100D) { signalQuality = 0.9f + random.nextFloat() * 0.05f; }
            else if (distance <= 250D) { signalQuality = 0.7f + random.nextFloat() * 0.1f; }
            else if (distance <= 500D) { signalQuality = 0.45f + random.nextFloat() * 0.1f; }
            else { signalQuality = 0.2f + random.nextFloat() * 0.1f; }
            
            short[] playerFinalAudio = applyFullRadioEffect(rawAudio, signalQuality, senderState);
            
            Position receiverPosition = api.createPosition(receiverPlayer.getX(), receiverPlayer.getY(), receiverPlayer.getZ());
            World receiverWorld = receiverPlayer.getWorld();
            LocationalAudioChannel channel = api.createLocationalAudioChannel(UUID.randomUUID(), api.fromServerLevel(receiverWorld), receiverPosition);

            if (channel != null) {
                channel.setFilter(player -> player.getUuid().equals(receiverPlayer.getUuid()));
                AudioPlayer player = api.createAudioPlayer(channel, api.createEncoder(), playerFinalAudio);
                player.startPlaying();
            }
        }
    }

    private int getCanal(ItemStack stack) { return Objects.requireNonNull(stack.getNbt()).getInt(WalkieTalkieItem.NBT_KEY_CANAL); }
    private int getRange(ItemStack stack) { WalkieTalkieItem item = (WalkieTalkieItem) Objects.requireNonNull(stack.getItem()); return item.getRange(); }
    private boolean isWalkieTalkieActivate(ItemStack stack) { return Objects.requireNonNull(stack.getNbt()).getBoolean(WalkieTalkieItem.NBT_KEY_ACTIVATE); }
    private boolean isWalkieTalkieMute(ItemStack stack) { return Objects.requireNonNull(stack.getNbt()).getBoolean(WalkieTalkieItem.NBT_KEY_MUTE); }
    private boolean canBroadcastToReceiver(PlayerEntity senderPlayer, PlayerEntity receiverPlayer, int receiverRange) { World senderWorld = senderPlayer.getWorld(); World receiverWorld = receiverPlayer.getWorld(); return Util.canBroadcastToReceiver(senderWorld, receiverWorld, senderPlayer.getPos(), receiverPlayer.getPos(), receiverRange); }
}