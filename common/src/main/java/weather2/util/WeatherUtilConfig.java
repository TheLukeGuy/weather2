package weather2.util;

import CoroUtil.config.ConfigCoroUtil;
import CoroUtil.util.CoroUtilFile;
import modconfig.ConfigMod;
import modconfig.IConfigCategory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtInt;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import org.apache.commons.lang3.StringUtils;
import weather2.ServerTickHandler;
import weather2.Weather;
import weather2.config.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WeatherUtilConfig {
    public static List<Integer> listDimensionsWeather = new ArrayList<>();
    public static List<Integer> listDimensionsClouds = new ArrayList<>();
    //used for deadly storms and sandstorms
    public static List<Integer> listDimensionsStorms = new ArrayList<>();
    public static List<Integer> listDimensionsWindEffects = new ArrayList<>();

    public static int CMD_BTN_PERF_STORM = 2;
    public static int CMD_BTN_PERF_NATURE = 3;
    public static int CMD_BTN_PERF_PRECIPRATE = 12;
    public static int CMD_BTN_PERF_SHADERS_PARTICLE = 18;
    public static int CMD_BTN_PERF_SHADERS_FOLIAGE = 19;

    public static int CMD_BTN_COMP_STORM = 4;
    public static int CMD_BTN_COMP_LOCK = 5;
    public static int CMD_BTN_COMP_PARTICLEPRECIP = 6;
    public static int CMD_BTN_COMP_SNOWFALLBLOCKS = 7;
    public static int CMD_BTN_COMP_LEAFFALLBLOCKS = 8;
    public static int CMD_BTN_COMP_PARTICLESNOMODS = 13;

    public static int CMD_BTN_PREF_RATEOFSTORM = 9;
    public static int CMD_BTN_PREF_CHANCEOFSTORM = 14;
    public static int CMD_BTN_PREF_CHANCEOFRAIN = 10;
    public static int CMD_BTN_PREF_BLOCKDESTRUCTION = 11;
    public static int CMD_BTN_PREF_TORNADOANDCYCLONES = 15;
    public static int CMD_BTN_PREF_SANDSTORMS = 16;
    public static int CMD_BTN_PREF_GLOBALRATE = 17;

    public static int CMD_BTN_HIGHEST_ID = 19;

    public static List<String> LIST_RATES = new ArrayList<>(Arrays.asList("High", "Medium", "Low"));
    public static List<String> LIST_RATES2 = new ArrayList<>(Arrays.asList("High", "Medium", "Low", "None"));
    public static List<String> LIST_TOGGLE = new ArrayList<>(Arrays.asList("Off", "On"));
    public static List<String> LIST_CHANCE = new ArrayList<>(Arrays.asList("1/2 Day", "1 Day", "2 Days", "3 Days", "4 Days", "5 Days", "6 Days", "7 Days", "8 Days", "9 Days", "10 Days", "Never"));

    public static List<String> LIST_STORMSWHEN = new ArrayList<>(Arrays.asList("Local Biomes", "Global Overcast"));
    public static List<String> LIST_LOCK = new ArrayList<>(Arrays.asList("Always Off", "Always On", "Don't lock"));
    public static List<String> LIST_GLOBALRATE = new ArrayList<>(Arrays.asList("Rand player", "Each player"));

    public static List<Integer> listSettingsClient = new ArrayList<>();
    public static List<Integer> listSettingsServer = new ArrayList<>();

    //for caching server data on client side (does not pertain to client only settings)
    public static NbtCompound nbtClientCache = new NbtCompound();

    //actual data that gets written out to disk
    public static NbtCompound nbtServerData = new NbtCompound();
    public static NbtCompound nbtClientData = new NbtCompound();

    static {
        listSettingsClient.add(CMD_BTN_PERF_STORM);
        listSettingsClient.add(CMD_BTN_PERF_NATURE);
        listSettingsClient.add(CMD_BTN_COMP_PARTICLEPRECIP);
        listSettingsClient.add(CMD_BTN_PERF_PRECIPRATE);
        listSettingsClient.add(CMD_BTN_COMP_PARTICLESNOMODS);
        listSettingsClient.add(CMD_BTN_PERF_SHADERS_PARTICLE);
        listSettingsClient.add(CMD_BTN_PERF_SHADERS_FOLIAGE);

        listSettingsServer.add(CMD_BTN_COMP_STORM);
        listSettingsServer.add(CMD_BTN_COMP_LOCK);
        listSettingsServer.add(CMD_BTN_COMP_SNOWFALLBLOCKS);
        listSettingsServer.add(CMD_BTN_COMP_LEAFFALLBLOCKS);
        listSettingsServer.add(CMD_BTN_PREF_RATEOFSTORM);
        listSettingsServer.add(CMD_BTN_PREF_CHANCEOFSTORM);
        listSettingsServer.add(CMD_BTN_PREF_CHANCEOFRAIN);
        listSettingsServer.add(CMD_BTN_PREF_BLOCKDESTRUCTION);
        listSettingsServer.add(CMD_BTN_PREF_TORNADOANDCYCLONES);
        listSettingsServer.add(CMD_BTN_PREF_SANDSTORMS);
        listSettingsServer.add(CMD_BTN_PREF_GLOBALRATE);
    }

    //client should call this on detecting a close/save of GUI
    public static void processNBTToModConfigClient() {
        nbtSaveDataClient();

        Weather.LOGGER.debug("processNBTToModConfigClient");

        Weather.LOGGER.debug("nbtClientData: " + nbtClientData);

        try {
            if (nbtClientData.contains("btn_" + CMD_BTN_COMP_PARTICLEPRECIP)) {
                ConfigParticle.Particle_RainSnow = LIST_TOGGLE.get(nbtClientData.getInt("btn_" + CMD_BTN_COMP_PARTICLEPRECIP)).equalsIgnoreCase("on");
            }

            if (nbtClientData.contains("btn_" + CMD_BTN_PERF_STORM)) {
                if (LIST_RATES.get(nbtClientData.getInt("btn_" + CMD_BTN_PERF_STORM)).equalsIgnoreCase("high")) {
                    ConfigMisc.Cloud_ParticleSpawnDelay = 0;
                    ConfigStorm.Storm_ParticleSpawnDelay = 1;
                    ConfigParticle.Sandstorm_Particle_Debris_effect_rate = 1;
                    ConfigParticle.Sandstorm_Particle_Dust_effect_rate = 1;
                } else if (LIST_RATES.get(nbtClientData.getInt("btn_" + CMD_BTN_PERF_STORM)).equalsIgnoreCase("medium")) {
                    ConfigMisc.Cloud_ParticleSpawnDelay = 2;
                    ConfigStorm.Storm_ParticleSpawnDelay = 3;
                    ConfigParticle.Sandstorm_Particle_Debris_effect_rate = 0.6D;
                    ConfigParticle.Sandstorm_Particle_Dust_effect_rate = 0.6D;
                } else if (LIST_RATES.get(nbtClientData.getInt("btn_" + CMD_BTN_PERF_STORM)).equalsIgnoreCase("low")) {
                    ConfigMisc.Cloud_ParticleSpawnDelay = 5;
                    ConfigStorm.Storm_ParticleSpawnDelay = 5;
                    ConfigParticle.Sandstorm_Particle_Debris_effect_rate = 0.3D;
                    ConfigParticle.Sandstorm_Particle_Dust_effect_rate = 0.3D;
                }
            }

            if (nbtClientData.contains("btn_" + CMD_BTN_PERF_NATURE)) {
                if (LIST_RATES2.get(nbtClientData.getInt("btn_" + CMD_BTN_PERF_NATURE)).equalsIgnoreCase("high")) {
                    ConfigParticle.Wind_Particle_effect_rate = 1F;
                } else if (LIST_RATES2.get(nbtClientData.getInt("btn_" + CMD_BTN_PERF_NATURE)).equalsIgnoreCase("medium")) {
                    ConfigParticle.Wind_Particle_effect_rate = 0.7F;
                } else if (LIST_RATES2.get(nbtClientData.getInt("btn_" + CMD_BTN_PERF_NATURE)).equalsIgnoreCase("low")) {
                    ConfigParticle.Wind_Particle_effect_rate = 0.3F;
                } else if (LIST_RATES2.get(nbtClientData.getInt("btn_" + CMD_BTN_PERF_NATURE)).equalsIgnoreCase("none")) {
                    ConfigParticle.Wind_Particle_effect_rate = 0.0F;
                }
            }

            if (nbtClientData.contains("btn_" + CMD_BTN_PERF_PRECIPRATE)) {
                //ConfigMisc.Particle_RainSnow = true;
                if (LIST_RATES2.get(nbtClientData.getInt("btn_" + CMD_BTN_PERF_PRECIPRATE)).equalsIgnoreCase("high")) {
                    ConfigParticle.Precipitation_Particle_effect_rate = 1D;
                } else if (LIST_RATES2.get(nbtClientData.getInt("btn_" + CMD_BTN_PERF_PRECIPRATE)).equalsIgnoreCase("medium")) {
                    ConfigParticle.Precipitation_Particle_effect_rate = 0.7D;
                } else if (LIST_RATES2.get(nbtClientData.getInt("btn_" + CMD_BTN_PERF_PRECIPRATE)).equalsIgnoreCase("low")) {
                    ConfigParticle.Precipitation_Particle_effect_rate = 0.3D;
                } else if (LIST_RATES2.get(nbtClientData.getInt("btn_" + CMD_BTN_PERF_PRECIPRATE)).equalsIgnoreCase("none")) {
                    ConfigParticle.Precipitation_Particle_effect_rate = 0D;
                    //ConfigMisc.Particle_RainSnow = false;
                }
            }

            if (nbtClientData.contains("btn_" + CMD_BTN_COMP_PARTICLESNOMODS)) {
                ConfigParticle.Particle_VanillaAndWeatherOnly = LIST_TOGGLE.get(nbtClientData.getInt("btn_" + CMD_BTN_COMP_PARTICLESNOMODS)).equalsIgnoreCase("on");
            }

            if (nbtClientData.contains("btn_" + CMD_BTN_PERF_SHADERS_PARTICLE)) {
                int val = nbtClientData.getInt("btn_" + CMD_BTN_PERF_SHADERS_PARTICLE);
                if (val == 0) {
                    ConfigCoroUtil.particleShaders = false;
                } else if (val == 1) {
                    ConfigCoroUtil.particleShaders = true;
                }
            }

            if (nbtClientData.contains("btn_" + CMD_BTN_PERF_SHADERS_FOLIAGE)) {
                int val = nbtClientData.getInt("btn_" + CMD_BTN_PERF_SHADERS_FOLIAGE);
                if (val == 0) {
                    ConfigCoroUtil.foliageShaders = false;
                } else if (val == 1) {
                    ConfigCoroUtil.foliageShaders = true;
                }
            }

            NbtCompound nbtDims = nbtClientData.getCompound("dimData");
            //Iterator it = nbtDims.getTags().iterator();

            Weather.LOGGER.debug("before cl: " + listDimensionsWindEffects);

            for (String tagName : nbtDims.getKeys()) {
                NbtInt entry = (NbtInt) nbtDims.get(tagName);
                String[] values = tagName.split("_");

                if (values[2].equals("3")) {
                    int dimID = Integer.parseInt(values[1]);
                    if (entry.intValue() == 0) {
                        //if off
                        if (listDimensionsWindEffects.contains(dimID)) {
                            listDimensionsWindEffects.remove((Object) dimID);
                        }
                    } else {
                        //if on
                        if (!listDimensionsWindEffects.contains(dimID)) {
                            listDimensionsWindEffects.add(dimID);
                        }
                    }
                }
            }

            Weather.LOGGER.debug("after cl: " + listDimensionsWindEffects);

            processListsReverse();
        } catch (Exception ex) {
            ex.printStackTrace();
        }


        ConfigMod.forceSaveAllFilesFromRuntimeSettings();

        //work lists here

        //client nbt to client mod config setttings, and server nbt to server mod config settings
        //invoke whatever method modconfig uses to write out its data, for both its client and server side
    }

    //server should call this on detecting of a save request (close of GUI packet send)
    public static void processNBTToModConfigServer() {
        nbtSaveDataServer();

        Weather.LOGGER.debug("processNBTToModConfigServer");

        Weather.LOGGER.debug("nbtServerData: " + nbtServerData);

        //String modID = "weather2Misc";

        try {
            if (nbtServerData.contains("btn_" + CMD_BTN_COMP_STORM)) {
                ConfigMisc.overcastMode = LIST_STORMSWHEN.get(nbtServerData.getInt("btn_" + CMD_BTN_COMP_STORM)).equalsIgnoreCase("Global Overcast");
            }

            if (nbtServerData.contains("btn_" + CMD_BTN_COMP_LOCK)) {
                int val = nbtServerData.getInt("btn_" + CMD_BTN_COMP_LOCK);
                if (val == 1) {
                    ConfigMisc.lockServerWeatherMode = 1;
                } else if (val == 0) {
                    ConfigMisc.lockServerWeatherMode = 0;
                } else {
                    ConfigMisc.lockServerWeatherMode = -1;
                }
            }

            if (nbtServerData.contains("btn_" + CMD_BTN_COMP_SNOWFALLBLOCKS)) {
                ConfigSnow.Snow_PerformSnowfall = nbtServerData.getInt("btn_" + CMD_BTN_COMP_SNOWFALLBLOCKS) == 1;
                //ConfigSnow.Snow_ExtraPileUp = val;
            }

            if (nbtServerData.contains("btn_" + CMD_BTN_PREF_RATEOFSTORM)) {
                int numDays = nbtServerData.getInt("btn_" + CMD_BTN_PREF_RATEOFSTORM);
                if (numDays == 0) {
                    ConfigStorm.Player_Storm_Deadly_TimeBetweenInTicks = 12000;
                    ConfigStorm.Server_Storm_Deadly_TimeBetweenInTicks = 12000;
                } else if (numDays == 11) {
                    //potentially remove the 'never' clause from here in favor of the dimension specific disabling of 'storms' which is already used in code
                    //for now consider this a second layer of rules to the storm creation process, probably not a user friendly idea
                    ConfigStorm.Player_Storm_Deadly_TimeBetweenInTicks = -1;
                    ConfigStorm.Server_Storm_Deadly_TimeBetweenInTicks = -1;
                } else {
                    ConfigStorm.Player_Storm_Deadly_TimeBetweenInTicks = 24000 * numDays;
                    ConfigStorm.Server_Storm_Deadly_TimeBetweenInTicks = 24000 * numDays;
                }
            }

            if (nbtServerData.contains("btn_" + CMD_BTN_PREF_CHANCEOFSTORM)) {
                if (LIST_RATES2.get(nbtServerData.getInt("btn_" + CMD_BTN_PREF_CHANCEOFSTORM)).equalsIgnoreCase("high")) {
                    ConfigStorm.Player_Storm_Deadly_OddsTo1 = 30;
                    ConfigStorm.Server_Storm_Deadly_OddsTo1 = 30;
                } else if (LIST_RATES2.get(nbtServerData.getInt("btn_" + CMD_BTN_PREF_CHANCEOFSTORM)).equalsIgnoreCase("medium")) {
                    ConfigStorm.Player_Storm_Deadly_OddsTo1 = 45;
                    ConfigStorm.Server_Storm_Deadly_OddsTo1 = 45;
                } else if (LIST_RATES2.get(nbtServerData.getInt("btn_" + CMD_BTN_PREF_CHANCEOFSTORM)).equalsIgnoreCase("low")) {
                    ConfigStorm.Player_Storm_Deadly_OddsTo1 = 60;
                    ConfigStorm.Server_Storm_Deadly_OddsTo1 = 60;
                }
            }

            if (nbtServerData.contains("btn_" + CMD_BTN_PREF_CHANCEOFRAIN)) {
                if (LIST_RATES2.get(nbtServerData.getInt("btn_" + CMD_BTN_PREF_CHANCEOFRAIN)).equalsIgnoreCase("high")) {
                    ConfigStorm.Storm_Rain_OddsTo1 = 150;
                    ConfigStorm.Storm_Rain_Overcast_OddsTo1 = ConfigStorm.Storm_Rain_OddsTo1 / 3;
                } else if (LIST_RATES2.get(nbtServerData.getInt("btn_" + CMD_BTN_PREF_CHANCEOFRAIN)).equalsIgnoreCase("medium")) {
                    ConfigStorm.Storm_Rain_OddsTo1 = 300;
                    ConfigStorm.Storm_Rain_Overcast_OddsTo1 = ConfigStorm.Storm_Rain_OddsTo1 / 3;
                } else if (LIST_RATES2.get(nbtServerData.getInt("btn_" + CMD_BTN_PREF_CHANCEOFRAIN)).equalsIgnoreCase("low")) {
                    ConfigStorm.Storm_Rain_OddsTo1 = 450;
                    ConfigStorm.Storm_Rain_Overcast_OddsTo1 = ConfigStorm.Storm_Rain_OddsTo1 / 3;
                } else if (LIST_RATES2.get(nbtServerData.getInt("btn_" + CMD_BTN_PREF_CHANCEOFRAIN)).equalsIgnoreCase("none")) {
                    ConfigStorm.Storm_Rain_OddsTo1 = -1;
                    ConfigStorm.Storm_Rain_Overcast_OddsTo1 = -1;
                }
            }

            if (nbtServerData.contains("btn_" + CMD_BTN_PREF_BLOCKDESTRUCTION)) {
                ConfigTornado.Storm_Tornado_grabBlocks = LIST_TOGGLE.get(nbtServerData.getInt("btn_" + CMD_BTN_PREF_BLOCKDESTRUCTION)).equalsIgnoreCase("on");
            }

            if (nbtServerData.contains("btn_" + CMD_BTN_PREF_TORNADOANDCYCLONES)) {
                ConfigTornado.Storm_NoTornadosOrCyclones = LIST_TOGGLE.get(nbtServerData.getInt("btn_" + CMD_BTN_PREF_TORNADOANDCYCLONES)).equalsIgnoreCase("off");
            }

            if (nbtServerData.contains("btn_" + CMD_BTN_PREF_SANDSTORMS)) {
                ConfigSand.Storm_NoSandstorms = LIST_TOGGLE.get(nbtServerData.getInt("btn_" + CMD_BTN_PREF_SANDSTORMS)).equalsIgnoreCase("off");
            }

            if (nbtServerData.contains("btn_" + CMD_BTN_PREF_GLOBALRATE)) {
                ConfigStorm.Server_Storm_Deadly_UseGlobalRate = nbtServerData.getInt("btn_" + CMD_BTN_PREF_GLOBALRATE) == 0;
                ConfigSand.Sandstorm_UseGlobalServerRate = nbtServerData.getInt("btn_" + CMD_BTN_PREF_GLOBALRATE) == 0;

                //System.out.println("ConfigStorm.Server_Storm_Deadly_UseGlobalRate: " + ConfigStorm.Server_Storm_Deadly_UseGlobalRate);
            }

            NbtCompound nbtDims = nbtServerData.getCompound("dimData");
            //Iterator it = nbtDims.getTags().iterator();

            Weather.LOGGER.debug("before: " + listDimensionsWeather);

            for (String tagName : nbtDims.getKeys()) {
                NbtInt entry = (NbtInt) nbtDims.get(tagName);
                String[] values = tagName.split("_");
                //if weather
                switch (values[2]) {
                    case "0": {
                        int dimID = Integer.parseInt(values[1]);
                        if (entry.intValue() == 0) {
                            //if off
                            if (listDimensionsWeather.contains(dimID)) {
                                listDimensionsWeather.remove(dimID);
                            }
                        } else {
                            //if on
                            if (!listDimensionsWeather.contains(dimID)) {
                                listDimensionsWeather.add(dimID);
                            }
                        }
                        break;
                    }
                    case "1": {
                        int dimID = Integer.parseInt(values[1]);
                        if (entry.intValue() == 0) {
                            //if off
                            if (listDimensionsClouds.contains(dimID)) {
                                listDimensionsClouds.remove(dimID);
                            }
                        } else {
                            //if on
                            if (!listDimensionsClouds.contains(dimID)) {
                                listDimensionsClouds.add(dimID);
                            }
                        }
                        break;
                    }
                    case "2": {
                        int dimID = Integer.parseInt(values[1]);
                        if (entry.intValue() == 0) {
                            //if off
                            if (listDimensionsStorms.contains(dimID)) {
                                listDimensionsStorms.remove(dimID);
                            }
                        } else {
                            //if on
                            if (!listDimensionsStorms.contains(dimID)) {
                                listDimensionsStorms.add(dimID);
                            }
                        }
                        break;
                    }
                }
                Weather.LOGGER.debug("dim: " + values[1] + " - setting ID: " + values[2] + " - data: " + entry.intValue());
            }

            Weather.LOGGER.debug("after: " + listDimensionsWeather);

            processListsReverse();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        for (IConfigCategory config : Weather.listConfigs) {
            //refresh configmods caches and data
            ConfigMod.configLookup.get(config.getRegistryName()).writeConfigFile(true);
            //not needed
            //ConfigMod.populateData(config.getRegistryName());
        }

        ServerTickHandler.syncServerConfigToClient();

        //ConfigMod.configLookup.get(modID).writeConfigFile(true);
        //ConfigMod.populateData(modID);

        //work lists here

        //client nbt to client mod config setttings, and server nbt to server mod config settings
        //invoke whatever method modconfig uses to write out its data, for both its client and server side
    }

    public static void nbtReceiveClientData(NbtCompound parNBT) {
        for (int i = 0; i <= CMD_BTN_HIGHEST_ID; i++) {
            if (parNBT.contains("btn_" + i)) {
                nbtServerData.putInt("btn_" + i, parNBT.getInt("btn_" + i));
            }
        }

        //also add dimension feature config, its iterated over
        nbtServerData.put("dimData", parNBT.getCompound("dimData"));

        processNBTToModConfigServer();
    }

    public static void nbtReceiveServerDataForCache(NbtCompound parNBT) {
        nbtClientCache = parNBT;

        Weather.LOGGER.debug("nbtClientCache: " + nbtServerData);
    }

    public static void nbtSaveDataClient() {
        nbtWriteNBTToDisk(nbtClientData, true);
    }

    public static void nbtSaveDataServer() {
        nbtWriteNBTToDisk(nbtServerData, false);
    }

    public static NbtCompound createNBTDimensionListing() {
        NbtCompound data = new NbtCompound();

        World[] worlds = DimensionManager.getWorlds();

        for (World world : worlds) {
            NbtCompound nbtDim = new NbtCompound();
            int dimID = world.getDimension();
            nbtDim.putInt("ID", dimID); //maybe redundant if we name tag as dimID too
            nbtDim.putString("name", world.getDimension().getName());
            nbtDim.putBoolean("weather", listDimensionsWeather.contains(dimID));
            nbtDim.putBoolean("clouds", listDimensionsClouds.contains(dimID));
            nbtDim.putBoolean("storms", listDimensionsStorms.contains(dimID));

            //PROCESS ME ELSEWHERE!!! - must be done in EZGUI post receiving of this data because client still needs this server created dimension listing first
            //nbtDim.setBoolean("effects", listDimensionsWindEffects.contains(dimID));
            data.put("" + dimID, nbtDim);
            ///data.setString("" + worlds[i].provider.dimensionId, worlds[i].provider.getDimensionName());
        }

        return data;
    }

    public static void processLists() {
        listDimensionsWeather = parseList(ConfigMisc.Dimension_List_Weather);
        listDimensionsClouds = parseList(ConfigMisc.Dimension_List_Clouds);
        listDimensionsStorms = parseList(ConfigMisc.Dimension_List_Storms);
        listDimensionsWindEffects = parseList(ConfigMisc.Dimension_List_WindEffects);
    }

    public static void processListsReverse() {
        ConfigMisc.Dimension_List_Weather = StringUtils.join(listDimensionsWeather, " ");
        ConfigMisc.Dimension_List_Clouds = StringUtils.join(listDimensionsClouds, " ");
        ConfigMisc.Dimension_List_Storms = StringUtils.join(listDimensionsStorms, " ");
        ConfigMisc.Dimension_List_WindEffects = StringUtils.join(listDimensionsWindEffects, " ");
    }

    public static List<Integer> parseList(String parData) {
        String listStr = parData;
        listStr = listStr.replace(",", " ");
        String[] arrStr = listStr.split(" ");
        Integer[] arrInt = new Integer[arrStr.length];
        for (int i = 0; i < arrStr.length; i++) {
            try {
                arrInt[i] = Integer.parseInt(arrStr[i]);
            } catch (Exception ex) {
                arrInt[i] = -999999; //set to -999999, hope no dimension id of this exists
            }
        }
        return new ArrayList(Arrays.asList(arrInt));
    }

    public static void nbtWriteNBTToDisk(NbtCompound parData, boolean saveForClient) {
        String fileURL;
        if (saveForClient) {
            fileURL = CoroUtilFile.getMinecraftSaveFolderPath() + File.separator + "Weather2" + File.separator + "EZGUIConfigClientData.dat";
        } else {
            fileURL = CoroUtilFile.getMinecraftSaveFolderPath() + File.separator + "Weather2" + File.separator + "EZGUIConfigServerData.dat";
        }

        try {
            FileOutputStream fos = new FileOutputStream(fileURL);
            NbtIo.writeCompressed(parData, fos);
            fos.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            Weather.LOGGER.debug("Error writing Weather2 EZ GUI data");
        }
    }

    public static void setOvercastModeServerSide(boolean val) {
        nbtServerData.putInt("btn_" + CMD_BTN_COMP_STORM, val ? 1 : 0);
        nbtSaveDataServer();
    }
}
