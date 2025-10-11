package com.hismeo.map_show;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MapShow.MODID)
public class MapShow {
    public static final String MODID = "map_show";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
    public MapShow(IEventBus modEventBus, ModContainer modContainer){
    }

    public static boolean isDev() {
        return FMLLoader.isProduction();
    }
}
