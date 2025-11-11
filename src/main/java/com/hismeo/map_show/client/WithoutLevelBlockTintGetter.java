package com.hismeo.map_show.client;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Cursor3D;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.client.ColorResolverManager;

public class WithoutLevelBlockTintGetter implements IBlockTintGetter {
    /**
     * @see ClientLevel#tintCaches
     */
    private final Object2ObjectArrayMap<ColorResolver, BlockTintCache> tintCaches = Util.make(new Object2ObjectArrayMap<>(3), map -> {
        map.put(BiomeColors.GRASS_COLOR_RESOLVER, new BlockTintCache(pos -> calculateBlockTint(pos, BiomeColors.GRASS_COLOR_RESOLVER)));
        map.put(BiomeColors.FOLIAGE_COLOR_RESOLVER, new BlockTintCache(pos -> calculateBlockTint(pos, BiomeColors.FOLIAGE_COLOR_RESOLVER)));
        map.put(BiomeColors.WATER_COLOR_RESOLVER, new BlockTintCache(pos -> calculateBlockTint(pos, BiomeColors.WATER_COLOR_RESOLVER)));

        for (ColorResolver resolver : ColorResolverManager.getRegisteredResolvers()) {
            map.put(resolver, new BlockTintCache(pos -> calculateBlockTint(pos, resolver)));
        }
    });
    private final BiomeGetter biomeGetter;

    public WithoutLevelBlockTintGetter(BiomeGetter biomeGetter) {
        this.biomeGetter = biomeGetter;
    }

    /**
     * @see ClientLevel#getBlockTint(BlockPos, ColorResolver)
     */
    @Override
    public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
        return tintCaches.get(colorResolver).getColor(blockPos);
    }

    /**
     * @see ClientLevel#onChunkLoaded(ChunkPos)
     */
    @Override
    public void onChunkLoaded(ChunkPos chunkPos) {
        tintCaches.forEach((resolver, cache) -> cache.invalidateForChunk(chunkPos.x, chunkPos.z));
    }

    /**
     * @see ClientLevel#clearTintCaches()
     */
    @Override
    public void clearTintCaches() {
        tintCaches.forEach((resolver, cache) -> cache.invalidateAll());
    }

    /**
     * @see ClientLevel#calculateBlockTint(BlockPos, ColorResolver)
     */
    private int calculateBlockTint(BlockPos pos, ColorResolver resolver) {
        int radius = Minecraft.getInstance().options.biomeBlendRadius().get();
        if (radius == 0) {
            return resolver.getColor(biomeGetter.getBiome(pos), pos.getX(), pos.getZ());
        }
        int diameterSqr = Mth.square(radius * 2 + 1);
        int r = 0;
        int g = 0;
        int b = 0;
        Cursor3D cursor3d = new Cursor3D(pos.getX() - radius, pos.getY(), pos.getZ() - radius, pos.getX() + radius, pos.getY(), pos.getZ() + radius);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        while (cursor3d.advance()) {
            mutable.set(cursor3d.nextX(), cursor3d.nextY(), cursor3d.nextZ());
            int color = resolver.getColor(biomeGetter.getBiome(mutable), mutable.getX(), mutable.getZ());
            r += (color & 0xFF0000) >> 16;
            g += (color & 0xFF00) >> 8;
            b += color & 0xFF;
        }

        return (r / diameterSqr & 0xFF) << 16 | (g / diameterSqr & 0xFF) << 8 | b / diameterSqr & 0xFF;
    }

    @FunctionalInterface
    public interface BiomeGetter {
        Biome getBiome(BlockPos pos);
    }
}
