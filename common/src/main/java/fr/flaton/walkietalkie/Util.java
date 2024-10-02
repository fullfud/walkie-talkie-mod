package fr.flaton.walkietalkie;

import fr.flaton.walkietalkie.config.ModConfig;
import fr.flaton.walkietalkie.item.WalkieTalkieItem;
import fr.flaton.walkietalkie.radio.Canal;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Util {

    public static @Nullable ItemStack getWalkieTalkieInHand(@NotNull Player player) {

        ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);

        if (mainHand.getItem() instanceof WalkieTalkieItem) {
            return mainHand;
        }
        if (offHand.getItem() instanceof WalkieTalkieItem) {
            return offHand;
        }
        return null;
    }

    public static boolean canBroadcastToReceiver(Level senderLevel, Level receiverLevel, Vec3 senderPos, Vec3 receiverPos, int range) {
        if (!ModConfig.crossDimensionsEnabled && !receiverLevel.dimension().equals(senderLevel.dimension()))
            return false;

        double senderCoordinateScale = senderLevel.dimensionType().coordinateScale();
        double receiverCoordinateScale = receiverLevel.dimensionType().coordinateScale();

        double appliedRange = ModConfig.applyDimensionScale ? range / Math.max(senderCoordinateScale, receiverCoordinateScale) : range;

        return senderPos.closerThan(receiverPos, appliedRange);
    }

    public static List<ItemStack> getWalkieTalkies(Player player) {

        List<ItemStack> itemStacks = new ArrayList<>();

        Inventory playerInventory = player.getInventory();
        List<ItemStack> inventory = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            inventory.add(playerInventory.items.get(i));
        }
        inventory.addAll(playerInventory.offhand);

        for (ItemStack stack : inventory) {
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.getItem() instanceof WalkieTalkieItem) {
                itemStacks.add(stack);
            }

        }

        return itemStacks;
    }

    public static @Nullable ItemStack getOptimalWalkieTalkieRange(Player player) {
        List<ItemStack> itemStacks = getWalkieTalkies(player);
        if (itemStacks.isEmpty()) {
            return null;
        }

        ItemStack itemStack = null;
        int range = 0;

        for (ItemStack stack : itemStacks) {

            int rng = WalkieTalkieItem.getRange(stack);

            if (rng > range) {
                itemStack = stack;
                range = rng;
            }
        }

        return itemStack;
    }

    public static @Nullable ItemStack getWalkieTalkieActivated(Player player) {
        ItemStack stack = getOptimalWalkieTalkieRange(player);
        if (stack != null && WalkieTalkieItem.isActivate(stack)) {
            return stack;
        }
        return null;
    }

    public static List<ItemStack> getActivatedWalkieTalkies(ServerPlayer player) {
        List<ItemStack> walkieTalkies = getWalkieTalkies(player);
        walkieTalkies.removeIf(walkieTalkie -> !WalkieTalkieItem.isActivate(walkieTalkie));
        return walkieTalkies;
    }

    public static Set<Canal> getCanals(ServerPlayer player) {
        Set<Canal> canals = new HashSet<>();
        for (ItemStack itemStack : getActivatedWalkieTalkies(player)) {
            canals.add(Canal.getOrCreate(WalkieTalkieItem.getCanal(itemStack)));
        }
        return canals;
    }

    public static int loop(int value, int min, int max) {

        if (value > max) {
            value = min;
        } else if (value < min) {
            value = max;
        }
        return value;

    }
}
