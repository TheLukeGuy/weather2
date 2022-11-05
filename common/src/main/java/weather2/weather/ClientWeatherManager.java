package weather2.weather;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import weather2.ClientTickHandler;
import weather2.Weather;
import weather2.entity.EntityLightningBolt;
import weather2.entity.EntityLightningBoltCustom;
import weather2.volcano.Volcano;
import weather2.weather.storm.CloudStorm;
import weather2.weather.storm.SandStorm;
import weather2.weather.storm.Storm;
import weather2.weather.storm.StormType;

import java.util.ArrayList;
import java.util.List;

public class ClientWeatherManager extends WeatherManager {
    //data for client, stormfronts synced from server

    //new for 1.10.2, replaces world.weatherEffects use
    public List<Particle> listWeatherEffectedParticles = new ArrayList<Particle>();

    public static CloudStorm closestStormCached;


    public ClientWeatherManager(int parDim) {
        super(parDim);
    }

    @Override
    public World getWorld() {
        return MinecraftClient.getInstance().world;
    }

    @Override
    public void tick() {
        super.tick();
    }

    public void nbtSyncFromServer(NbtCompound parNBT) {
        //check command
        //commands:
        //new storm
        //update storm
        //remove storm

        //new volcano
        //update volcano
        //remove volcano???

        String command = parNBT.getString("command");

        switch (command) {
            case "syncStormNew": {
                //Weather.dbg("creating client side storm");
                NbtCompound stormNBT = parNBT.getCompound("data");
                long ID = stormNBT.getLong("ID");
                Weather.LOGGER.debug("syncStormNew, ID: " + ID);

                StormType weatherObjectType = StormType.get(stormNBT.getInt("weatherObjectType"));

                Storm wo = null;
                if (weatherObjectType == StormType.CLOUD) {
                    wo = new CloudStorm(ClientTickHandler.weatherManager);
                } else if (weatherObjectType == StormType.SAND) {
                    wo = new SandStorm(ClientTickHandler.weatherManager);
                }

                //StormObject so
                wo.getNbtCache().setNewNbt(stormNBT);
                wo.nbtSyncFromServer();
                wo.getNbtCache().updateCacheFromNew();

                addStorm(wo);
                break;
            }
            case "syncStormRemove": {
                //Weather.dbg("removing client side storm");
                NbtCompound stormNBT = parNBT.getCompound("data");
                long ID = stormNBT.getLong("ID");

                Storm so = stormsById.get(ID);
                if (so != null) {
                    removeStorm(ID);
                } else {
                    Weather.LOGGER.debug("error removing storm, cant find by ID: " + ID);
                }
                break;
            }
            case "syncStormUpdate": {
                //Weather.dbg("updating client side storm");
                NbtCompound stormNBT = parNBT.getCompound("data");
                long ID = stormNBT.getLong("ID");

                Storm so = stormsById.get(ID);
                if (so != null) {
                    so.getNbtCache().setNewNbt(stormNBT);
                    so.nbtSyncFromServer();
                    so.getNbtCache().updateCacheFromNew();
                } else {
                    Weather.LOGGER.debug("error syncing storm, cant find by ID: " + ID + ", probably due to client resetting and waiting on full resync (this is ok)");
                    //Weather.dbgStackTrace();
                }
                break;
            }
            case "syncVolcanoNew": {
                Weather.LOGGER.debug("creating client side volcano");
                NbtCompound stormNBT = parNBT.getCompound("data");
                //long ID = stormNBT.getLong("ID");

                Volcano so = new Volcano(ClientTickHandler.weatherManager);
                so.nbtSyncFromServer(stormNBT);

                addVolcano(so);
                break;
            }
            case "syncVolcanoRemove": {
                Weather.LOGGER.debug("removing client side volcano");
                NbtCompound stormNBT = parNBT.getCompound("data");
                long ID = stormNBT.getLong("ID");

                Volcano so = volcanoesById.get(ID);
                if (so != null) {
                    removeVolcano(ID);
                }
                break;
            }
            case "syncVolcanoUpdate": {
                Weather.LOGGER.debug("updating client side volcano");
                NbtCompound stormNBT = parNBT.getCompound("data");
                long ID = stormNBT.getLong("ID");

                Volcano so = volcanoesById.get(ID);
                if (so != null) {
                    so.nbtSyncFromServer(stormNBT);
                } else {
                    Weather.LOGGER.debug("error syncing volcano, cant find by ID: " + ID);
                }
                break;
            }
            case "syncWindUpdate": {
                //Weather.dbg("updating client side wind");

                NbtCompound nbt = parNBT.getCompound("data");

                windManager.nbtSyncFromServer(nbt);
                break;
            }
            case "syncLightningNew": {
                //Weather.dbg("updating client side wind");

                NbtCompound nbt = parNBT.getCompound("data");

                int posXS = nbt.getInt("posX");
                int posYS = nbt.getInt("posY");
                int posZS = nbt.getInt("posZ");

                boolean custom = nbt.getBoolean("custom");

                //Weather.dbg("uhhh " + parNBT);

                Entity ent;
                if (!custom) {
                    ent = new EntityLightningBolt(getWorld(), posXS, posYS, posZS);
                } else {
                    ent = new EntityLightningBoltCustom(getWorld(), posXS, posYS, posZS);
                }
                ent.serverPosX = posXS;
                ent.serverPosY = posYS;
                ent.serverPosZ = posZS;
                ent.yaw = 0.0F;
                ent.pitch = 0.0F;
                ent.setEntityId(nbt.getInt("entityID"));
                getWorld().addWeatherEffect(ent);
                break;
            }
            case "syncWeatherUpdate":
                vanillaRainActiveOnServer = parNBT.getBoolean("isVanillaRainActiveOnServer");
                vanillaThunderActiveOnServer = parNBT.getBoolean("isVanillaThunderActiveOnServer");
                vanillaRainTimeOnServer = parNBT.getInt("vanillaRainTimeOnServer");
                break;
        }
    }

    public void addWeatheredParticle(Particle particle) {
        listWeatherEffectedParticles.add(particle);
    }

    @Override
    public void reset() {
        super.reset();

        listWeatherEffectedParticles.clear();

        closestStormCached = null;
    }
}
