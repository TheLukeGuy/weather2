package weather2.util;

import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import weather2.Weather;

import java.lang.reflect.Method;

public class WeatherUtilCompatibility {
    private static boolean tanInstalled = false;
    private static boolean checkTAN = true;

    private static boolean sereneSeasonsInstalled = false;
    private static boolean checkSereneSeasons = true;

    private static Class<?> class_TAN_ASMHelper = null;
    private static Method method_TAN_getFloatTemperature = null;

    private static Class<?> class_SereneSeasons_ASMHelper = null;
    private static Method method_sereneSeasons_getFloatTemperature = null;

    public static float getAdjustedTemperature(World world, Biome biome, BlockPos pos) {
        //TODO: consider caching results in a blockpos,float hashmap for a second or 2
        if (isTANInstalled()) {
            try {
                if (method_TAN_getFloatTemperature == null) {
                    method_TAN_getFloatTemperature = class_TAN_ASMHelper.getDeclaredMethod("getFloatTemperature", Biome.class, BlockPos.class);
                }
                return (float) method_TAN_getFloatTemperature.invoke(null, biome, pos);
            } catch (Exception ex) {
                ex.printStackTrace();
                //prevent error spam
                tanInstalled = false;
                return biome.getTemperature(pos);
            }
        } else if (isSereneSeasonsInstalled()) {
            try {
                if (method_sereneSeasons_getFloatTemperature == null) {
                    method_sereneSeasons_getFloatTemperature = class_SereneSeasons_ASMHelper.getDeclaredMethod("getFloatTemperature", World.class, Biome.class, BlockPos.class);
                }
                return (float) method_sereneSeasons_getFloatTemperature.invoke(null, world, biome, pos);
            } catch (Exception ex) {
                ex.printStackTrace();
                //prevent error spam
                sereneSeasonsInstalled = false;
                return biome.getTemperature(pos);
            }
        } else {
            return biome.getTemperature(pos);
        }
    }

    /**
     * Check if tough as nails is installed
     */
    public static boolean isTANInstalled() {
        if (checkTAN) {
            try {
                checkTAN = false;
                class_TAN_ASMHelper = Class.forName("toughasnails.season.SeasonASMHelper");
                tanInstalled = true;
            } catch (Exception ex) {
                //not installed
                //ex.printStackTrace();
            }

            Weather.LOGGER.info("CoroUtil detected Tough As Nails Seasons " + (tanInstalled ? "Installed" : "Not Installed") + " for use");
        }

        return tanInstalled;
    }

    /**
     * Check if Serene Seasons is installed
     */
    public static boolean isSereneSeasonsInstalled() {
        if (checkSereneSeasons) {
            try {
                checkSereneSeasons = false;
                class_SereneSeasons_ASMHelper = Class.forName("sereneseasons.api.season.BiomeHooks");
                sereneSeasonsInstalled = true;
            } catch (Exception ex) {
                //not installed
                //ex.printStackTrace();
            }

            Weather.LOGGER.info("CoroUtil detected Serene Seasons " + (sereneSeasonsInstalled ? "Installed" : "Not Installed") + " for use");
        }

        return sereneSeasonsInstalled;
    }

    public static boolean canTornadoGrabBlockRefinedRules(BlockState state) {
        Identifier registeredName = state.getBlock().getRegistryName();
        if (registeredName.getNamespace().equals("dynamictrees")) {
            return !registeredName.getPath().contains("rooty") && !registeredName.getPath().contains("branch");
        }
        return true;
    }
}
