// Файл: SpeakerBlockEntity.java (ФИНАЛЬНАЯ ВЕРСИЯ)

package fr.flaton.walkietalkie.block.entity;

import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;
import fr.flaton.walkietalkie.Util;
import fr.flaton.walkietalkie.WalkieTalkieVoiceChatPlugin;
import fr.flaton.walkietalkie.config.ModConfig;
import fr.flaton.walkietalkie.screen.SpeakerScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class SpeakerBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {

    private static final List<SpeakerBlockEntity> speakerBlockEntities = new ArrayList<>();

    public static final String NBT_KEY_ACTIVATE = "speaker.activate";
    public static final String NBT_KEY_CANAL = "speaker.canal";

    protected final PropertyDelegate propertyDelegate;

    boolean activated;
    int canal = 1;

    // Оставляем твою логику кэширования канала
    private LocationalAudioChannel channel = null;

    public SpeakerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPEAKER.get(), pos, state);
        speakerBlockEntities.add(this);

        this.propertyDelegate = new PropertyDelegate() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> SpeakerBlockEntity.this.activated ? 1 : 0;
                    case 1 -> SpeakerBlockEntity.this.canal;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> SpeakerBlockEntity.this.activated = value == 1;
                    case 1 -> SpeakerBlockEntity.this.canal = value;
                    default -> {
                    }
                }
            }

            @Override
            public int size() {
                return 2;
            }
        };
    }

    // --- ОСТАЛЬНАЯ ЧАСТЬ ТВОЕГО КЛАССА БЕЗ ИЗМЕНЕНИЙ ---
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new SpeakerScreenHandler(syncId, this.propertyDelegate, ScreenHandlerContext.create(this.world, this.getPos()));
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("gui.walkietalkie.speaker.title");
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        this.activated = nbt.getBoolean(NBT_KEY_ACTIVATE);
        this.canal = nbt.getInt(NBT_KEY_CANAL);
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putBoolean(NBT_KEY_ACTIVATE, this.activated);
        nbt.putInt(NBT_KEY_CANAL, this.canal);
    }

    @Override
    public boolean onSyncedBlockEvent(int type, int data) {
        return super.onSyncedBlockEvent(type, data);
    }

    public static List<SpeakerBlockEntity> getSpeakersActivatedInRange(int canal, World world, Vec3d pos, int range) {
        speakerBlockEntities.removeIf(BlockEntity::isRemoved);
        List<SpeakerBlockEntity> list = new ArrayList<>();
        for (SpeakerBlockEntity speaker : speakerBlockEntities) {
            if (!speaker.hasWorld()) {
                continue;
            }
            if (!ModConfig.crossDimensionsEnabled
                    && !world.getRegistryKey().getRegistry().equals(speaker.getWorld().getRegistryKey().getRegistry())) {
                continue;
            }
            if (!speaker.canBroadcastToSpeaker(world, pos, speaker, range)) {
                continue;
            }
            if (speaker.activated) {
                if (speaker.canal == canal) {
                    list.add(speaker);
                }
            }
        }
        return list;
    }

    private boolean canBroadcastToSpeaker(World senderWorld, Vec3d senderPos, SpeakerBlockEntity speaker, int range) {
        World receiverWorld = speaker.getWorld();
        if (receiverWorld == null) {
            return false;
        }
        return Util.canBroadcastToReceiver(senderWorld, receiverWorld, senderPos, speaker.pos.toCenterPos(), range);
    }

    // --- КОНЕЦ НЕИЗМЕНЕННОЙ ЧАСТИ ---


    /**
     * Старый метод. Он может понадобиться, если где-то еще используется,
     * поэтому мы его оставляем. Но наша новая логика его не вызывает.
     */
    public void playSound(VoicechatServerApi api, MicrophonePacketEvent event) {
        if (this.channel == null) {
            if (world == null || api == null) return;
            Position pos = api.createPosition(this.getPos().getX() + 0.5D, this.getPos().getY() + 0.5D, this.getPos().getZ() + 0.5D);
            this.channel = api.createLocationalAudioChannel(UUID.randomUUID(), api.fromServerLevel(this.world), pos);
            if (this.channel == null) {
                return;
            }
            this.channel.setCategory(WalkieTalkieVoiceChatPlugin.SPEAKER_CATEGORY);
            this.channel.setDistance(ModConfig.speakerDistance + 1F);
            if (!ModConfig.voiceDuplication && event.getSenderConnection() != null) {
                this.channel.setFilter(serverPlayer -> !serverPlayer.getEntity().equals(event.getSenderConnection().getPlayer().getEntity()));
            }
        }
        this.channel.send(event.getPacket().getOpusEncodedData());
    }

    /**
     * НОВЫЙ МЕТОД для проигрывания зашумленного звука из StaticSoundPacket.
     * Он использует твою же логику кэширования канала.
     */
    public void playSound(VoicechatServerApi api, StaticSoundPacket packet, ServerPlayerEntity sender) {
        if (this.channel == null) {
            if (world == null || api == null) return;
            Position pos = api.createPosition(this.getPos().getX() + 0.5D, this.getPos().getY() + 0.5D, this.getPos().getZ() + 0.5D);
            // Используем UUID.randomUUID() как и в твоем старом методе, чтобы создать уникальный канал
            this.channel = api.createLocationalAudioChannel(UUID.randomUUID(), api.fromServerLevel(this.world), pos);
            if (this.channel == null) {
                return;
            }
            this.channel.setCategory(WalkieTalkieVoiceChatPlugin.SPEAKER_CATEGORY);
            this.channel.setDistance(ModConfig.speakerDistance + 1F);
            if (!ModConfig.voiceDuplication) {
                // Применяем фильтр, чтобы говорящий не слышал сам себя из динамика
                this.channel.setFilter(serverPlayer -> !serverPlayer.equals(sender));
            }
        }
        // Создаем AudioPlayer для проигрывания нашего готового пакета
        AudioPlayer audioPlayer = api.createAudioPlayer(this.channel, api.createEncoder(), packet.getOpusEncodedData());
        audioPlayer.startPlaying();
    }
}