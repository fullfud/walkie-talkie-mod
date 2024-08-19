package fr.flaton.walkietalkie.radio;

import java.util.*;


public class Canal {

    private static final Map<Integer, Canal> CANALS = new HashMap<>();

    private final int canal;
    private final Set<Member> members = new HashSet<>();

    private Canal(int canal) {
        this.canal = canal;
    }

    public static Canal getOrCreate(int canalNum) {
        return CANALS.computeIfAbsent(canalNum, Canal::new);
    }

    public void addMember(Member member) {
        members.add(member);
    }

    public void removeMember(Member member) {
        members.remove(member);
        if (members.isEmpty()) {
            CANALS.remove(canal);
        }
    }

    public List<Member> getMembers() {
        members.removeIf(member -> !member.getCanals().contains(this));
        return new ArrayList<>(members);
    }

    public static Set<Canal> getCanals() {
        return new HashSet<>(CANALS.values());
    }
}
