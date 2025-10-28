package com.hismeo.map_show.client.render;

import com.mojang.blaze3d.vertex.VertexBuffer;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.ChunkPos;

import java.util.Map;

public record RenderedChunk(ChunkPos chunkPos, Map<RenderType, VertexBuffer> renderLayer) {
    public RenderedChunk(ChunkPos chunkPos) {
        this(chunkPos, new Reference2ObjectOpenHashMap<>());
        for (RenderType chunkBufferLayer : RenderType.chunkBufferLayers()) {
            renderLayer.put(chunkBufferLayer, new VertexBuffer(VertexBuffer.Usage.STATIC));
        }
    }
}
