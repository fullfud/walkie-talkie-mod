package fr.flaton.walkietalkie.network.packet.c2s;

import dev.architectury.networking.NetworkManager;
import fr.flaton.walkietalkie.Constants;
import fr.flaton.walkietalkie.ModSoundEvents;
import fr.flaton.walkietalkie.Util;
import fr.flaton.walkietalkie.item.WalkieTalkieItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

public class ActivateKeyPressedC2SPacket implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ActivateKeyPressedC2SPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "activate_keypressed_c2s"));
    public static final ActivateKeyPressedC2SPacket INSTANCE = new ActivateKeyPressedC2SPacket();
    public static final StreamCodec<ByteBuf, ActivateKeyPressedC2SPacket> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    public static void receive(ActivateKeyPressedC2SPacket type, NetworkManager.PacketContext packetContext) {
        ServerPlayer player = (ServerPlayer) packetContext.getPlayer();

        ItemStack stack = Util.getWalkieTalkieInHand(player);

        if (stack == null)
            stack = Util.getOptimalWalkieTalkieRange(player);
        if (stack == null) {
            return;
        }

        boolean activated = WalkieTalkieItem.isActivate(stack);

        WalkieTalkieItem.setActivate(stack, !activated);

        Holder<SoundEvent> holder = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(activated ? ModSoundEvents.OFF_SOUND_EVENT.get() : ModSoundEvents.ON_SOUND_EVENT.get());
        player.connection.send(new ClientboundSoundPacket(holder, SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(), 1f, 1f, 0));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
