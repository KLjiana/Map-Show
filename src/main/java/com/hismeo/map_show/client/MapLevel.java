package com.hismeo.map_show.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

/**
 * @see net.minecraft.client.multiplayer.ClientLevel
 */
public class MapLevel implements BlockAndTintGetter {
    private final ResourceKey<Level> dimension;
    private final MapChunkCache chunkSource;
    private final DimensionSpecialEffects effects;
    private final int height;
    private final int minY;

    public MapLevel(ResourceKey<Level> dimension, DimensionSpecialEffects effects, int viewDistance, int height, int minY) {
        this.dimension = dimension;
        this.chunkSource = new MapChunkCache(this, viewDistance);
        this.effects = effects;
        this.height = height;
        this.minY = minY;
    }

    public void unload(MapChunk chunk) {}

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        boolean flag = effects.constantAmbientLight();
        if (!shade) {
            return flag ? 0.9F : 1.0F;
        }
        return switch (direction) {
            case DOWN -> flag ? 0.9F : 0.5F;
            case UP -> flag ? 0.9F : 1.0F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.6F;
        };
    }

    @Override
    public LevelLightEngine getLightEngine() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
        return 0; // todo
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        return null; // todo
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        if (isOutsideBuildHeight(pos)) {
            return Blocks.VOID_AIR.defaultBlockState();
        }
        return chunkSource.getChunk(pos.getX() >> 4, pos.getZ() >> 4).getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getMinBuildHeight() {
        return minY;
    }
}
