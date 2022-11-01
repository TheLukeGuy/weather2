package weather2.weathersystem;

import CoroUtil.util.CoroUtilFile;
import CoroUtil.util.CoroUtilPhysics;
import CoroUtil.util.Vec3;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.commons.io.FileUtils;
import weather2.ServerTickHandler;
import weather2.Weather;
import weather2.config.ConfigStorm;
import weather2.volcano.VolcanoObject;
import weather2.weathersystem.storm.SandstormObject;
import weather2.weathersystem.storm.StormObject;
import weather2.weathersystem.storm.WeatherObject;
import weather2.weathersystem.storm.WeatherObjectType;
import weather2.weathersystem.wind.WindManager;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class WeatherManager {
    public RegistryKey<World> world;

    private final List<WeatherObject> storms = new ArrayList<>();
    public Map<Long, WeatherObject> stormsById = new HashMap<>();
    public Map<Integer, List<StormObject>> stormsByLayer = new HashMap<>();

    private final List<VolcanoObject> volcanoes = new ArrayList<>();
    public Map<Long, VolcanoObject> volcanoesById = new HashMap<>();

    public WindManager windManager = new WindManager(this);

    // Only used on the client
    public boolean vanillaRainActiveOnServer = false;
    public boolean vanillaThunderActiveOnServer = false;
    public int vanillaRainTimeOnServer = 0;

    public long lastStormFormed = 0;
    public long lastSandstormFormed = 0;

    // 0 = none, 1 = usual max overcast
    public float cloudIntensity = 1f;

    private final Set<Long> listWeatherBlockDamageDeflector = new HashSet<>();

    public WeatherManager(RegistryKey<World> world) {
        this.world = world;
        stormsByLayer.put(0, new ArrayList<>());
        stormsByLayer.put(1, new ArrayList<>());
        stormsByLayer.put(2, new ArrayList<>());
    }

    public void reset() {
        for (WeatherObject storm : storms) {
            storm.reset();
        }

        storms.clear();
        stormsById.clear();
        for (List<StormObject> layerStorms : stormsByLayer.values()) {
            layerStorms.clear();
        }

        for (VolcanoObject volcano : volcanoes) {
            volcano.reset();
        }

        volcanoes.clear();
        volcanoesById.clear();

        windManager.reset();
    }

    public World getWorld() {
        return null;
    }

    public void tick() {
        World world = getWorld();
        if (world == null) {
            return;
        }

        // Tick storms
        for (WeatherObject storm : storms) {
            if (!storm.isDead) {
                storm.tick();
            } else if (this instanceof ServerWeatherManager) {
                removeStormObject(storm.ID);
                ((ServerWeatherManager) this).syncStormRemove(storm);
            } else if (world.isClient) {
                Weather.dbg("WARNING!!! - detected isDead storm object still in client side list, had to remove " +
                        "storm object with ID " + storm.ID + " from client side, wasn't properly removed via main " +
                        "channels");
                removeStormObject(storm.ID);
            }
        }

        // Tick volcanoes
        for (VolcanoObject volcano : volcanoes) {
            volcano.tick();
        }

        // Tick wind
        windManager.tick();
    }

    public void tickRender(float partialTick) {
        World world = getWorld();
        if (world == null) {
            return;
        }

        // Tick storms
        for (WeatherObject storm : storms) {
            if (storm != null) {
                storm.tickRender(partialTick);
            }
        }
    }

    public List<WeatherObject> getStorms() {
        return storms;
    }

    public List<StormObject> getStormsByLayer(int layer) {
        return stormsByLayer.get(layer);
    }

    public StormObject getStormById(long id) {
        WeatherObject storm = stormsById.get(id);
        if (storm instanceof StormObject) {
            return (StormObject) storm;
        } else {
            return null;
        }
    }

    public void addStormObject(WeatherObject so) {
        if (!stormsById.containsKey(so.ID)) {
            storms.add(so);
            stormsById.put(so.ID, so);
            if (so instanceof StormObject) {
                StormObject so2 = (StormObject) so;
                stormsByLayer.get(so2.layer).add(so2);
            }
        } else {
            Weather.dbg("Weather2 WARNING!!! Received new storm create for an ID that is already active! design bug or edgecase with PlayerEvent.Clone, ID: " + so.ID);
            Weather.dbgStackTrace();
        }
    }

    public void removeStormObject(long ID) {
        WeatherObject so = stormsById.get(ID);

        if (so != null) {
            so.setDead();
            storms.remove(so);
            stormsById.remove(ID);
            if (so instanceof StormObject) {
                StormObject so2 = (StormObject) so;
                stormsByLayer.get(so2.layer).remove(so2);
            }
        } else {
            Weather.dbg("error looking up storm ID on server for removal: " + ID + " - lookup count: " + stormsById.size() + " - last used ID: " + WeatherObject.lastUsedStormID);
        }
    }

    public List<VolcanoObject> getVolcanoObjects() {
        return volcanoes;
    }

    public void addVolcanoObject(VolcanoObject so) {
        if (!volcanoesById.containsKey(so.ID)) {
            volcanoes.add(so);
            volcanoesById.put(so.ID, so);
        } else {
            Weather.dbg("Weather2 WARNING!!! Client received new volcano create for an ID that is already active! design bug");
        }
    }

    public void removeVolcanoObject(long ID) {
        VolcanoObject vo = volcanoesById.get(ID);

        if (vo != null) {
            vo.setDead();
            volcanoes.remove(vo);
            volcanoesById.remove(ID);

            Weather.dbg("removing volcano");
        }
    }

    public StormObject getClosestStormAny(Vec3 parPos, double maxDist) {
        return getClosestStorm(parPos, maxDist, -1, true);
    }

    public StormObject getClosestStorm(Vec3 parPos, double maxDist, int severityFlagMin) {
        return getClosestStorm(parPos, maxDist, severityFlagMin, false);
    }

    public StormObject getClosestStorm(Vec3 parPos, double maxDist, int severityFlagMin, boolean orRain) {
        StormObject closestStorm = null;
        double closestDist = 9999999;

        List<WeatherObject> listStorms = getStorms();

        for (WeatherObject wo : listStorms) {
            if (wo instanceof StormObject) {
                StormObject storm = (StormObject) wo;
                if (storm.isDead) continue;
                double dist = storm.pos.distanceTo(parPos);
                if (dist < closestDist && dist <= maxDist) {
                    if ((storm.attrib_precipitation && orRain) || (severityFlagMin == -1 || storm.levelCurIntensityStage >= severityFlagMin)) {
                        closestStorm = storm;
                        closestDist = dist;
                    }
                }
            }

        }

        return closestStorm;
    }

    public boolean isPrecipitatingAt(BlockPos pos) {
        return isPrecipitatingAt(new Vec3(pos));
    }

    public boolean isPrecipitatingAt(Vec3 parPos) {
        List<WeatherObject> listStorms = getStorms();

        for (WeatherObject wo : listStorms) {
            if (wo instanceof StormObject) {
                StormObject storm = (StormObject) wo;
                if (storm.isDead) continue;
                if (storm.attrib_precipitation) {
                    double dist = storm.pos.distanceTo(parPos);
                    if (dist < storm.size) {
                        return true;
                    }
                }

            }

        }

        return false;
    }

    /**
     * Gets the most intense sandstorm, used for effects and sounds
     */
    public SandstormObject getClosestSandstormByIntensity(Vec3 parPos/*, double maxDist*/) {
        SandstormObject bestStorm = null;
        double closestDist = 9999999;
        double mostIntense = 0;

        List<WeatherObject> listStorms = getStorms();

        for (WeatherObject wo : listStorms) {
            if (wo instanceof SandstormObject) {
                SandstormObject sandstorm = (SandstormObject) wo;
                if (sandstorm.isDead) continue;

                List<Vec3> points = sandstorm.getSandstormAsShape();

                double scale = sandstorm.getSandstormScale();
                boolean inStorm = CoroUtilPhysics.isInConvexShape(parPos, points);
                double dist = CoroUtilPhysics.getDistanceToShape(parPos, points);
                //if best is within storm, compare intensity
                if (inStorm) {
                    //System.out.println("in storm");
                    closestDist = 0;
                    if (scale > mostIntense) {
                        mostIntense = scale;
                        bestStorm = sandstorm;
                    }
                    //if best is not within storm, compare distance to shape
                } else if (closestDist > 0/* && dist < maxDist*/) {
                    if (dist < closestDist) {
                        closestDist = dist;
                        bestStorm = sandstorm;
                    }
                }
            }
        }

        return bestStorm;
    }

    public List<WeatherObject> getStormsAroundForDeflector(Vec3 parPos, double maxDist) {
        List<WeatherObject> storms = new ArrayList<>();

        for (int i = 0; i < getStorms().size(); i++) {
            WeatherObject wo = getStorms().get(i);
            if (wo.isDead) continue;
            if (wo instanceof StormObject) {
                StormObject storm = (StormObject) wo;
                if (storm.pos.distanceTo(parPos) < maxDist && ((storm.attrib_precipitation && ConfigStorm.Storm_Deflector_RemoveRainstorms) || storm.levelCurIntensityStage >= ConfigStorm.Storm_Deflector_MinStageRemove)) {
                    storms.add(storm);
                }
            } else if (wo instanceof SandstormObject && ConfigStorm.Storm_Deflector_RemoveSandstorms) {
                SandstormObject sandstorm = (SandstormObject) wo;
                List<Vec3> points = sandstorm.getSandstormAsShape();
                double distToStorm = CoroUtilPhysics.getDistanceToShape(parPos, points);
                if (distToStorm < maxDist) {
                    storms.add(wo);
                }
            }
        }

        return storms;
    }

    public List<WeatherObject> getStormsAround(Vec3 parPos, double maxDist) {
        List<WeatherObject> storms = new ArrayList<>();

        for (int i = 0; i < getStorms().size(); i++) {
            WeatherObject wo = getStorms().get(i);
            if (wo.isDead) continue;
            if (wo instanceof StormObject) {
                StormObject storm = (StormObject) wo;
                if (storm.pos.distanceTo(parPos) < maxDist && (storm.attrib_precipitation || storm.levelCurIntensityStage > StormObject.STATE_NORMAL)) {
                    storms.add(storm);
                }
            } else if (wo instanceof SandstormObject) {
                SandstormObject sandstorm = (SandstormObject) wo;
                List<Vec3> points = sandstorm.getSandstormAsShape();
                double distToStorm = CoroUtilPhysics.getDistanceToShape(parPos, points);
                if (distToStorm < maxDist) {
                    storms.add(wo);
                }
            }
        }

        return storms;
    }

    public void writeToFile() {
        NbtCompound mainNBT = new NbtCompound();
        NbtCompound listVolcanoesNBT = new NbtCompound();
        for (VolcanoObject td : volcanoes) {
            NbtCompound teamNBT = new NbtCompound();
            td.writeToNBT(teamNBT);
            listVolcanoesNBT.put("volcano_" + td.ID, teamNBT);
        }
        mainNBT.put("volcanoData", listVolcanoesNBT);
        mainNBT.putLong("lastUsedIDVolcano", VolcanoObject.lastUsedID);

        NbtCompound listStormsNBT = new NbtCompound();
        for (WeatherObject obj : storms) {
            obj.getNbtCache().setUpdateForced(true);
            obj.writeToNBT();
            obj.getNbtCache().setUpdateForced(false);
            listStormsNBT.put("storm_" + obj.ID, obj.getNbtCache().getNewNBT());
        }
        mainNBT.put("stormData", listStormsNBT);
        mainNBT.putLong("lastUsedIDStorm", WeatherObject.lastUsedStormID);

        mainNBT.putLong("lastStormFormed", lastStormFormed);

        mainNBT.putLong("lastSandstormFormed", lastSandstormFormed);

        mainNBT.putFloat("cloudIntensity", this.cloudIntensity);

        mainNBT.put("windMan", windManager.writeToNBT(new NbtCompound()));

        String saveFolder = CoroUtilFile.getWorldSaveFolderPath() + CoroUtilFile.getWorldFolderName() + "weather2" + File.separator;

        try {
            //Write out to file
            if (!(new File(saveFolder).exists())) (new File(saveFolder)).mkdirs();
            FileOutputStream fos = new FileOutputStream(saveFolder + "WeatherData_" + dim + ".dat");
            NbtIo.writeCompressed(mainNBT, fos);
            fos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void readFromFile() {
        NbtCompound rtsNBT = new NbtCompound();

        String saveFolder = CoroUtilFile.getWorldSaveFolderPath() + CoroUtilFile.getWorldFolderName() + "weather2" + File.separator;

        boolean readFail = false;

        try {
            if ((new File(saveFolder + "WeatherData_" + dim + ".dat")).exists()) {
                rtsNBT = NbtIo.readCompressed(Files.newInputStream(Paths.get(saveFolder + "WeatherData_" + dim + ".dat")));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            readFail = true;
        }

        //If reading file was ok, make a backup and shift names for second backup
        if (!readFail) {
            try {
                File tmp = (new File(saveFolder + "WeatherData_" + dim + "_BACKUP0.dat"));
                if (tmp.exists())
                    FileUtils.copyFile(tmp, (new File(saveFolder + "WeatherData_" + dim + "_BACKUP1.dat")));
                if ((new File(saveFolder + "WeatherData_" + dim + ".dat").exists()))
                    FileUtils.copyFile((new File(saveFolder + "WeatherData_" + dim + ".dat")), (new File(saveFolder + "WeatherData_" + dim + "_BACKUP0.dat")));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            System.out.println("WARNING! Weather2 File: WeatherData.dat failed to load, automatically restoring to backup from previous game run");
            try {
                //auto restore from most recent backup
                if ((new File(saveFolder + "WeatherData_" + dim + "_BACKUP0.dat")).exists()) {
                    rtsNBT = NbtIo.readCompressed(Files.newInputStream(Paths.get(saveFolder + "WeatherData_" + dim + "_BACKUP0.dat")));
                } else {
                    System.out.println("WARNING! Failed to find backup file WeatherData_BACKUP0.dat, nothing loaded");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println("WARNING! Error loading backup file WeatherData_BACKUP0.dat, nothing loaded");
            }
        }

        lastStormFormed = rtsNBT.getLong("lastStormFormed");
        lastSandstormFormed = rtsNBT.getLong("lastSandstormFormed");

        //prevent setting to 0 for worlds updating to new weather version
        if (rtsNBT.contains("cloudIntensity")) {
            cloudIntensity = rtsNBT.getFloat("cloudIntensity");
        }

        VolcanoObject.lastUsedID = rtsNBT.getLong("lastUsedIDVolcano");
        WeatherObject.lastUsedStormID = rtsNBT.getLong("lastUsedIDStorm");

        windManager.readFromNBT(rtsNBT.getCompound("windMan"));

        NbtCompound nbtVolcanoes = rtsNBT.getCompound("volcanoData");

        Iterator<String> it = nbtVolcanoes.getKeys().iterator();

        while (it.hasNext()) {
            String tagName = it.next();
            NbtCompound teamData = nbtVolcanoes.getCompound(tagName);

            VolcanoObject to = new VolcanoObject(ServerTickHandler.lookupDimToWeatherMan.get(0)/*-1, -1, null*/);
            try {
                to.readFromNBT(teamData);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            //to.initAITree();
            addVolcanoObject(to);

            //THIS LINE NEEDS REFINING FOR PLAYERS WHO JOIN AFTER THE FACT!!!
            ((ServerWeatherManager) (this)).syncVolcanoNew(to);

            to.initPost();
        }

        NbtCompound nbtStorms = rtsNBT.getCompound("stormData");

        it = nbtStorms.getKeys().iterator();

        while (it.hasNext()) {
            String tagName = it.next();
            NbtCompound data = nbtStorms.getCompound(tagName);

            if (ServerTickHandler.lookupDimToWeatherMan.get(dim) != null) {
                WeatherObject wo = null;
                if (data.getInt("weatherObjectType") == WeatherObjectType.CLOUD.ordinal()) {
                    wo = new StormObject(this/*-1, -1, null*/);
                } else if (data.getInt("weatherObjectType") == WeatherObjectType.SAND.ordinal()) {
                    wo = new SandstormObject(this);
                    //initStormNew???
                }
                try {
                    wo.getNbtCache().setNewNBT(data);
                    wo.readFromNBT();
                    wo.getNbtCache().updateCacheFromNew();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                addStormObject(wo);

                //TODO: possibly unneeded/redundant/bug inducing, packets will be sent upon request from client
                ((ServerWeatherManager) (this)).syncStormNew(wo);
            } else {
                System.out.println("WARNING: trying to load storm objects for missing dimension: " + dim);
            }
        }
    }

    public WindManager getWindManager() {
        return this.windManager;
    }

    public HashSet<Long> getListWeatherBlockDamageDeflector() {
        return listWeatherBlockDamageDeflector;
    }
}
