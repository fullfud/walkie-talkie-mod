package fr.flaton.walkietalkie.radio;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import fr.flaton.walkietalkie.Util;
import fr.flaton.walkietalkie.item.WalkieTalkieItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;


public class SoundManager {

    private static SoundManager instance;

    SoundManager() {
        instance = this;
    }

    public void onMicPacket(MicrophonePacketEvent event) {

        VoicechatConnection senderConnection = event.getSenderConnection();

        if (senderConnection == null)
            return;

        if (!(senderConnection.getPlayer().getPlayer() instanceof Player senderPlayer))
            return;

        ItemStack senderWalkie = Util.getWalkieTalkieInHand(senderPlayer);

        if (senderWalkie == null)
            return;

        if (!WalkieTalkieItem.isActivate(senderWalkie) || WalkieTalkieItem.isMute(senderWalkie))
            return;

        int senderCanal = WalkieTalkieItem.getCanal(senderWalkie);
        int senderRange = WalkieTalkieItem.getRange(senderWalkie);

        Member source = Member.get(senderPlayer.getUUID());
        if (source == null)
            return;

        List<Member> listeners = new ArrayList<>(Canal.getOrCreate(senderCanal).getMembers());
        listeners.removeIf(member -> !isValidListener(source, member, senderRange));

        for (Member member : listeners) {
            if (member != source) {
                member.sendAudio(event.getPacket());
            }
        }
    }

    private boolean isValidListener(Member source, Member member, int range) {
        return Util.canBroadcastToReceiver(source.getLevel(), member.getLevel(), source.getPos(), member.getPos(), range);
    }

    public static SoundManager getInstance() {
        if (instance == null) instance = new SoundManager();
        return instance;
    }

    public static void serverTick(MinecraftServer server) {
        Member.serverTick(server);
    }
}
