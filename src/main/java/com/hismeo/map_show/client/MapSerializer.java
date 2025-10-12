package com.hismeo.map_show.client;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;

import java.util.Objects;

/**
 * @see net.minecraft.world.level.chunk.storage.ChunkSerializer
 */
public class MapSerializer {
    public static MapChunk read(MapLevel level, ChunkPos pos, CompoundTag chunkTag) {
        ChunkPos chunkpos = new ChunkPos(chunkTag.getInt("xPos"), chunkTag.getInt("zPos"));
        if (!Objects.equals(pos, chunkpos)) {
            throw new IllegalArgumentException();
        }

        ListTag sectionsTag = chunkTag.getList("sections", Tag.TAG_COMPOUND);
        MapChunkSection[] sections = new MapChunkSection[level.getSectionsCount()];

        for (int i = 0; i < sectionsTag.size(); i++) {
            CompoundTag sectionTag = sectionsTag.getCompound(i);
            PalettedContainer<BlockState> container;
            if (sectionTag.contains("block_states", Tag.TAG_COMPOUND)) {
                container = ChunkSerializer.BLOCK_STATE_CODEC.parse(NbtOps.INSTANCE, sectionTag.getCompound("block_states"))
                        .getOrThrow(ChunkSerializer.ChunkReadException::new);
            } else {
                container = new PalettedContainer<>(
                        Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES
                );
            }

            sections[i] = new MapChunkSection(container);
        }

        return new MapChunk(level, pos, sections);
    }

    public static CompoundTag write(MapChunk chunk) {
        ChunkPos chunkpos = chunk.getPos();
        CompoundTag chunkTag = new CompoundTag();
        chunkTag.putInt("xPos", chunkpos.x);
        chunkTag.putInt("zPos", chunkpos.z);

        MapChunkSection[] sections = chunk.sections;
        ListTag sectionsTag = new ListTag();

        for (MapChunkSection section : sections) {
            CompoundTag sectionTag = new CompoundTag();
            sectionTag.put("block_states", ChunkSerializer.BLOCK_STATE_CODEC.encodeStart(NbtOps.INSTANCE, section.states()).getOrThrow());
            sectionsTag.add(sectionTag);
        }

        chunkTag.put("sections", sectionsTag);
        return chunkTag;
    }
}
