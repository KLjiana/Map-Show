package com.hismeo.map_show.client.render;

import com.google.common.collect.ImmutableMap;
import com.hismeo.map_show.client.MapChunk;
import com.hismeo.map_show.client.MapChunkCache;
import com.hismeo.map_show.client.MapLevel;
import com.hismeo.map_show.client.render.block.BlockRenderAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectFunction;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.Util;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Map;
import java.util.stream.Collectors;

//TODO 初始加载卡顿优化
//TODO 切换世界时缓冲区为清除（疑似共用？
//TODO 是否有不必要的实例创建
//TODO 半透明提取 单独提交（排序
public class ChunkRenderAllocator {
    private final BlockRenderAllocator blockRenderAllocator;
    private final MapLevel mapLevel;
    private final Object2ObjectOpenHashMap<ChunkPos, RenderedChunk> renderingChunk = new Object2ObjectOpenHashMap<>();

    public ChunkRenderAllocator(MapLevel mapLevel, BlockColors blockColors, BlockModelShaper blockModelShaper) {
        this.mapLevel = mapLevel;
        this.blockRenderAllocator = new BlockRenderAllocator(blockColors, blockModelShaper);
    }

    public boolean isRendering(ChunkPos chunkPos) {
        return renderingChunk.containsKey(chunkPos);
    }

    //TODO CACHE
    public void renderCurrentChunk(PoseStack poseStack, Vector3f cameraPos) {
        MapChunkCache source = mapLevel.getChunkSource();
        int viewCenterX = source.viewCenterX();
        int viewCenterZ = source.viewCenterZ();
        int radius = source.chunkRadius();
        ModelBlockRenderer.enableCaching();

        if (false) {
            //DEBUG
            renderSingleChunk(poseStack, source.getChunk(viewCenterX, viewCenterZ), cameraPos);
//            poseStack.translate(0, 0, 16);
            renderSingleChunk(poseStack, source.getChunk(viewCenterX, viewCenterZ + 1), cameraPos);
        } else {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int chunkX = viewCenterX + dx;
                    int chunkZ = viewCenterZ + dz;
                    MapChunk chunk = source.getChunk(chunkX, chunkZ);
                    if (chunk == null) continue;

                    poseStack.pushPose();
//                    poseStack.translate(dx * 16, 0, dz * 16);
                    renderSingleChunk(poseStack, chunk, cameraPos);
                    poseStack.popPose();
                }
            }
        }
        ModelBlockRenderer.clearCache();
    }

    protected void renderSingleChunk(PoseStack poseStack, MapChunk chunk, Vector3f cameraPos) {
        renderingChunk.computeIfAbsent(chunk.getPos(), pos -> {
            RenderedChunk renderedChunk = new RenderedChunk((ChunkPos) pos);
            var meshDataMap = buildChunkBuffer(new PoseStack(), chunk, cameraPos);
            uploadChunkVBO(meshDataMap, renderedChunk.renderLayer());
            return renderedChunk;
        });


        renderChunkVBO(poseStack, renderingChunk.get(chunk.getPos()).renderLayer());
    }

    protected Map<RenderType, MeshData> buildChunkBuffer(PoseStack poseStack, MapChunk chunk, Vector3f cameraPos) {
        if (chunk != null) {
            BlockPos minPos = chunk.getPos().getWorldPosition().atY(mapLevel.getMinBuildHeight());
            BlockPos maxPos = minPos.offset(15, mapLevel.getHeight() - 1, 15);

            var buffers = new Reference2ObjectArrayMap<RenderType, BufferBuilder>(RenderType.chunkBufferLayers().size());
            var meshes = new Reference2ObjectArrayMap<RenderType, MeshData>();

            for (BlockPos blockPos : BlockPos.betweenClosed(minPos, maxPos)) {
                BlockState blockstate = mapLevel.getBlockState(blockPos);
                blockRenderAllocator.renderBlockLayer(buffers, mapLevel, blockstate, blockPos, poseStack);
            }

            buffers.forEach((renderType, bufferBuilder) -> {
                MeshData meshData = bufferBuilder.build();
                if (meshData != null) {
                    meshes.put(renderType, meshData);
                }
            });

            buffers.clear();
            return meshes;
        }
        return ImmutableMap.of();
    }

    protected void uploadChunkVBO(Map<RenderType, MeshData> meshes, Map<RenderType, VertexBuffer> renderLayer) {
        meshes.forEach((renderType, meshData) -> {
            VertexBuffer vertexBuffer = renderLayer.get(renderType);
            if (vertexBuffer.isInvalid()) {
                meshData.close();
            } else {
                vertexBuffer.bind();
                vertexBuffer.upload(meshData);
                VertexBuffer.unbind();
            }
        });
    }

    protected void renderChunkVBO(PoseStack poseStack, Map<RenderType, VertexBuffer> renderLayer) {
        renderLayer.forEach((renderType, vertexBuffer) -> {
            if (vertexBuffer.isInvalid() || vertexBuffer.mode == null || vertexBuffer.getIndexType() == null) return;
            renderType.setupRenderState();
            vertexBuffer.bind();
            Matrix4f modelViewMatrix = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(poseStack.last().pose());
            vertexBuffer.drawWithShader(modelViewMatrix, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            VertexBuffer.unbind();
            renderType.clearRenderState();
        });
    }
}
