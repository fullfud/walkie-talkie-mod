package fr.flaton.walkietalkie;

import fr.flaton.walkietalkie.config.ModConfig;
import fr.flaton.walkietalkie.item.WalkieTalkieItem;
import fr.flaton.walkietalkie.radio.Canal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Util {

    public static ItemStack getWalkieTalkieInHand(PlayerEntity player) {

        ItemStack mainHand = player.getStackInHand(Hand.MAIN_HAND);
        ItemStack offHand = player.getStackInHand(Hand.OFF_HAND);

        if (mainHand.getItem() instanceof WalkieTalkieItem) {
            return mainHand;
        }
        if (offHand.getItem() instanceof WalkieTalkieItem) {
            return offHand;
        }
        return null;
    }

    public static boolean canBroadcastToReceiver(World senderWorld, World receiverWorld, Vec3d senderPos, Vec3d receiverPos, int range) {
        if (!ModConfig.crossDimensionsEnabled && !receiverWorld.getDimension().equals(senderWorld.getDimension()))
            return false;

        double senderCoordinateScale = senderWorld.getDimension().coordinateScale();
        double receiverCoordinateScale = receiverWorld.getDimension().coordinateScale();

        double appliedRange = ModConfig.applyDimensionScale ? range / Math.max(senderCoordinateScale, receiverCoordinateScale) : range;

        return senderPos.isInRange(receiverPos, appliedRange);
    }

    public static List<ItemStack> getWalkieTalkies(PlayerEntity player) {

        List<ItemStack> itemStacks = new ArrayList<>();

        PlayerInventory playerInventory = player.getInventory();
        List<ItemStack> inventory = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            inventory.add(playerInventory.main.get(i));
        }
        inventory.addAll(playerInventory.offHand);

        for (ItemStack stack : inventory) {
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.getItem() instanceof WalkieTalkieItem && stack.hasNbt()) {
                itemStacks.add(stack);
            }

        }

        return itemStacks;
    }

    public static @Nullable ItemStack getOptimalWalkieTalkieRange(PlayerEntity player) {
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

    public static @Nullable ItemStack getWalkieTalkieActivated(PlayerEntity player) {
        ItemStack stack = getOptimalWalkieTalkieRange(player);
        if (stack != null && WalkieTalkieItem.isActivate(stack)) {
            return stack;
        }
        return null;
    }

    public static List<ItemStack> getActivatedWalkieTalkies(ServerPlayerEntity player) {
        List<ItemStack> walkieTalkies = getWalkieTalkies(player);
        walkieTalkies.removeIf(walkieTalkie -> !WalkieTalkieItem.isActivate(walkieTalkie));
        return walkieTalkies;
    }

    public static Set<Canal> getCanals(ServerPlayerEntity player) {
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
