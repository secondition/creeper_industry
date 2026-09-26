package com.secondition.creeperindustry.content.snapshot;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class SnapshotSavedData extends SavedData {
    static final String NAME = "creeper_industry_snapshots";
    private final Map<UUID, SnapshotRecord> records = new LinkedHashMap<>();

    static Factory<SnapshotSavedData> factory() {
        return new Factory<>(SnapshotSavedData::new, SnapshotSavedData::load);
    }

    static SnapshotSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        SnapshotSavedData data = new SnapshotSavedData();
        ListTag snapshots = tag.getList("snapshots", 10);
        for (int i = 0; i < snapshots.size(); i++) {
            SnapshotRecord record = SnapshotRecord.load(snapshots.getCompound(i));
            data.records.put(record.id, record);
        }
        return data;
    }

    Map<UUID, SnapshotRecord> records() {
        return records;
    }

    void put(SnapshotRecord record) {
        records.put(record.id, record);
        setDirty();
    }

    void remove(UUID id) {
        if (records.remove(id) != null) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag snapshots = new ListTag();
        records.values().forEach(record -> snapshots.add(record.save()));
        tag.put("snapshots", snapshots);
        return tag;
    }
}
