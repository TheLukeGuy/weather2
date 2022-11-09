package sh.lpx.weather2.client.render.foliage;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;

import java.util.concurrent.ConcurrentHashMap;

public class FoliageData {
    //orig values
    public static ConcurrentHashMap<BlockState, BakedModel> backupBakedModelStore = new ConcurrentHashMap<>();
}
