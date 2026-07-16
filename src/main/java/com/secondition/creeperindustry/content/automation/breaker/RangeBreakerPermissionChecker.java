package com.secondition.creeperindustry.content.automation.breaker;

import com.mojang.authlib.GameProfile;
import com.secondition.creeperindustry.CITags;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayerFactory;

public final class RangeBreakerPermissionChecker {
    private static final GameProfile BREAKER_PROFILE = new GameProfile(
            UUID.fromString("35f1c8fb-73fa-4b73-aec5-bf291ea16415"),
            "[CI Breaker]"
    );

    public boolean isStructurallyBreakable(ServerLevel level, BlockPos pos, BlockState state) {
        return !state.isAir()
                && !(state.getBlock() instanceof LiquidBlock)
                && !state.is(CITags.Blocks.SIGNAL_RANGE_BREAKER_IMMUNE)
                && state.getDestroySpeed(level, pos) >= 0.0F;
    }

    public boolean mayBreak(ServerLevel level, BlockPos machinePos, BlockPos targetPos, BlockState targetState) {
        ServerPlayer breaker = breakerActor(level, machinePos);
        return !CommonHooks.fireBlockBreak(level, GameType.SURVIVAL, breaker, targetPos, targetState).isCanceled();
    }

    public ServerPlayer breakerActor(ServerLevel level, BlockPos machinePos) {
        ServerPlayer breaker = FakePlayerFactory.get(level, BREAKER_PROFILE);
        breaker.setPos(machinePos.getX() + 0.5D, machinePos.getY() + 0.5D, machinePos.getZ() + 0.5D);
        return breaker;
    }
}
