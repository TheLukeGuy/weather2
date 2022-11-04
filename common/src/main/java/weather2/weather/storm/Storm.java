package weather2.weather.storm;

import CoroUtil.util.Vec3;
import weather2.util.CachedNbtCompound;
import weather2.weather.WeatherManager;

public class Storm {
    public static long lastStormId = 0;
    public long id; // TODO: Use UUIDs for storm IDs
    public boolean dead = false;

    /**
     * used to count up to a threshold to finally remove weather objects,
     * solves issue of simbox cutoff removing storms for first few ticks as player is joining in singleplayer
     * helps with multiplayer, requiring 30 seconds of no players near before removal
     */
    public int ticksSinceNoNearPlayer = 0;

    public WeatherManager manager;

    public Vec3 pos = new Vec3(0, 0, 0);
    public Vec3 posGround = new Vec3(0, 0, 0);
    public Vec3 motion = new Vec3(0, 0, 0);

    //used as radius
    public int size = 50;
    public int maxSize = 0;

    public StormType weatherObjectType = StormType.CLOUD;

    private final CachedNbtCompound nbtCache;

    public Storm(WeatherManager parManager) {
        manager = parManager;
        nbtCache = new CachedNbtCompound();
    }

    public void initFirstTime() {
        id = lastStormId++;
    }

    public void tick() {
    }

    public void tickRender(float partialTick) {
    }

    public void reset() {
        setDead();
    }

    public void setDead() {
        dead = true;

        if (manager.getWorld().isClient) {
            cleanupClient();
        }
        cleanup();
    }

    public void cleanup() {
        manager = null;
    }

    public void cleanupClient() {
    }

    public int getUpdateRateForNetwork() {
        return 40;
    }

    public void readFromNBT() {
    }

    public void writeToNBT() {
    }

    public void nbtSyncFromServer() {
        CachedNbtCompound parNBT = this.getNbtCache();
        id = parNBT.getLong("ID");
        //Weather.dbg("StormObject " + ID + " receiving sync");

        pos = new Vec3(parNBT.getDouble("posX"), parNBT.getDouble("posY"), parNBT.getDouble("posZ"));
        //motion = new Vec3(parNBT.getDouble("motionX"), parNBT.getDouble("motionY"), parNBT.getDouble("motionZ"));
        motion = new Vec3(parNBT.getDouble("vecX"), parNBT.getDouble("vecY"), parNBT.getDouble("vecZ"));
        size = parNBT.getInt("size");
        maxSize = parNBT.getInt("maxSize");
        this.weatherObjectType = StormType.get(parNBT.getInt("weatherObjectType"));
    }

    public void nbtSyncForClient() {
        CachedNbtCompound nbt = this.getNbtCache();
        nbt.putDouble("posX", pos.xCoord);
        nbt.putDouble("posY", pos.yCoord);
        nbt.putDouble("posZ", pos.zCoord);

        nbt.putDouble("vecX", motion.xCoord);
        nbt.putDouble("vecY", motion.yCoord);
        nbt.putDouble("vecZ", motion.zCoord);

        nbt.putLong("ID", id);
        //just blind set ID into non cached data so client always has it, no need to check for forced state and restore orig state
        nbt.getNewNbt().putLong("ID", id);

        nbt.setInt("size", size);
        nbt.setInt("maxSize", maxSize);
        nbt.setInt("weatherObjectType", this.weatherObjectType.ordinal());
    }

    public CachedNbtCompound getNbtCache() {
        return nbtCache;
    }
}
