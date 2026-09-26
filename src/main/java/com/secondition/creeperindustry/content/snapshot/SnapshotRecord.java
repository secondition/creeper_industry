package com.secondition.creeperindustry.content.snapshot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class SnapshotRecord {
    final UUID id;
    final ResourceLocation sourceDimension;
    final ResourceLocation dimension;
    final ChunkPos center;
    final long sourceTick;
    final int slowdown;
    final int radius;
    final long createdAt;
    final List<CompoundTag> chunks;
    final List<CompoundTag> entities;

    SnapshotRecord(UUID id, ResourceLocation sourceDimension, ResourceLocation dimension,
            ChunkPos center, long sourceTick, int slowdown, int radius, long createdAt,
            List<CompoundTag> chunks, List<CompoundTag> entities) {
        this.id = id;
        this.sourceDimension = sourceDimension;
        this.dimension = dimension;
        this.center = center;
        this.sourceTick = sourceTick;
        this.slowdown = slowdown;
        this.radius = radius;
        this.createdAt = createdAt;
        this.chunks = new ArrayList<>(chunks);
        this.entities = new ArrayList<>(entities);
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        tag.putString("source_dimension", sourceDimension.toString());
        tag.putString("dimension", dimension.toString());
        tag.putInt("center_x", center.x);
        tag.putInt("center_z", center.z);
        tag.putLong("source_tick", sourceTick);
        tag.putInt("slowdown", slowdown);
        tag.putInt("radius", radius);
        tag.putLong("created_at", createdAt);
        ListTag chunkTags = new ListTag();
        chunks.forEach(chunk -> chunkTags.add(chunk.copy()));
        tag.put("chunks", chunkTags);
        ListTag entityTags = new ListTag();
        entities.forEach(entity -> entityTags.add(entity.copy()));
        tag.put("entities", entityTags);
        return tag;
    }

    static SnapshotRecord load(CompoundTag tag) {
        List<CompoundTag> chunks = new ArrayList<>();
        for (int i = 0; i < tag.getList("chunks", 10).size(); i++) {
            chunks.add(tag.getList("chunks", 10).getCompound(i).copy());
        }
        List<CompoundTag> entities = new ArrayList<>();
        for (int i = 0; i < tag.getList("entities", 10).size(); i++) {
            entities.add(tag.getList("entities", 10).getCompound(i).copy());
        }
        return new SnapshotRecord(
                tag.getUUID("id"),
                ResourceLocation.parse(tag.getString("source_dimension")),
                ResourceLocation.parse(tag.getString("dimension")),
                new ChunkPos(tag.getInt("center_x"), tag.getInt("center_z")),
                tag.getLong("source_tick"),
                tag.getInt("slowdown"),
                tag.contains("radius") ? tag.getInt("radius") : 4,
                tag.getLong("created_at"),
                chunks,
                entities);
    }
}
