package com.hismeo.map_show.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

/**
 * @see net.minecraft.client.multiplayer.ClientLevel
 */
public class MapLevel implements BlockAndTintGetter, BiomeManager.NoiseBiomeSource {
    protected final ResourceKey<Level> dimension;
    protected final MapChunkCache chunkSource;
    protected final DimensionSpecialEffects effects;
    private final int height;
    private final int minBuildHeight;

    private final BiomeManager biomeManager;
    private final IBlockTintGetter blockTintGetter;
    private final RegistryAccess registryAccess;

    /**
     * 当前玩家所处世界以外的世界渲染
     */
    public MapLevel(MapData data, int viewDistance, long biomeZoomSeed) {
        this.dimension = data.dimension();
        this.chunkSource = new MapChunkCache(this, viewDistance);
        this.effects = data.effects();
        this.height = data.height();
        this.minBuildHeight = data.minBuildHeight();
        this.biomeManager = new BiomeManager(this, biomeZoomSeed);
        this.blockTintGetter = new WithoutLevelBlockTintGetter(pos -> biomeManager.getBiome(pos).value());
        this.registryAccess = data.registryAccess();
    }

    /**
     * 基于当前玩家所处世界进行渲染
     */
    public MapLevel(ClientLevel level, int viewDistance) {
        this.dimension = level.dimension();
        this.chunkSource = new MapChunkCache(this, viewDistance);
        this.effects = level.effects();
        this.height = level.getHeight();
        this.minBuildHeight = level.getMinBuildHeight();
        this.biomeManager = level.getBiomeManager();
        this.blockTintGetter = level::getBlockTint;
        this.registryAccess = level.registryAccess();
    }

    public MapData asData() {
        return new MapData(
                dimension,
                effects,
                height,
                minBuildHeight,
                registryAccess
        );
    }

    public RegistryAccess registryAccess() {
        return registryAccess;
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
        return blockTintGetter.getBlockTint(blockPos, colorResolver);
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

    public void unload(MapChunk chunk) {
        chunkSource.pendingSaveChunks.add(chunk);
        chunkSource.visibleMapChunks.remove(chunk.pos.toLong());
    }

    public void onChunkLoaded(MapChunk chunk) {
        blockTintGetter.onChunkLoaded(chunk.pos);
        chunkSource.pendingSaveChunks.remove(chunk);
        chunkSource.visibleMapChunks.put(chunk.pos.toLong(), chunk);
    }

    /// @see LevelReader#getNoiseBiome(int, int, int)
    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z) {
        MapChunk chunk = chunkSource.getChunk(QuartPos.toSection(x), QuartPos.toSection(z));
        return chunk == null ? registryAccess.holderOrThrow(Biomes.PLAINS) : chunk.getNoiseBiome(x, y, z);
    }
}
