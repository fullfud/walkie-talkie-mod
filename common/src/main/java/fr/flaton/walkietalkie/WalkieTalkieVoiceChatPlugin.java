// Файл: WalkieTalkieVoiceChatPlugin.java (с симуляцией радио-эффекта)

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

    // --- Методы до обработки звука остаются без изменений ---
    @Override public String getPluginId() { return Constants.MOD_ID; }
    @Override public void registerEvents(EventRegistration registration) { /* ... */ }
    private void onServerStarted(VoicechatServerStartedEvent event) { /* ... */ }
    @Nullable private int[][] getIcon(String path) { /* ... */ return null; }

    // --- НОВЫЕ МЕТОДЫ ОБРАБОТКИ ЗВУКА ---

    /**
     * Применяет к звуку эффект "дешевого динамика рации".
     * Он делает звук более плоским и добавляет легкие искажения.
     * @param rawAudio Исходный сырой звук
     * @return Обработанный звук
     */
    private short[] applyRadioEffect(short[] rawAudio) {
        // --- Поиграйся с этими значениями для настройки "жестяного" звука ---
        final float VOLUME_MULTIPLIER = 0.8f; // Уменьшаем общую громкость голоса
        final short CLIPPING_THRESHOLD = 20000; // Порог, выше которого звук "хрипит"
        // ----------------------------------------------------------------

        short[] filteredAudio = new short[rawAudio.length];
        for (int i = 0; i < rawAudio.length; i++) {
            // Уменьшаем громкость и применяем клиппинг (искажение)
            int sample = (int) (rawAudio[i] * VOLUME_MULTIPLIER);
            sample = Math.max(-CLIPPING_THRESHOLD, Math.min(CLIPPING_THRESHOLD, sample));
            filteredAudio[i] = (short) sample;
        }
        return filteredAudio;
    }

    /**
     * Смешивает обработанный голос с белым шумом.
     * @param voiceAudio Голос, уже обработанный радио-фильтром
     * @param mixFactor Насколько сильно подмешивать шум (0.0 - нет шума, 1.0 - только шум)
     * @return Финальный звук
     */
    private short[] addWhiteNoise(short[] voiceAudio, float mixFactor) {
        if (voiceAudio.length == 0 || mixFactor <= 0f) {
            return voiceAudio;
        }
        
        // --- Поиграйся с этим значением для настройки громкости самого ШУМА ---
        final float NOISE_AMPLITUDE = 6000f; // Максимальная громкость белого шума
        // ---------------------------------------------------------------------

        float clampedMix = Math.max(0f, Math.min(1f, mixFactor));
        short[] outputAudio = new short[voiceAudio.length];

        for (int i = 0; i < voiceAudio.length; i++) {
            float voiceSample = voiceAudio[i];
            float noiseSample = (random.nextFloat() * 2f - 1f) * NOISE_AMPLITUDE;
            float mixedSample = (voiceSample * (1f - clampedMix)) + (noiseSample * clampedMix);
            outputAudio[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, mixedSample));
        }
        return outputAudio;
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

        // --- НОВЫЙ ПОРЯДОК ОБРАБОТКИ ---
        // 1. Сначала применяем к голосу эффект рации
        short[] filteredAudio = applyRadioEffect(rawAudio);
        
        int senderCanal = getCanal(senderItemStack);

        // 2. Обрабатываем звук для динамиков
        float speakerNoiseIntensity = 0.08f; // 8% шума
        short[] speakerFinalAudio = addWhiteNoise(filteredAudio, speakerNoiseIntensity);
        SpeakerBlockEntity.getSpeakersActivatedInRange(senderCanal, senderPlayer.getWorld(), senderPlayer.getPos(), getRange(senderItemStack))
                .forEach(speakerBlockEntity -> speakerBlockEntity.playSound(api, speakerFinalAudio, senderPlayer));

        // 3. Обрабатываем звук для каждого игрока индивидуально
        for (PlayerEntity receiverPlayerEntity : Objects.requireNonNull(senderPlayer.getServer()).getPlayerManager().getPlayerList()) {
            if (!(receiverPlayerEntity instanceof ServerPlayerEntity receiverPlayer) || receiverPlayer.getUuid().equals(senderPlayer.getUuid())) continue;
            // ... (все твои проверки)
            if (!ModConfig.crossDimensionsEnabled && !receiverPlayer.getWorld().getDimension().equals(senderPlayer.getWorld().getDimension())) continue;
            ItemStack receiverStack = Util.getWalkieTalkieActivated(receiverPlayer);
            if (receiverStack == null) continue;
            if (!canBroadcastToReceiver(senderPlayer, receiverPlayer, getRange(receiverStack)) || getCanal(receiverStack) != senderCanal) continue;

            double distance = senderPlayer.getPos().distanceTo(receiverPlayer.getPos());
            float noiseIntensity; // Это теперь фактор СМЕШИВАНИЯ

            // Ступенчатая логика остается, но теперь она управляет процентом смешивания
            if (distance <= 100D) { noiseIntensity = 0.05f; } // 5% шума
            else if (distance <= 250D) { noiseIntensity = 0.12f; } // 12% шума
            else if (distance <= 500D) { noiseIntensity = 0.20f; } // 20% шума
            else { noiseIntensity = 0.30f; } // 30% шума
            
            // Генерируем финальный звук для этого игрока
            short[] playerFinalAudio = addWhiteNoise(filteredAudio, noiseIntensity);
            
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

    // --- Остальные утилитные методы без изменений ---
    private int getCanal(ItemStack stack) { return Objects.requireNonNull(stack.getNbt()).getInt(WalkieTalkieItem.NBT_KEY_CANAL); }
    private int getRange(ItemStack stack) { WalkieTalkieItem item = (WalkieTalkieItem) Objects.requireNonNull(stack.getItem()); return item.getRange(); }
    private boolean isWalkieTalkieActivate(ItemStack stack) { return Objects.requireNonNull(stack.getNbt()).getBoolean(WalkieTalkieItem.NBT_KEY_ACTIVATE); }
    private boolean isWalkieTalkieMute(ItemStack stack) { return Objects.requireNonNull(stack.getNbt()).getBoolean(WalkieTalkieItem.NBT_KEY_MUTE); }
    private boolean canBroadcastToReceiver(PlayerEntity senderPlayer, PlayerEntity receiverPlayer, int receiverRange) { World senderWorld = senderPlayer.getWorld(); World receiverWorld = receiverPlayer.getWorld(); return Util.canBroadcastToReceiver(senderWorld, receiverWorld, senderPlayer.getPos(), receiverPlayer.getPos(), receiverRange); }
}