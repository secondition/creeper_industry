package com.secondition.creeperindustry.content.snapshot;

import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;

final class SnapshotLevelData extends DerivedLevelData {
    private long gameTime;

    SnapshotLevelData(WorldData worldData, ServerLevelData wrapped, long gameTime) {
        super(worldData, wrapped);
        this.gameTime = gameTime;
    }

    @Override
    public long getGameTime() {
        return gameTime;
    }

    @Override
    public void setGameTime(long time) {
        gameTime = time;
    }
}
