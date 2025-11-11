package com.hismeo.map_show.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ColorResolver;

@FunctionalInterface
public interface IBlockTintGetter {
    int getBlockTint(BlockPos blockPos, ColorResolver colorResolver);

    default void onChunkLoaded(ChunkPos chunkPos) {}

    default void clearTintCaches() {}
}
