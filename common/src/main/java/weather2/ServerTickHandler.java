package weather2;

import CoroUtil.packet.PacketHelper;
import modconfig.ConfigMod;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import weather2.config.ConfigMisc;
import weather2.util.WeatherUtilConfig;
import weather2.weather.ServerWeatherManager;
import weather2.weather.WeatherManager;

import java.util.ArrayList;
import java.util.HashMap;

public class ServerTickHandler {
    //Used for easy iteration, could be replaced
    public static ArrayList<ServerWeatherManager> listWeatherMans;

    //Main lookup method for dim to weather systems
    public static HashMap<Integer, ServerWeatherManager> lookupDimToWeatherMan;

    public static World lastWorld;

    static {
        listWeatherMans = new ArrayList<>();
        lookupDimToWeatherMan = new HashMap<>();
    }

    public static void onTickInGame() {
        if (FMLCommonHandler.instance() == null || FMLCommonHandler.instance().getMinecraftServerInstance() == null) {
            return;
        }

        World world = FMLCommonHandler.instance().getMinecraftServerInstance().getWorld(0);

        if (world != null && lastWorld != world) {
            lastWorld = world;
        }

        //regularly save data
        if (world != null) {
            if (world.getTime() % ConfigMisc.Misc_AutoDataSaveIntervalInTicks == 0) {
                Weather.writeOutData(false);
            }
        }

        World[] worlds = DimensionManager.getWorlds();

        //add use of CSV of supported dimensions here once feature is added, for now just overworld

        for (World value : worlds) {
            if (!lookupDimToWeatherMan.containsKey(value.getDimension())) {
                if (WeatherUtilConfig.listDimensionsWeather.contains(value.getDimension())) {
                    addWorldToWeather(value.getDimension());
                }
            }

            //tick it
            ServerWeatherManager wms = lookupDimToWeatherMan.get(value.getDimension());
            if (wms != null) {
                lookupDimToWeatherMan.get(value.getDimension()).tick();
            }
        }

        if (ConfigMisc.Aesthetic_Only_Mode) {
            if (!ConfigMisc.overcastMode) {
                ConfigMisc.overcastMode = true;
                Weather.LOGGER.debug("detected Aesthetic_Only_Mode on, setting overcast mode on");
                WeatherUtilConfig.setOvercastModeServerSide(ConfigMisc.overcastMode);
                ConfigMod.forceSaveAllFilesFromRuntimeSettings();
                syncServerConfigToClient();
            }
        }

        //TODO: only sync when things change? is now sent via PlayerLoggedInEvent at least
        if (world.getTime() % 200 == 0) {
            syncServerConfigToClient();
        }
    }

    //must only be used when world is active, soonest allowed is TickType.WORLDLOAD
    public static void addWorldToWeather(int dim) {
        Weather.LOGGER.debug("Registering Weather2 manager for dim: " + dim);
        ServerWeatherManager wm = new ServerWeatherManager(dim);

        listWeatherMans.add(wm);
        lookupDimToWeatherMan.put(dim, wm);

        wm.readFromFile();
    }

    public static void removeWorldFromWeather(int dim) {
        Weather.LOGGER.debug("Weather2: Unregistering manager for dim: " + dim);
        ServerWeatherManager wm = lookupDimToWeatherMan.get(dim);

        if (wm != null) {
            listWeatherMans.remove(wm);
            lookupDimToWeatherMan.remove(dim);
        }

        //wm.readFromFile();
        wm.writeToFile();
    }

    public static void playerClientRequestsFullSync(ServerPlayerEntity entP) {
        ServerWeatherManager wm = lookupDimToWeatherMan.get(entP.world.getDimension());
        if (wm != null) {
            wm.playerJoinedWorldSyncFull(entP);
        }
    }

    public static void reset() {
        Weather.LOGGER.debug("Weather2: ServerTickHandler resetting");
        //World worlds[] = DimensionManager.getWorlds();
        //for (int i = 0; i < worlds.length; i++) {
        for (WeatherManager wm : listWeatherMans) {
            int dim = wm.dim;
            if (lookupDimToWeatherMan.containsKey(dim)) {
                removeWorldFromWeather(dim);
            }
        }

        //should never happen
        if (listWeatherMans.size() > 0 || lookupDimToWeatherMan.size() > 0) {
            Weather.LOGGER.debug("Weather2: reset state failed to manually clear lists, listWeatherMans.size(): " + listWeatherMans.size() + " - lookupDimToWeatherMan.size(): " + lookupDimToWeatherMan.size() + " - forcing a full clear of lists");
            listWeatherMans.clear();
            lookupDimToWeatherMan.clear();
        }
    }

    public static ServerWeatherManager getWeatherSystemForDim(int dimID) {
        return lookupDimToWeatherMan.get(dimID);
    }

    public static void syncServerConfigToClient() {
        //packets
        NbtCompound data = new NbtCompound();
        data.putString("packetCommand", "ClientConfigData");
        data.putString("command", "syncUpdate");
        //data.setTag("data", parManager.nbtSyncForClient());

        ClientConfigData.writeNBT(data);

        Weather.eventChannel.sendToAll(PacketHelper.getNBTPacket(data, Weather.eventChannelName));
    }

    public static void syncServerConfigToClientPlayer(ServerPlayerEntity player) {
        //packets
        NbtCompound data = new NbtCompound();
        data.putString("packetCommand", "ClientConfigData");
        data.putString("command", "syncUpdate");
        //data.setTag("data", parManager.nbtSyncForClient());

        ClientConfigData.writeNBT(data);

        Weather.eventChannel.sendTo(PacketHelper.getNBTPacket(data, Weather.eventChannelName), player);
    }
}
