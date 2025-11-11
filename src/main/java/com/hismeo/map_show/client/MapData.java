package com.hismeo.map_show.client;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record MapData(
        ResourceKey<Level> dimension,
        DimensionSpecialEffects effects,
        int height,
        int minBuildHeight,
        RegistryAccess registryAccess
) {}
