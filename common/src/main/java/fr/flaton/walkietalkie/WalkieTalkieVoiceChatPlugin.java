// Файл: WalkieTalkieVoiceChatPlugin.java (с шумом от расстояния)

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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Enumeration;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

@ForgeVoicechatPlugin
public class WalkieTalkieVoiceChatPlugin implements VoicechatPlugin {

    public final static String SPEAKER_CATEGORY = "speakers";
    private static final Random random = new Random();

    @Nullable
    public static VoicechatServerApi api;

    // --- Все методы до onMicPacket остаются без изменений ---
    @Override
    public String getPluginId() { return Constants.MOD_ID; }
    @Override
    public void registerEvents(EventRegistration registration) { /* ... */ }
    private void onServerStarted(VoicechatServerStartedEvent event) { /* ... */ }
    @Nullable
    private int[][] getIcon(String path) { /* ... */ return null; }
    private short[] addWhiteNoise(short[] rawAudio, float intensity) { /* ... */ return rawAudio; }


    private void onMicPacket(MicrophonePacketEvent event) {
        if (api == null || event.getSenderConnection() == null) {
            return;
        }

        if (!(event.getSenderConnection().getPlayer().getPlayer() instanceof ServerPlayerEntity senderPlayer)) {
            return;
        }

        ItemStack senderItemStack = Util.getWalkieTalkieInHand(senderPlayer);

        if (senderItemStack == null || !isWalkieTalkieActivate(senderItemStack) || isWalkieTalkieMute(senderItemStack)) {
            return;
        }

        event.cancel();

        byte[] opusData = event.getPacket().getOpusEncodedData();
        if (opusData.length == 0) {
            return;
        }

        OpusDecoder decoder = api.createDecoder();
        short[] rawAudio = decoder.decode(opusData);
        decoder.close();
        
        int senderCanal = getCanal(senderItemStack);

        // --- Обработка для стационарных динамиков (Speakers) ---
        // У них будет фиксированный средний уровень шума
        float speakerNoiseIntensity = 0.005f; 
        short[] speakerNoisyAudio = addWhiteNoise(rawAudio, speakerNoiseIntensity);

        SpeakerBlockEntity.getSpeakersActivatedInRange(senderCanal, senderPlayer.getWorld(), senderPlayer.getPos(), getRange(senderItemStack))
                .forEach(speakerBlockEntity -> speakerBlockEntity.playSound(api, speakerNoisyAudio, senderPlayer));

        // --- Обработка для игроков (с расчетом расстояния) ---
        for (PlayerEntity receiverPlayerEntity : Objects.requireNonNull(senderPlayer.getServer()).getPlayerManager().getPlayerList()) {
            if (!(receiverPlayerEntity instanceof ServerPlayerEntity receiverPlayer) || receiverPlayer.getUuid().equals(senderPlayer.getUuid())) {
                continue;
            }
            if (!ModConfig.crossDimensionsEnabled && !receiverPlayer.getWorld().getDimension().equals(senderPlayer.getWorld().getDimension())) {
                continue;
            }
            ItemStack receiverStack = Util.getWalkieTalkieActivated(receiverPlayer);
            if (receiverStack == null) {
                continue;
            }
            if (!canBroadcastToReceiver(senderPlayer, receiverPlayer, getRange(receiverStack)) || getCanal(receiverStack) != senderCanal) {
                continue;
            }
            
            // --- НОВАЯ ЛОГИКА РАСЧЕТА ШУМА ОТ РАССТОЯНИЯ ---
            
            // 1. Рассчитываем дистанцию
            double distance = senderPlayer.getPos().distanceTo(receiverPlayer.getPos());
            
            // 2. Задаем параметры шума (можешь их менять по вкусу)
            float minNoise = 0.002f;         // Минимальный шум (близко)
            float maxNoise = 0.01f;          // Максимальный шум (далеко)
            float startScalingDistance = 100f; // Дистанция, с которой шум начинает расти
            float maxScalingDistance = 500f;   // Дистанция, на которой шум достигает максимума
            
            float noiseIntensity;
            
            // 3. Определяем интенсивность шума
            if (distance <= startScalingDistance) {
                noiseIntensity = minNoise;
            } else if (distance >= maxScalingDistance) {
                noiseIntensity = maxNoise;
            } else {
                // Плавный рост шума между start и max дистанцией
                float progress = (float) ((distance - startScalingDistance) / (maxScalingDistance - startScalingDistance));
                noiseIntensity = minNoise + (maxNoise - minNoise) * progress;
            }
            
            // 4. Генерируем уникальный зашумленный звук для этого получателя
            short[] noisyRawAudioForPlayer = addWhiteNoise(rawAudio, noiseIntensity);

            // 5. Отправляем звук игроку
            Position receiverPosition = api.createPosition(receiverPlayer.getX(), receiverPlayer.getY(), receiverPlayer.getZ());
            World receiverWorld = receiverPlayer.getWorld();
            
            LocationalAudioChannel channel = api.createLocationalAudioChannel(UUID.randomUUID(), api.fromServerLevel(receiverWorld), receiverPosition);

            if (channel != null) {
                channel.setFilter(player -> player.getUuid().equals(receiverPlayer.getUuid()));
                AudioPlayer player = api.createAudioPlayer(channel, api.createEncoder(), noisyRawAudioForPlayer);
                player.startPlaying();
            }
        }
    }

    // --- Остальные утилитные методы без изменений ---
    private int getCanal(ItemStack stack) { return Objects.requireNonNull(stack.getNbt()).getInt(WalkieTalkieItem.NBT_KEY_CANAL); }
    private int getRange(ItemStack stack) { WalkieTalkieItem item = (WalkieTalkieItem) Objects.requireNonNull(stack.getItem()); return item.getRange(); }
    private boolean isWalkieTalkieActivate(ItemStack stack) { return Objects.requireNonNull(stack.getNbt()).getBoolean(WalkieTalkieItem.NBT_KEY_ACTIVATE); }
    private boolean isWalkieTalkieMute(ItemStack stack) { return Objects.requireNonNull(stack.getNbt()).getBoolean(WalkieTalkieItem.NBT_KEY_MUTE); }
    private boolean canBroadcastToReceiver(PlayerEntity senderPlayer, PlayerEntity receiverPlayer, int receiverRange) { World senderWorld = senderPlayer.getWorld(); World receiverWorld = receiverPlayer.getWorld(); return Util.canBroadcastToReceiver(senderWorld, receiverWorld, senderPlayer.getPos(), receiverPlayer.getPos(), receiverRange); }
}