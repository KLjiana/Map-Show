package com.hismeo.map_show.client;

import com.mojang.serialization.Codec;
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
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.IOWorker;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/// @see ChunkSerializer
/// @see net.minecraft.world.level.chunk.storage.ChunkStorage
public class MapSerializer {
    private final IOWorker worker;

    public MapSerializer(String levelName, ResourceKey<Level> dimension, Path folder) {
        this.worker = new IOWorker(new RegionStorageInfo(levelName, dimension, "map"), folder, false); // 即不使用DSYNC
    }

    /// @see ChunkMap#scheduleChunkLoad(ChunkPos)
    private CompletableFuture<@Nullable MapChunk> scheduleChunkLoad(MapLevel level, ChunkPos chunkPos) {
        return worker.loadAsync(chunkPos).thenApplyAsync(tag ->
                tag.map(compoundTag ->
                        read(level, chunkPos, compoundTag, level.registryAccess())
                ).orElse(null), Util.ioPool() // todo 评估ioPool
        );
    }

    /// @see ChunkMap#save(ChunkAccess)
    public void save(MapChunk chunk, RegistryAccess registryAccess) {
        try {
            worker.store(chunk.getPos(), write(chunk, registryAccess)).exceptionally(p_351776_ -> null);
        } catch (Exception ignored) {}
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

            sections[i] = new MapChunkSection(states, biomes);
        }

        return new MapChunk(level, pos, sections);
    }

    private static CompoundTag write(MapChunk chunk, RegistryAccess registryAccess) {
        ChunkPos chunkpos = chunk.getPos();
        CompoundTag chunkTag = new CompoundTag();
        chunkTag.putInt("xPos", chunkpos.x);
        chunkTag.putInt("zPos", chunkpos.z);

        MapChunkSection[] sections = chunk.sections;
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
