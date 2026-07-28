package com.secondition.creeperindustry.content.logistics.rocket.runtime;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.secondition.creeperindustry.CIChunkTickets;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;

public final class RocketFlightRuntime {
    public static final int MAX_ACTIVE_FLIGHTS = 64;
    public static final int MAX_TICKETS = 128;
    private final Map<UUID, LinkedHashSet<ChunkPos>> tickets = new HashMap<>();
    private final Map<UUID, Integer> unclaimedRestoredFlights = new HashMap<>();
    private long nextColdLoadGameTime;

    public boolean reserve(ServerLevel level, UUID id, ChunkPos initial) {
        if (tickets.size() >= MAX_ACTIVE_FLIGHTS || ticketCount() >= MAX_TICKETS) return false;
        LinkedHashSet<ChunkPos> held = new LinkedHashSet<>();
        if (!add(level, id, initial)) return false;
        held.add(initial); tickets.put(id, held); return true;
    }

    public void claim(ServerLevel level, UUID id, ChunkPos current) {
        unclaimedRestoredFlights.remove(id);
        if (!tickets.containsKey(id)) reserve(level, id, current);
    }

    public boolean ensure(ServerLevel level, UUID id, ChunkPos next) {
        LinkedHashSet<ChunkPos> held = tickets.get(id);
        if (held == null) return false;
        if (!held.contains(next)) {
            boolean alreadyTicking = level.getChunkSource().isPositionTicking(next.toLong());
            if (!alreadyTicking && level.getGameTime() < nextColdLoadGameTime) return false;
            if (ticketCount() >= MAX_TICKETS || !add(level, id, next)) return false;
            if (!alreadyTicking) nextColdLoadGameTime = level.getGameTime() + 5L;
            held.add(next);
        }
        while (held.size() > 2) {
            ChunkPos oldest = held.iterator().next(); held.remove(oldest); remove(level, id, oldest);
        }
        return level.getChunkSource().isPositionTicking(next.toLong());
    }

    public void finish(ServerLevel level, UUID id) {
        unclaimedRestoredFlights.remove(id);
        Set<ChunkPos> held = tickets.remove(id);
        if (held != null) held.forEach(chunk -> remove(level, id, chunk));
    }

    public void close(ServerLevel level) { for (UUID id : Set.copyOf(tickets.keySet())) finish(level, id); }
    public void restore(TicketHelper helper) {
        helper.getEntityTickets().forEach((id, ticketSet) -> {
            if (tickets.size() >= MAX_ACTIVE_FLIGHTS) { helper.removeAllTickets(id); return; }
            LinkedHashSet<ChunkPos> held = new LinkedHashSet<>();
            ticketSet.ticking().forEach(chunkLong -> {
                if (held.size() < 2 && ticketCount() + held.size() < MAX_TICKETS) held.add(new ChunkPos(chunkLong));
                else helper.removeTicket(id, chunkLong, true);
            });
            ticketSet.nonTicking().forEach(chunkLong -> helper.removeTicket(id, chunkLong, false));
            if (!held.isEmpty()) tickets.put(id, held);
            if (!held.isEmpty()) unclaimedRestoredFlights.put(id, 0);
        });
    }
    public void tick(ServerLevel level) {
        for (UUID id : Set.copyOf(unclaimedRestoredFlights.keySet())) {
            int age = unclaimedRestoredFlights.merge(id, 1, Integer::sum);
            if (age > 200) finish(level, id);
        }
    }
    private boolean add(ServerLevel level, UUID id, ChunkPos chunk) { return CIChunkTickets.GUIDED_ROCKET.forceChunk(level, id, chunk.x, chunk.z, true, true); }
    private void remove(ServerLevel level, UUID id, ChunkPos chunk) { CIChunkTickets.GUIDED_ROCKET.forceChunk(level, id, chunk.x, chunk.z, false, true); }
    private int ticketCount() { return tickets.values().stream().mapToInt(Set::size).sum(); }
}
