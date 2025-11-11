package com.hismeo.map_show.mixin.client;

import com.hismeo.map_show.client.ClientMap;
import com.hismeo.map_show.client.MapChunk;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin extends ChunkAccess {
    @Shadow
    @Final
    Level level;

    public LevelChunkMixin(ChunkPos chunkPos, UpgradeData upgradeData, LevelHeightAccessor levelHeightAccessor, Registry<Biome> biomeRegistry, long inhabitedTime, @Nullable LevelChunkSection[] sections, @Nullable BlendingData blendingData) {
        super(chunkPos, upgradeData, levelHeightAccessor, biomeRegistry, inhabitedTime, sections, blendingData);
    }

    @Inject(method = "replaceBiomes", at = @At("HEAD"))
    private void cacheLevel(FriendlyByteBuf buffer, CallbackInfo ci, @Share("mapChunk") LocalRef<MapChunk> mapChunk) {
        if (level instanceof ClientLevel clientLevel) {
            // 客户端只能拥有一个client level，所以可以直接使用
            mapChunk.set(ClientMap.getCurrentLevel(clientLevel).getChunkSource().getChunk(chunkPos.x, chunkPos.z));
        }
    }

    @Inject(method = "replaceBiomes", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;readBiomes(Lnet/minecraft/network/FriendlyByteBuf;)V", shift = At.Shift.AFTER))
    private void replaceMapBiomes(FriendlyByteBuf buffer, CallbackInfo ci, @Local(ordinal = 1) int index, @Local LevelChunkSection section, @Share("mapChunk") LocalRef<MapChunk> mapChunk) {
        MapChunk chunk = mapChunk.get();
        if (chunk != null) {
            // 因为是直接复制的层次，所以索引一致
            chunk.getSections()[index].replaceBiomes(section);
        }
    }
}
