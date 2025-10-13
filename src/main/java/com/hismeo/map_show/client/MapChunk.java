package com.hismeo.map_show.client;

import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

/**
 * @see net.minecraft.world.level.chunk.LevelChunk
 */
public class MapChunk implements BlockGetter {
    protected final MapLevel level;
    protected final ChunkPos pos;
    protected final MapChunkSection[] sections;
    protected boolean scheduledForDrop;

    public MapChunk(MapLevel level, ChunkPos pos, MapChunkSection[] sections) {
        this.level = level;
        this.pos = pos;
        this.sections = sections;
    }

    public ChunkPos getPos() {
        return pos;
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        return null;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        int i = pos.getX();
        int j = pos.getY();
        int k = pos.getZ();
        try {
            int l = this.getSectionIndex(j);
            if (l >= 0 && l < this.sections.length) {
                MapChunkSection section = this.sections[l];
                //if (!section.hasOnlyAir()) {
                return section.getBlockState(i & 15, j & 15, k & 15);
                //}
            }

            return Blocks.AIR.defaultBlockState();
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Getting block state");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block being got");
            crashreportcategory.setDetail("Location", () -> CrashReportCategory.formatLocation(this, i, j, k));
            throw new ReportedException(crashreport);
        }
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        try {
            int i = this.getSectionIndex(y);
            if (i >= 0 && i < this.sections.length) {
                MapChunkSection section = this.sections[i];
                //if (!section.hasOnlyAir()) {
                return section.getFluidState(x & 15, y & 15, z & 15);
                //}
            }

            return Fluids.EMPTY.defaultFluidState();
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Getting fluid state");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block being got");
            crashreportcategory.setDetail("Location", () -> CrashReportCategory.formatLocation(this, x, y, z));
            throw new ReportedException(crashreport);
        }
    }

    @Override
    public int getHeight() {
        return level.getHeight();
    }

    @Override
    public int getMinBuildHeight() {
        return level.getMinBuildHeight();
    }

    public static MapChunk fromVanilla(MapLevel level, ChunkAccess chunkAccess, boolean copy) {
        MapChunkSection[] sections = new MapChunkSection[chunkAccess.getSectionsCount()];
        LevelChunkSection[] vanillaSections = chunkAccess.getSections();
        for (int i = 0; i < vanillaSections.length; i++) {
            sections[i] = MapChunkSection.fromVanilla(vanillaSections[i], copy);
        }
        return new MapChunk(level, chunkAccess.getPos(), sections);
    }
}
