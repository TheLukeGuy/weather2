package weather2.weathersystem.storm;

import CoroUtil.util.Vec3;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import weather2.util.CachedNbtCompound;
import weather2.weathersystem.WeatherManager;

public class Storm {
    public static long lastUsedStormID = 0; //ID starts from 0 for each game start, no storm nbt disk reload for now
    public long ID; //loosely accurate ID for tracking, but we wanted to persist between world reloads..... need proper UUID??? I guess add in UUID later and dont persist, start from 0 per game run
    public boolean isDead = false;

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

    private CachedNbtCompound nbtCache;

    //private NBTTagCompound cachedClientNBTState;

    public Storm(WeatherManager parManager) {
        manager = parManager;
        nbtCache = new CachedNbtCompound();
    }

    public void initFirstTime() {
        ID = lastUsedStormID++;
    }

    public void tick() {

    }

    public void tickRender(float partialTick) {

    }

    public void reset() {
        setDead();
    }

    public void setDead() {
        //Weather.dbg("storm killed, ID: " + ID);

        isDead = true;

        //cleanup memory
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT/*manager.getWorld().isRemote*/) {
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
        ID = parNBT.getLong("ID");
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

        nbt.putLong("ID", ID);
        //just blind set ID into non cached data so client always has it, no need to check for forced state and restore orig state
        nbt.getNewNbt().setLong("ID", ID);

        nbt.setInt("size", size);
        nbt.setInt("maxSize", maxSize);
        nbt.setInt("weatherObjectType", this.weatherObjectType.ordinal());
    }

    public CachedNbtCompound getNbtCache() {
        return nbtCache;
    }

    public void setNbtCache(CachedNbtCompound nbtCache) {
        this.nbtCache = nbtCache;
    }
}
