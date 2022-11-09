package sh.lpx.weather2.weather;

import CoroUtil.packet.PacketHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.level.ServerWorldProperties;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.event.FMLInterModComms;
import sh.lpx.weather2.Weather;
import sh.lpx.weather2.config.ConfigMisc;
import sh.lpx.weather2.config.ConfigSand;
import sh.lpx.weather2.config.ConfigStorm;
import sh.lpx.weather2.player.PlayerData;
import sh.lpx.weather2.util.CachedNbtCompound;
import sh.lpx.weather2.util.WeatherUtilBlock;
import sh.lpx.weather2.util.WeatherUtilConfig;
import sh.lpx.weather2.util.WeatherUtilEntity;
import sh.lpx.weather2.volcano.Volcano;
import sh.lpx.weather2.weather.storm.CloudStorm;
import sh.lpx.weather2.weather.storm.SandStorm;
import sh.lpx.weather2.weather.storm.Storm;
import sh.lpx.weather2.weather.wind.WindManager;

import java.util.Random;

public class ServerWeatherManager extends WeatherManager {
    public ServerWeatherManager(int parDim) {
        super(parDim);
    }

    @Override
    public World getWorld() {
        return DimensionManager.getWorld(dim);
    }

    @Override
    public void tick() {
        super.tick();

        World world = getWorld();

        //wrap back to ID 0 just in case someone manages to hit 9223372036854775807 O_o
        if (Storm.lastStormId == Long.MAX_VALUE) {
            Storm.lastStormId = 0;
        }

        tickWeatherCoverage();

        if (world != null) {
            //sync storms

            //System.out.println("getStormObjects().size(): " + getStormObjects().size());

            for (int i = 0; i < getStorms().size(); i++) {
                Storm wo = getStorms().get(i);
                int updateRate = wo.getUpdateRateForNetwork();
                if (world.getTime() % updateRate == 0) {
                    syncStormUpdate(wo);
                }
            }

            //sync volcanos
            if (world.getTime() % 40 == 0) {
                for (int i = 0; i < getVolcanoes().size(); i++) {
                    syncVolcanoUpdate(getVolcanoes().get(i));
                }
            }

            //sync wind
            if (world.getTime() % 60 == 0) {
                syncWindUpdate(windManager);
            }

            //IMC
            if (world.getTime() % 60 == 0) {
                nbtStormsForIMC();
            }

            //temp
            //getVolcanoObjects().clear();

            //sim box work
            int rate = 20;
            if (world.getTime() % rate == 0) {
                for (int i = 0; i < getStorms().size(); i++) {
                    Storm so = getStorms().get(i);
                    PlayerEntity closestPlayer = WeatherUtilEntity.getClosestPlayerAny(world, so.posGround.x, so.posGround.y, so.posGround.z, ConfigMisc.Misc_simBoxRadiusCutoff);

                    //isDead check is done in WeatherManagerBase
                    if (closestPlayer == null || ConfigMisc.Aesthetic_Only_Mode) {
                        so.ticksSinceNoNearPlayer += rate;
                        //finally remove if nothing near for 30 seconds, gives multiplayer server a chance to get players in
                        if (so.ticksSinceNoNearPlayer > 20 * 30 || ConfigMisc.Aesthetic_Only_Mode) {
                            if (world.getPlayers().size() == 0) {
                                Weather.LOGGER.debug("removing distant storm: " + so.id + ", running without players");
                            } else {
                                Weather.LOGGER.debug("removing distant storm: " + so.id);
                            }

                            removeStorm(so.id);
                            syncStormRemove(so);
                        }
                    } else {
                        so.ticksSinceNoNearPlayer = 0;
                    }
                }

                Random rand = new Random();

                //cloud formation spawning - REFINE ME!
                if (!ConfigMisc.Aesthetic_Only_Mode && WeatherUtilConfig.listDimensionsClouds.contains(world.getDimension())) {
                    for (int i = 0; i < world.getPlayers().size(); i++) {
                        PlayerEntity entP = world.getPlayers().get(i);

                        //Weather.dbg("getStormObjects().size(): " + getStormObjects().size());

                        //layer 0
                        if (getCloudStormsByLayer(0).size() < ConfigStorm.Storm_MaxPerPlayerPerLayer * world.getPlayers().size()) {
                            if (rand.nextInt(5) == 0) {
                                //if (rand.nextFloat() <= cloudIntensity) {
                                trySpawnStormCloudNearPlayerForLayer(entP, 0);
                                //}
                            }
                        }

                        //layer 1
                        if (getCloudStormsByLayer(1).size() < ConfigStorm.Storm_MaxPerPlayerPerLayer * world.getPlayers().size()) {
                            if (ConfigMisc.Cloud_Layer1_Enable) {
                                if (rand.nextInt(5) == 0) {
                                    //if (rand.nextFloat() <= cloudIntensity) {
                                    trySpawnStormCloudNearPlayerForLayer(entP, 1);
                                    //}
                                }
                            }
                        }
                    }
                }
            }

            //if dimension can have storms, tick sandstorm spawning every 10 seconds
            if (!ConfigMisc.Aesthetic_Only_Mode && !ConfigSand.Storm_NoSandstorms && WeatherUtilConfig.listDimensionsStorms.contains(world.getDimension()) && world.getTime() % 200 == 0 && windManager.isHighWindEventActive()) {
                Random rand = new Random();
                if (ConfigSand.Sandstorm_OddsTo1 <= 0 || rand.nextInt(ConfigSand.Sandstorm_OddsTo1) == 0) {
                    if (ConfigSand.Sandstorm_UseGlobalServerRate) {
                        //get a random player to try and spawn for, will recycle another if it cant spawn
                        if (world.getPlayers().size() > 0) {
                            PlayerEntity entP = world.getPlayers().get(rand.nextInt(world.getPlayers().size()));

                            boolean sandstormMade = trySandstormForPlayer(entP, lastSandstormFormed);
                            if (sandstormMade) {
                                lastSandstormFormed = world.getTime();
                            }
                        }
                    } else {
                        for (int i = 0; i < world.getPlayers().size(); i++) {
                            PlayerEntity entP = world.getPlayers().get(i);
                            NbtCompound playerNBT = PlayerData.getPlayerNBT(entP.getEntityName());
                            boolean sandstormMade = trySandstormForPlayer(entP, playerNBT.getLong("lastSandstormTime"));
                            if (sandstormMade) {
                                playerNBT.putLong("lastSandstormTime", world.getTime());
                            }
                        }
                    }
                }
            }
        }
    }

    public void tickWeatherCoverage() {
        World world = this.getWorld();
        if (world != null) {
            ServerWorldProperties properties = (ServerWorldProperties) world.getLevelProperties();
            if (!ConfigMisc.overcastMode) {
                if (ConfigMisc.lockServerWeatherMode != -1) {
                    properties.setRaining(ConfigMisc.lockServerWeatherMode == 1);
                    properties.setThundering(ConfigMisc.lockServerWeatherMode == 1);
                }
            }

            if (ConfigStorm.preventServerThunderstorms) {
                properties.setThundering(false);
            }

            //if (ConfigMisc.overcastMode) {
            if (world.getTime() % 40 == 0) {
                vanillaRainActiveOnServer = getWorld().isRaining();
                vanillaThunderActiveOnServer = getWorld().isThundering();
                vanillaRainTimeOnServer = properties.getRainTime();
                syncWeatherVanilla();
            }
            //}

            if (world.getTime() % 200 == 0) {
                Random rand = new Random();
                cloudIntensity += (float) ((rand.nextDouble() * ConfigMisc.Cloud_Coverage_Random_Change_Amount) - (rand.nextDouble() * ConfigMisc.Cloud_Coverage_Random_Change_Amount));
                if (ConfigMisc.overcastMode && world.isRaining()) {
                    cloudIntensity = 1;
                } else {
                    if (cloudIntensity < ConfigMisc.Cloud_Coverage_Min_Percent / 100F) {
                        cloudIntensity = (float) ConfigMisc.Cloud_Coverage_Min_Percent / 100F;
                    } else if (cloudIntensity > ConfigMisc.Cloud_Coverage_Max_Percent / 100F) {
                        cloudIntensity = (float) ConfigMisc.Cloud_Coverage_Max_Percent / 100F;
                    }
                }

                //force full cloudIntensity if server side raining
                //note: storms also revert to clouded storms for same condition

            }

            //temp lock to max for fps comparisons
            //cloudIntensity = 1F;
        }
    }

    public boolean trySandstormForPlayer(PlayerEntity player, long lastSandstormTime) {
        boolean sandstormMade = false;
        if (lastSandstormTime == 0 || lastSandstormTime + ConfigSand.Sandstorm_TimeBetweenInTicks < player.getEntityWorld().getTime()) {
            sandstormMade = trySpawnSandstormNearPos(player.getEntityWorld(), player.getPos());
        }
        return sandstormMade;
    }

    public boolean trySpawnSandstormNearPos(World world, Vec3d posIn) {
        /*
          1. Start upwind
          2. Find random spot near there loaded and in desert
          3. scan upwind and downwind, require a good stretch of sand for a storm
         */

        int searchRadius = 512;

        double angle = windManager.getWindAngleForClouds();
        //-1 for upwind
        double dirX = -Math.sin(Math.toRadians(angle));
        double dirZ = Math.cos(Math.toRadians(angle));
        double vecX = dirX * searchRadius / 2 * -1;
        double vecZ = dirZ * searchRadius / 2 * -1;

        Random rand = new Random();

        BlockPos foundPos;

        int findTriesMax = 30;
        for (int i = 0; i < findTriesMax; i++) {

            int x = MathHelper.floor(posIn.x + vecX + rand.nextInt(searchRadius * 2) - searchRadius);
            int z = MathHelper.floor(posIn.z + vecZ + rand.nextInt(searchRadius * 2) - searchRadius);

            BlockPos pos = new BlockPos(x, 0, z);

            if (!world.canSetBlock(pos)) continue;
            Biome biomeIn = world.getBiome(pos);

            if (SandStorm.isDesert(biomeIn, true)) {
                //found
                foundPos = pos;
                //break;

                //check left and right about 20 blocks, if its not still desert, force retry
                double dirXLeft = -Math.sin(Math.toRadians(angle - 90));
                double dirZLeft = Math.cos(Math.toRadians(angle - 90));
                double dirXRight = -Math.sin(Math.toRadians(angle + 90));
                double dirZRight = Math.cos(Math.toRadians(angle + 90));

                double distLeftRight = 20;
                BlockPos posLeft = new BlockPos(foundPos.getX() + (dirXLeft * distLeftRight), 0, foundPos.getZ() + (dirZLeft * distLeftRight));
                if (!world.canSetBlock(posLeft)) continue;
                if (!SandStorm.isDesert(world.getBiome(posLeft))) continue;

                BlockPos posRight = new BlockPos(foundPos.getX() + (dirXRight * distLeftRight), 0, foundPos.getZ() + (dirZRight * distLeftRight));
                if (!world.canSetBlock(posRight)) continue;
                if (!SandStorm.isDesert(world.getBiome(posRight))) continue;

                //go as far upwind as possible until no desert / unloaded area

                BlockPos posFind = new BlockPos(foundPos);
                BlockPos posFindLastGoodUpwind = new BlockPos(foundPos);
                BlockPos posFindLastGoodDownwind = new BlockPos(foundPos);
                double tickDist = 10;

                while (world.canSetBlock(posFind) && SandStorm.isDesert(world.getBiome(posFind))) {
                    //update last good
                    posFindLastGoodUpwind = new BlockPos(posFind);

                    //scan against wind (upwind)
                    int xx = MathHelper.floor(posFind.getX() + (dirX * -1D * tickDist));
                    int zz = MathHelper.floor(posFind.getZ() + (dirZ * -1D * tickDist));

                    posFind = new BlockPos(xx, 0, zz);
                }

                //reset for downwind scan
                posFind = new BlockPos(foundPos);

                while (world.canSetBlock(posFind) && SandStorm.isDesert(world.getBiome(posFind))) {
                    //update last good
                    posFindLastGoodDownwind = new BlockPos(posFind);

                    //scan with wind (downwind)
                    int xx = MathHelper.floor(posFind.getX() + (dirX * 1D * tickDist));
                    int zz = MathHelper.floor(posFind.getZ() + (dirZ * 1D * tickDist));

                    posFind = new BlockPos(xx, 0, zz);
                }

                int minDistanceOfDesertStretchNeeded = 200;
                double dist = posFindLastGoodUpwind.getSquaredDistance(posFindLastGoodDownwind.getX(), posFindLastGoodDownwind.getY(), posFindLastGoodDownwind.getZ());

                if (dist >= minDistanceOfDesertStretchNeeded) {

                    SandStorm sandstorm = new SandStorm(this);

                    sandstorm.initFirstTime();
                    BlockPos posSpawn = new BlockPos(WeatherUtilBlock.getPrecipitationHeightSafe(world, posFindLastGoodUpwind)).add(0, 1, 0);
                    sandstorm.initSandstormSpawn(new Vec3d(posSpawn.getX(), posSpawn.getY(), posSpawn.getZ()));
                    addStorm(sandstorm);
                    syncStormNew(sandstorm);

                    Weather.LOGGER.debug("found decent spot and stretch for sandstorm, stretch: " + dist);
                    return true;
                }


            }
        }

        Weather.LOGGER.debug("couldnt spawn sandstorm");
        return false;
    }

    public void trySpawnStormCloudNearPlayerForLayer(PlayerEntity entP, int layer) {
        Random rand = new Random();

        int tryCountMax = 10;
        int tryCountCur = 0;
        int spawnX;
        int spawnZ;
        Vec3d tryPos = null;
        CloudStorm soClose = null;
        PlayerEntity playerClose = null;

        int closestToPlayer = 128;

        //use 256 or the cutoff val if its configured small
        float windOffsetDist = Math.min(256, ConfigMisc.Misc_simBoxRadiusCutoff / 4 * 3);
        double angle = windManager.getWindAngleForClouds();
        double vecX = -Math.sin(Math.toRadians(angle)) * windOffsetDist;
        double vecZ = Math.cos(Math.toRadians(angle)) * windOffsetDist;

        while (tryCountCur++ == 0 || (tryCountCur < tryCountMax && (soClose != null || playerClose != null))) {
            spawnX = (int) (entP.getX() - vecX + rand.nextInt(ConfigMisc.Misc_simBoxRadiusSpawn) - rand.nextInt(ConfigMisc.Misc_simBoxRadiusSpawn));
            spawnZ = (int) (entP.getZ() - vecZ + rand.nextInt(ConfigMisc.Misc_simBoxRadiusSpawn) - rand.nextInt(ConfigMisc.Misc_simBoxRadiusSpawn));
            tryPos = new Vec3d(spawnX, CloudStorm.layers.get(layer), spawnZ);
            soClose = getClosestCloudStorm(tryPos, ConfigMisc.Cloud_Formation_MinDistBetweenSpawned);
            playerClose = entP.world.getClosestPlayer(spawnX, 50, spawnZ, closestToPlayer, false);
        }

        if (soClose == null) {
            //Weather.dbg("spawning storm at: " + spawnX + " - " + spawnZ);

            CloudStorm so = new CloudStorm(this);
            so.initFirstTime();
            so.pos = tryPos;
            so.layer = layer;
            //make only layer 0 produce deadly storms
            if (layer != 0) {
                so.canBeDeadly = false;
            }
            so.userSpawnedFor = entP.getEntityName();
            if (rand.nextFloat() >= cloudIntensity) {
                so.setCloudlessStorm(true);
            }
            addStorm(so);
            syncStormNew(so);
        }
    }

    public void playerJoinedWorldSyncFull(ServerPlayerEntity entP) {
        Weather.LOGGER.debug("Weather2: playerJoinedWorldSyncFull for dim: " + dim);
        World world = getWorld();
        if (world != null) {
            Weather.LOGGER.debug("Weather2: playerJoinedWorldSyncFull, sending " + getStorms().size() + " weather objects to: " + entP.getName() + ", dim: " + dim);
            //sync storms
            for (int i = 0; i < getStorms().size(); i++) {
                syncStormNew(getStorms().get(i), entP);
            }

            //sync volcanos
            for (int i = 0; i < getVolcanoes().size(); i++) {
                syncVolcanoNew(getVolcanoes().get(i), entP);
            }
        }
    }

    //populate data with rain storms and deadly storms
    public void nbtStormsForIMC() {
        NbtCompound data = new NbtCompound();

        for (int i = 0; i < getStorms().size(); i++) {
            Storm wo = getStorms().get(i);

            if (wo instanceof CloudStorm) {
                CloudStorm so = (CloudStorm) wo;
                if (so.levelCurIntensityStage > 0 || so.attrib_precipitation) {
                    NbtCompound nbtStorm = so.nbtForIMC();

                    data.put("storm_" + so.id, nbtStorm);
                }
            }
        }

        if (!data.isEmpty()) {
            FMLInterModComms.sendRuntimeMessage(Weather.instance, Weather.MOD_ID, "weather.storms", data);
        }
    }

    public void syncLightningNew(Entity parEnt, boolean custom) {
        NbtCompound data = new NbtCompound();
        data.putString("packetCommand", "WeatherData");
        data.putString("command", "syncLightningNew");
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("posX", MathHelper.floor(parEnt.getX()/* * 32.0D*/));
        nbt.putInt("posY", MathHelper.floor(parEnt.getY()/* * 32.0D*/));
        nbt.putInt("posZ", MathHelper.floor(parEnt.getZ()/* * 32.0D*/));
        nbt.putInt("entityID", parEnt.getEntityId());
        nbt.putBoolean("custom", custom);
        data.put("data", nbt);
        Weather.eventChannel.sendToDimension(PacketHelper.getNBTPacket(data, Weather.eventChannelName), getWorld().getDimension());
        FMLInterModComms.sendRuntimeMessage(Weather.instance, Weather.MOD_ID, "weather.lightning", data);
    }

    public void syncWindUpdate(WindManager parManager) {
        //packets
        NbtCompound data = new NbtCompound();
        data.putString("packetCommand", "WeatherData");
        data.putString("command", "syncWindUpdate");
        data.put("data", parManager.nbtSyncForClient());
        Weather.eventChannel.sendToDimension(PacketHelper.getNBTPacket(data, Weather.eventChannelName), getWorld().getDimension());
        FMLInterModComms.sendRuntimeMessage(Weather.instance, Weather.MOD_ID, "weather.wind", data);
    }

    public void syncStormNew(Storm parStorm) {
        syncStormNew(parStorm, null);
    }

    public void syncStormNew(Storm parStorm, ServerPlayerEntity entP) {
        NbtCompound data = new NbtCompound();
        data.putString("packetCommand", "WeatherData");
        data.putString("command", "syncStormNew");

        CachedNbtCompound cache = parStorm.getNbtCache();
        cache.setForcingUpdates(true);
        parStorm.nbtSyncForClient();
        cache.setForcingUpdates(false);
        data.put("data", cache.getNewNbt());

        if (entP == null) {
            Weather.eventChannel.sendToDimension(PacketHelper.getNBTPacket(data, Weather.eventChannelName), getWorld().getDimension());
        } else {
            Weather.eventChannel.sendTo(PacketHelper.getNBTPacket(data, Weather.eventChannelName), entP);
        }
        //PacketDispatcher.sendPacketToAllAround(parStorm.pos.xCoord, parStorm.pos.yCoord, parStorm.pos.zCoord, syncRange, getWorld().provider.dimensionId, WeatherPacketHelper.createPacketForServerToClientSerialization("WeatherData", data));
    }

    public void syncStormUpdate(Storm parStorm) {
        //packets
        NbtCompound data = new NbtCompound();
        data.putString("packetCommand", "WeatherData");
        data.putString("command", "syncStormUpdate");
        parStorm.getNbtCache().setNewNbt(new NbtCompound());
        parStorm.nbtSyncForClient();
        data.put("data", parStorm.getNbtCache().getNewNbt());
        Weather.eventChannel.sendToDimension(PacketHelper.getNBTPacket(data, Weather.eventChannelName), getWorld().getDimension());
    }

    public void syncStormRemove(Storm parStorm) {
        //packets
        NbtCompound data = new NbtCompound();
        data.putString("packetCommand", "WeatherData");
        data.putString("command", "syncStormRemove");
        parStorm.nbtSyncForClient();
        data.put("data", parStorm.getNbtCache().getNewNbt());
        //data.setTag("data", parStorm.nbtSyncForClient(new NBTTagCompound()));
        //fix for client having broken states
        data.getCompound("data").putBoolean("isDead", true);
        Weather.eventChannel.sendToDimension(PacketHelper.getNBTPacket(data, Weather.eventChannelName), getWorld().getDimension());
    }

    public void syncVolcanoNew(Volcano parStorm) {
        syncVolcanoNew(parStorm, null);
    }

    public void syncVolcanoNew(Volcano parStorm, ServerPlayerEntity entP) {
        NbtCompound data = new NbtCompound();
        data.putString("packetCommand", "WeatherData");
        data.putString("command", "syncVolcanoNew");
        data.put("data", parStorm.nbtSyncForClient());

        if (entP == null) {
            Weather.eventChannel.sendToDimension(PacketHelper.getNBTPacket(data, Weather.eventChannelName), getWorld().getDimension());
        } else {
            Weather.eventChannel.sendTo(PacketHelper.getNBTPacket(data, Weather.eventChannelName), entP);
        }
        //PacketDispatcher.sendPacketToAllAround(parStorm.pos.xCoord, parStorm.pos.yCoord, parStorm.pos.zCoord, syncRange, getWorld().provider.dimensionId, WeatherPacketHelper.createPacketForServerToClientSerialization("WeatherData", data));
    }

    public void syncVolcanoUpdate(Volcano parStorm) {
        //packets
        NbtCompound data = new NbtCompound();
        data.putString("packetCommand", "WeatherData");
        data.putString("command", "syncVolcanoUpdate");
        data.put("data", parStorm.nbtSyncForClient());
        Weather.eventChannel.sendToDimension(PacketHelper.getNBTPacket(data, Weather.eventChannelName), getWorld().getDimension());
    }

    public void syncWeatherVanilla() {
        NbtCompound data = new NbtCompound();
        data.putString("packetCommand", "WeatherData");
        data.putString("command", "syncWeatherUpdate");
        data.putBoolean("isVanillaRainActiveOnServer", vanillaRainActiveOnServer);
        data.putBoolean("isVanillaThunderActiveOnServer", vanillaThunderActiveOnServer);
        data.putInt("vanillaRainTimeOnServer", vanillaRainTimeOnServer);
        Weather.eventChannel.sendToDimension(PacketHelper.getNBTPacket(data, Weather.eventChannelName), getWorld().getDimension());
    }
}
