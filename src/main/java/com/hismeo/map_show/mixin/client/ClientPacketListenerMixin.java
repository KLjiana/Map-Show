package com.hismeo.map_show.mixin.client;

import com.hismeo.map_show.client.ClientMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Shadow
    private ClientLevel level;

    @Inject(method = "handleSetChunkCacheRadius", at = @At("TAIL"))
    private void setChunkCacheRadius(ClientboundSetChunkCacheRadiusPacket packet, CallbackInfo ci) {
        ClientMap.getCurrentLevel(level).getChunkSource().updateViewRadius(packet.getRadius());
    }

    @Inject(method = "handleSetChunkCacheCenter", at = @At("TAIL"))
    private void setChunkCacheCenter(ClientboundSetChunkCacheCenterPacket packet, CallbackInfo ci) {
        ClientMap.getCurrentLevel(level).getChunkSource().updateViewCenter(packet.getX(), packet.getZ());
    }
}
