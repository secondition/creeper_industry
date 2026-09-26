package com.secondition.creeperindustry.content.snapshot;

import com.mojang.serialization.Lifecycle;
import com.secondition.creeperindustry.CreeperIndustry;
import com.secondition.creeperindustry.content.energy.signal.SignalDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import net.neoforged.fml.util.ObfuscationReflectionHelper;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.concurrent.Executor;

public final class SnapshotDimensionManager {
    private static final RegistrationInfo REGISTRATION = new RegistrationInfo(Optional.empty(), Lifecycle.stable());
    private static final Map<ResourceKey<Level>, Session> sessions = new HashMap<>();
    private static final Map<UUID, Baseline> players = new HashMap<>();
    private static final List<CaptureRequest> captureRequests = new ArrayList<>();

    private record CaptureRequest(MinecraftServer server, ResourceKey<Level> dimension,
            BlockPos origin, int slowdown, int radius,
            java.util.function.Consumer<SnapshotRecord> result) {}

    private record Baseline(ServerLevel level, double x, double y, double z, float yaw, float pitch,
            CompoundTag state) {}

    private SnapshotDimensionManager() {}

    static final class Session {
        final SnapshotRecord record;
        final ServerLevel level;
        final SnapshotLevelData data;
        int playback;
        boolean paused = true;
        final Set<BlockPos> modified = new HashSet<>();

        Session(SnapshotRecord record, ServerLevel level, SnapshotLevelData data) {
            this.record = record;
            this.level = level;
            this.data = data;
        }
    }

    public static OptionalLong time(Level level) {
        return session(level).map(session -> OptionalLong.of(session.data.getGameTime())).orElseGet(OptionalLong::empty);
    }

    public static SignalDefinition scaleSignal(Level level, SignalDefinition signal) {
        return session(level).map(session -> signal.atSnapshot(session.record.sourceTick, session.record.slowdown))
                .orElse(signal);
    }

    public static boolean isSnapshot(Level level) {
        return session(level).isPresent();
    }

    public static boolean isSnapshotDimension(Level level) {
        return level.dimension().location().getNamespace().equals(CreeperIndustry.MODID)
                && level.dimension().location().getPath().startsWith("snapshot/");
    }

    private static Optional<Session> session(Level level) {
        if (!(level instanceof ServerLevel server)) return Optional.empty();
        return sessions.values().stream().filter(session -> session.level == server).findFirst();
    }

    public static void requestCapture(MinecraftServer server, ServerLevel source,
            BlockPos origin, int slowdown, int radius,
            java.util.function.Consumer<SnapshotRecord> result) {
        captureRequests.add(new CaptureRequest(server, source.dimension(), origin, slowdown,
                radius, result));
    }

    public static void processCaptureRequests() {
        for (CaptureRequest request : List.copyOf(captureRequests)) {
            ServerLevel source = request.server().getLevel(request.dimension());
            if (source == null) continue;
            try {
                SnapshotRecord record = capture(request.server(), source, request.origin(),
                        request.slowdown(), request.radius());
                request.result().accept(record);
            } catch (RuntimeException exception) {
                CreeperIndustry.LOGGER.error("Failed to capture snapshot", exception);
            }
            captureRequests.remove(request);
        }
    }

    public static SnapshotRecord capture(MinecraftServer server, ServerLevel source,
            BlockPos origin, int slowdown, int radius) {
        if (slowdown != 1 && slowdown != 2 && slowdown != 4 && slowdown != 8)
            throw new IllegalArgumentException("Unsupported snapshot slowdown");
        SnapshotSavedData saved = server.overworld().getDataStorage().computeIfAbsent(
                SnapshotSavedData.factory(), SnapshotSavedData.NAME);
        while (saved.records().size() >= SnapshotConfig.MAX_SNAPSHOTS.get()) {
            SnapshotRecord oldest = saved.records().values().stream()
                    .filter(record -> sessions.get(key(record)) == null
                            || sessions.get(key(record)).level.players().isEmpty())
                    .min(Comparator.comparingLong(record -> record.createdAt))
                    .orElseThrow(() -> new IllegalStateException("All snapshots are occupied"));
            remove(server, oldest, saved);
        }
        radius = Math.clamp(radius, 2, 4);
        UUID id = UUID.randomUUID();
        ChunkPos center = new ChunkPos(origin);
        List<CompoundTag> chunks = new ArrayList<>();
        for (int z = center.z - radius; z <= center.z + radius; z++) {
            for (int x = center.x - radius; x <= center.x + radius; x++) {
                LevelChunk chunk = source.getChunkSource().getChunkNow(x, z);
                if (chunk == null) continue;
                CompoundTag chunkTag = new CompoundTag();
                chunkTag.putInt("x", x);
                chunkTag.putInt("z", z);
                ListTag blocks = new ListTag();
                BlockPos min = new BlockPos(x << 4, source.getMinBuildHeight(), z << 4);
                BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
                for (int y = source.getMinBuildHeight(); y < source.getMaxBuildHeight(); y++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        for (int localX = 0; localX < 16; localX++) {
                            pos.set(min.getX() + localX, y, min.getZ() + localZ);
                            if (chunk.getBlockState(pos).isAir()) continue;
                            CompoundTag block = new CompoundTag();
                            block.putInt("x", pos.getX());
                            block.putInt("y", y);
                            block.putInt("z", pos.getZ());
                            block.put("state", net.minecraft.nbt.NbtUtils.writeBlockState(chunk.getBlockState(pos)));
                            blocks.add(block);
                        }
                    }
                }
                chunkTag.put("blocks", blocks);
                ListTag blockEntities = new ListTag();
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    CompoundTag entityTag = entity.saveWithFullMetadata(server.registryAccess());
                    // The copied table must carry its own snapshot identity.
                    if (entity instanceof SnapshotTableBlockEntity)
                        entityTag.putUUID(SnapshotTableBlockEntity.SNAPSHOT_ID_TAG, id);
                    blockEntities.add(entityTag);
                }
                chunkTag.put("block_entities", blockEntities);
                chunks.add(chunkTag);
            }
        }
        List<CompoundTag> entities = new ArrayList<>();
        for (var entity : source.getAllEntities()) {
            if (entity instanceof net.minecraft.server.level.ServerPlayer) continue;
            if (new ChunkPos(entity.blockPosition()).getChessboardDistance(center) > radius) continue;
            CompoundTag entityTag = new CompoundTag();
            if (entity.save(entityTag)) entities.add(entityTag);
        }
        SnapshotRecord record = new SnapshotRecord(id, source.dimension().location(),
                CreeperIndustry.asResource("snapshot/" + id), center, source.getGameTime(),
                slowdown, radius, source.getGameTime(), chunks, entities);
        saved.put(record);
        create(server, record, source);
        return record;
    }

    private static void remove(MinecraftServer server, SnapshotRecord record, SnapshotSavedData saved) {
        Session session = sessions.remove(key(record));
        if (session != null) {
            try {
                session.level.close();
            } catch (java.io.IOException exception) {
                CreeperIndustry.LOGGER.error("Failed to close snapshot {}", record.id, exception);
            }
            server.forgeGetWorldMap().remove(key(record));
            server.markWorldsDirty();
        }
        saved.remove(record.id);
    }

    public static int slowdown(UUID id) {
        return sessions.values().stream().filter(session -> session.record.id.equals(id))
                .map(session -> session.record.slowdown).findFirst().orElse(0);
    }

    public static void toggle(net.minecraft.server.level.ServerPlayer player, UUID id) {
        if (players.containsKey(player.getUUID())) {
            exit(player);
            return;
        }
        Session session = sessions.values().stream().filter(value -> value.record.id.equals(id)).findFirst().orElse(null);
        if (session == null) return;
        players.put(player.getUUID(), new Baseline((ServerLevel) player.level(), player.getX(),
                player.getY(), player.getZ(), player.getYRot(), player.getXRot(),
                player.saveWithoutId(new CompoundTag())));
        player.teleportTo(session.level, player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot());
    }

    public static void exit(net.minecraft.server.level.ServerPlayer player) {
        Baseline baseline = players.remove(player.getUUID());
        if (baseline == null) return;
        player.load(baseline.state.copy());
        player.teleportTo(baseline.level, baseline.x, baseline.y, baseline.z,
                baseline.yaw, baseline.pitch);
    }

    public static boolean apply(com.secondition.creeperindustry.content.energy.signal.CreativeSignalSourceBlockEntity snapshotSource) {
        Session session = find(snapshotSource.getLevel() instanceof ServerLevel level ? level : null).orElse(null);
        if (session == null || snapshotSource.getLevel() == null) return false;
        MinecraftServer server = session.level.getServer();
        ServerLevel source = server.getLevel(ResourceKey.create(Registries.DIMENSION,
                session.record.sourceDimension));
        if (source == null || !(source.getBlockEntity(snapshotSource.getBlockPos())
                instanceof com.secondition.creeperindustry.content.energy.signal.CreativeSignalSourceBlockEntity target))
            return false;
        CompoundTag baseline = null;
        for (var chunkTag : session.record.chunks) {
            var entities = chunkTag.getList("block_entities", 10);
            for (int i = 0; i < entities.size(); i++) {
                CompoundTag entity = entities.getCompound(i);
                if (entity.hasUUID("continuous_source_id")
                        && entity.getUUID("continuous_source_id").equals(snapshotSource.sourceId())) {
                    baseline = entity;
                    break;
                }
            }
        }
        if (baseline == null || !target.matchesConfiguration(baseline)) return false;
        target.copyConfiguration(snapshotSource);
        return true;
    }

    public static void markChanged(ServerLevel level, BlockPos pos) {
        find(level).ifPresent(session -> session.modified.add(pos.immutable()));
    }

    public static void replay(UUID id) {
        sessions.values().stream().filter(session -> session.record.id.equals(id)).findFirst()
                .ifPresent(SnapshotDimensionManager::play);
    }

    public static void sync(net.minecraft.server.level.ServerPlayer player) {
        sessions.keySet().forEach(key -> PacketDistributor.sendToPlayer(player,
                new SnapshotDimensionPacket(key)));
    }

    public static void restore(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        SnapshotSavedData data = overworld.getDataStorage().computeIfAbsent(SnapshotSavedData.factory(), SnapshotSavedData.NAME);
        for (SnapshotRecord record : data.records().values()) {
            try {
                ServerLevel source = server.getLevel(ResourceKey.create(Registries.DIMENSION, record.sourceDimension));
                if (source == null) continue;
                create(server, record, source);
            } catch (RuntimeException exception) {
                CreeperIndustry.LOGGER.error("Failed to restore snapshot {}", record.id, exception);
            }
        }
    }

    static ServerLevel create(MinecraftServer server, SnapshotRecord record, ServerLevel source) {
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION,
                record.dimension);
        ServerLevel existing = server.getLevel(key);
        if (existing != null) return existing;

        LevelStem stem = new LevelStem(source.dimensionTypeRegistration(),
                source.getChunkSource().getGenerator());
        Registry<LevelStem> dimensions = server.registryAccess().registryOrThrow(Registries.LEVEL_STEM);
        ResourceKey<LevelStem> stemKey = ResourceKey.create(Registries.LEVEL_STEM, record.dimension);
        if (dimensions instanceof MappedRegistry<LevelStem> writable) {
            writable.unfreeze();
            if (writable.get(stemKey) == null) writable.register(stemKey, stem, REGISTRATION);
        }

        Map<ResourceKey<Level>, ServerLevel> worldMap = server.forgeGetWorldMap();
        Executor executor = field(server, "executor");
        LevelStorageAccess storage = field(server, "storageSource");
        ChunkProgressListenerFactory progress = field(server, "progressListenerFactory");
        WorldData worldData = server.getWorldData();
        SnapshotLevelData levelData = new SnapshotLevelData(worldData, worldData.overworldData(), record.sourceTick);
        ServerLevel level = new ServerLevel(server, executor, storage, levelData, key, stem,
                progress.create(11), worldData.isDebugWorld(), source.getSeed(), List.of(), false,
                (RandomSequences) null);
        source.getWorldBorder().addListener(new net.minecraft.world.level.border.BorderChangeListener.DelegateBorderChangeListener(level.getWorldBorder()));
        worldMap.put(key, level);
        server.markWorldsDirty();
        NeoForge.EVENT_BUS.post(new LevelEvent.Load(level));
        PacketDistributor.sendToAllPlayers(new SnapshotDimensionPacket(key));
        Session session = new Session(record, level, levelData);
        sessions.put(key, session);
        restoreWorld(session, server);
        return level;
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(MinecraftServer server, String name) {
        return (T) ObfuscationReflectionHelper.getPrivateValue(MinecraftServer.class, server, name);
    }

    private static void restoreWorld(Session session, MinecraftServer server) {
        ServerLevel level = session.level;
        for (var chunkTag : session.record.chunks) {
            int chunkX = chunkTag.getInt("x");
            int chunkZ = chunkTag.getInt("z");
            level.getChunk(chunkX, chunkZ);
            var blocks = chunkTag.getList("blocks", 10);
            for (int i = 0; i < blocks.size(); i++) {
                var block = blocks.getCompound(i);
                BlockPos pos = new BlockPos(block.getInt("x"), block.getInt("y"), block.getInt("z"));
                level.setBlock(pos, net.minecraft.nbt.NbtUtils.readBlockState(
                        server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK),
                        block.getCompound("state")), 3);
            }
            var blockEntities = chunkTag.getList("block_entities", 10);
            for (int i = 0; i < blockEntities.size(); i++) {
                var tag = blockEntities.getCompound(i);
                BlockPos pos = new BlockPos(tag.getInt("x"), tag.getInt("y"), tag.getInt("z"));
                var entity = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,
                        level.getBlockState(pos), tag, server.registryAccess());
                if (entity != null) level.setBlockEntity(entity);
            }
        }
        for (var entityTag : session.record.entities) {
            net.minecraft.world.entity.EntityType.loadEntityRecursive(entityTag, level, entity -> {
                level.addFreshEntity(entity);
                return entity;
            });
        }
    }

    public static void play(Session session) {
        resetWorld(session);
        session.playback = 0;
        session.paused = false;
        session.data.setGameTime(session.record.sourceTick);
    }

    private static void resetWorld(Session session) {
        List<net.minecraft.world.entity.Entity> entities = new ArrayList<>();
        session.level.getAllEntities().forEach(entities::add);
        for (var entity : entities) {
            if (!(entity instanceof net.minecraft.server.level.ServerPlayer)) entity.discard();
        }
        Set<BlockPos> changed = new HashSet<>(session.modified);
        for (var chunkTag : session.record.chunks) {
            var blocks = chunkTag.getList("blocks", 10);
            for (int i = 0; i < blocks.size(); i++) {
                var block = blocks.getCompound(i);
                changed.add(new BlockPos(block.getInt("x"), block.getInt("y"), block.getInt("z")));
            }
        }
        for (BlockPos pos : changed) {
            session.level.removeBlockEntity(pos);
            session.level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
        session.modified.clear();
        com.secondition.creeperindustry.content.energy.signal.runtime.SignalRuntimeAccess
                .remove(session.level);
        restoreWorld(session, session.level.getServer());
        session.data.setGameTime(session.record.sourceTick);
    }

    public static void tick(ServerLevel level) {
        Session session = sessions.values().stream().filter(value -> value.level == level).findFirst().orElse(null);
        if (session == null || session.paused) return;
        session.playback++;
        session.data.setGameTime(session.record.sourceTick + session.playback);
        com.secondition.creeperindustry.content.energy.signal.runtime.SignalRuntimeAccess
                .get(level).tick(level);
        if (session.playback >= session.record.slowdown) session.paused = true;
    }

    public static Optional<Session> find(ServerLevel level) {
        return sessions.values().stream().filter(session -> session.level == level).findFirst();
    }

    public static void close(MinecraftServer server) {
        for (var player : new ArrayList<>(players.keySet())) {
            var entity = server.getPlayerList().getPlayer(player);
            if (entity != null) exit(entity);
        }
        sessions.clear();
    }

    public static ResourceKey<Level> key(SnapshotRecord record) {
        return ResourceKey.create(Registries.DIMENSION, record.dimension);
    }

    public static SnapshotRecord record(ServerLevel level) {
        return find(level).map(session -> session.record).orElse(null);
    }


}
