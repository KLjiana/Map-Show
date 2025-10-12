package com.hismeo.map_show.client.screen;

import com.hismeo.map_show.client.MapChunk;
import com.hismeo.map_show.client.MapChunkCache;
import com.hismeo.map_show.client.MapLevel;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.Map;

/**
 * @see com.hismeo.map_show.client.ClientMap#getCurrentLevel(ClientLevel) 获取MapLevel
 */
public class MapScreen extends Screen {
    private final static float ROT_SPEED = 0.5f;
    private final static float TRAN_SPEED = 1.0f;
    private final LerpStorage xTran = new LerpStorage(0.0f);
    private final LerpStorage yTran = new LerpStorage(0.0f);
    private final LerpStorage xRot = new LerpStorage(45.0f);
    private final LerpStorage yRot = new LerpStorage(25.0f);
    private final LerpStorage scale = new LerpStorage(20.0f);
    private double lastMouseX;
    private double lastMouseY;
    private final MapLevel mapLevel;
    private BlockRenderDispatcher blockRenderer;

    public MapScreen(@NotNull MapLevel mapLevel) {
        //TODO title补齐
        super(CommonComponents.EMPTY);
        this.mapLevel = mapLevel;
    }

    @Override
    protected void init() {
        super.init();
        this.blockRenderer = minecraft.getBlockRenderer();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderDebugMessage(guiGraphics);
        PoseStack poseStack = guiGraphics.pose();
        MultiBufferSource.BufferSource bufferSource = guiGraphics.bufferSource();

        poseStack.pushPose();

        poseStack.translate(width / 2f + xTran.lerp(partialTick), height / 2f + yTran.lerp(partialTick), 0);

        float s = scale.lerp(partialTick);
        poseStack.scale(s, -s, s);
        poseStack.mulPose(Axis.XP.rotationDegrees(yRot.lerp(partialTick)));
        poseStack.mulPose(Axis.YP.rotationDegrees(xRot.lerp(partialTick)));

//        renderDebugSingleBlock(poseStack, bufferSource);
        renderCurrentChunk(poseStack, bufferSource);

        poseStack.popPose();
    }

    protected void renderDebugSingleBlock(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource) {
        blockRenderer.renderSingleBlock(Blocks.GRASS_BLOCK.defaultBlockState(), poseStack, bufferSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        bufferSource.endBatch();
//        testQuad(bufferSource, poseStack);
    }

    protected void renderDebugMessage(GuiGraphics guiGraphics) {
        if (FMLLoader.isProduction()) return;
        guiGraphics.drawString(font, "xTran: %s, %s".formatted(xTran.getNumber(), xTran.getoNumber()), 0, 0, 0xFFFFFF, false);
        guiGraphics.drawString(font, "yTran: %s, %s".formatted(yTran.getNumber(), yTran.getoNumber()), 0, 10, 0xFFFFFF, false);
        guiGraphics.drawString(font, "xRot: %s, %s".formatted(xRot.getNumber(), xRot.getoNumber()), 0, 20, 0xFFFFFF, false);
        guiGraphics.drawString(font, "yRot: %s, %s".formatted(yRot.getNumber(), yRot.getoNumber()), 0, 30, 0xFFFFFF, false);
        guiGraphics.drawString(font, "scale: %s, %s".formatted(scale.getNumber(), scale.getoNumber()), 0, 40, 0xFFFFFF, false);
    }

    private void testQuad(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.DEBUG_QUADS);
        PoseStack.Pose last = poseStack.last();
        consumer.addVertex(last, 0.0f, 0.0f, 0.0f).setColor(1.0F, 1.0F, 1.0F, 1.0F);
        consumer.addVertex(last, -5f, 0.0f, 0.0f).setColor(1.0F, 1.0F, 1.0F, 1.0F);
        consumer.addVertex(last, -5f, -5f, 0.0f).setColor(1.0F, 1.0F, 1.0F, 1.0F);
        consumer.addVertex(last, 0.0f, -5f, 0.0f).setColor(1.0F, 1.0F, 1.0F, 1.0F);
        bufferSource.endBatch();
    }

    //TODO CACHE
    protected void renderCurrentChunk(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource) {
        MapChunkCache source = mapLevel.getChunkSource();
        int viewCenterX = source.viewCenterX();
        int viewCenterZ = source.viewCenterZ();
        int radius = source.chunkRadius();
        RandomSource randomsource = RandomSource.create();
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
                    bufferSource.endBatch(liquidRendertype);
                }

                if (blockstate.getRenderShape() == RenderShape.MODEL) {
                    BakedModel model = blockRenderer.getBlockModel(blockstate);
                    ModelData modelData = mapLevel.getModelData(blockpos2);
                    modelData = model.getModelData(mapLevel, blockpos2, blockstate, modelData);
                    randomSource.setSeed(blockstate.getSeed(blockpos2));

                    for (RenderType blockRendertype : model.getRenderTypes(blockstate, randomSource, modelData)) {
//                        BufferBuilder bufferbuilder1 = this.getOrBeginLayer(map, sectionBufferBuilderPack, blockRendertype);
                        poseStack.pushPose();
                        poseStack.translate((float) SectionPos.sectionRelative(blockpos2.getX()), (float) SectionPos.sectionRelative(blockpos2.getY()), (float) SectionPos.sectionRelative(blockpos2.getZ()));
                        VertexConsumer consumer = bufferSource.getBuffer(blockRendertype);
                        blockRenderer.renderBatched(blockstate, blockpos2, mapLevel, poseStack, consumer, true, randomSource, modelData, blockRendertype);
                        bufferSource.endBatch(blockRendertype);
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        double deltaX = mouseX - lastMouseX;
        double deltaY = mouseY - lastMouseY;

        if (button == 0) {
            xTran.updateNumber(xTran.getNumber() + (float) (deltaX * TRAN_SPEED));
            yTran.updateNumber(yTran.getNumber() + (float) (deltaY * TRAN_SPEED));
        }

        if (button == 1) {
            xRot.updateNumber(xRot.getNumber() + (float) (deltaX * ROT_SPEED));
            yRot.updateNumber(yRot.getNumber() + (float) (deltaY * ROT_SPEED));
            yRot.updateNumber(Math.clamp(yRot.getNumber(), -60, 90));
        }

        lastMouseX = mouseX;
        lastMouseY = mouseY;

        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        float newScale = scale.getNumber() * (1 + (float) scrollY * 0.1f);
        scale.updateNumber(Math.clamp(newScale, 5f, 60f));
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
