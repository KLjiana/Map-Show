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
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.function.BooleanSupplier;

/// 不允许对其进行setBlockState之类的操作
/// @see LevelChunkSection
public class MapChunkSection {
    private final PalettedContainer<BlockState> states;
    private PalettedContainerRO<Holder<Biome>> biomes;
    private BooleanSupplier hasOnlyAirSupplier = () -> false;

    public MapChunkSection(PalettedContainer<BlockState> states, PalettedContainerRO<Holder<Biome>> biomes) {
        this.states = states;
        this.biomes = biomes;
    }

    /// 仅用于无法实时更新的情况
    public void recalcBlockCounts() {
        MutableInt nonEmptyBlockCount = new MutableInt();
        states.count((state, count) -> {
            if (state.isEmpty() && state.getFluidState().isEmpty()) return;
            nonEmptyBlockCount.add(count);
        });
        boolean hasOnlyAir = nonEmptyBlockCount.intValue() == 0;
        this.hasOnlyAirSupplier = () -> hasOnlyAir;
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

    public boolean hasOnlyAir() {
        return hasOnlyAirSupplier.getAsBoolean();
    }

    public Holder<Biome> getNoiseBiome(int x, int y, int z) {
        return biomes.get(x, y, z);
    }

    /// 不复制直接替换
    public void replaceBiomes(LevelChunkSection levelChunkSection) {
        this.biomes = levelChunkSection.getBiomes();
    }

    public static MapChunkSection fromVanilla(LevelChunkSection levelChunkSection, boolean copy) {
        PalettedContainer<BlockState> states = levelChunkSection.getStates();
        PalettedContainerRO<Holder<Biome>> biomes = levelChunkSection.getBiomes();
        MapChunkSection section;
        if (copy) {
            /// @see ClientboundChunksBiomesPacket.ChunkBiomeData
            byte[] buffer = new byte[biomes.getSerializedSize()];
            FriendlyByteBuf byteBuf = new FriendlyByteBuf(Unpooled.wrappedBuffer(buffer));
            PalettedContainer<Holder<Biome>> neoBiomes = biomes.recreate();
            biomes.write(byteBuf);
            neoBiomes.read(byteBuf);
            section = new MapChunkSection(states.copy(), neoBiomes);
            section.recalcBlockCounts();
        } else {
            section = new MapChunkSection(states, biomes);
            section.hasOnlyAirSupplier = levelChunkSection::hasOnlyAir;
        }
        return section;
    }
}
