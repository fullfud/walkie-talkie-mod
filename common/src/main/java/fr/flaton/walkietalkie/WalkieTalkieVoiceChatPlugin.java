// Файл: WalkieTalkieVoiceChatPlugin.java (ПОЛНАЯ ВЕРСИЯ С ЗВУКАМИ НАЧАЛА/КОНЦА)

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
import fr.flaton.walkietalkie.sound.ModSounds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
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
    
    // Частота дискретизации VoiceChat
    private static final int SAMPLE_RATE = 48000;
    
    // Переменные для фильтров (для каждого потока аудио)
    private float[] highpassHistory = new float[2];
    private float[] lowpassHistory = new float[2];
    private float[] noiseFilterHistory = new float[2];
    
    // Отслеживание активных передач для звуков начала/конца
    private final Map<UUID, Long> activeTransmissions = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> transmissionReceivers = new ConcurrentHashMap<>();
    private static final long TRANSMISSION_TIMEOUT = 500; // 500мс тишины = конец передачи

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
        
        // Запускаем поток для проверки таймаутов передач
        startTransmissionTimeoutChecker();
    }

    // Поток для проверки окончания передач
    private void startTransmissionTimeoutChecker() {
        Thread timeoutChecker = new Thread(() -> {
            while (api != null) {
                try {
                    Thread.sleep(100); // Проверяем каждые 100мс
                    long currentTime = System.currentTimeMillis();
                    
                    activeTransmissions.entrySet().removeIf(entry -> {
                        if (currentTime - entry.getValue() > TRANSMISSION_TIMEOUT) {
                            // Передача закончилась, воспроизводим звук окончания
                            handleTransmissionEnd(entry.getKey());
                            return true;
                        }
                        return false;
                    });
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        timeoutChecker.setDaemon(true);
        timeoutChecker.start();
    }

    // Обработка начала передачи
    private void handleTransmissionStart(ServerPlayerEntity sender, Set<ServerPlayerEntity> receivers) {
        UUID senderId = sender.getUuid();
        
        if (!activeTransmissions.containsKey(senderId)) {
            // Новая передача - воспроизводим звук начала
            
            // Звук для отправителя
            sender.getWorld().playSound(null, sender.getX(), sender.getY(), sender.getZ(),
                    new Identifier(Constants.MOD_ID, "walkietalkie_on"),
                    SoundCategory.PLAYERS, 0.5f, 1.0f);
            
            // Звук для получателей
            Set<UUID> receiverIds = new HashSet<>();
            for (ServerPlayerEntity receiver : receivers) {
                receiverIds.add(receiver.getUuid());
                receiver.getWorld().playSound(null, receiver.getX(), receiver.getY(), receiver.getZ(),
                        new Identifier(Constants.MOD_ID, "walkietalkie_on"),
                        SoundCategory.PLAYERS, 0.5f, 1.0f);
            }
            
            transmissionReceivers.put(senderId, receiverIds);
        }
        
        activeTransmissions.put(senderId, System.currentTimeMillis());
    }

    // Обработка конца передачи
    private void handleTransmissionEnd(UUID senderId) {
        // Находим всех, кто участвовал в передаче
        Set<UUID> receiverIds = transmissionReceivers.remove(senderId);
        
        if (receiverIds != null && api != null) {
            // Воспроизводим звук окончания для всех участников
            for (UUID playerId : receiverIds) {
                PlayerEntity player = getPlayerByUuid(playerId);
                if (player != null) {
                    player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
                            new Identifier(Constants.MOD_ID, "walkietalkie_off"),
                            SoundCategory.PLAYERS, 0.5f, 1.0f);
                }
            }
            
            // Звук для отправителя
            PlayerEntity sender = getPlayerByUuid(senderId);
            if (sender != null) {
                sender.getWorld().playSound(null, sender.getX(), sender.getY(), sender.getZ(),
                        new Identifier(Constants.MOD_ID, "walkietalkie_off"),
                        SoundCategory.PLAYERS, 0.5f, 1.0f);
            }
        }
    }

    // Вспомогательный метод для получения игрока по UUID
    private PlayerEntity getPlayerByUuid(UUID uuid) {
        if (api == null) return null;
        for (VoicechatConnection connection : api.getConnectionManager().getConnections()) {
            if (connection.getPlayer().getUuid().equals(uuid)) {
                return connection.getPlayer().getPlayer();
            }
        }
        return null;
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

    // Применяет полосовой фильтр для имитации узкой полосы частот рации
    private short[] applyBandpassFilter(short[] input) {
        short[] output = new short[input.length];
        
        // Коэффициенты фильтров
        float highpassCutoff = 300.0f / SAMPLE_RATE;
        float lowpassCutoff = 3400.0f / SAMPLE_RATE;
        
        float highpassRC = (float)(1.0 / (2.0 * Math.PI * highpassCutoff));
        float lowpassRC = (float)(1.0 / (2.0 * Math.PI * lowpassCutoff));
        
        float highpassAlpha = highpassRC / (highpassRC + 1);
        float lowpassAlpha = 1 / (lowpassRC + 1);
        
        for (int i = 0; i < input.length; i++) {
            // Highpass filter (убирает низкие частоты)
            float currentSample = input[i] / 32768.0f;
            highpassHistory[0] = highpassAlpha * (highpassHistory[0] + currentSample - highpassHistory[1]);
            highpassHistory[1] = currentSample;
            
            // Lowpass filter (убирает высокие частоты)
            lowpassHistory[0] = lowpassHistory[0] + lowpassAlpha * (highpassHistory[0] - lowpassHistory[0]);
            
            output[i] = (short)(lowpassHistory[0] * 32768.0f);
        }
        
        return output;
    }

    // Применяет компрессию как в реальных рациях
    private short[] applyCompression(short[] input) {
        short[] output = new short[input.length];
        
        float threshold = 0.4f;
        float ratio = 4.0f; // 4:1 компрессия
        float attack = 0.001f;
        float release = 0.05f;
        float makeupGain = 2.2f;
        
        float envelope = 0.0f;
        
        for (int i = 0; i < input.length; i++) {
            float sample = input[i] / 32768.0f;
            float absSample = Math.abs(sample);
            
            // Обновляем огибающую
            float rate = absSample > envelope ? attack : release;
            envelope = envelope + rate * (absSample - envelope);
            
            // Применяем компрессию
            float gain = 1.0f;
            if (envelope > threshold) {
                float excess = envelope - threshold;
                float compressedExcess = excess / ratio;
                gain = (threshold + compressedExcess) / envelope;
            }
            
            float compressedSample = sample * gain * makeupGain;
            
            // Мягкое ограничение
            compressedSample = (float)Math.tanh(compressedSample * 0.7) * 1.4f;
            
            output[i] = (short)(compressedSample * 32768.0f * 0.9f);
        }
        
        return output;
    }

    // Генерирует реалистичный радио-шум
    private short[] generateRadioNoise(int length, float intensity) {
        short[] noise = new short[length];
        
        float pink = 0;
        float brown = 0;
        
        for (int i = 0; i < length; i++) {
            // Белый шум
            float white = (random.nextFloat() * 2f - 1f);
            
            // Розовый шум (1/f шум)
            pink = 0.99f * pink + 0.01f * white;
            
            // Коричневый шум (1/f² шум)  
            brown = 0.998f * brown + 0.002f * white;
            
            // Смешиваем разные типы шума
            float noiseSample = (white * 0.2f + pink * 0.5f + brown * 0.3f);
            
            // Добавляем редкие потрескивания
            if (random.nextFloat() < 0.00005f) {
                noiseSample += (random.nextFloat() - 0.5f) * 5.0f;
            }
            
            // Добавляем слабые периодические помехи (60Hz hum)
            float hum = (float)Math.sin(2 * Math.PI * 60 * i / SAMPLE_RATE) * 0.03f;
            noiseSample += hum;
            
            // Фильтруем шум через lowpass для более реалистичного звука
            noiseFilterHistory[0] = noiseFilterHistory[0] * 0.9f + noiseSample * 0.1f;
            
            noise[i] = (short)(noiseFilterHistory[0] * intensity * 3000);
        }
        
        return noise;
    }

    // Добавляет искажения характерные для радио
    private short[] addRadioDistortion(short[] input, float amount) {
        short[] output = new short[input.length];
        
        for (int i = 0; i < input.length; i++) {
            float sample = input[i] / 32768.0f;
            
            // Tube-like saturation
            float drive = 1.0f + amount * 2.0f;
            float distorted = (float)Math.tanh(sample * drive) / drive;
            
            // Добавляем немного четных гармоник
            distorted = distorted + (distorted * distorted * 0.05f * amount);
            
            // Легкая асимметрия (как в реальных схемах)
            if (distorted > 0) {
                distorted = distorted * (1.0f + amount * 0.1f);
            }
            
            output[i] = (short)(distorted * 32768.0f * 0.95f);
        }
        
        return output;
    }

    // Основной метод применения всех радио эффектов
    private short[] applyFullRadioEffect(short[] rawAudio, float signalQuality) {
        // Сбрасываем историю фильтров для нового аудио
        highpassHistory = new float[2];
        lowpassHistory = new float[2];
        noiseFilterHistory = new float[2];
        
        // 1. Применяем полосовой фильтр
        short[] filtered = applyBandpassFilter(rawAudio);
        
        // 2. Применяем компрессию
        short[] compressed = applyCompression(filtered);
        
        // 3. Добавляем искажения в зависимости от качества сигнала
        float distortionAmount = (1.0f - signalQuality) * 0.3f;
        short[] distorted = addRadioDistortion(compressed, distortionAmount);
        
        // 4. Генерируем радио-шум
        short[] noise = generateRadioNoise(distorted.length, 1.0f);
        
        // 5. Смешиваем сигнал и шум
        short[] output = new short[distorted.length];
        float signalLevel = 0.6f + signalQuality * 0.4f;
        float noiseLevel = (1.0f - signalQuality) * 0.4f;
        
        for (int i = 0; i < output.length; i++) {
            float mixed = distorted[i] * signalLevel + noise[i] * noiseLevel;
            output[i] = (short)Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, mixed));
        }
        
        return output;
    }

    private void onMicPacket(MicrophonePacketEvent event) {
        if (api == null || event.getSenderConnection() == null) return;
        if (!(event.getSenderConnection().getPlayer().getPlayer() instanceof ServerPlayerEntity senderPlayer)) return;

        ItemStack senderItemStack = Util.getWalkieTalkieInHand(senderPlayer);
        if (senderItemStack == null || !isWalkieTalkieActivate(senderItemStack) || isWalkieTalkieMute(senderItemStack)) return;

        event.cancel();

        byte[] opusData = event.getPacket().getOpusEncodedData();
        if (opusData.length == 0) return;

        OpusDecoder decoder = api.createDecoder();
        short[] rawAudio = decoder.decode(opusData);
        decoder.close();

        int senderCanal = getCanal(senderItemStack);
        
        // Собираем всех получателей для обработки звуков начала/конца
        Set<ServerPlayerEntity> validReceivers = new HashSet<>();

        // Обработка для динамиков - высокое качество сигнала
        short[] speakerFinalAudio = applyFullRadioEffect(rawAudio, 0.85f);
        SpeakerBlockEntity.getSpeakersActivatedInRange(senderCanal, senderPlayer.getWorld(), senderPlayer.getPos(), getRange(senderItemStack))
                .forEach(speakerBlockEntity -> speakerBlockEntity.playSound(api, speakerFinalAudio, senderPlayer));

        // Обработка для игроков с рациями
        for (PlayerEntity receiverPlayerEntity : Objects.requireNonNull(senderPlayer.getServer()).getPlayerManager().getPlayerList()) {
            if (!(receiverPlayerEntity instanceof ServerPlayerEntity receiverPlayer) || receiverPlayer.getUuid().equals(senderPlayer.getUuid())) continue;
            if (!ModConfig.crossDimensionsEnabled && !receiverPlayer.getWorld().getDimension().equals(senderPlayer.getWorld().getDimension())) continue;
            ItemStack receiverStack = Util.getWalkieTalkieActivated(receiverPlayer);
            if (receiverStack == null) continue;
            if (!canBroadcastToReceiver(senderPlayer, receiverPlayer, getRange(receiverStack)) || getCanal(receiverStack) != senderCanal) continue;

            validReceivers.add(receiverPlayer);
            
            double distance = senderPlayer.getPos().distanceTo(receiverPlayer.getPos());
            float signalQuality;

            // Определяем качество сигнала по расстоянию
            if (distance <= 100D) { 
                signalQuality = 0.9f + random.nextFloat() * 0.05f; // 90-95%
            } else if (distance <= 250D) { 
                signalQuality = 0.7f + random.nextFloat() * 0.1f; // 70-80%
            } else if (distance <= 500D) { 
                signalQuality = 0.45f + random.nextFloat() * 0.1f; // 45-55%
            } else { 
                signalQuality = 0.2f + random.nextFloat() * 0.1f; // 20-30%
            }
            
            // Применяем все радио эффекты с учетом качества сигнала
            short[] playerFinalAudio = applyFullRadioEffect(rawAudio, signalQuality);
            
            Position receiverPosition = api.createPosition(receiverPlayer.getX(), receiverPlayer.getY(), receiverPlayer.getZ());
            World receiverWorld = receiverPlayer.getWorld();
            LocationalAudioChannel channel = api.createLocationalAudioChannel(UUID.randomUUID(), api.fromServerLevel(receiverWorld), receiverPosition);

            if (channel != null) {
                channel.setFilter(player -> player.getUuid().equals(receiverPlayer.getUuid()));
                AudioPlayer player = api.createAudioPlayer(channel, api.createEncoder(), playerFinalAudio);
                player.startPlaying();
            }
        }
        
        // Обрабатываем звуки начала передачи
        handleTransmissionStart(senderPlayer, validReceivers);
    }

    private int getCanal(ItemStack stack) { return Objects.requireNonNull(stack.getNbt()).getInt(WalkieTalkieItem.NBT_KEY_CANAL); }
    private int getRange(ItemStack stack) { WalkieTalkieItem item = (WalkieTalkieItem) Objects.requireNonNull(stack.getItem()); return item.getRange(); }
    private boolean isWalkieTalkieActivate(ItemStack stack) { return Objects.requireNonNull(stack.getNbt()).getBoolean(WalkieTalkieItem.NBT_KEY_ACTIVATE); }
    private boolean isWalkieTalkieMute(ItemStack stack) { return Objects.requireNonNull(stack.getNbt()).getBoolean(WalkieTalkieItem.NBT_KEY_MUTE); }
    private boolean canBroadcastToReceiver(PlayerEntity senderPlayer, PlayerEntity receiverPlayer, int receiverRange) { World senderWorld = senderPlayer.getWorld(); World receiverWorld = receiverPlayer.getWorld(); return Util.canBroadcastToReceiver(senderWorld, receiverWorld, senderPlayer.getPos(), receiverPlayer.getPos(), receiverRange); }
}