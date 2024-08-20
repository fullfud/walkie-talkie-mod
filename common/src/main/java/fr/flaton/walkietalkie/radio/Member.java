package fr.flaton.walkietalkie.radio;

import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.packets.MicrophonePacket;
import fr.flaton.walkietalkie.Util;
import fr.flaton.walkietalkie.block.entity.SpeakerBlockEntity;
import fr.flaton.walkietalkie.config.ModConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static fr.flaton.walkietalkie.WalkieTalkieVoiceChatPlugin.voiceChatAPI;

public class Member {

    private final static Map<UUID, Member> MEMBERS = new HashMap<>();

    private final UUID uuid;
    private Vec3d pos;
    private World world;
    private final Set<Canal> canals = new HashSet<>();

    private short volume = 0;

    private final OpusDecoder decoder;
    private final List<short[]> packetBuffer = new ArrayList<>();
    private AudioPlayer audioPlayer;
    private AudioChannel audioChannel;

    public static void serverTick(MinecraftServer server) {
        List<ServerPlayerEntity> playerList = server.getPlayerManager().getPlayerList();
        List<Member> members = new ArrayList<>();

        for (ServerPlayerEntity player : playerList) {
            Member member = Member.get(player.getUuid(), player.getPos(), player.getWorld(), Util.getCanals(player));
            if (member !=null)
                members.add(member);
        }
        for (SpeakerBlockEntity speaker : SpeakerBlockEntity.getActiveSpeakers()) {
            Member member = Member.get(speaker.getUuid(), speaker.getPos().toCenterPos(), speaker.getWorld(), speaker.getCanal());
            if (member !=null)
                members.add(member);
        }

        for (Member member : new HashSet<>(MEMBERS.values())) {
            if (!members.contains(member)) {
                for (Canal canal : member.canals) {
                    canal.removeMember(member);
                }
                MEMBERS.remove(member.uuid);
                if (member.audioPlayer != null) {
                    member.audioPlayer.stopPlaying();
                }
                if (member.decoder != null) {
                    member.decoder.close();
                }
            }
        }
    }

    public static Member get(UUID uuid) {
        return MEMBERS.get(uuid);
    }

    private static @Nullable Member get(UUID uuid, Vec3d pos, World world, Set<Canal> canals) {
        if (canals.isEmpty()) return null;
        Member member = MEMBERS.computeIfAbsent(uuid, Member::new);
        member.update(pos, world, canals);
        return member;
    }

    private Member(UUID uuid) {
        this.uuid = uuid;
        this.decoder = voiceChatAPI.createDecoder();
    }

    private void update(Vec3d pos, World world, Set<Canal> canals) {
        this.pos = pos;
        this.world = world;

        for (Canal canal : canals) {
            if (!this.canals.contains(canal)) {
                canal.addMember(this);
                this.canals.add(canal);
            }
        }

        for (Canal canal : new HashSet<>(this.canals)) {
            if (!canals.contains(canal)) {
                canal.removeMember(this);
                this.canals.remove(canal);
            }
        }
    }

    public UUID getUuid() {
        return uuid;
    }

    public Vec3d getPos() {
        return pos;
    }

    public World getWorld() {
        return world;
    }

    public Set<Canal> getCanals() {
        return canals;
    }

    public void sendAudio(MicrophonePacket packet) {
        byte[] data = packet.getOpusEncodedData();
        short[] decoded = decoder.decode(data);
        packetBuffer.add(decoded);

        if (getAudioPlayer() != null)
            audioPlayer.startPlaying();
    }

    private AudioPlayer getAudioPlayer() {
        if (audioPlayer == null) {
            VoicechatConnection connection = voiceChatAPI.getConnectionOf(uuid);
            if (connection == null) { // Speaker
                LocationalAudioChannel locationalAudioChannel = voiceChatAPI.createLocationalAudioChannel(uuid,
                        voiceChatAPI.fromServerLevel(world),
                        voiceChatAPI.createPosition(pos.x, pos.y, pos.z));
                locationalAudioChannel.setDistance(ModConfig.speakerDistance);
                audioChannel = locationalAudioChannel;
            } else { // Player
                audioChannel = voiceChatAPI.createEntityAudioChannel(uuid, connection.getPlayer());
            }

            audioPlayer = voiceChatAPI.createAudioPlayer(audioChannel, voiceChatAPI.createEncoder(), this::getAudio);
        }

        return audioPlayer;
    }

    private short[] getAudio() {
        short[] audio = getCombinedAudio();
        if (audio == null) {
            volume = 0;
            audioPlayer.stopPlaying();
            audioPlayer = null;
            return null;
        }
        volume = getVolume(audio);
        return audio;
    }

    private short getVolume(short[] audio) {
        short max = 0;
        for (short num : audio) {
            if (num > max) {
                max = num; // Mise à jour du maximum
            }
        }
        return max;
    }

    public short[] getCombinedAudio() {
        if (packetBuffer.isEmpty()) {
            return null;
        }

        short[] result = new short[960];
        int sample;
        for (int i = 0; i < result.length; i++) {
            sample = 0;
            for (short[] audio : packetBuffer) {
                sample += audio[i];
            }
            if (sample > Short.MAX_VALUE) {
                result[i] = Short.MAX_VALUE;
            } else if (sample < Short.MIN_VALUE) {
                result[i] = Short.MIN_VALUE;
            } else {
                result[i] = (short) sample;
            }
        }
        packetBuffer.clear();
        return result;
    }

    public boolean isListening() {
        return volume != 0;
    }

    public short getVolume() {
        return volume;
    }
}