package com.hismeo.map_show.client.screen;

import com.hismeo.map_show.client.ClientMap;
import com.hismeo.map_show.client.MapChunk;
import com.hismeo.map_show.client.MapChunkCache;
import com.hismeo.map_show.client.MapLevel;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

/**
 * @see com.hismeo.map_show.client.ClientMap#getCurrentLevel(ClientLevel) 获取MapLevel
 */
public class MapScreen extends Screen {
    private final LerpStorage xTran = new LerpStorage(0.0f);
    private final LerpStorage yTran = new LerpStorage(0.0f);
    private final LerpStorage zTran = new LerpStorage(0.0f);
    private final LerpStorage xRot = new LerpStorage(0.0f);
    private final LerpStorage yRot = new LerpStorage(0.0f);
    private final LerpStorage zRot = new LerpStorage(0.0f);
    private final LerpStorage scale = new LerpStorage(0.0f);
    private final MapLevel mapLevel;
    private BlockRenderDispatcher blockRenderer;
    private MultiBufferSource.BufferSource bufferSource;

    public MapScreen(@NotNull MapLevel mapLevel) {
        //TODO title补齐
        super(CommonComponents.EMPTY);
        this.mapLevel = mapLevel;
    }

    @Override
    protected void init() {
        super.init();
        this.blockRenderer = minecraft.getBlockRenderer();
        this.bufferSource = minecraft.renderBuffers().bufferSource();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        PoseStack poseStack = guiGraphics.pose();
        poseStack.popPose();
        renderCurrentChunk(poseStack);
        poseStack.pushPose();
    }

    protected void renderCurrentChunk(PoseStack poseStack) {
        MapChunkCache source = mapLevel.getChunkSource();
        MapChunk chunk = source.getChunk(source.viewCenterX(), source.viewCenterZ());
        RandomSource randomsource = RandomSource.create();
        if (chunk != null) {
            BlockPos minPos = chunk.getPos().getWorldPosition().above(mapLevel.getMinBuildHeight());
            BlockPos maxPos = minPos.offset(15, mapLevel.getMaxBuildHeight(), 15);
            for(BlockPos blockpos2 : BlockPos.betweenClosed(minPos, maxPos)) {
                BlockState blockstate = mapLevel.getBlockState(blockpos2);
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
                    this.blockRenderer.renderLiquid(blockpos2, mapLevel, consumer, blockstate, fluidstate);
                }

                if (blockstate.getRenderShape() == RenderShape.MODEL) {
                    BakedModel model = this.blockRenderer.getBlockModel(blockstate);
                    ModelData modelData = mapLevel.getModelData(blockpos2);
                    modelData = model.getModelData(mapLevel, blockpos2, blockstate, modelData);
                    randomsource.setSeed(blockstate.getSeed(blockpos2));

                    for(RenderType blockRendertype : model.getRenderTypes(blockstate, randomsource, modelData)) {
//                        BufferBuilder bufferbuilder1 = this.getOrBeginLayer(map, sectionBufferBuilderPack, blockRendertype);
                        poseStack.pushPose();
                        poseStack.translate((float) SectionPos.sectionRelative(blockpos2.getX()), (float)SectionPos.sectionRelative(blockpos2.getY()), (float)SectionPos.sectionRelative(blockpos2.getZ()));
                        VertexConsumer consumer = bufferSource.getBuffer(blockRendertype);
                        this.blockRenderer.renderBatched(blockstate, blockpos2, mapLevel, poseStack, consumer, true, randomsource, modelData, blockRendertype);
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
