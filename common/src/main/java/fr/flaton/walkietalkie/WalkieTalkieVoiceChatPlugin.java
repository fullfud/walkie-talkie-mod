// Файл: WalkieTalkieVoiceChatPlugin.java (Полная версия)

package fr.flaton.walkietalkie;

import de.maxhenkel.voicechat.api.*;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;
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

    private byte[] addWhiteNoise(byte[] opusData, float intensity) {
        if (opusData.length == 0) {
            return opusData;
        }
        byte[] noisyData = new byte[opusData.length];
        System.arraycopy(opusData, 0, noisyData, 0, opusData.length);
        int noiseAmount = (int) (opusData.length * intensity);
        for (int i = 0; i < noiseAmount; i++) {
            int randomIndex = random.nextInt(opusData.length);
            noisyData[randomIndex] = (byte) (random.nextInt(256) - 128);
        }
        return noisyData;
    }

    private void onMicPacket(MicrophonePacketEvent event) {
        if (event.getSenderConnection() == null) {
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
        
        MicrophonePacket packet = event.getPacket();
        byte[] originalOpusData = packet.getOpusEncodedData();

        if (originalOpusData.length == 0) {
            return;
        }
        
        float noiseIntensity = 0.15f; // Можешь изменить это значение
        byte[] noisyOpusData = addWhiteNoise(originalOpusData, noiseIntensity);

        // Создаем новый пакет с зашумленными данными
        StaticSoundPacket noisyPacket = api.createStaticSoundPacketBuilder()
                .setOpusEncodedData(noisyOpusData)
                .setCategory(SPEAKER_CATEGORY)
                .build();
        
        int senderCanal = getCanal(senderItemStack);

        // Отправляем на динамики
        SpeakerBlockEntity.getSpeakersActivatedInRange(senderCanal, senderPlayer.getWorld(), senderPlayer.getPos(), getRange(senderItemStack))
                .forEach(speakerBlockEntity -> speakerBlockEntity.playSound(api, noisyPacket, senderPlayer));

        // Отправляем другим игрокам
        for (PlayerEntity receiverPlayerEntity : Objects.requireNonNull(senderPlayer.getServer()).getPlayerManager().getPlayerList()) {
            if (!(receiverPlayerEntity instanceof ServerPlayerEntity receiverPlayer)) {
                continue;
            }

            if (receiverPlayer.getUuid().equals(senderPlayer.getUuid())) {
                continue;
            }
            
            if (!ModConfig.crossDimensionsEnabled && !receiverPlayer.getWorld().getDimension().equals(senderPlayer.getWorld().getDimension())) {
                continue;
            }
            ItemStack receiverStack = Util.getWalkieTalkieActivated(receiverPlayer);
            if (receiverStack == null) {
                continue;
            }
            int receiverRange = getRange(receiverStack);
            int receiverCanal = getCanal(receiverStack);
            if (!canBroadcastToReceiver(senderPlayer, receiverPlayer, receiverRange) || receiverCanal != senderCanal) {
                continue;
            }

            VoicechatConnection connection = api.getConnectionOf(receiverPlayer.getUuid());
            if (connection != null) {
                api.sendStaticSoundPacketTo(connection, noisyPacket);
            }
        }
    }
    
    private int getCanal(ItemStack stack) { 
        return Objects.requireNonNull(stack.getNbt()).getInt(WalkieTalkieItem.NBT_KEY_CANAL); 
    }

    private int getRange(ItemStack stack) { 
        WalkieTalkieItem item = (WalkieTalkieItem) Objects.requireNonNull(stack.getItem()); 
        return item.getRange(); 
    }

    private boolean isWalkieTalkieActivate(ItemStack stack) { 
        return Objects.requireNonNull(stack.getNbt()).getBoolean(WalkieTalkieItem.NBT_KEY_ACTIVATE); 
    }

    private boolean isWalkieTalkieMute(ItemStack stack) { 
        return Objects.requireNonNull(stack.getNbt()).getBoolean(WalkieTalkieItem.NBT_KEY_MUTE); 
    }

    private boolean canBroadcastToReceiver(PlayerEntity senderPlayer, PlayerEntity receiverPlayer, int receiverRange) { 
        World senderWorld = senderPlayer.getWorld(); 
        World receiverWorld = receiverPlayer.getWorld(); 
        return Util.canBroadcastToReceiver(senderWorld, receiverWorld, senderPlayer.getPos(), receiverPlayer.getPos(), receiverRange); 
    }
}