package weather2.weathersystem.wind;

import CoroUtil.util.CoroUtilEntOrParticle;
import CoroUtil.util.Vec3;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import weather2.Weather;
import weather2.config.ConfigMisc;
import weather2.config.ConfigWind;
import weather2.util.WeatherUtilEntity;
import weather2.weathersystem.WeatherManager;
import weather2.weathersystem.ServerWeatherManager;
import weather2.weathersystem.storm.StormObject;

import java.util.Random;

public class WindManager {
    //2 wind layers:

    //1: event wind:
    //1a: storm event, pulling wind into tornado
    //1b: wind gusts
    //2: high level wind that clouds use

    //particles use in priority order: storm event, if no event, gust, if no gust, global wind

    //global wind wont have gusts, but slowly changes intensity and angle

    //weather effect wind will have gusts and overrides from weather events

    //small design exception:
    //- gusts are server side global, as planned
    //- events are client side player, required small adjustments

    public WeatherManager manager;

    //global
    public float windAngleGlobal;
    public float windSpeedGlobal = 0;
    public float windSpeedGlobalChangeRate = 0.05F;
    public int windSpeedGlobalRandChangeTimer = 0;
    public int windSpeedGlobalRandChangeDelay = 10;

    //generic?

    //events - design derp, we're making this client side, so its set based on closest storm to the client side player
    public float windAngleEvent = 0;
    public BlockPos windOriginEvent = BlockPos.ORIGIN;
    public float windSpeedEvent = 0;
    public int windTimeEvent = 0; //its assumed this will get set by whatever initializes an event, and this class counts it down from a couple seconds, helps wind system know what takes priority

    //gusts
    public float windAngleGust = 0;
    public float windSpeedGust = 0;
    public int windTimeGust = 0;
    public int windGustEventTimeRand = 60;
    public float chanceOfWindGustEvent = 0.5F;

    //low wind event
    public int lowWindTimer = 0;

    //high wind event
    public int highWindTimer = 0;


    public WindManager(WeatherManager parManager) {
        manager = parManager;

        Random rand = new Random();

        windAngleGlobal = rand.nextInt(360);
    }

    //Speed getters\\

    //WIP
    public float getWindSpeedForPriority() {
        //gets event wind, or if none, global, etc
        if (windTimeEvent > 0) {
            return getWindSpeedForEvents();
        } else if (windTimeGust > 0) {
            return getWindSpeedForGusts();
        } else {
            return getWindSpeedForClouds();
        }
    }

    public float getWindSpeedForEvents() {
        if (windTimeEvent > 0) {
            return windSpeedEvent;
        } else {
            return 0;
        }
    }

    public float getWindSpeedForGusts() {
        return windSpeedGust;
    }

    public float getWindSpeedForClouds() {
        return windSpeedGlobal;
    }

    //Angle getters\\

    /**
     * WIP, Returns angle in degrees, 0-360
     */
    public float getWindAngleForPriority(Vec3 pos) {
        //gets event wind, or if none, global, etc
        if (windTimeEvent > 0) {
            return getWindAngleForEvents(pos);
        } else if (windTimeGust > 0) {
            return getWindAngleForGusts();
        } else {
            return getWindAngleForClouds();
        }
    }

    /**
     * Returns angle in degrees, 0-360
     */
    public float getWindAngleForEvents() {
        return windAngleEvent;
    }

    public float getWindAngleForEvents(Vec3 pos) {
        if (pos != null && !windOriginEvent.equals(BlockPos.ORIGIN)) {
            double var11 = windOriginEvent.getX() + 0.5D - pos.xCoord;
            double var15 = windOriginEvent.getZ() + 0.5D - pos.zCoord;
            return (-((float) Math.atan2(var11, var15)) * 180.0F / (float) Math.PI) - 45;
        } else {
            return windAngleEvent;
        }
    }

    /**
     * Returns angle in degrees, 0-360
     */
    public float getWindAngleForGusts() {
        return windAngleGust;
    }

    /**
     * Returns angle in degrees, 0-360
     */
    public float getWindAngleForClouds() {
        return windAngleGlobal;
    }

    public void setWindTimeGust(int parVal) {
        windTimeGust = parVal;
        syncData();
        //Weather.dbg("Wind gust time set: " + parVal);
    }

    public void setWindTimeEvent(int parVal) {
        windTimeEvent = parVal;
        //syncData(); - might be too often
        //Weather.dbg("Wind event time set: " + parVal);
    }

    public void tick() {
        Random rand = new Random();

        if (!ConfigWind.Misc_windOn) {
            windSpeedGlobal = 0;
            windSpeedGust = 0;
            windTimeGust = 0;
            //windSpeedSmooth = 0;
        } else {

            if (!manager.getWorld().isClient) {
                //WIND SPEED\\

                //global random wind speed change

                if (!ConfigWind.Wind_LowWindEvents) {
                    lowWindTimer = 0;
                }

                if (lowWindTimer <= 0) {
                    if (windSpeedGlobalRandChangeTimer-- <= 0) {
                        //standard wind adjustment
                        if (highWindTimer <= 0) {
                            windSpeedGlobal += (rand.nextDouble() * windSpeedGlobalChangeRate) - (windSpeedGlobalChangeRate / 2);
                            //only increase for high wind
                        } else {
                            windSpeedGlobal += (rand.nextDouble() * windSpeedGlobalChangeRate)/* - (windSpeedGlobalChangeRate / 2)*/;
                        }
                        windSpeedGlobalRandChangeTimer = windSpeedGlobalRandChangeDelay;
                    }

                    //only allow for low wind if high wind not active
                    if (highWindTimer <= 0) {
                        if (ConfigWind.Wind_LowWindEvents) {
                            if (rand.nextInt(ConfigWind.lowWindOddsTo1) == 0) {
                                startLowWindEvent();
                                Weather.dbg("low wind event started, for ticks: " + lowWindTimer);
                            }
                        }
                    } else {
                        //fix edge case where if a high wind event is manually started, low wind could still be trying to take control
                        stopLowWindEvent();
                    }

                    if (ConfigWind.Wind_HighWindEvents && highWindTimer <= 0) {
                        if (rand.nextInt(ConfigWind.highWindOddsTo1) == 0) {
                            startHighWindEvent();
                            Weather.dbg("high wind event started, for ticks: " + highWindTimer);
                        }
                    }
                } else {
                    lowWindTimer--;
                    if (lowWindTimer <= 0) {
                        Weather.dbg("low wind event ended");
                    }
                    windSpeedGlobal -= 0.01F;
                }

                if (highWindTimer > 0) {
                    highWindTimer--;
                    if (highWindTimer <= 0) {
                        Weather.dbg("high wind event ended");
                    }
                }

                //enforce mins and maxs of wind speed
                if (windSpeedGlobal < ConfigWind.windSpeedMin) {
                    windSpeedGlobal = (float) ConfigWind.windSpeedMin;
                }

                if (windSpeedGlobal > ConfigWind.windSpeedMax) {
                    windSpeedGlobal = (float) ConfigWind.windSpeedMax;
                }

                if (windTimeGust > 0) {
                    windTimeGust--;

                    if (windTimeGust == 0) {
                        syncData();
                    }
                }

                if (ConfigMisc.overcastMode && manager.getWorld().isRaining()) {
                    if (windSpeedGlobal < ConfigWind.windSpeedMinGlobalOvercastRaining) {
                        windSpeedGlobal = (float) ConfigWind.windSpeedMinGlobalOvercastRaining;
                    }
                }

                //WIND SPEED //

                //WIND ANGLE\\

                //windGustEventTimeRand = 100;

                float randGustWindFactor = 1F;

                //gust data
                if (this.windTimeGust == 0 && lowWindTimer <= 0/* && highWindTimer <= 0*/) {
                    if (chanceOfWindGustEvent > 0F) {
                        if (rand.nextInt((int) ((100 - chanceOfWindGustEvent) * randGustWindFactor)) == 0) {
                            windSpeedGust = windSpeedGlobal + rand.nextFloat() * 0.6F;
                            windAngleGust = windAngleGlobal + rand.nextInt(120) - 60;

                            setWindTimeGust(rand.nextInt(windGustEventTimeRand));
                            //windEventTime += windTime;
                            //unneeded since priority system determines wind to use
                            //directionBeforeGust = windAngleGlobal;
                        }
                    }
                }

                //global wind angle
                //windAngleGlobal += ((new Random()).nextInt(5) - 2) * 0.2F;
                windAngleGlobal += (rand.nextFloat() * ConfigWind.globalWindChangeAmountRate) - (rand.nextFloat() * ConfigWind.globalWindChangeAmountRate);

                if (windAngleGlobal < -180) {
                    windAngleGlobal += 360;
                }

                if (windAngleGlobal > 180) {
                    windAngleGlobal -= 360;
                }

                //WIND ANGLE //
            } else {
                tickClient();
            }
        }
    }

    public void startHighWindEvent() {
        highWindTimer = ConfigWind.highWindTimerEnableAmountBase + (new Random()).nextInt(ConfigWind.highWindTimerEnableAmountRnd);
    }

    public boolean isHighWindEventActive() {
        return highWindTimer > 0;
    }

    public void stopHighWindEvent() {
        highWindTimer = 0;
    }

    public void startLowWindEvent() {
        lowWindTimer = ConfigWind.lowWindTimerEnableAmountBase + (new Random()).nextInt(ConfigWind.lowWindTimerEnableAmountRnd);
    }

    public void stopLowWindEvent() {
        lowWindTimer = 0;
    }

    public void tickClient() {
        ClientPlayerEntity entP = MinecraftClient.getInstance().player;

        if (windTimeEvent > 0) {
            windTimeEvent--;
        }

        //event data
        if (entP != null) {
            if (manager.getWorld().getTime() % 10 == 0) {
                StormObject so = manager.getClosestStorm(new Vec3(entP.getX(), StormObject.layers.get(0), entP.getZ()), 256, StormObject.STATE_HIGHWIND);

                if (so != null) {
                    windOriginEvent = new BlockPos(so.posGround.xCoord, so.posGround.yCoord, so.posGround.zCoord);

                    setWindTimeEvent(80);

                    //player pos aiming at storm
                    double var11 = so.posGround.xCoord - entP.getX();
                    double var15 = so.posGround.zCoord - entP.getZ();

                    windAngleEvent = -((float) Math.atan2(var11, var15)) * 180.0F / (float) Math.PI;
                    windSpeedEvent = 2F; //make dynamic?

                    //Weather.dbg("!!!!!!!!!!!!!!!!!!!storm event near: " + stormDist);
                }
            }
        }
    }

    public NbtCompound nbtSyncForClient() {
        NbtCompound data = new NbtCompound();

        //idea: only sync the wind data client cares about (the active priority wind)

        data.putFloat("windSpeedGlobal", windSpeedGlobal);
        data.putFloat("windAngleGlobal", windAngleGlobal);
        data.putFloat("windSpeedGust", windSpeedGust);
        data.putFloat("windAngleGust", windAngleGust);

        data.putInt("windTimeGust", windTimeGust);

        return data;
    }

    public void nbtSyncFromServer(NbtCompound parNBT) {
        windSpeedGlobal = parNBT.getFloat("windSpeedGlobal");
        windAngleGlobal = parNBT.getFloat("windAngleGlobal");
        windSpeedGust = parNBT.getFloat("windSpeedGust");
        windAngleGust = parNBT.getFloat("windAngleGust");

        windTimeGust = parNBT.getInt("windTimeGust");
    }

    public void syncData() {
        if (manager instanceof ServerWeatherManager) {
            ((ServerWeatherManager) manager).syncWindUpdate(this);
        }
    }

    public void reset() {
        manager = null;
    }

    /**
     * To solve the problem of speed going overkill due to bad formulas
     * <p>
     * end goal: make object move at speed of wind
     * - object has a weight that slows that adjustment
     * - conservation of momentum
     * <p>
     * calculate force based on wind speed vs objects speed
     * - use that force to apply to weight of object
     * - profit
     */
    public void applyWindForceNew(Object ent, float multiplier, float maxSpeed) {
        Vec3 pos = new Vec3(CoroUtilEntOrParticle.getPosX(ent), CoroUtilEntOrParticle.getPosY(ent), CoroUtilEntOrParticle.getPosZ(ent));

        Vec3 motion = applyWindForceImpl(pos, new Vec3(CoroUtilEntOrParticle.getMotionX(ent), CoroUtilEntOrParticle.getMotionY(ent), CoroUtilEntOrParticle.getMotionZ(ent)),
                WeatherUtilEntity.getWeight(ent), multiplier, maxSpeed);

        CoroUtilEntOrParticle.setMotionX(ent, motion.xCoord);
        CoroUtilEntOrParticle.setMotionZ(ent, motion.zCoord);
    }

    /**
     * Handle generic uses of wind force, for stuff like weather objects that arent entities or paticles
     */
    public Vec3 applyWindForceImpl(Vec3 pos, Vec3 motion, float weight, float multiplier, float maxSpeed) {
        WindManager windMan = this;//ClientTickHandler.weatherManager.windMan;

        float windSpeed = windMan.getWindSpeedForPriority();
        float windAngle = windMan.getWindAngleForPriority(pos);

        float windX = (float) -Math.sin(Math.toRadians(windAngle)) * windSpeed;
        float windZ = (float) Math.cos(Math.toRadians(windAngle)) * windSpeed;

        float objX = (float) motion.xCoord;//CoroUtilEntOrParticle.getMotionX(ent);
        float objZ = (float) motion.zCoord;//CoroUtilEntOrParticle.getMotionZ(ent);

        float windWeight = 1F;
        float objWeight = weight;

        //divide by zero protection
        if (objWeight <= 0) {
            objWeight = 0.001F;
        }

        float weightDiff = windWeight / objWeight;

        float vecX = (objX - windX) * weightDiff;
        float vecZ = (objZ - windZ) * weightDiff;

        vecX *= multiplier;
        vecZ *= multiplier;

        //copy over existing motion data
        Vec3 newMotion = new Vec3(motion);

        double speedCheck = (Math.abs(vecX) + Math.abs(vecZ)) / 2D;
        if (speedCheck < maxSpeed) {
            newMotion.xCoord = objX - vecX;
            newMotion.zCoord = objZ - vecZ;
        }

        return newMotion;
    }

    public Vec3 getWindForce() {
        float windSpeed = this.getWindSpeedForPriority();
        float windAngle = this.getWindAngleForPriority(null);
        float windX = (float) -Math.sin(Math.toRadians(windAngle)) * windSpeed;
        float windZ = (float) Math.cos(Math.toRadians(windAngle)) * windSpeed;
        return new Vec3(windX, 0, windZ);
    }

    public void readFromNBT(NbtCompound data) {
        windSpeedGlobal = data.getFloat("windSpeedGlobal");
        windAngleGlobal = data.getFloat("windAngleGlobal");

        windSpeedGust = data.getFloat("windSpeedGust");
        windAngleGust = data.getFloat("windAngleGust");
        windTimeGust = data.getInt("windTimeGust");

        windSpeedEvent = data.getFloat("windSpeedEvent");
        windAngleEvent = data.getFloat("windAngleEvent");
        windTimeEvent = data.getInt("windTimeEvent");

        lowWindTimer = data.getInt("lowWindTimer");
        highWindTimer = data.getInt("highWindTimer");
    }

    public NbtCompound writeToNBT(NbtCompound data) {
        data.putFloat("windSpeedGlobal", windSpeedGlobal);
        data.putFloat("windAngleGlobal", windAngleGlobal);

        data.putFloat("windSpeedGust", windSpeedGust);
        data.putFloat("windAngleGust", windAngleGust);
        data.putInt("windTimeGust", windTimeGust);

        data.putFloat("windSpeedEvent", windSpeedEvent);
        data.putFloat("windAngleEvent", windAngleEvent);
        data.putInt("windTimeEvent", windTimeEvent);

        data.putInt("lowWindTimer", lowWindTimer);
        data.putInt("highWindTimer", highWindTimer);

        return data;
    }
}
