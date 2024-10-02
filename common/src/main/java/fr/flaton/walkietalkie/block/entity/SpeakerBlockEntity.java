package fr.flaton.walkietalkie.block.entity;

import fr.flaton.walkietalkie.radio.Canal;
import fr.flaton.walkietalkie.screen.SpeakerScreenHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.*;


public class SpeakerBlockEntity extends BlockEntity implements MenuProvider {

    private static final Map<UUID, SpeakerBlockEntity> SPEAKERS = new HashMap<>();

    public static final String TAG_KEY_ACTIVATE = "speaker.activate";
    public static final String TAG_KEY_CANAL = "speaker.canal";

    protected final ContainerData containerData;

    private boolean activated;
    private int canal = 1;

    private final UUID uuid;

    public SpeakerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPEAKER.get(), pos, state);

        uuid = UUID.randomUUID();
        SPEAKERS.put(uuid, this);

        this.containerData = new ContainerData() {
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
            public int getCount() {
                return 2;
            }
        };
    }

    @Override
    public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
        return new SpeakerScreenHandler(syncId, this.containerData, ContainerLevelAccess.create(this.level, this.getBlockPos()));
    }

    @Override
    @NotNull
    public Component getDisplayName() {
        return Component.translatable("gui.walkietalkie.speaker.title");
    }

    @Override
    protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        this.activated = compoundTag.getBoolean(TAG_KEY_ACTIVATE);
        this.canal = compoundTag.getInt(TAG_KEY_CANAL);
    }


    @Override
    protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        compoundTag.putBoolean(TAG_KEY_ACTIVATE, this.activated);
        compoundTag.putInt(TAG_KEY_CANAL, this.canal);
    }

    @Override
    public boolean triggerEvent(int type, int data) {
        return super.triggerEvent(type, data);
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
