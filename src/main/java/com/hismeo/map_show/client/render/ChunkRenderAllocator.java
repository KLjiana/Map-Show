package com.hismeo.map_show.client.render;

import com.hismeo.map_show.client.MapChunk;
import com.hismeo.map_show.client.MapChunkCache;
import com.hismeo.map_show.client.MapLevel;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.Map;

public class ChunkRenderAllocator {
    //TODO
    private ObjectArrayList renderingChunk = new ObjectArrayList<>();
    private boolean rendering = false;
    private BlockRenderAllocator blockRenderAllocator;
    private MapLevel mapLevel;
    private BlockRenderDispatcher blockRenderer;

    public ChunkRenderAllocator(MapLevel mapLevel) {
        this.mapLevel = mapLevel;
    }

    public void setBlockRenderer(BlockRenderDispatcher blockRenderer) {
        this.blockRenderer = blockRenderer;
    }

    public void renderSingleBlock(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, ModelData modelData, RenderType renderType) {
        blockRenderer.renderSingleBlock(state, poseStack, bufferSource, packedLight, packedOverlay, modelData, renderType);
    }

    //TODO CACHE
    protected void renderCurrentChunk(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource) {
        MapChunkCache source = mapLevel.getChunkSource();
        int viewCenterX = source.viewCenterX();
        int viewCenterZ = source.viewCenterZ();
        int radius = source.chunkRadius();
        RandomSource randomsource = RandomSource.create();
        ModelBlockRenderer.enableCaching();
        renderSingleChunk(poseStack, bufferSource, source.getChunk(viewCenterX, viewCenterZ), randomsource);

        //TODO CACHE!!!!
//        for (int dx = -radius; dx <= radius; dx++) {
//            for (int dz = -radius; dz <= radius; dz++) {
//                int chunkX = viewCenterX + dx;
//                int chunkZ = viewCenterZ + dz;
//                MapChunk chunk = source.getChunk(chunkX, chunkZ);
//                if (chunk == null) continue;
//
//                poseStack.pushPose();
//                poseStack.translate(dx * 16, 0, dz * 16);
//                renderSingleChunk(poseStack, bufferSource, chunk, randomsource);
//                poseStack.popPose();
//            }
//        }
        bufferSource.endLastBatch();
        bufferSource.endBatch(RenderType.entitySolid(TextureAtlas.LOCATION_BLOCKS));
        bufferSource.endBatch(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        bufferSource.endBatch(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        bufferSource.endBatch(RenderType.entitySmoothCutout(TextureAtlas.LOCATION_BLOCKS));
        ModelBlockRenderer.clearCache();
    }

    //FIXME y轴重叠问题
    protected void renderSingleChunk(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, MapChunk chunk, RandomSource randomSource) {
        if (chunk != null) {
            BlockPos minPos = chunk.getPos().getWorldPosition().above(mapLevel.getMinBuildHeight());
            BlockPos maxPos = minPos.offset(15, mapLevel.getMaxBuildHeight(), 15);
            for (BlockPos blockpos2 : BlockPos.betweenClosed(minPos, maxPos)) {
                BlockState blockstate = mapLevel.getBlockState(blockpos2);
//                if (blockstate.is(Blocks.AIR)) return;
//                if (blockstate.isSolidRender(mapLevel, blockpos2)) {
//                    visgraph.setOpaque(blockpos2);
//                }

//                if (blockstate.hasBlockEntity()) {
//                    BlockEntity blockentity = mapLevel.getBlockEntity(blockpos2);
//                    if (blockentity != null) {
//                        this.handleBlockEntity(sectioncompiler$results, blockentity);
//                    }
//                }

                FluidState fluidstate = blockstate.getFluidState();
                if (!fluidstate.isEmpty()) {
                    RenderType liquidRendertype = ItemBlockRenderTypes.getRenderLayer(fluidstate);
//                    BufferBuilder bufferbuilder = this.getOrBeginLayer(map, sectionBufferBuilderPack, liquidRendertype);
                    VertexConsumer consumer = bufferSource.getBuffer(liquidRendertype);
                    blockRenderer.renderLiquid(blockpos2, mapLevel, consumer, blockstate, fluidstate);
                    bufferSource.endBatch();
                }

                if (blockstate.getRenderShape() == RenderShape.MODEL) {
                    BakedModel model = blockRenderer.getBlockModel(blockstate);
                    ModelData modelData = mapLevel.getModelData(blockpos2);
                    modelData = model.getModelData(mapLevel, blockpos2, blockstate, modelData);
                    randomSource.setSeed(blockstate.getSeed(blockpos2));

                    for (RenderType blockRendertype : model.getRenderTypes(blockstate, randomSource, modelData)) {
//                        BufferBuilder bufferbuilder1 = this.getOrBeginLayer(map, sectionBufferBuilderPack, blockRendertype);
                        poseStack.pushPose();
                        poseStack.translate((float) SectionPos.sectionRelative(blockpos2.getX()), blockpos2.getY(), (float) SectionPos.sectionRelative(blockpos2.getZ()));
                        VertexConsumer consumer = bufferSource.getBuffer(blockRendertype);
                        blockRenderer.renderBatched(blockstate, blockpos2, mapLevel, poseStack, consumer, true, randomSource, modelData, blockRendertype);
                        poseStack.popPose();
                    }
                }
            }
        }
    }

    //Should we need?
    private BufferBuilder getOrBeginLayer(Map<RenderType, BufferBuilder> bufferLayers, SectionBufferBuilderPack sectionBufferBuilderPack, RenderType renderType) {
        BufferBuilder bufferbuilder = bufferLayers.get(renderType);
        if (bufferbuilder == null) {
            ByteBufferBuilder bytebufferbuilder = sectionBufferBuilderPack.buffer(renderType);
            bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
            bufferLayers.put(renderType, bufferbuilder);
        }

        return bufferbuilder;
    }
}
