package fr.flaton.walkietalkie.screen;

import fr.flaton.walkietalkie.block.ModBlocks;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class SpeakerScreenHandler extends AbstractContainerMenu {
    private final ContainerData containerData;

    private final ContainerLevelAccess levelAccess;

    public SpeakerScreenHandler(int i, Inventory inventory) {
        this(i, new SimpleContainerData(2), ContainerLevelAccess.NULL);
    }

    public SpeakerScreenHandler(int syncId, ContainerData containerData, ContainerLevelAccess levelAccess) {
        super(ModScreenHandlers.SPEAKER.get(), syncId);
        this.containerData = containerData;
        this.levelAccess = levelAccess;

        addDataSlots(containerData);
    }

    public boolean isActivate() {
        return containerData.get(0) > 0;
    }

    public int getCanal() {
        return containerData.get(1);
    }

    public void setCanal(int canal) {
        containerData.set(1, canal);
    }

    public void setActivate(boolean activate) {
        containerData.set(0, activate ? 1 : 0);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int i) {
        return null;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.levelAccess, player, ModBlocks.SPEAKER.get());
    }
}
