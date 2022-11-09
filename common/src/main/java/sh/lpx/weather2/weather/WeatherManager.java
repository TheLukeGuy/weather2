package sh.lpx.weather2.weather;

import sh.lpx.weather2.ServerTickHandler;
import sh.lpx.weather2.Weather;
import sh.lpx.weather2.config.ConfigStorm;
import sh.lpx.weather2.volcano.Volcano;
import sh.lpx.weather2.weather.storm.CloudStorm;
import sh.lpx.weather2.weather.storm.SandStorm;
import sh.lpx.weather2.weather.storm.StormType;
import sh.lpx.weather2.util.WeatherUtilFile;
import sh.lpx.weather2.util.WeatherUtilPhysics;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import org.apache.commons.io.FileUtils;
import sh.lpx.weather2.weather.storm.Storm;
import sh.lpx.weather2.weather.wind.WindManager;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class WeatherManager {
    public RegistryKey<World> world;

    private final List<Storm> storms = new ArrayList<>();
    public Map<Long, Storm> stormsById = new HashMap<>();
    public List<List<CloudStorm>> cloudStormsByLayer = new ArrayList<>();

    private final List<Volcano> volcanoes = new ArrayList<>();
    public Map<Long, Volcano> volcanoesById = new HashMap<>();

    public WindManager windManager = new WindManager(this);

    // Only used on the client
    public boolean vanillaRainActiveOnServer = false;
    public boolean vanillaThunderActiveOnServer = false;
    public int vanillaRainTimeOnServer = 0;

    public long lastCloudStormFormed = 0;
    public long lastSandstormFormed = 0;

    // 0 = none, 1 = usual max overcast
    public float cloudIntensity = 1f;

    private final Set<Long> listWeatherBlockDamageDeflector = new HashSet<>();

    public WeatherManager(RegistryKey<World> world) {
        this.world = world;
        for (int i = 0; i < 3; i++) {
            cloudStormsByLayer.add(new ArrayList<>());
        }
    }

    public void reset() {
        for (Storm storm : storms) {
            storm.reset();
        }

        storms.clear();
        stormsById.clear();
        for (List<CloudStorm> layerStorms : cloudStormsByLayer) {
            layerStorms.clear();
        }

        for (Volcano volcano : volcanoes) {
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
        for (Storm storm : storms) {
            if (!storm.dead) {
                storm.tick();
            } else if (this instanceof ServerWeatherManager) {
                removeStorm(storm.id);
                ((ServerWeatherManager) this).syncStormRemove(storm);
            } else if (world.isClient) {
                Weather.LOGGER.warn("Found a dead storm with ID " + storm.id + " that wasn't properly removed!");
                removeStorm(storm.id);
            }
        }

        // Tick volcanoes
        for (Volcano volcano : volcanoes) {
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
        for (Storm storm : storms) {
            if (storm != null) {
                storm.tickRender(partialTick);
            }
        }
    }

    public List<Storm> getStorms() {
        return storms;
    }

    public List<CloudStorm> getCloudStormsByLayer(int layer) {
        return cloudStormsByLayer.get(layer);
    }

    public CloudStorm getCloudStormById(long id) {
        Storm storm = stormsById.get(id);
        if (storm instanceof CloudStorm) {
            return (CloudStorm) storm;
        } else {
            return null;
        }
    }

    public void addStorm(Storm storm) {
        if (stormsById.containsKey(storm.id)) {
            Weather.LOGGER.error("Tried adding a storm with ID " + storm.id + ", but that ID is already active!");
            return;
        }

        storms.add(storm);
        stormsById.put(storm.id, storm);
        if (storm instanceof CloudStorm) {
            CloudStorm cloudStorm = (CloudStorm) storm;
            cloudStormsByLayer.get(cloudStorm.layer).add(cloudStorm);
        }
    }

    public void removeStorm(long id) {
        Storm storm = stormsById.get(id);
        if (storm == null) {
            Weather.LOGGER.warn("Tried removing a storm with ID " + id + ", but no storm with that ID exists!");
            return;
        }

        storm.setDead();
        storms.remove(storm);
        stormsById.remove(id);
        if (storm instanceof CloudStorm) {
            CloudStorm cloudStorm = (CloudStorm) storm;
            cloudStormsByLayer.get(cloudStorm.layer).remove(cloudStorm);
        }
    }

    public List<Volcano> getVolcanoes() {
        return volcanoes;
    }

    public void addVolcano(Volcano volcano) {
        if (volcanoesById.containsKey(volcano.ID)) {
            Weather.LOGGER.error("Tried adding a volcano with ID " + volcano.ID + ", but that ID is already active!");
            return;
        }
        volcanoes.add(volcano);
        volcanoesById.put(volcano.ID, volcano);
    }

    public void removeVolcano(long id) {
        Volcano volcano = volcanoesById.get(id);
        if (volcano == null) {
            Weather.LOGGER.warn("Tried removing a volcano with ID " + id + ", but no volcano with that ID exists!");
            return;
        }

        volcano.setDead();
        volcanoes.remove(volcano);
        volcanoesById.remove(id);
    }

    public CloudStorm getClosestCloudStorm(Vec3d pos, double maxDistance) {
        return getClosestCloudStorm(pos, maxDistance, -1, true);
    }

    public CloudStorm getClosestCloudStorm(Vec3d pos, double maxDistance, int minIntensity) {
        return getClosestCloudStorm(pos, maxDistance, minIntensity, false);
    }

    public CloudStorm getClosestCloudStorm(Vec3d pos, double maxDistance, int minIntensity, boolean orRain) {
        CloudStorm closestStorm = null;
        double closestDistance = Double.MAX_VALUE;

        for (Storm storm : storms) {
            if (!(storm instanceof CloudStorm)) {
                continue;
            }
            CloudStorm cloudStorm = (CloudStorm) storm;
            if (cloudStorm.dead) {
                continue;
            }

            double distance = cloudStorm.pos.distanceTo(pos);
            if (distance < closestDistance && distance <= maxDistance) {
                if ((cloudStorm.attrib_precipitation && orRain) || (minIntensity == -1 || cloudStorm.levelCurIntensityStage >= minIntensity)) {
                    closestStorm = cloudStorm;
                    closestDistance = distance;
                }
            }
        }

        return closestStorm;
    }

    public boolean isPrecipitatingAt(BlockPos pos) {
        return isPrecipitatingAt(new Vec3d(pos.getX(), pos.getY(), pos.getZ()));
    }

    public boolean isPrecipitatingAt(Vec3d pos) {
        for (Storm storm : storms) {
            if (!(storm instanceof CloudStorm)) {
                continue;
            }

            CloudStorm cloudStorm = (CloudStorm) storm;
            if (cloudStorm.dead) {
                continue;
            }
            if (cloudStorm.attrib_precipitation) {
                double distance = cloudStorm.pos.distanceTo(pos);
                if (distance < cloudStorm.size) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Gets the most intense sandstorm, used for effects and sounds
     */
    public SandStorm getClosestSandStormByIntensity(Vec3d pos) {
        SandStorm closestStorm = null;
        double closestDistance = Double.MAX_VALUE;
        double highestIntensity = 0;

        for (Storm storm : storms) {
            if (!(storm instanceof SandStorm)) {
                continue;
            }
            SandStorm sandStorm = (SandStorm) storm;
            if (sandStorm.dead) {
                continue;
            }

            List<Vec3d> points = sandStorm.getSandstormAsShape();

            double intensity = sandStorm.getSandstormScale();
            boolean inStorm = WeatherUtilPhysics.isInConvexShape(pos, points);
            double distance = WeatherUtilPhysics.getDistanceToShape(pos, points);
            if (inStorm) {
                // If the position is within the storm, compare intensities
                closestDistance = 0;
                if (intensity > highestIntensity) {
                    highestIntensity = intensity;
                    closestStorm = sandStorm;
                }
            } else if (closestDistance > 0) {
                // If the position is not within the storm, compare distances
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestStorm = sandStorm;
                }
            }
        }

        return closestStorm;
    }

    public List<Storm> getStormsAround(Vec3d pos, double maxDistance) {
        return getStormsAround(pos, maxDistance, false);
    }

    public List<Storm> getStormsAround(Vec3d pos, double maxDistance, boolean forDeflector) {
        List<Storm> results = new ArrayList<>();
        for (Storm storm : storms) {
            if (storm.dead) continue;
            if (storm instanceof CloudStorm) {
                CloudStorm cloudStorm = (CloudStorm) storm;

                boolean withinMaxDistance = cloudStorm.pos.distanceTo(pos) < maxDistance;
                boolean raining = cloudStorm.attrib_precipitation && (!forDeflector || ConfigStorm.Storm_Deflector_RemoveRainstorms);
                boolean intense;
                if (forDeflector) {
                    intense = cloudStorm.levelCurIntensityStage >= ConfigStorm.Storm_Deflector_MinStageRemove;
                } else {
                    intense = cloudStorm.levelCurIntensityStage > CloudStorm.STATE_NORMAL;
                }
                if (withinMaxDistance && (raining || intense)) {
                    results.add(cloudStorm);
                }
            } else if (storm instanceof SandStorm && (!forDeflector || ConfigStorm.Storm_Deflector_RemoveSandstorms)) {
                List<Vec3d> points = ((SandStorm) storm).getSandstormAsShape();
                double distanceToStorm = WeatherUtilPhysics.getDistanceToShape(pos, points);
                if (distanceToStorm < maxDistance) {
                    results.add(storm);
                }
            }
        }
        return results;
    }

    public void writeToFile() {
        NbtCompound mainNBT = new NbtCompound();
        NbtCompound listVolcanoesNBT = new NbtCompound();
        for (Volcano td : volcanoes) {
            NbtCompound teamNBT = new NbtCompound();
            td.writeToNBT(teamNBT);
            listVolcanoesNBT.put("volcano_" + td.ID, teamNBT);
        }
        mainNBT.put("volcanoData", listVolcanoesNBT);
        mainNBT.putLong("lastUsedIDVolcano", Volcano.lastUsedID);

        NbtCompound listStormsNBT = new NbtCompound();
        for (Storm obj : storms) {
            obj.getNbtCache().setForcingUpdates(true);
            obj.writeToNBT();
            obj.getNbtCache().setForcingUpdates(false);
            listStormsNBT.put("storm_" + obj.id, obj.getNbtCache().getNewNbt());
        }
        mainNBT.put("stormData", listStormsNBT);
        mainNBT.putLong("lastUsedIDStorm", Storm.lastStormId);

        mainNBT.putLong("lastStormFormed", lastCloudStormFormed);

        mainNBT.putLong("lastSandstormFormed", lastSandstormFormed);

        mainNBT.putFloat("cloudIntensity", this.cloudIntensity);

        mainNBT.put("windMan", windManager.writeToNBT(new NbtCompound()));

        String saveFolder = WeatherUtilFile.getWorldSaveFolderPath() + WeatherUtilFile.getWorldFolderName() + "sh/lpx/weather2" + File.separator;

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

        String saveFolder = WeatherUtilFile.getWorldSaveFolderPath() + WeatherUtilFile.getWorldFolderName() + "sh/lpx/weather2" + File.separator;

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

        lastCloudStormFormed = rtsNBT.getLong("lastStormFormed");
        lastSandstormFormed = rtsNBT.getLong("lastSandstormFormed");

        //prevent setting to 0 for worlds updating to new weather version
        if (rtsNBT.contains("cloudIntensity")) {
            cloudIntensity = rtsNBT.getFloat("cloudIntensity");
        }

        Volcano.lastUsedID = rtsNBT.getLong("lastUsedIDVolcano");
        Storm.lastStormId = rtsNBT.getLong("lastUsedIDStorm");

        windManager.readFromNBT(rtsNBT.getCompound("windMan"));

        NbtCompound nbtVolcanoes = rtsNBT.getCompound("volcanoData");

        Iterator<String> it = nbtVolcanoes.getKeys().iterator();

        while (it.hasNext()) {
            String tagName = it.next();
            NbtCompound teamData = nbtVolcanoes.getCompound(tagName);

            Volcano to = new Volcano(ServerTickHandler.lookupDimToWeatherMan.get(0)/*-1, -1, null*/);
            try {
                to.readFromNBT(teamData);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            //to.initAITree();
            addVolcano(to);

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
                Storm wo = null;
                if (data.getInt("weatherObjectType") == StormType.CLOUD.ordinal()) {
                    wo = new CloudStorm(this/*-1, -1, null*/);
                } else if (data.getInt("weatherObjectType") == StormType.SAND.ordinal()) {
                    wo = new SandStorm(this);
                    //initStormNew???
                }
                try {
                    wo.getNbtCache().setNewNbt(data);
                    wo.readFromNBT();
                    wo.getNbtCache().updateCacheFromNew();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                addStorm(wo);

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

    public Set<Long> getListWeatherBlockDamageDeflector() {
        return listWeatherBlockDamageDeflector;
    }
}
