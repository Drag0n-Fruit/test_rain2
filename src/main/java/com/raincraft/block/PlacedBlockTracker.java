package com.raincraft.block;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

public class PlacedBlockTracker {
    public static void mark(ServerWorld world, BlockPos pos) {
        CollapseManager.get(world).markPlaced(pos);
    }

    public static void unmark(ServerWorld world, BlockPos pos) {
        CollapseManager.get(world).unmark(pos);
    }
}
