package com.hismeo.map_show;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;

public class MapUtils {
    /**
     * 将绝对坐标压缩为相对坐标
     */
    public static int compressRelativePos(BlockPos pos) {
        return ((pos.getX() & 0xF) << 16) | ((pos.getY() + 2048) << 4) | (pos.getZ() & 0xF);
    }

    /**
     * 将相对坐标解压为绝对坐标
     */
    public static BlockPos decompressRelativePos(ChunkPos chunkPos, int compressed) {
        int x = (compressed >>> 16) & 0xF;
        int y = ((compressed >>> 4) & 0xFFF) - 2048;
        int z = compressed & 0xF;
        return chunkPos.getBlockAt(x, y, z);
    }

    public static PalettedContainer<BlockState> decodeChunk(ChunkPos pos, CompoundTag tag) {
        if (tag.contains("block_states", 10)) {
            return ChunkSerializer.BLOCK_STATE_CODEC.parse(NbtOps.INSTANCE, tag.getCompound("block_states"))
                    .promotePartial(p_188283_ -> MapShow.LOGGER.error("Unable to decode chunk {}", pos))
                    .getOrThrow(ChunkSerializer.ChunkReadException::new);
        } else {
            return new PalettedContainer<>(
                    Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES
            );
        }
    }

    public static void encodeChunk(PalettedContainer<BlockState> container, CompoundTag tag) {
        tag.put("block_states", ChunkSerializer.BLOCK_STATE_CODEC.encodeStart(NbtOps.INSTANCE, container).getOrThrow());
    }
}
