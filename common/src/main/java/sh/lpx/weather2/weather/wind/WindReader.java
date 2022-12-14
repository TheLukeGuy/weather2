package sh.lpx.weather2.weather.wind;

import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import sh.lpx.weather2.ClientTickHandler;
import sh.lpx.weather2.ServerTickHandler;
import sh.lpx.weather2.weather.WeatherManager;

public class WindReader {
    /*
     *
     * not exactly a proper api class as it depends on weather2 imports, IMC method to come in future
     *
     * 2 wind layers (in order of DOMINANT priority):
     *
     * 1: event wind:
     * 1a: storm event, pulling wind into tornado
     * 1b: wind gusts
     * 2: high level wind that clouds use
     *
     * EnumTypes explained:
     * DOMINANT: The priority taking wind data for the location
     * CLOUD: Wind data used for clouds / high level things
     * EVENT: Wind data used for storm events
     * GUST: Wind data used for wind gusts
     *
     * WindType.EVENT is client side only, due to wind technically being a global thing on server side, it was required to make events easily location based for player
     */

    public enum WindType {
        PRIORITY,
        EVENT,
        GUST,
        CLOUD
    }

    public static float getWindAngle(World parWorld, Vec3d parLocation) {
        return getWindAngle(parWorld, parLocation, WindType.PRIORITY);
    }

    public static float getWindAngle(World parWorld, Vec3d parLocation, WindType parWindType) {
        WeatherManager wMan;
        if (parWorld.isClient) {
            wMan = getWeatherManagerClient();
        } else {
            wMan = ServerTickHandler.lookupDimToWeatherMan.get(parWorld.getDimension());
        }

        if (wMan != null) {
            if (parWindType == WindType.PRIORITY) {
                return wMan.windManager.getWindAngleForPriority(null);
            } else if (parWindType == WindType.EVENT) {
                return wMan.windManager.getWindAngleForEvents();
            } else if (parWindType == WindType.GUST) {
                return wMan.windManager.getWindAngleForGusts();
            } else if (parWindType == WindType.CLOUD) {
                return wMan.windManager.getWindAngleForClouds();
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    public static float getWindSpeed(World parWorld, Vec3d parLocation) {
        return getWindSpeed(parWorld, parLocation, WindType.PRIORITY);
    }

    public static float getWindSpeed(World parWorld, Vec3d parLocation, WindType parWindType) {
        WeatherManager wMan;
        if (parWorld.isClient) {
            wMan = getWeatherManagerClient();
        } else {
            wMan = ServerTickHandler.lookupDimToWeatherMan.get(parWorld.getDimension());
        }

        if (wMan != null) {
            if (parWindType == WindType.PRIORITY) {
                return wMan.windManager.getWindSpeedForPriority();
            } else if (parWindType == WindType.EVENT) {
                return wMan.windManager.getWindSpeedForEvents();
            } else if (parWindType == WindType.GUST) {
                return wMan.windManager.getWindSpeedForGusts();
            } else if (parWindType == WindType.CLOUD) {
                return wMan.windManager.getWindSpeedForClouds();
            } else {
                return 0;
            }
        } else {
            return 0;
        }
    }

    private static WeatherManager getWeatherManagerClient() {
        return ClientTickHandler.weatherManager;
    }
}
