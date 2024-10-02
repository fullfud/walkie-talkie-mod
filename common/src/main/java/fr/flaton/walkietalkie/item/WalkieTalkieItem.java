package fr.flaton.walkietalkie.item;

import fr.flaton.walkietalkie.client.gui.screen.WalkieTalkieScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class WalkieTalkieItem extends Item {

    private final int RANGE;


    public WalkieTalkieItem(net.minecraft.world.item.Item.Properties settings, int range) {
        super(settings);
        RANGE = range;
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {

        if (level.isClientSide()) {
            ItemStack stack = player.getItemInHand(hand);

            new WalkieTalkieScreen(stack);
            return InteractionResultHolder.success(stack);
        }

        return super.use(level, player, hand);
    }

    public static int getCanal(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.CANAL.get(), 1);
    }

    public static int getRange(ItemStack stack) {
        if (stack.getItem() instanceof WalkieTalkieItem item) {
            return item.getRange();
        }
        return -1;
    }

    private int getRange() {
        return RANGE;
    }

    public static boolean isActivate(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.ACTIVATE.get(), false);
    }

    public static boolean isMute(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.MUTE.get(), false);
    }

    public static void setCanal(ItemStack stack, int canal) {
        stack.set(ModDataComponents.CANAL.get(), canal);
    }

    public static void setActivate(ItemStack stack, boolean activate) {
        stack.set(ModDataComponents.ACTIVATE.get(), activate);
    }

    public static void setMute(ItemStack stack, boolean mute) {
        stack.set(ModDataComponents.MUTE.get(), mute);
    }


}
