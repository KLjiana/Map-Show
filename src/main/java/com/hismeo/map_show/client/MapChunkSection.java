package com.hismeo.map_show.client;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.material.FluidState;

import java.util.function.Predicate;

/**
 * @see net.minecraft.world.level.chunk.LevelChunkSection
 */
public record MapChunkSection(PalettedContainer<BlockState> states) {
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
        return states.getSerializedSize();
    }

    public boolean maybeHas(Predicate<BlockState> predicate) {
        return states.maybeHas(predicate);
    }

    public static MapChunkSection fromVanilla(LevelChunkSection levelChunkSection, boolean copy) {
        PalettedContainer<BlockState> states = levelChunkSection.getStates();
        return new MapChunkSection(copy ? states.copy() : states);
    }
}
