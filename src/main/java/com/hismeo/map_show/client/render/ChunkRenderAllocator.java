package com.hismeo.map_show.client.render;

import com.hismeo.map_show.client.MapChunk;
import com.hismeo.map_show.client.MapChunkCache;
import com.hismeo.map_show.client.MapLevel;
import com.hismeo.map_show.client.render.block.BlockRenderAllocator;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.stream.Collectors;

public class ChunkRenderAllocator {
    private final Map<RenderType, VertexBuffer> vboMap = RenderType.chunkBufferLayers()
            .stream()
            .collect(Collectors.toMap(renderType -> renderType, renderType -> new VertexBuffer(VertexBuffer.Usage.STATIC)));
    private final ObjectArrayList<RenderedChunk> renderingChunk = new ObjectArrayList<>();
    private final boolean rendering = false;
    private final BlockRenderAllocator blockRenderAllocator;
    private final MapLevel mapLevel;

    public ChunkRenderAllocator(MapLevel mapLevel, BlockColors blockColors, BlockModelShaper blockModelShaper) {
        this.mapLevel = mapLevel;
        this.blockRenderAllocator = new BlockRenderAllocator(blockColors, blockModelShaper);
    }

    //TODO CACHE
    public void renderCurrentChunk(PoseStack poseStack) {
        MapChunkCache source = mapLevel.getChunkSource();
        int viewCenterX = source.viewCenterX();
        int viewCenterZ = source.viewCenterZ();
        int radius = source.chunkRadius();
        ModelBlockRenderer.enableCaching();

        if (true) {
            //DEBUG
            renderSingleChunk(poseStack, source.getChunk(viewCenterX, viewCenterZ));
        } else {
            //TODO CACHE!!!!
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    int chunkX = viewCenterX + dx;
                    int chunkZ = viewCenterZ + dz;
                    MapChunk chunk = source.getChunk(chunkX, chunkZ);
                    if (chunk == null) continue;

                    poseStack.pushPose();
                    poseStack.translate(dx * 16, 0, dz * 16);
                    renderSingleChunk(poseStack, chunk);
                    poseStack.popPose();
                }
            }
        }
        ModelBlockRenderer.clearCache();
    }

    protected void renderSingleChunk(PoseStack poseStack, MapChunk chunk) {
        if (chunk != null) {
            BlockPos minPos = chunk.getPos().getWorldPosition().above(mapLevel.getMinBuildHeight());
            BlockPos maxPos = minPos.offset(15, mapLevel.getMaxBuildHeight(), 15);
            Map<RenderType, BufferBuilder> map = new Reference2ObjectArrayMap<>(RenderType.chunkBufferLayers().size());
            for (BlockPos blockPos : BlockPos.betweenClosed(minPos, maxPos)) {
                BlockState blockstate = mapLevel.getBlockState(blockPos);
                blockRenderAllocator.renderBlockLayer(map, mapLevel, blockstate, blockPos, poseStack);
            }

            Map<RenderType, MeshData> meshes = new Reference2ObjectArrayMap<>();
            for (var entry : map.entrySet()) {
                MeshData meshData = entry.getValue().build();
                if (meshData != null) {
                    meshes.put(entry.getKey(), meshData);
                }
            }
            meshes.forEach((renderType, meshData) -> {
                VertexBuffer vertexBuffer = vboMap.get(renderType);
                if (vertexBuffer.isInvalid()) {
                    meshData.close();
                } else {
                                        vertexBuffer.bind();
                    vertexBuffer.upload(meshData);
                    vertexBuffer.drawWithShader(RenderSystem.getModelViewMatrix(), RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
                    VertexBuffer.unbind();
                }
            });
        }
    }
}
