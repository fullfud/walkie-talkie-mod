package fr.flaton.walkietalkie.block.entity;

import fr.flaton.walkietalkie.Util;
import fr.flaton.walkietalkie.radio.Canal;
import fr.flaton.walkietalkie.screen.SpeakerScreenHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.*;


public class SpeakerBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {

    private static final Map<UUID, SpeakerBlockEntity> SPEAKERS = new HashMap<>();

    public static final String NBT_KEY_ACTIVATE = "speaker.activate";
    public static final String NBT_KEY_CANAL = "speaker.canal";

    protected final PropertyDelegate propertyDelegate;

    private boolean activated;
    private int canal = 1;

    private final UUID uuid;

    public SpeakerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPEAKER.get(), pos, state);

        uuid = UUID.randomUUID();
        SPEAKERS.put(uuid, this);

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

    private boolean canBroadcastToSpeaker(World senderWorld, Vec3d senderPos, SpeakerBlockEntity speaker, int range) {
        World receiverWorld = speaker.getWorld();

        if (receiverWorld == null) {
            return false;
        }

        return Util.canBroadcastToReceiver(senderWorld, receiverWorld, senderPos, speaker.pos.toCenterPos(), range);
    }

    public UUID getUuid() {
        return uuid;
    }

    public static List<SpeakerBlockEntity> getActiveSpeakers() {
        Iterator<Map.Entry<UUID, SpeakerBlockEntity>> iterator = SPEAKERS.entrySet().iterator();
        List<SpeakerBlockEntity> activeSpeakers = new ArrayList<>();

        while (iterator.hasNext()) {
            Map.Entry<UUID, SpeakerBlockEntity> entry = iterator.next();
            SpeakerBlockEntity speakerBlockEntity = entry.getValue();

            if (speakerBlockEntity.isRemoved()) {
                iterator.remove();
            } else if (speakerBlockEntity.activated) {
                activeSpeakers.add(speakerBlockEntity);
            }
        }

        return activeSpeakers;
    }

    public Set<Canal> getCanal() {
        return Set.of(Canal.getOrCreate(canal));
    }
}
