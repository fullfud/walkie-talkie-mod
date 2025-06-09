// Файл: WalkieTalkieVoiceChatPlugin.java (Полный код со ступенчатым шумом)

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

    private short[] addWhiteNoise(short[] rawAudio, float mixFactor) {
        if (rawAudio.length == 0 || mixFactor <= 0f) {
            return rawAudio;
        }

        float clampedMix = Math.max(0f, Math.min(1f, mixFactor));
        short[] outputAudio = new short[rawAudio.length];

        for (int i = 0; i < rawAudio.length; i++) {
            float voiceSample = rawAudio[i];
            float noiseSample = (random.nextFloat() * 2f - 1f) * Short.MAX_VALUE;
            float mixedSample = (voiceSample * (1f - clampedMix)) + (noiseSample * clampedMix);
            outputAudio[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, mixedSample));
        }
        return outputAudio;
    }

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
        float speakerNoiseIntensity = 0.008f;
        short[] speakerNoisyAudio = addWhiteNoise(rawAudio, speakerNoiseIntensity);

        SpeakerBlockEntity.getSpeakersActivatedInRange(senderCanal, senderPlayer.getWorld(), senderPlayer.getPos(), getRange(senderItemStack))
                .forEach(speakerBlockEntity -> speakerBlockEntity.playSound(api, speakerNoisyAudio, senderPlayer));

        // --- Обработка для игроков (с расчетом расстояния) ---
        for (PlayerEntity receiverPlayerEntity : Objects.requireNonNull(senderPlayer.getServer()).getPlayerManager().getPlayerList()) {
            if (!(receiverPlayerEntity instanceof ServerPlayerEntity receiverPlayer) || receiverPlayer.getUuid().equals(senderPlayer.getUuid())) {
                continue;
            }
            if (!ModConfig.crossDimensionsEnabled && !receiverPlayer.getWorld().getDimension().equals(senderPlayer.getWorld().getDimension())) continue;
            ItemStack receiverStack = Util.getWalkieTalkieActivated(receiverPlayer);
            if (receiverStack == null) continue;
            if (!canBroadcastToReceiver(senderPlayer, receiverPlayer, getRange(receiverStack)) || getCanal(receiverStack) != senderCanal) continue;

            // --- НОВАЯ СТУПЕНЧАТАЯ ЛОГИКА ШУМА ---
            
            double distance = senderPlayer.getPos().distanceTo(receiverPlayer.getPos());
            float noiseIntensity;

            // Уровень 1: Чистый сигнал (до 100 блоков)
            if (distance <= 100D) {
                noiseIntensity = 0.002f; // 0.2%
            } 
            // Уровень 2: Легкие помехи (100-250 блоков)
            else if (distance <= 250D) {
                noiseIntensity = 0.008f; // 0.8%
            } 
            // Уровень 3: Сильные помехи (250-500 блоков)
            else if (distance <= 500D) {
                noiseIntensity = 0.015f; // 1.5%
            }
            // Уровень 4: Предел связи (дальше 500 блоков)
            else {
                noiseIntensity = 0.025f; // 2.5%
            }
            
            short[] noisyRawAudioForPlayer = addWhiteNoise(rawAudio, noiseIntensity);

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

    private int getCanal(ItemStack stack) { return Objects.requireNonNull(stack.getNbt()).getInt(WalkieTalkieItem.NBT_KEY_CANAL); }
    private int getRange(ItemStack stack) { WalkieTalkieItem item = (WalkieTalkieItem) Objects.requireNonNull(stack.getItem()); return item.getRange(); }
    private boolean isWalkieTalkieActivate(ItemStack stack) { return Objects.requireNonNull(stack.getNbt()).getBoolean(WalkieTalkieItem.NBT_KEY_ACTIVATE); }
    private boolean isWalkieTalkieMute(ItemStack stack) { return Objects.requireNonNull(stack.getNbt()).getBoolean(WalkieTalkieItem.NBT_KEY_MUTE); }
    private boolean canBroadcastToReceiver(PlayerEntity senderPlayer, PlayerEntity receiverPlayer, int receiverRange) { World senderWorld = senderPlayer.getWorld(); World receiverWorld = receiverPlayer.getWorld(); return Util.canBroadcastToReceiver(senderWorld, receiverWorld, senderPlayer.getPos(), receiverPlayer.getPos(), receiverRange); }
}