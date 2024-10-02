package fr.flaton.walkietalkie.network.packet.s2c;

import dev.architectury.networking.NetworkManager;
import fr.flaton.walkietalkie.Constants;
import fr.flaton.walkietalkie.client.gui.screen.WalkieTalkieScreen;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public record UpdateWalkieTalkieS2CPacket(ItemStack stack) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<UpdateWalkieTalkieS2CPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "update_walkietalkie_s2c"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateWalkieTalkieS2CPacket> STREAM_CODEC = StreamCodec.composite(ItemStack.STREAM_CODEC, UpdateWalkieTalkieS2CPacket::stack, UpdateWalkieTalkieS2CPacket::new);

    public static void receive(UpdateWalkieTalkieS2CPacket type, NetworkManager.PacketContext packetContext) {
        ItemStack stack = type.stack();

        WalkieTalkieScreen.getInstance().updateButtons(stack);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
