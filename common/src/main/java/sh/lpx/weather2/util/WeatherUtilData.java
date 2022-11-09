package sh.lpx.weather2.util;

import net.minecraft.entity.Entity;

public class WeatherUtilData {
    public static String weather2_WindWeight = "weather2_WindWeight";
    public static String weather2_WindAffected = "weather2_WindAffected";

    public static boolean isWindAffected(Entity ent) {
        return ent.getEntityData().getBoolean(weather2_WindAffected);
    }

    public static float getWindWeight(Entity ent) {
        return ent.getEntityData().getFloat(weather2_WindWeight);
    }

    public static boolean isWindWeightSet(Entity ent) {
        return ent.getEntityData().hasKey(weather2_WindWeight);
    }
}
