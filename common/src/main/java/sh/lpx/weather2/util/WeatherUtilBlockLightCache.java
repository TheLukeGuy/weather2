package sh.lpx.weather2.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;
import net.minecraft.world.World;

import java.util.HashMap;

/**
 * Created by corosus on 01/06/17.
 */
public class WeatherUtilBlockLightCache {
    /**
     * For reference:
     * no light lookups = 145 fps
     * 3D coord hashed light lookups = 120 fps
     * triple hashmap lookups = 105 fps
     */

    public static HashMap<Long, Float> lookupPosToBrightness = new HashMap<>();
    public static HashMap<Integer, HashMap<Integer, HashMap<Integer, Float>>> lookupPosToBrightness2 = new HashMap<>();

    private static final int NUM_X_BITS = 1 + MathHelper.log2(MathHelper.smallestEncompassingPowerOfTwo(30000000));
    private static final int NUM_Z_BITS = NUM_X_BITS;
    private static final int NUM_Y_BITS = 64 - NUM_X_BITS - NUM_Z_BITS;
    private static final int Y_SHIFT = NUM_Z_BITS;
    private static final int X_SHIFT = Y_SHIFT + NUM_Y_BITS;
    private static final long X_MASK = (1L << NUM_X_BITS) - 1L;
    private static final long Y_MASK = (1L << NUM_Y_BITS) - 1L;
    private static final long Z_MASK = (1L << NUM_Z_BITS) - 1L;

    public static float brightnessPlayer = 0F;

    public static float getBrightnessCached(World world, float x, float y, float z) {
        //if (true) return 0.5F;

        int xx = MathHelper.floor(x);
        int yy = MathHelper.floor(y);
        int zz = MathHelper.floor(z);

        long hash = ((long) xx & X_MASK) << X_SHIFT | ((long) yy & Y_MASK) << Y_SHIFT | ((long) zz & Z_MASK);
        Float brightness = lookupPosToBrightness.get(hash);
        if (brightness != null) {
            return brightness;
        } else {
            float brightnesss = getBrightnessFromLightmap(world, x, y, z);
            lookupPosToBrightness.put(hash, brightnesss/* + 0.001F*/);
            return brightnesss;
        }
    }

    public static void clear() {
        lookupPosToBrightness.clear();
        lookupPosToBrightness2.clear();
    }

    public static float getBrightnessFromLightmap(World world, float x, float y, float z) {
        BlockPos pos = new BlockPos(x, y, z);
        int i = world.getLightLevel(LightType.SKY, pos);
        int j = world.getLightLevel(LightType.BLOCK, pos);

        int[] texData = MinecraftClient.getInstance().entityRenderer.lightmapTexture.getTextureData();

        int color = texData[(i * 16) + j];

        return color;
    }
}
