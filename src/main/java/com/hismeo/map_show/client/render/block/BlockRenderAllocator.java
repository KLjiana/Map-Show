package com.hismeo.map_show.client.render.block;

import com.hismeo.map_show.MapShow;
import com.hismeo.map_show.client.MapLevel;
import com.mojang.blaze3d.vertex.*;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.Util;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

//TODO 分包 解耦 合并逻辑
public class BlockRenderAllocator {
    private static final List<RenderType> RENDER_TYPES = RenderType.chunkBufferLayers();
    private final Map<RenderType, ByteBufferBuilder> buffers = Util.make(new Reference2ObjectArrayMap<>(RENDER_TYPES.size()), map -> {
        for (RenderType rendertype : RENDER_TYPES) {
            map.put(rendertype, new ByteBufferBuilder(rendertype.bufferSize()));
        }
    });
    private final BlockColors blockColors;
    private final LiquidRenderer liquidRenderer;
    private final NormalRenderer normalRenderer;
    private final BlockModelShaper blockModelShaper;

    public BlockRenderAllocator(BlockColors blockColors, BlockModelShaper blockModelShaper) {
        this.blockColors = blockColors;
        this.liquidRenderer = new LiquidRenderer();
        this.normalRenderer = new NormalRenderer(blockColors);
        this.blockModelShaper = blockModelShaper;
    }

    public Map<RenderType, BufferBuilder> renderBlockLayer(Map<RenderType, BufferBuilder> map, BlockAndTintGetter getter, BlockState blockState, BlockPos blockPos, PoseStack poseStack) {
//                if (blockState.is(Blocks.AIR)) return;
//                if (blockState.isSolidRender(getter, blockpos2)) {
//                    visgraph.setOpaque(blockpos2);
//                }

//                if (blockState.hasBlockEntity()) {
//                    BlockEntity blockentity = getter.getBlockEntity(blockpos2);
//                    if (blockentity != null) {
//                        this.handleBlockEntity(sectioncompiler$results, blockentity);
//                    }
//                }


        poseStack.pushPose();
        FluidState fluidstate = blockState.getFluidState();
        if (!fluidstate.isEmpty()) {
            RenderType liquidRendertype = ItemBlockRenderTypes.getRenderLayer(fluidstate);
            BufferBuilder bufferBuilder = this.getBuffer(map, liquidRendertype);
            liquidRenderer.renderLiquid(poseStack, getter, blockPos, bufferBuilder, blockState, fluidstate);
        }

        if (blockState.getRenderShape() == RenderShape.MODEL) {
            BakedModel model = blockModelShaper.getBlockModel(blockState);
            ModelData modelData = getter.getModelData(blockPos);
            modelData = model.getModelData(getter, blockPos, blockState, modelData);
            RandomSource randomSource = RandomSource.create();
            randomSource.setSeed(blockState.getSeed(blockPos));

            //TODO vertexsort
            for (RenderType blockRendertype : model.getRenderTypes(blockState, randomSource, modelData)) {
                poseStack.pushPose();
                poseStack.translate((float) SectionPos.sectionRelative(blockPos.getX()), blockPos.getY(), (float) SectionPos.sectionRelative(blockPos.getZ()));
                BufferBuilder bufferBuilder = this.getBuffer(map, blockRendertype);
                normalRenderer.tesselateBlock(getter, model, blockState, blockPos, poseStack, bufferBuilder, true, randomSource, blockState.getSeed(blockPos), OverlayTexture.NO_OVERLAY, modelData, blockRendertype);
                poseStack.popPose();
            }
        }
        poseStack.popPose();
        return map;
    }

    public BufferBuilder getBuffer(Map<RenderType, BufferBuilder> map, @NotNull RenderType renderType) {
        return map.computeIfAbsent(renderType, lambdaType -> new BufferBuilder(getByteBuffer(renderType), VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK));
    }

    public ByteBufferBuilder getByteBuffer(@NotNull RenderType renderType) {
        return buffers.computeIfAbsent(renderType, type -> new ByteBufferBuilder(type.bufferSize()));
    }
}
