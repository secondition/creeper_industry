package com.secondition.creeperindustry.content.explosion.wave;

import com.secondition.creeperindustry.content.energy.signal.*;
import com.secondition.creeperindustry.content.explosion.wave.network.PeriodicWavePacket;

import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/** Analytic traveling periodic fields; no per-crest server objects or explosion instances. */
public final class MachineWaveField {
    private static final class Version {
        final ContinuousSignalSource source;
        long end = Long.MAX_VALUE;

        Version(ContinuousSignalSource source) {
            this.source = source;
        }

        PeriodicWavePacket packet() {
            var p = source.position();
            var s = source.signal();
            return new PeriodicWavePacket(
                    source.id(),
                    source.gameTime(),
                    end,
                    p.x,
                    p.y,
                    p.z,
                    s.amplitude(),
                    s.periodUnits(),
                    s.phaseUnits());
        }
    }

    private final List<Version> versions = new ArrayList<>();
    private Map<UUID, Long> previous = new HashMap<>();
    private final Map<UUID, Set<String>> seen = new HashMap<>();

    public void change(ServerLevel level, UUID id, ContinuousSignalSource next) {
        for (Version version : versions)
            if (version.source.id().equals(id) && version.end == Long.MAX_VALUE) {
                version.end = level.getGameTime();
                send(level, version);
            }
        if (next != null) {
            Version version = new Version(next);
            versions.add(version);
            send(level, version);
        }
    }

    private static String key(Version v) {
        return v.source.id() + ":" + v.source.gameTime();
    }

    private void send(ServerLevel level, Version version) {
        for (ServerPlayer player : level.players())
            if (player.position().distanceTo(version.source.position())
                    < Math.abs(version.source.signal().amplitude()) + 128) {
                PacketDistributor.sendToPlayer(player, version.packet());
                seen.computeIfAbsent(player.getUUID(), k -> new HashSet<>()).add(key(version));
            }
    }

    public void tick(ServerLevel level) {
        long time = level.getGameTime();
        versions.removeIf(
                v ->
                        v.end != Long.MAX_VALUE
                                && time > v.end + Math.abs(v.source.signal().amplitude()) + 2);
        if (time % 20 == 0) {
            Set<UUID> players = new HashSet<>();
            Set<String> live =
                    versions.stream()
                            .map(MachineWaveField::key)
                            .collect(java.util.stream.Collectors.toSet());
            for (ServerPlayer player : level.players()) {
                players.add(player.getUUID());
                Set<String> known = seen.computeIfAbsent(player.getUUID(), k -> new HashSet<>());
                known.retainAll(live);
                for (Version version : versions) {
                    boolean nearby =
                            player.position().distanceTo(version.source.position())
                                    < Math.abs(version.source.signal().amplitude()) + 128;
                    if (!nearby) known.remove(key(version));
                    else if (known.add(key(version)) || time % 100 == 0)
                        PacketDistributor.sendToPlayer(player, version.packet());
                }
            }
            seen.keySet().retainAll(players);
        }
        Map<Entity, Double> sums = new HashMap<>();
        Map<Entity, Vec3> forces = new HashMap<>();
        for (Version version : versions) {
            var source = version.source;
            var signal = source.signal();
            if (signal.periodTicks() <= 1) continue;
            double radius = Math.abs(signal.amplitude());
            Vec3 origin = source.position();
            for (Entity entity :
                    level.getEntitiesOfClass(
                            Entity.class,
                            new AABB(origin, origin).inflate(radius),
                            e -> !e.isSpectator())) {
                Vec3 point = entity.getBoundingBox().getCenter();
                double distance = point.distanceTo(origin);
                double magnitude = Math.max(0, radius - distance);
                if (magnitude <= 0) continue;
                long sourceTime =
                        time * 20
                                - SignalTime.travelUnits(
                                        distance,
                                        WavePropagationProfile.DEFAULT
                                                .propagationSpeedBlocksPerTick());
                if (sourceTime < source.gameTime() * 20
                        || (version.end != Long.MAX_VALUE && sourceTime >= version.end * 20))
                    continue;
                double value =
                        (Math.floorMod(sourceTime + signal.phaseUnits(), signal.periodUnits())
                                                < signal.periodUnits() / 2
                                        ? 1
                                        : -1)
                                * Math.signum(signal.amplitude())
                                * magnitude;
                sums.merge(entity, value, Double::sum);
                Vec3 direction = point.subtract(origin).normalize();
                forces.merge(entity, direction.scale(value), Vec3::add);
            }
        }
        Map<UUID, Long> current = new HashMap<>();
        for (var entry : sums.entrySet()) {
            Entity entity = entry.getKey();
            long value = Math.round(entry.getValue() * 1000);
            current.put(entity.getUUID(), value);
            if (value == 0 || previous.getOrDefault(entity.getUUID(), 0L) == value) continue;
            double resistance =
                    entity instanceof LivingEntity living
                            ? living.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE)
                            : 0;
            Vec3 force = forces.get(entity).scale(0.08 * (1 - resistance));
            entity.push(force);
            entity.hurtMarked = true;
            double damage = Math.max(0, Math.abs(value) / 1000.0 - 2) * 0.5;
            if (damage > 0) entity.hurt(level.damageSources().generic(), (float) damage);
        }
        previous = current;
    }

    public void clear() {
        versions.clear();
        previous.clear();
        seen.clear();
    }
}
