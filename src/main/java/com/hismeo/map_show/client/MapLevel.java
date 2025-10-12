package com.hismeo.map_show.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
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
    protected final ResourceKey<Level> dimension;
    protected final MapChunkCache chunkSource;
    protected final DimensionSpecialEffects effects;
    private final int height;
    private final int minBuildHeight;

    public MapLevel(ResourceKey<Level> dimension, DimensionSpecialEffects effects, int viewDistance, int height, int minBuildHeight) {
        this.dimension = dimension;
        this.chunkSource = new MapChunkCache(this, viewDistance);
        this.effects = effects;
        this.height = height;
        this.minBuildHeight = minBuildHeight;
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        boolean ambientLight = effects.constantAmbientLight();
        if (!shade) {
            return ambientLight ? 0.9F : 1.0F;
        }
        return switch (direction) {
            case DOWN -> ambientLight ? 0.9F : 0.5F;
            case UP -> ambientLight ? 0.9F : 1.0F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.6F;
        };
    }

    @Override
    public LevelLightEngine getLightEngine() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getBrightness(LightLayer lightType, BlockPos blockPos) {
        return 15;
    }

    @Override
    public int getRawBrightness(BlockPos blockPos, int amount) {
        return 15;
    }

    @Override
    public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
        return 0x77DD77; // todo
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
        MapChunk chunk = chunkSource.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
        return chunk == null ? Blocks.BARRIER.defaultBlockState() : chunk.getBlockState(pos);
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
        return minBuildHeight;
    }

    public MapChunkCache getChunkSource() {
        return chunkSource;
    }
}
