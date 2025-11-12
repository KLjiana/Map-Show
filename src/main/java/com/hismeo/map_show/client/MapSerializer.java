package com.hismeo.map_show.client;

import com.hismeo.map_show.MapShow;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.ChunkStorage;
import net.minecraft.world.level.chunk.storage.IOWorker;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/// @see ChunkSerializer
/// @see net.minecraft.world.level.chunk.storage.ChunkStorage
public class MapSerializer implements AutoCloseable {
    public static final MapChunk UNKNOWN_MAP_CHUNK = new MapChunk(null, null, null);
    public static final CompletableFuture<MapChunk> FAILED_TO_LOAD_MAP_CHUNK = CompletableFuture.completedFuture(UNKNOWN_MAP_CHUNK);
    private final IOWorker worker;

    public MapSerializer(String levelName, ResourceKey<Level> dimension, Path folder) {
        this.worker = new IOWorker(new RegionStorageInfo(levelName, dimension, "map"), folder, false); // 即不使用DSYNC
    }

    /// @return 返回加载任务，如果不存在该区块则返回{@link MapSerializer#UNKNOWN_MAP_CHUNK}
    ///         如果加载失败则返回{@link MapSerializer#FAILED_TO_LOAD_MAP_CHUNK}
    /// @see ChunkMap#scheduleChunkLoad(ChunkPos)
    public CompletableFuture<MapChunk> scheduleChunkLoad(MapLevel level, ChunkPos chunkPos) {
        return worker.loadAsync(chunkPos).thenApplyAsync(tag ->
                tag.map(compoundTag ->
                        read(level, chunkPos, compoundTag, level.registryAccess())
                ).orElse(UNKNOWN_MAP_CHUNK), Util.ioPool() // todo 评估ioPool
        ).exceptionallyCompose(throwable -> {
            MapShow.LOGGER.warn("Error reading MapChunk [{}, {}]", chunkPos.x, chunkPos.z, throwable);
            return MapSerializer.FAILED_TO_LOAD_MAP_CHUNK;
        });
    }

    /// @see ChunkMap#scheduleUnload(long, ChunkHolder)
    public void scheduleUnload(MapChunk chunk) {
        /// 从原版创建的chunk才需要保存
        if (chunk.isFromVanilla()) {
            try {
                CompoundTag tag = write(chunk.pos, chunk.sections, chunk.level.registryAccess());
                worker.store(chunk.pos, tag).exceptionally(throwable -> {
                    MapShow.LOGGER.warn("Error saving MapChunk [{}, {}]", chunk.pos.x, chunk.pos.z, throwable);
                    return null;
                });
            } catch (Exception ignored) {}
        }
    }

    /// @param flush 退出存档为true，其它情况为false
    /// @see ChunkMap#saveAllChunks(boolean)
    public void saveAllChunks(MapLevel level, boolean flush) {
        level.chunkSource.pendingSaveChunks.clear();
        for (Long2ObjectMap.Entry<MapChunk> entry : level.chunkSource.visibleMapChunks.long2ObjectEntrySet()) {
            scheduleUnload(entry.getValue());
        }
        if (flush) {
            /// @see ChunkStorage#flushWorker()
            worker.synchronize(true).join();
        }
    }

    @Override
    public void close() throws IOException {
        worker.close();
    }

    private static MapChunk read(MapLevel level, ChunkPos pos, CompoundTag chunkTag, RegistryAccess registryAccess) {
        ChunkPos chunkpos = new ChunkPos(chunkTag.getInt("xPos"), chunkTag.getInt("zPos"));
        if (!Objects.equals(pos, chunkpos)) {
            throw new IllegalArgumentException();
        }

        ListTag sectionsTag = chunkTag.getList("sections", Tag.TAG_COMPOUND);
        MapChunkSection[] sections = new MapChunkSection[level.getSectionsCount()];

        Registry<Biome> biomeRegistry = registryAccess.registryOrThrow(Registries.BIOME);
        Codec<PalettedContainerRO<Holder<Biome>>> biomeCodec = ChunkSerializer.makeBiomeCodec(biomeRegistry);

        for (int i = 0; i < sectionsTag.size(); i++) {
            CompoundTag sectionTag = sectionsTag.getCompound(i);

            PalettedContainer<BlockState> states;
            if (sectionTag.contains("block_states", Tag.TAG_COMPOUND)) {
                states = ChunkSerializer.BLOCK_STATE_CODEC.parse(NbtOps.INSTANCE, sectionTag.getCompound("block_states")).getOrThrow(ChunkSerializer.ChunkReadException::new);
            } else {
                states = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES);
            }

            PalettedContainerRO<Holder<Biome>> biomes;
            if (sectionTag.contains("biomes", Tag.TAG_COMPOUND)) {
                biomes = biomeCodec.parse(NbtOps.INSTANCE, sectionTag.getCompound("biomes")).getOrThrow(ChunkSerializer.ChunkReadException::new);
            } else {
                biomes = new PalettedContainer<>(biomeRegistry.asHolderIdMap(), biomeRegistry.getHolderOrThrow(Biomes.PLAINS), PalettedContainer.Strategy.SECTION_BIOMES);
            }

            MapChunkSection section = new MapChunkSection(states, biomes);
            section.recalcBlockCounts();
            sections[i] = section;
        }

        return new MapChunk(level, pos, sections);
    }

    private static CompoundTag write(ChunkPos pos, MapChunkSection[] sections, RegistryAccess registryAccess) {
        CompoundTag chunkTag = new CompoundTag();
        chunkTag.putInt("xPos", pos.x);
        chunkTag.putInt("zPos", pos.z);

        ListTag sectionsTag = new ListTag();

        Registry<Biome> biomeRegistry = registryAccess.registryOrThrow(Registries.BIOME);
        Codec<PalettedContainerRO<Holder<Biome>>> biomeCodec = ChunkSerializer.makeBiomeCodec(biomeRegistry);

        for (MapChunkSection section : sections) {
            CompoundTag sectionTag = new CompoundTag();

            sectionTag.put("block_states", ChunkSerializer.BLOCK_STATE_CODEC.encodeStart(NbtOps.INSTANCE, section.getStates()).getOrThrow());
            sectionTag.put("biomes", biomeCodec.encodeStart(NbtOps.INSTANCE, section.getBiomes()).getOrThrow());

            sectionsTag.add(sectionTag);
        }

        chunkTag.put("sections", sectionsTag);
        return chunkTag;
    }
}
