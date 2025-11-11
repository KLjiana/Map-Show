package com.hismeo.map_show.client;

import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.material.FluidState;

import java.util.function.Predicate;

/// @see LevelChunkSection
public class MapChunkSection {
    private final PalettedContainer<BlockState> states;
    private PalettedContainerRO<Holder<Biome>> biomes;

    public MapChunkSection(PalettedContainer<BlockState> states, PalettedContainerRO<Holder<Biome>> biomes) {
        this.states = states;
        this.biomes = biomes;
    }

    public PalettedContainer<BlockState> getStates() {
        return states;
    }

    public PalettedContainerRO<Holder<Biome>> getBiomes() {
        return biomes;
    }

    public BlockState getBlockState(int x, int y, int z) {
        return states.get(x, y, z);
    }

    public FluidState getFluidState(int x, int y, int z) {
        return states.get(x, y, z).getFluidState();
    }

    public void acquire() {
        states.acquire();
    }

    public void release() {
        states.release();
    }

    public BlockState setBlockState(int x, int y, int z, BlockState state) {
        return setBlockState(x, y, z, state, true);
    }

    public BlockState setBlockState(int x, int y, int z, BlockState state, boolean useLocks) {
        if (useLocks) {
            return states.getAndSet(x, y, z, state);
        } else {
            return states.getAndSetUnchecked(x, y, z, state);
        }
    }

    public int getSerializedSize() {
        return LevelChunkSection.BIOME_CONTAINER_BITS + states.getSerializedSize() + biomes.getSerializedSize();
    }

    public boolean maybeHas(Predicate<BlockState> predicate) {
        return states.maybeHas(predicate);
    }

    public Holder<Biome> getNoiseBiome(int x, int y, int z) {
        return biomes.get(x, y, z);
    }

    /// 不复制直接替换
    public void replaceBiomes(LevelChunkSection levelChunkSection) {
        this.biomes = levelChunkSection.getBiomes();
    }

    public static MapChunkSection fromVanilla(LevelChunkSection levelChunkSection, boolean copy) {
        PalettedContainerRO<Holder<Biome>> biomes = levelChunkSection.getBiomes();
        if (copy) {
            /// @see ClientboundChunksBiomesPacket.ChunkBiomeData
            byte[] buffer = new byte[biomes.getSerializedSize()];
            FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(buffer));
            PalettedContainer<Holder<Biome>> neoBiomes = biomes.recreate();
            biomes.write(byteBuf);
            neoBiomes.read(byteBuf);
            return new MapChunkSection(levelChunkSection.getStates().copy(), neoBiomes);
        }
        return new MapChunkSection(levelChunkSection.getStates(), biomes);
    }
}
