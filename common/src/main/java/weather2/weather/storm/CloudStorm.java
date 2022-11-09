package weather2.weather.storm;

import CoroUtil.config.ConfigCoroUtil;
import CoroUtil.util.*;
import extendedrenderer.ExtendedRenderer;
import extendedrenderer.particle.ParticleRegistry;
import extendedrenderer.particle.behavior.ParticleBehaviorFog;
import extendedrenderer.particle.entity.EntityRotFX;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import weather2.ServerTickHandler;
import weather2.Weather;
import weather2.config.ConfigMisc;
import weather2.config.ConfigSnow;
import weather2.config.ConfigStorm;
import weather2.config.ConfigTornado;
import weather2.entity.EntityIceBall;
import weather2.entity.EntityLightningBolt;
import weather2.player.PlayerData;
import weather2.util.*;
import weather2.weather.ServerWeatherManager;
import weather2.weather.WeatherManager;
import weather2.weather.wind.WindManager;

import java.util.*;

public class CloudStorm extends Storm {
    //used on both server and client side, mark things SideOnly where needed

    //size, state

    //should they extend entity?

    //management stuff

    public String userSpawnedFor = "";

    //newer cloud managing list for more strict render optimized positioning
    public HashMap<Integer, EntityRotFX> lookupParticlesCloud;

    public HashMap<Integer, EntityRotFX> lookupParticlesCloudLower;

    public HashMap<Integer, EntityRotFX> lookupParticlesFunnel;

    public List<EntityRotFX> listParticlesCloud;
    public List<EntityRotFX> listParticlesGround;
    public List<EntityRotFX> listParticlesFunnel;
    public ParticleBehaviorFog particleBehaviorFog;

    public int sizeMaxFunnelParticles = 600;

    //public WeatherEntityConfig conf = WeatherTypes.weatherEntTypes.get(1);
    //this was pulled over from weather1 i believe
    //public int curWeatherType = 1; //NEEDS SYNCING

    //basic info
    public static int static_YPos_layer0 = ConfigMisc.Cloud_Layer0_Height;
    public static int static_YPos_layer1 = ConfigMisc.Cloud_Layer1_Height;
    public static int static_YPos_layer2 = ConfigMisc.Cloud_Layer2_Height;
    public static List<Integer> layers = new ArrayList<>(Arrays.asList(static_YPos_layer0, static_YPos_layer1, static_YPos_layer2));
    public int layer = 0;

    public boolean angleIsOverridden = false;
    public float angleMovementTornadoOverride = 0;

    //growth / progression info

    public boolean isGrowing = true;

    //cloud formation data, helps storms
    public int levelWater = 0; //builds over water and humid biomes, causes rainfall (not technically a storm)
    public float levelWindMomentum = 0; //high elevation builds this, plains areas lowers it, 0 = no additional speed ontop of global speed
    public float levelTemperature = 0; //negative for cold, positive for warm, we subtract 0.7 from vanilla values to make forest = 0, plains 0.1, ocean -0.5, etc
    //public float levelWindDirectionAdjust = 0; //for persistant direction change i- wait just calculate on the fly based on temperature

    public int levelWaterStartRaining = 100;

    //storm data, used when its determined a storm will happen from cloud front collisions
    public int levelStormIntensityMax = 0; //calculated from colliding warm and cold fronts, used to determine how crazy a storm _will_ get

    //revision, ints for each stage of intensity, and a float for the intensity of THAT current stage
    public int levelCurIntensityStage = 0; //since we want storms to build up to a climax still, this will start from 0 and peak to levelStormIntensityMax
    public float levelCurStagesIntensity = 0;
    //public boolean isRealStorm = false;
    public boolean hasStormPeaked = false;

    public int maxIntensityStage = STATE_STAGE5;

    //used to mark difference between land and water based storms
    public int stormType = TYPE_LAND;
    public static int TYPE_LAND = 0; //for tornados
    public static int TYPE_WATER = 1; //for tropical cyclones / hurricanes

    //used to mark intensity stages
    public static int STATE_NORMAL = 0;
    public static int STATE_THUNDER = 1;
    public static int STATE_HIGHWIND = 2;
    public static int STATE_HAIL = 3;
    public static int STATE_FORMING = 4; //forming tornado for land, for water... stage 0 or something?
    public static int STATE_STAGE1 = 5; //these are for both tornados (land) and tropical cyclones (water)
    public static int STATE_STAGE2 = 6;
    public static int STATE_STAGE3 = 7;
    public static int STATE_STAGE4 = 8;
    public static int STATE_STAGE5 = 9; //counts as hurricane for water

    //helper val, adjust with flags method
    public static float levelStormIntensityFormingStartVal = STATE_FORMING;


    //spin speed for potential tornado formations, should go up with intensity increase;
    public double spinSpeed = 0.02D;

    //PENDING REVISION \\ - use based on levelStormIntensityCur ???

    //states that combine all lesser states
    //public int state = STATE_NORMAL;


    //used for sure, rain is dependant on water level values
    public boolean attrib_precipitation = false;
    public boolean attrib_waterSpout = false;

    //copied from EntTornado
    //buildup var - unused in new system currently, but might be needed for touchdown effect

    //unused tornado scale, always 1F
    public float scale = 1F;
    public float strength = 100;
    public int maxHeight = 60;

    public int currentTopYBlock = -1;

    public TornadoHelper tornadoHelper = new TornadoHelper(this);

    //public Set<ChunkCoordIntPair> doneChunks = new HashSet<ChunkCoordIntPair>();
    public int updateLCG = (new Random()).nextInt();

    public float formingStrength = 0; //for transition from 0 (in clouds) to 1 (touch down)

    public Vec3d posBaseFormationPos = new Vec3d(pos.x, pos.y, pos.z); //for formation / touchdown progress, where all the ripping methods scan from

    public boolean naturallySpawned = true;
    //to prevent things like it progressing to next stage before weather machine undoes it
    public boolean weatherMachineControlled = false;
    public boolean canSnowFromCloudTemperature = false;
    public boolean alwaysProgresses = false;


    //to let client know server is raining (since we override client side raining state for render changes)
    //public boolean overCastModeAndRaining = false;

    //there is an issue with rainstorms sometimes never going away, this is a patch to mend the underlying issue i cant find yet
    public long ticksSinceLastPacketReceived = 0;

    //public static long lastStormFormed = 0;

    public boolean canBeDeadly = true;

    /**
     * Populate sky with stormless/cloudless storm objects in order to allow clear skies with current design
     */
    public boolean cloudlessStorm = false;

    public boolean isFirenado = false;

    public List<LivingEntity> listEntitiesUnderClouds = new ArrayList<>();

    public CloudStorm(WeatherManager parManager) {
        super(parManager);

        pos = new Vec3(0, static_YPos_layer0, 0);
        maxSize = ConfigStorm.Storm_MaxRadius;

        if (parManager.getWorld().isClient) {
            listParticlesCloud = new ArrayList<>();
            listParticlesFunnel = new ArrayList<>();
            listParticlesGround = new ArrayList<>();
            lookupParticlesCloud = new HashMap<>();
            lookupParticlesCloudLower = new HashMap<>();
            lookupParticlesFunnel = new HashMap<>();
            //renderBlock = new RenderCubeCloud();
        }
    }

    public void initFirstTime() {
        super.initFirstTime();

        Biome bgb = manager.getWorld().getBiome(new BlockPos(MathHelper.floor(pos.x), 0, MathHelper.floor(pos.z)));


        float temp = 1;

        if (bgb != null) {
            //temp = bgb.getFloatTemperature(new BlockPos(MathHelper.floor(pos.xCoord), MathHelper.floor(pos.yCoord), MathHelper.floor(pos.zCoord)));
            temp = CoroUtilCompatibility.getAdjustedTemperature(manager.getWorld(), bgb, new BlockPos(MathHelper.floor(pos.x), MathHelper.floor(pos.y), MathHelper.floor(pos.z)));
        }

        //initial setting, more apparent than gradual adjustments
        if (naturallySpawned) {
            levelTemperature = getTemperatureMCToWeatherSys(temp);
        }
        //levelWater = 0;
        levelWindMomentum = 0;

        //Weather.dbg("initialize temp to: " + levelTemperature + " - biome: " + bgb.biomeName);
    }

    public boolean isCloudlessStorm() {
        return cloudlessStorm;
    }

    public void setCloudlessStorm(boolean cloudlessStorm) {
        this.cloudlessStorm = cloudlessStorm;
    }

    public boolean isPrecipitating() {
        return attrib_precipitation;
    }

    public void setPrecipitating(boolean parVal) {
        attrib_precipitation = parVal;
    }

    public boolean isRealStorm() {
        return levelCurIntensityStage > STATE_NORMAL;
    }

    public boolean isTornadoFormingOrGreater() {
        return stormType == TYPE_LAND && levelCurIntensityStage >= STATE_FORMING;
    }

    public boolean isCycloneFormingOrGreater() {
        return stormType == TYPE_WATER && levelCurIntensityStage >= STATE_FORMING;
    }

    public boolean isSpinning() {
        return levelCurIntensityStage >= STATE_HIGHWIND;
    }

    public boolean isHurricane() {
        return levelCurIntensityStage >= STATE_STAGE5;
    }

    @Override
    public void readFromNBT() {
        super.readFromNBT();
        nbtSyncFromServer();

        CachedNbtCompound var1 = this.getNbtCache();


        angleIsOverridden = var1.getBoolean("angleIsOverridden");
        angleMovementTornadoOverride = var1.getFloat("angleMovementTornadoOverride");

        userSpawnedFor = var1.getString("userSpawnedFor");
    }

    @Override
    public void writeToNBT() {
        super.writeToNBT();
        nbtSyncForClient();

        CachedNbtCompound nbt = this.getNbtCache();


        nbt.putBoolean("angleIsOverridden", angleIsOverridden);
        nbt.putFloat("angleMovementTornadoOverride", angleMovementTornadoOverride);

        nbt.putString("userSpawnedFor", userSpawnedFor);

    }

    //receiver method
    @Override
    public void nbtSyncFromServer() {
        CachedNbtCompound parNBT = this.getNbtCache();

        super.nbtSyncFromServer();

        attrib_precipitation = parNBT.getBoolean("attrib_rain");
        attrib_waterSpout = parNBT.getBoolean("attrib_waterSpout");

        currentTopYBlock = parNBT.getInt("currentTopYBlock");

        levelTemperature = parNBT.getFloat("levelTemperature");
        levelWater = parNBT.getInt("levelWater");

        layer = parNBT.getInt("layer");

        levelCurIntensityStage = parNBT.getInt("levelCurIntensityStage");
        levelStormIntensityMax = parNBT.getInt("levelStormIntensityMax");
        levelCurStagesIntensity = parNBT.getFloat("levelCurStagesIntensity");
        stormType = parNBT.getInt("stormType");

        hasStormPeaked = parNBT.getBoolean("hasStormPeaked");

        //overCastModeAndRaining = parNBT.getBoolean("overCastModeAndRaining");

        dead = parNBT.getBoolean("isDead");

        cloudlessStorm = parNBT.getBoolean("cloudlessStorm");

        isFirenado = parNBT.getBoolean("isFirenado");

        ticksSinceLastPacketReceived = 0;//manager.getWorld().getTotalWorldTime();

        weatherMachineControlled = parNBT.getBoolean("weatherMachineControlled");
    }

    //compose nbt data for packet (and serialization in future)
    @Override
    public void nbtSyncForClient() {
        super.nbtSyncForClient();

        CachedNbtCompound data = this.getNbtCache();

        data.putBoolean("attrib_rain", attrib_precipitation);
        data.putBoolean("attrib_waterSpout", attrib_waterSpout);

        data.setInt("currentTopYBlock", currentTopYBlock);

        data.putFloat("levelTemperature", levelTemperature);
        data.setInt("levelWater", levelWater);

        data.setInt("layer", layer);

        data.setInt("levelCurIntensityStage", levelCurIntensityStage);
        data.putFloat("levelCurStagesIntensity", levelCurStagesIntensity);
        data.putFloat("levelStormIntensityMax", levelStormIntensityMax);
        data.setInt("stormType", stormType);

        data.putBoolean("hasStormPeaked", hasStormPeaked);

        //data.setBoolean("overCastModeAndRaining", overCastModeAndRaining);

        data.putBoolean("isDead", dead);

        data.putBoolean("cloudlessStorm", cloudlessStorm);


        data.putBoolean("isFirenado", isFirenado);

        data.putBoolean("weatherMachineControlled", weatherMachineControlled);
    }

    public NbtCompound nbtForIMC() {
        //we basically need all the same data minus a few soooo whatever
        nbtSyncForClient();
        return getNbtCache().getNewNbt();
    }

    public void tickRender(float partialTick) {
        super.tickRender(partialTick);

        //renderBlock.doRenderClouds(this, 0, 0, 0, 0, partialTick);

        //TODO: consider only putting funnel in this method since its the fast part, the rest might be slow enough to only need to do per gametick

        if (!MinecraftClient.getInstance().isPaused()) {
            //ParticleBehaviorFog.newCloudWay = true;

            Iterator<Map.Entry<Integer, EntityRotFX>> it = lookupParticlesCloud.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, EntityRotFX> entry = it.next();
                EntityRotFX ent = entry.getValue();
                if (!ent.isAlive()) {
                    it.remove();
                } else {
                    int i = entry.getKey();
                    Vec3d tryPos;
                    double spawnRad = 120;//(ticksExisted % 100) + 10;
                    double speed = 2D / (spawnRad);
                    if (isSpinning()) {
                        speed = 50D / (spawnRad);
                    }
                    ent.rotationSpeedAroundCenter = (float) speed;
                    if (i == 0) {
                        tryPos = new Vec3d(pos.x, layers.get(layer), pos.z);
                        ent.rotationYaw = ent.rotationAroundCenter;
                    } else {
                        double rad = Math.toRadians(ent.rotationAroundCenter - ent.rotationSpeedAroundCenter + (ent.rotationSpeedAroundCenter * partialTick));
                        double x = -Math.sin(rad) * spawnRad;
                        double z = Math.cos(rad) * spawnRad;
                        tryPos = new Vec3d(pos.x + x, layers.get(layer), pos.z + z);

                        double var16 = this.pos.x - ent.getPosX();
                        double var18 = this.pos.z - ent.getPosZ();
                        ent.rotationYaw = (float) (Math.atan2(var18, var16) * 180.0D / Math.PI) - 90.0F;


                    }
                    ent.setPos(tryPos.x, tryPos.y, tryPos.z);
                }
            }

            it = lookupParticlesCloudLower.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, EntityRotFX> entry = it.next();
                EntityRotFX ent = entry.getValue();
                if (!ent.isAlive()) {
                    it.remove();
                } else {
                    int i = entry.getKey();
                    Vec3d tryPos;

                    ent.setScale(800);

                    int layerRot = i / 16;
                    double spawnRad = 80;
                    if (layerRot == 1) {
                        spawnRad = 60;
                        ent.setScale(600);
                    }
                    double speed = 50D / (spawnRad * 2D);

                    ent.rotationSpeedAroundCenter = (float) speed;
                    double rad = Math.toRadians(ent.rotationAroundCenter - ent.rotationSpeedAroundCenter + (ent.rotationSpeedAroundCenter * partialTick));
                    double x = -Math.sin(rad) * spawnRad;
                    double z = Math.cos(rad) * spawnRad;
                    tryPos = new Vec3d(pos.x + x, layers.get(layer) - 20, pos.z + z);

                    ent.setPos(tryPos.x, tryPos.y, tryPos.z);

                    double var16 = this.pos.x - ent.getPosX();
                    double var18 = this.pos.z - ent.getPosZ();
                    ent.rotationYaw = (float) (Math.atan2(var18, var16) * 180.0D / Math.PI) - 90.0F;
                    ent.rotationPitch = -20F;// - (ent.getEntityId() % 10);
                }
            }
        }
    }

    public void tick() {
        super.tick();
        //Weather.dbg("ticking storm " + ID + " - manager: " + manager);

        //adjust posGround to be pos with the ground Y pos for convinient usage
        posGround = new Vec3d(pos.x, currentTopYBlock, pos.z);

        if (manager.getWorld().isClient) {
            if (!MinecraftClient.getInstance().isPaused()) {

                ticksSinceLastPacketReceived++;

                //if (layer == 0) {
                tickClient();
                //}

                if (isTornadoFormingOrGreater() || isCycloneFormingOrGreater()) {
                    tornadoHelper.tick(manager.getWorld());
                }

                if (levelCurIntensityStage >= STATE_HIGHWIND) {
                    if (manager.getWorld().isClient) {
                        tornadoHelper.soundUpdates(true, isTornadoFormingOrGreater() || isCycloneFormingOrGreater());
                    }
                }

                tickMovementClient();
            }
        } else {
            if (isCloudlessStorm()) {
                if (ConfigMisc.overcastMode && manager.getWorld().isRaining()) {
                    this.setCloudlessStorm(false);
                }
            }

            if (isTornadoFormingOrGreater() || isCycloneFormingOrGreater()) {
                tornadoHelper.tick(manager.getWorld());
            }

            if (levelCurIntensityStage >= STATE_HIGHWIND) {
                if (manager.getWorld().isClient) {
                    tornadoHelper.soundUpdates(true, isTornadoFormingOrGreater() || isCycloneFormingOrGreater());
                }
            }

            tickMovement();

            //System.out.println("cloud motion: " + motion + " wind angle: " + angle);

            if (layer == 0) {
                if (!isCloudlessStorm()) {
                    tickWeatherEvents();
                    tickProgression();
                    tickSnowFall();
                }
            } else {
                //make layer 1 max size for visuals
                size = maxSize;
            }

            //overCastModeAndRaining = ConfigMisc.overcastMode && manager.getWorld().isRaining();

        }

        if (layer == 0) {
            //sync X Y Z, Y gets changed below
            posBaseFormationPos = new Vec3d(pos.x, pos.y, pos.z);

            if (levelCurIntensityStage >= CloudStorm.levelStormIntensityFormingStartVal) {
                if (levelCurIntensityStage >= CloudStorm.levelStormIntensityFormingStartVal + 1) {
                    formingStrength = 1;
                    posBaseFormationPos.y = posGround.y;
                } else {

                    //make it so storms touchdown at 0.5F intensity instead of 1 then instantly start going back up, keeps them down for a full 1F worth of intensity val
                    float intensityAdj = Math.min(1F, levelCurStagesIntensity * 2F);

                    //shouldnt this just be intensityAdj?
                    formingStrength = (levelCurIntensityStage + intensityAdj) - CloudStorm.levelStormIntensityFormingStartVal;
                    double yDiff = pos.y - posGround.y;
                    posBaseFormationPos.y = pos.y - (yDiff * formingStrength);
                }
            } else {
                if (levelCurIntensityStage == STATE_HIGHWIND) {
                    formingStrength = 1;
                    posBaseFormationPos.y = posGround.y;
                } else {
                    formingStrength = 0;
                    posBaseFormationPos.y = pos.y;
                }
            }


        }

    }

    public void tickMovement() {
        //storm movement via wind
        float angle = getAdjustedAngle();

        if (angleIsOverridden) {
            angle = angleMovementTornadoOverride;
        }

        //despite overridden angle, still avoid obstacles

        //slight randomness to angle
        Random rand = new Random();
        angle += (rand.nextFloat() - rand.nextFloat()) * 0.15F;

        //avoid large obstacles
        double scanDist = 50;
        double scanX = this.pos.x + (-Math.sin(Math.toRadians(angle)) * scanDist);
        double scanZ = this.pos.z + (Math.cos(Math.toRadians(angle)) * scanDist);

        int height = WeatherUtilBlock.getPrecipitationHeightSafe(this.manager.getWorld(), new BlockPos(scanX, 0, scanZ)).getY();

        if (this.pos.yCoord < height) {
            float angleAdj = 45;
            if (this.id % 2 == 0) {
                angleAdj = -45;
            }
            angle += angleAdj;
        }

        //Weather.dbg("cur angle: " + angle);

        double vecX = -Math.sin(Math.toRadians(angle));
        double vecZ = Math.cos(Math.toRadians(angle));

        float cloudSpeedAmp = 0.2F;


        float finalSpeed = getAdjustedSpeed() * cloudSpeedAmp;

        if (levelCurIntensityStage >= STATE_FORMING) {
            finalSpeed = 0.2F;
        } else if (levelCurIntensityStage >= STATE_THUNDER) {
            finalSpeed = 0.05F;
        }

        if (levelCurIntensityStage >= levelStormIntensityFormingStartVal) {
            finalSpeed /= levelCurIntensityStage - levelStormIntensityFormingStartVal + 1F;
        }

        if (finalSpeed < 0.03F) {
            finalSpeed = 0.03F;
        }

        if (finalSpeed > 0.3F) {
            finalSpeed = 0.3F;
        }

        if (!weatherMachineControlled) {
            motion.x = vecX * finalSpeed;
            motion.z = vecZ * finalSpeed;

            //actually move storm
            pos.x += motion.x;
            pos.z += motion.z;
        }
    }

    public void tickMovementClient() {
        if (!weatherMachineControlled) {
            pos.x += motion.x;
            pos.z += motion.z;
        }
    }

    public void tickWeatherEvents() {
        Random rand = new Random();
        World world = manager.getWorld();

        currentTopYBlock = WeatherUtilBlock.getPrecipitationHeightSafe(world, new BlockPos(MathHelper.floor(pos.x), 0, MathHelper.floor(pos.z))).getY();
        //Weather.dbg("currentTopYBlock: " + currentTopYBlock);
        if (levelCurIntensityStage >= STATE_THUNDER) {
            if (rand.nextInt(Math.max(1, ConfigStorm.Storm_LightningStrikeBaseValueOddsTo1 - (levelCurIntensityStage * 10))) == 0) {
                int x = (int) (pos.x + rand.nextInt(size) - rand.nextInt(size));
                int z = (int) (pos.z + rand.nextInt(size) - rand.nextInt(size));
                if (world.canSetBlock(new BlockPos(x, 0, z))) {
                    int y = world.getPrecipitationHeight(new BlockPos(x, 0, z)).getY();
                    //if (world.canLightningStrikeAt(x, y, z)) {
                    addWeatherEffectLightning(new EntityLightningBolt(world, x, y, z), false);
                    //}
                }
            }
        }

        //dont forget, this doesnt account for storm size, so small storms have high concentration of hail, as it grows, it appears to lessen in rate
        if (isPrecipitating() && levelCurIntensityStage == STATE_HAIL && stormType == TYPE_LAND) {
            //if (rand.nextInt(1) == 0) {
            for (int i = 0; i < Math.max(1, ConfigStorm.Storm_HailPerTick * (size / maxSize)); i++) {
                int x = (int) (pos.x + rand.nextInt(size) - rand.nextInt(size));
                int z = (int) (pos.z + rand.nextInt(size) - rand.nextInt(size));
                if (world.canSetBlock(new BlockPos(x, static_YPos_layer0, z)) && (world.getClosestPlayer(x, 50, z, 80, false) != null)) {
                    //int y = world.getPrecipitationHeight(x, z);
                    //if (world.canLightningStrikeAt(x, y, z)) {
                    EntityIceBall hail = new EntityIceBall(world);
                    hail.setPosition(x, layers.get(layer), z);
                    world.spawnEntity(hail);
                    //world.addWeatherEffect(new EntityLightningBolt(world, (double)x, (double)y, (double)z));
                    //}

                    //System.out.println("spawned hail: " );
                }
            }
        }

        trackAndExtinguishEntities();
    }

    public void trackAndExtinguishEntities() {
        if (ConfigStorm.Storm_Rain_TrackAndExtinguishEntitiesRate <= 0) return;

        if (isPrecipitating()) {
            //efficient caching
            if ((manager.getWorld().getTime() + (id * 20)) % ConfigStorm.Storm_Rain_TrackAndExtinguishEntitiesRate == 0) {
                listEntitiesUnderClouds.clear();
                BlockPos posBP = new BlockPos(posGround.x, posGround.y, posGround.z);
                List<LivingEntity> listEnts = manager.getWorld().getEntitiesByClass(LivingEntity.class, new Box(posBP).expand(size), null);
                for (LivingEntity ent : listEnts) {
                    // TODO: Ensure this should be isSkyVisibleAllowingSea instead of isSkyVisible
                    if (ent.world.isSkyVisibleAllowingSea(ent.getBlockPos())) {
                        listEntitiesUnderClouds.add(ent);
                    }
                }
            }

            for (LivingEntity ent : listEntitiesUnderClouds) {
                ent.extinguish();
            }
        }
    }

    public void tickSnowFall() {
        if (!ConfigSnow.Snow_PerformSnowfall) return;

        if (!isPrecipitating()) return;

        World world = manager.getWorld();

        int xx;
        int zz;

        for (xx = (int) (pos.x - size / 2); xx < pos.x + size / 2; xx += 16) {
            for (zz = (int) (pos.z - size / 2); zz < pos.z + size / 2; zz += 16) {
                int chunkX = xx / 16;
                int chunkZ = zz / 16;
                int x = chunkX * 16;
                int z = chunkZ * 16;
                //world.theProfiler.startSection("getChunk");

                //afterthought, for weather 2.3.7
                if (!world.canSetBlock(new BlockPos(x, 128, z))) {
                    continue;
                }

                Chunk chunk = world.getChunk(chunkX, chunkZ);
                int i1;
                int xxx;
                int zzz;
                int setBlockHeight;

                if (world.provider.canDoRainSnowIce(chunk) && (ConfigSnow.Snow_RarityOfBuildup == 0 || world.random.nextInt(ConfigSnow.Snow_RarityOfBuildup) == 0)) {
                    updateLCG = updateLCG * 3 + 1013904223;
                    i1 = updateLCG >> 2;
                    xxx = i1 & 15;
                    zzz = i1 >> 8 & 15;

                    double d0 = pos.x - (xx + xxx);
                    double d2 = pos.z - (zz + zzz);
                    if ((double) MathHelper.sqrt(d0 * d0 + d2 * d2) > size) continue;

                    //snow loops past 6 for some reason

                    setBlockHeight = world.getPrecipitationHeight(new BlockPos(xxx + x, 0, zzz + z)).getY();

                    if (canSnowAtBody(xxx + x, setBlockHeight, zzz + z) && Blocks.SNOW.canPlaceBlockAt(world, new BlockPos(xxx + x, setBlockHeight, zzz + z))) {
                        //if (entP != null && entP.getDistance(xx, entP.posY, zz) < 16) {
                        WindManager windMan = manager.getWindManager();
                        float angle = windMan.getWindAngleForClouds();

                        Vec3d vecPos = new Vec3d(xxx + x, setBlockHeight, zzz + z);

                        //int y = WeatherUtilBlock.getPrecipitationHeightSafe(world, new BlockPos(vecPos.xCoord, 0, vecPos.zCoord)).getY();
                        //vecPos.yCoord = y;

                        //avoid unloaded areas
                        if (!world.canSetBlock(new BlockPos(vecPos))) continue;

                        //make sure vanilla style 1 layer of snow everywhere can also happen
                        //but only when we arent in global overcast mode
                        if (!ConfigMisc.overcastMode) {

                            //TODO: consider letting this run outside of ConfigSnow.Snow_PerformSnowfall config option
                            //since our version canSnowAtBody returns true for existing snow layers, we need to check we have air here for basic 1 layer place
                            if (world.isAir(new BlockPos(vecPos))) {
                                world.setBlockState(new BlockPos(vecPos), Blocks.SNOW.getDefaultState());
                            }
                        }

                        //do wind/wall based snowfall
                        WeatherUtilBlock.fillAgainstWallSmoothly(world, vecPos, angle/* + angleRand*/, 15, 2, Blocks.SNOW);
                    }
                }
            }
        }
    }

    public boolean canSnowAtBody(int par1, int par2, int par3) {
        World world = manager.getWorld();

        Biome biomegenbase = world.getBiome(new BlockPos(par1, 0, par3));

        BlockPos pos = new BlockPos(par1, par2, par3);

        if (biomegenbase == null) return false;

        //float f = biomegenbase.getFloatTemperature(pos);

        float temperature = CoroUtilCompatibility.getAdjustedTemperature(world, biomegenbase, pos);

        if ((!canSnowFromCloudTemperature || !(levelTemperature > 0)) && (canSnowFromCloudTemperature || !(temperature > 0.15F))) {
            if (par2 >= 0 && par2 < 256 && world.getLightLevel(LightType.BLOCK, pos) < 10) {
                /*Block l = world.getBlockState(new BlockPos(par1, par2 - 1, par3)).getBlock();
                Block i1 = world.getBlockState(new BlockPos(par1, par2, par3)).getBlock();

                if ((CoroUtilBlock.isAir(i1) || i1 == Blocks.SNOW_LAYER)*//* && Block.snow.canPlaceBlockAt(world, par1, par2, par3)*//* && CoroUtilBlock.isAir(l) && l != Blocks.ICE && l.getMaterial(l.getDefaultState()).blocksMovement())
                {
                    return true;
                }*/
                BlockState iblockstate1 = world.getBlockState(pos);

                //TODO: incoming new way to detect if blocks can be snowed on https://github.com/MinecraftForge/MinecraftForge/pull/4569/files
                //might not require any extra work from me?

                return (iblockstate1.getBlock().isAir(iblockstate1, world, pos) || iblockstate1.getBlock() == Blocks.SNOW) && Blocks.SNOW.canPlaceBlockAt(world, pos);
            }

        }
        return false;
    }

    public void tickProgression() {
        World world = manager.getWorld();

        //storm progression, heavy WIP
        if (world.getTime() % 3 == 0) {
            if (isGrowing) {
                if (size < maxSize) {
                    size++;
                }
            }
        }

        float tempAdjustRate = (float) ConfigStorm.Storm_TemperatureAdjustRate;//0.1F;
        int levelWaterBuildRate = ConfigStorm.Storm_Rain_WaterBuildUpRate;
        int levelWaterSpendRate = ConfigStorm.Storm_Rain_WaterSpendRate;
        int randomChanceOfWaterBuildFromWater = ConfigStorm.Storm_Rain_WaterBuildUpOddsTo1FromSource;
        int randomChanceOfWaterBuildFromNothing = ConfigStorm.Storm_Rain_WaterBuildUpOddsTo1FromNothing;
        int randomChanceOfWaterBuildFromOvercastRaining;
        randomChanceOfWaterBuildFromOvercastRaining = 10;
        //int randomChanceOfRain = ConfigMisc.Player_Storm_Rain_OddsTo1;

        boolean isInOcean = false;
        boolean isOverWater = false;

        if (world.getTime() % ConfigStorm.Storm_AllTypes_TickRateDelay == 0) {
            NbtCompound playerNBT = PlayerData.getPlayerNBT(userSpawnedFor);

            long lastStormDeadlyTime = playerNBT.getLong("lastStormDeadlyTime");
            //long lastStormRainTime = playerNBT.getLong("lastStormRainTime");

            Biome bgb = world.getBiome(new BlockPos(MathHelper.floor(pos.x), 0, MathHelper.floor(pos.z)));

            //temperature scan
            if (bgb != null) {
                isInOcean = bgb.getCategory() == Biome.Category.OCEAN;

                //float biomeTempAdj = getTemperatureMCToWeatherSys(bgb.getFloatTemperature(new BlockPos(MathHelper.floor(pos.xCoord), MathHelper.floor(pos.yCoord), MathHelper.floor(pos.zCoord))));
                float biomeTempAdj = getTemperatureMCToWeatherSys(CoroUtilCompatibility.getAdjustedTemperature(manager.getWorld(), bgb, new BlockPos(MathHelper.floor(pos.x), MathHelper.floor(pos.y), MathHelper.floor(pos.z))));
                if (levelTemperature > biomeTempAdj) {
                    levelTemperature -= tempAdjustRate;
                } else {
                    levelTemperature += tempAdjustRate;
                }
            }

            boolean performBuildup = false;

            Random rand = new Random();

            if (!isPrecipitating() && rand.nextInt(randomChanceOfWaterBuildFromNothing) == 0) {
                performBuildup = true;
            }

            if (!isPrecipitating() && ConfigMisc.overcastMode && manager.getWorld().isRaining() &&
                    rand.nextInt(randomChanceOfWaterBuildFromOvercastRaining) == 0) {
                performBuildup = true;
            }

            Block blockID = world.getBlockState(new BlockPos(MathHelper.floor(pos.x), currentTopYBlock - 1, MathHelper.floor(pos.z))).getBlock();
            if (blockID.getDefaultState().getMaterial() != Material.AIR) {
                //Block block = Block.blocksList[blockID];
                if (blockID.getDefaultState().getMaterial() == Material.WATER) {
                    isOverWater = true;
                }
            }

            //water scan - dont build up if raining already
            if (!performBuildup && !isPrecipitating() && rand.nextInt(randomChanceOfWaterBuildFromWater) == 0) {
                if (isOverWater) {
                    performBuildup = true;
                }

                if (!performBuildup && bgb != null && (isInOcean || bgb.getCategory() == Biome.Category.SWAMP || bgb.getCategory() == Biome.Category.JUNGLE || bgb.getCategory() == Biome.Category.RIVER)) {
                    performBuildup = true;
                }
            }

            if (performBuildup) {
                //System.out.println("RAIN BUILD TEMP OFF");
                levelWater += levelWaterBuildRate;
                Weather.LOGGER.debug("building rain: " + levelWater);
            }

            //water values adjust when raining
            if (isPrecipitating()) {
                levelWater -= levelWaterSpendRate;

                if (levelWater < 0) levelWater = 0;

                if (levelWater == 0) {
                    setPrecipitating(false);
                    Weather.LOGGER.debug("ending raining for: " + id);
                }
            } else {
                if (levelWater >= levelWaterStartRaining) {
                    if (ConfigMisc.overcastMode) {
                        if (manager.getWorld().isRaining()) {
                            if (ConfigStorm.Storm_Rain_Overcast_OddsTo1 != -1 && rand.nextInt(ConfigStorm.Storm_Rain_Overcast_OddsTo1) == 0) {
                                setPrecipitating(true);
                                Weather.LOGGER.debug("starting raining for: " + id);
                            }
                        }
                    } else {
                        if (ConfigStorm.Storm_Rain_OddsTo1 != -1 && rand.nextInt(ConfigStorm.Storm_Rain_OddsTo1) == 0) {
                            setPrecipitating(true);
                            Weather.LOGGER.debug("starting raining for: " + id);
                        }
                    }
                }

            }

            //actual storm formation chance

            ServerWeatherManager wm = ServerTickHandler.lookupDimToWeatherMan.get(world.getDimension());

            boolean tryFormStorm = false;

            if (this.canBeDeadly && this.levelCurIntensityStage == STATE_NORMAL) {
                if (ConfigStorm.Server_Storm_Deadly_UseGlobalRate) {
                    if (ConfigStorm.Server_Storm_Deadly_TimeBetweenInTicks != -1) {
                        if (wm.lastCloudStormFormed == 0 || wm.lastCloudStormFormed + ConfigStorm.Server_Storm_Deadly_TimeBetweenInTicks < world.getTime()) {
                            tryFormStorm = true;
                        }
                    }
                } else {
                    if (ConfigStorm.Player_Storm_Deadly_TimeBetweenInTicks != -1) {
                        if (lastStormDeadlyTime == 0 || lastStormDeadlyTime + ConfigStorm.Player_Storm_Deadly_TimeBetweenInTicks < world.getTime()) {
                            tryFormStorm = true;
                        }
                    }
                }
            }

            if (weatherMachineControlled) {
                return;
            }

            if ((!ConfigMisc.overcastMode || manager.getWorld().isRaining()) && WeatherUtilConfig.listDimensionsStorms.contains(manager.getWorld().getDimension()) && tryFormStorm) {
                int stormFrontCollideDist = ConfigStorm.Storm_Deadly_CollideDistance;
                int randomChanceOfCollide = ConfigStorm.Player_Storm_Deadly_OddsTo1;

                if (ConfigStorm.Server_Storm_Deadly_UseGlobalRate) {
                    randomChanceOfCollide = ConfigStorm.Server_Storm_Deadly_OddsTo1;
                }

                if (isInOcean && (ConfigStorm.Storm_OddsTo1OfOceanBasedStorm > 0 && rand.nextInt(ConfigStorm.Storm_OddsTo1OfOceanBasedStorm) == 0)) {
                    PlayerEntity entP = world.getPlayerEntityByName(userSpawnedFor);

                    initRealStorm(entP, null);

                    if (ConfigStorm.Server_Storm_Deadly_UseGlobalRate) {
                        wm.lastCloudStormFormed = world.getTime();
                    } else {
                        playerNBT.putLong("lastStormDeadlyTime", world.getTime());
                    }
                } else if (!isInOcean && ConfigStorm.Storm_OddsTo1OfLandBasedStorm > 0 && rand.nextInt(ConfigStorm.Storm_OddsTo1OfLandBasedStorm) == 0) {
                    PlayerEntity entP = world.getPlayerEntityByName(userSpawnedFor);

                    initRealStorm(entP, null);

                    if (ConfigStorm.Server_Storm_Deadly_UseGlobalRate) {
                        wm.lastCloudStormFormed = world.getTime();
                    } else {
                        playerNBT.putLong("lastStormDeadlyTime", world.getTime());
                    }
                } else if (rand.nextInt(randomChanceOfCollide) == 0) {
                    for (int i = 0; i < manager.getStorms().size(); i++) {
                        Storm wo = manager.getStorms().get(i);

                        if (wo instanceof CloudStorm) {
                            CloudStorm so = (CloudStorm) wo;

                            boolean startStorm = false;

                            if (so.id != this.id && so.levelCurIntensityStage <= 0 && !so.isCloudlessStorm() && !so.weatherMachineControlled) {
                                if (so.pos.distanceTo(pos) < stormFrontCollideDist) {
                                    if (this.levelTemperature < 0) {
                                        if (so.levelTemperature > 0) {
                                            startStorm = true;
                                        }
                                    } else if (this.levelTemperature > 0) {
                                        if (so.levelTemperature < 0) {
                                            startStorm = true;
                                        }
                                    }
                                }
                            }

                            if (startStorm) {
                                //Weather.dbg("start storm!");

                                playerNBT.putLong("lastStormDeadlyTime", world.getTime());

                                //EntityPlayer entP = manager.getWorld().getClosestPlayer(pos.xCoord, pos.yCoord, pos.zCoord, -1);
                                PlayerEntity entP = world.getPlayerEntityByName(userSpawnedFor);

                                initRealStorm(entP, so);

                                break;
                            }
                        }
                    }
                }
            }

            if (isRealStorm()) {
                //force storms to die if its no longer raining while overcast mode is active
                if (ConfigMisc.overcastMode) {
                    if (!manager.getWorld().isRaining()) {
                        hasStormPeaked = true;
                    }
                }

                //force rain on while real storm and not dying
                if (!hasStormPeaked) {
                    levelWater = levelWaterStartRaining;
                    setPrecipitating(true);
                }

                if ((levelCurIntensityStage == STATE_HIGHWIND || levelCurIntensityStage == STATE_HAIL) && isOverWater) {
                    if (ConfigStorm.Storm_OddsTo1OfHighWindWaterSpout != 0 && rand.nextInt(ConfigStorm.Storm_OddsTo1OfHighWindWaterSpout) == 0) {
                        attrib_waterSpout = true;
                    }
                } else {
                    attrib_waterSpout = false;
                }

                float levelStormIntensityRate = 0.02F;
                float minIntensityToProgress = 0.6F;
                //change since storms have a predetermined max now, nevermind, storms take too long, limited simbox area
                //minIntensityToProgress = 0.8F;
                //int oddsTo1OfIntensityProgressionBase = ConfigStorm.Storm_OddsTo1OfProgressionBase;

                //speed up forming and greater progression when past forming state
                if (levelCurIntensityStage >= levelStormIntensityFormingStartVal) {
                    levelStormIntensityRate *= 3;
                    //oddsTo1OfIntensityProgressionBase /= 3;
                }

                //int oddsTo1OfIntensityProgression = oddsTo1OfIntensityProgressionBase + (levelCurIntensityStage * ConfigStorm.Storm_OddsTo1OfProgressionStageMultiplier);

                if (!hasStormPeaked) {

                    levelCurStagesIntensity += levelStormIntensityRate;

                    if (levelCurIntensityStage < maxIntensityStage && (!ConfigTornado.Storm_NoTornadosOrCyclones || levelCurIntensityStage < STATE_FORMING - 1)) {
                        if (levelCurStagesIntensity >= minIntensityToProgress) {
                            //Weather.dbg("storm ID: " + this.ID + " trying to hit next stage");
                            if (alwaysProgresses || levelCurIntensityStage < levelStormIntensityMax/*rand.nextInt(oddsTo1OfIntensityProgression) == 0*/) {
                                stageNext();
                                Weather.LOGGER.debug("storm ID: " + this.id + " - growing, stage: " + levelCurIntensityStage);
                                //mark is tropical cyclone if needed! and never unmark it!
                                if (isInOcean) {
                                    //make it ONLY allow to change during forming stage, so it locks in
                                    if (levelCurIntensityStage == STATE_FORMING) {
                                        Weather.LOGGER.debug("storm ID: " + this.id + " marked as tropical cyclone!");
                                        stormType = TYPE_WATER;

                                        //reroll dice on ocean storm since we only just define it here
                                        levelStormIntensityMax = rollDiceOnMaxIntensity();
                                        Weather.LOGGER.debug("rerolled odds for ocean storm, max stage will be: " + levelStormIntensityMax);
                                    }
                                }
                            }
                        }
                    }

                    Weather.LOGGER.debug("storm ID: " + this.id + " - growing, stage " + levelCurIntensityStage + " of max " + levelStormIntensityMax + ", at intensity: " + levelCurStagesIntensity);

                    if (levelCurStagesIntensity >= 1F) {
                        Weather.LOGGER.debug("storm peaked at: " + levelCurIntensityStage);
                        hasStormPeaked = true;
                    }
                } else {
                    if (ConfigMisc.overcastMode && manager.getWorld().isRaining()) {
                        levelCurStagesIntensity -= levelStormIntensityRate * 0.9F;
                    } else {
                        levelCurStagesIntensity -= levelStormIntensityRate * 0.3F;
                    }

                    if (levelCurStagesIntensity <= 0) {
                        stagePrev();
                        Weather.LOGGER.debug("storm ID: " + this.id + " - dying, stage: " + levelCurIntensityStage);
                        if (levelCurIntensityStage <= 0) {
                            setNoStorm();
                        }
                    }
                }

                //levelStormIntensityCur value ranges and what they influence
                //revised to remove rain and factor in tropical storm / hurricane
                //1 = thunderstorm (and more rain???)
                //2 = high wind
                //3 = hail
                //4 = tornado forming OR tropical cyclone (forming?) - logic splits off here where its marked as hurricane if its over water
                //5 = F1 OR TC 2
                //6 = F2 OR TC 3
                //7 = F3 OR TC 4
                //8 = F4 OR TC 5
                //9 = F5 OR hurricane ??? (perhaps hurricanes spawn differently, like over ocean only, and sustain when hitting land for a bit)

                //what about tropical storm? that is a mini hurricane, perhaps also ocean based

                //levelWindMomentum = rate of increase of storm??? (in addition to the pre storm system speeds)


                //POST DEV NOTES READ!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!:

                //it might be a good idea to make something else determine increase from high winds to tornado and higher
                //using temperatures is a little unstable at such a large range of variation....

                //updateStormFlags();
                //curWeatherType = Math.min(WeatherTypes.weatherEntTypes.size()-1, Math.max(1, levelCurIntensityStage - 1));
            } else {
                if (ConfigMisc.overcastMode) {
                    if (!manager.getWorld().isRaining()) {
                        if (attrib_precipitation) {
                            setPrecipitating(false);
                        }
                    }
                }
            }
        }
    }

    public WeatherEntityConfig getWeatherEntityConfigForStorm() {
        //default spout
        WeatherEntityConfig weatherConfig = WeatherTypes.weatherEntTypes.get(0);
        if (levelCurIntensityStage >= STATE_STAGE5) {
            weatherConfig = WeatherTypes.weatherEntTypes.get(5);
        } else if (levelCurIntensityStage >= STATE_STAGE4) {
            weatherConfig = WeatherTypes.weatherEntTypes.get(4);
        } else if (levelCurIntensityStage >= STATE_STAGE3) {
            weatherConfig = WeatherTypes.weatherEntTypes.get(3);
        } else if (levelCurIntensityStage >= STATE_STAGE2) {
            weatherConfig = WeatherTypes.weatherEntTypes.get(2);
        } else if (levelCurIntensityStage >= STATE_STAGE1) {
            weatherConfig = WeatherTypes.weatherEntTypes.get(1);
        } else if (levelCurIntensityStage >= STATE_FORMING) {
            weatherConfig = WeatherTypes.weatherEntTypes.get(0);
        }
        return weatherConfig;
    }

    public void stageNext() {
        levelCurIntensityStage++;
        levelCurStagesIntensity = 0F;
        if (ConfigTornado.Storm_Tornado_aimAtPlayerOnSpawn) {
            if (!hasStormPeaked && levelCurIntensityStage == STATE_FORMING) {
                aimStormAtClosestOrProvidedPlayer(null);
            }
        }
    }

    public void stagePrev() {
        levelCurIntensityStage--;
        levelCurStagesIntensity = 1F;
    }

    public void initRealStorm(PlayerEntity entP, CloudStorm stormToAbsorb) {
        //new way of storm progression
        levelCurIntensityStage = STATE_THUNDER;


        //isRealStorm = true;
        if (naturallySpawned) {
            this.levelWater = this.levelWaterStartRaining * 2;
        }

        this.levelStormIntensityMax = rollDiceOnMaxIntensity();
        Weather.LOGGER.debug("rolled odds for storm, unless it becomes ocean storm, max stage will be: " + levelStormIntensityMax);

        this.attrib_precipitation = true;

        if (stormToAbsorb != null) {
            Weather.LOGGER.debug("stormfront collision happened between ID " + this.id + " and " + stormToAbsorb.id);
            manager.removeStorm(stormToAbsorb.id);
            ((ServerWeatherManager) manager).syncStormRemove(stormToAbsorb);
        } else {
            Weather.LOGGER.debug("ocean storm happened, ID " + this.id);
        }

        if (ConfigTornado.Storm_Tornado_aimAtPlayerOnSpawn) {
            //if (entP != null) {
            aimStormAtClosestOrProvidedPlayer(entP);
            //}
        }
    }

    public int rollDiceOnMaxIntensity() {
        Random rand = new Random();
        int randVal = rand.nextInt(100);
        if (stormType == TYPE_LAND) {
            if (randVal <= ConfigStorm.Storm_PercentChanceOf_F5_Tornado) {
                return STATE_STAGE5;
            } else if (randVal <= ConfigStorm.Storm_PercentChanceOf_F4_Tornado) {
                return STATE_STAGE4;
            } else if (randVal <= ConfigStorm.Storm_PercentChanceOf_F3_Tornado) {
                return STATE_STAGE3;
            } else if (randVal <= ConfigStorm.Storm_PercentChanceOf_F2_Tornado) {
                return STATE_STAGE2;
            } else if (randVal <= ConfigStorm.Storm_PercentChanceOf_F1_Tornado) {
                return STATE_STAGE1;
            } else if (randVal <= ConfigStorm.Storm_PercentChanceOf_F0_Tornado) {
                return STATE_FORMING;
            } else if (randVal <= ConfigStorm.Storm_PercentChanceOf_Hail) {
                return STATE_HAIL;
            } else if (randVal <= ConfigStorm.Storm_PercentChanceOf_HighWind) {
                return STATE_HIGHWIND;
            }
        } else if (stormType == TYPE_WATER) {
            if (randVal <= ConfigStorm.Storm_PercentChanceOf_C5_Cyclone) {
                return STATE_STAGE5;
            } else if (randVal <= ConfigStorm.Storm_PercentChanceOf_C4_Cyclone) {
                return STATE_STAGE4;
            } else if (randVal <= ConfigStorm.Storm_PercentChanceOf_C3_Cyclone) {
                return STATE_STAGE3;
            } else if (randVal <= ConfigStorm.Storm_PercentChanceOf_C2_Cyclone) {
                return STATE_STAGE2;
            } else if (randVal <= ConfigStorm.Storm_PercentChanceOf_C1_Cyclone) {
                return STATE_STAGE1;
            } else if (randVal <= ConfigStorm.Storm_PercentChanceOf_C0_Cyclone) {
                return STATE_FORMING;
            } else if (randVal <= ConfigStorm.Storm_PercentChanceOf_Hail) {
                return STATE_HAIL;
            } else if (randVal <= ConfigStorm.Storm_PercentChanceOf_HighWind) {
                return STATE_HIGHWIND;
            }
        }

        return STATE_THUNDER;
    }

    public void aimStormAtClosestOrProvidedPlayer(PlayerEntity entP) {
        if (entP == null) {
            entP = manager.getWorld().getClosestPlayer(pos.x, pos.y, pos.z, -1, false);
        }

        if (entP != null) {
            Random rand = new Random();
            double var11 = entP.getX() - pos.x;
            double var15 = entP.getZ() - pos.z;
            float yaw = -(float) (Math.atan2(var11, var15) * 180.0D / Math.PI);
            //weather override!
            //yaw = weatherMan.wind.direction;
            int size = ConfigTornado.Storm_Tornado_aimAtPlayerAngleVariance;
            if (size > 0) {
                yaw += rand.nextInt(size) - (size / 2);
            }

            angleIsOverridden = true;
            angleMovementTornadoOverride = yaw;

            Weather.LOGGER.debug("stormfront aimed at player " + entP.getEntityName());
        }
    }

    //FYI rain doesnt count as storm
    public void setNoStorm() {
        Weather.LOGGER.debug("storm ID: " + this.id + " - ended storm event");
        levelCurIntensityStage = STATE_NORMAL;
        levelCurStagesIntensity = 0;
    }

    public void tickClient() {
        if (isCloudlessStorm()) return;

        if (particleBehaviorFog == null) {
            particleBehaviorFog = new ParticleBehaviorFog(pos);
            //particleBehaviorFog.sourceEntity = this;
        } else {
            if (!MinecraftClient.getInstance().isInSingleplayer() || !(MinecraftClient.getInstance().currentScreen instanceof GameMenuScreen)) {
                particleBehaviorFog.tickUpdateList();
            }
        }

        PlayerEntity entP = MinecraftClient.getInstance().player;

        spinSpeed = 0.02D;
        double spinSpeedMax = 0.4D;
        if (isCycloneFormingOrGreater()) {
            spinSpeed = spinSpeedMax * 0.00D + ((levelCurIntensityStage - levelStormIntensityFormingStartVal + 1) * spinSpeedMax * 0.2D);
            //Weather.dbg("spin speed: " + spinSpeed);
        } else if (isTornadoFormingOrGreater()) {
            spinSpeed = spinSpeedMax * 0.2D;
        } else if (levelCurIntensityStage >= STATE_HIGHWIND) {
            spinSpeed = spinSpeedMax * 0.05D;
        } else {
            spinSpeed = spinSpeedMax * 0.02D;
        }

        //bonus!
        if (isHurricane()) {
            spinSpeed += 0.1D;
        }

        if (size == 0) size = 1;
        int delay = Math.max(1, (int) (100F / size * 1F));
        int loopSize = 1;//(int)(1 * size * 0.1F);

        int extraSpawning = 0;

        if (isSpinning()) {
            loopSize += 4;
            extraSpawning = 300;
        }

        //adjust particle creation rate for upper tropical cyclone work
        if (stormType == TYPE_WATER) {
            if (levelCurIntensityStage >= STATE_STAGE5) {
                loopSize = 10;
                extraSpawning = 800;
            } else if (levelCurIntensityStage >= STATE_STAGE4) {
                loopSize = 8;
                extraSpawning = 700;
            } else if (levelCurIntensityStage >= STATE_STAGE3) {
                loopSize = 6;
                extraSpawning = 500;
            } else if (levelCurIntensityStage >= STATE_STAGE2) {
                loopSize = 4;
                extraSpawning = 400;
            } else {
                extraSpawning = 300;
            }
        }

        //Weather.dbg("size: " + size + " - delay: " + delay);

        Random rand = new Random();

        Vec3d playerAdjPos = new Vec3d(entP.getX(), pos.y, entP.getZ());
        double maxSpawnDistFromPlayer = 512;

        //maintain clouds new system

        //spawn clouds
        if (ConfigCoroUtil.optimizedCloudRendering) {
            //1 in middle, 8 around it
            int count = 8 + 1;

            for (int i = 0; i < count; i++) {
                if (!lookupParticlesCloud.containsKey(i)) {
                    //position doesnt matter, set by renderer while its invisible still
                    Vec3d tryPos = new Vec3d(pos.x, layers.get(layer), pos.z);
                    EntityRotFX particle;
                    if (WeatherUtil.isAprilFoolsDay()) {
                        particle = spawnFogParticle(tryPos.x, tryPos.y, tryPos.z, 0, ParticleRegistry.chicken);
                    } else {
                        particle = spawnFogParticle(tryPos.x, tryPos.y, tryPos.z, 0, ParticleRegistry.cloud256_test);
                    }

                    //offset starting rotation for even distribution except for middle one
                    if (i != 0) {
                        double rotPos = (i - 1);
                        float radStart = (float) ((360D / 8D) * rotPos);
                        particle.rotationAroundCenter = radStart;
                    }

                    lookupParticlesCloud.put(i, particle);
                }
            }

            if (isSpinning()) {
                //2 layers of 16
                count = 16 * 2;

                for (int i = 0; i < count; i++) {
                    if (!lookupParticlesCloudLower.containsKey(i)) {
                        //position doesnt matter, set by renderer while its invisible still
                        Vec3d tryPos = new Vec3d(pos.x, layers.get(layer), pos.z);
                        EntityRotFX particle;
                        if (WeatherUtil.isAprilFoolsDay()) {
                            particle = spawnFogParticle(tryPos.x, tryPos.y, tryPos.z, 1, ParticleRegistry.chicken);
                        } else {
                            particle = spawnFogParticle(tryPos.x, tryPos.y, tryPos.z, 1, ParticleRegistry.cloud256_test);
                        }

                        //set starting offset for even distribution
                        double rotPos = i % 15;
                        float radStart = (float) ((360D / 16D) * rotPos);
                        particle.rotationAroundCenter = radStart;

                        lookupParticlesCloudLower.put(i, particle);
                    }
                }
            }
        }

        if (this.manager.getWorld().getTime() % (delay + (isSpinning() ? ConfigStorm.Storm_ParticleSpawnDelay : ConfigMisc.Cloud_ParticleSpawnDelay)) == 0) {
            for (int i = 0; i < loopSize; i++) {
                if (!ConfigCoroUtil.optimizedCloudRendering && listParticlesCloud.size() < (size + extraSpawning) / 1F) {
                    double spawnRad = size;

                    //Weather.dbg("listParticlesCloud.size(): " + listParticlesCloud.size());

                    Vec3d tryPos = new Vec3d(pos.x + (rand.nextDouble() * spawnRad) - (rand.nextDouble() * spawnRad), layers.get(layer), pos.z + (rand.nextDouble() * spawnRad) - (rand.nextDouble() * spawnRad));
                    if (tryPos.distanceTo(playerAdjPos) < maxSpawnDistFromPlayer) {
                        if (getAvoidAngleIfTerrainAtOrAheadOfPosition(getAdjustedAngle(), tryPos) == 0) {
                            EntityRotFX particle;
                            if (WeatherUtil.isAprilFoolsDay()) {
                                particle = spawnFogParticle(tryPos.x, tryPos.y, tryPos.z, 0, ParticleRegistry.chicken);
                            } else {

                                particle = spawnFogParticle(tryPos.x, tryPos.y, tryPos.z, 0);
                                if (isFirenado && isSpinning()) {
                                    //if (particle.getEntityId() % 20 < 5) {
                                    particle.setParticleTexture(ParticleRegistry.cloud256_fire);
                                    particle.setRBGColorF(1F, 1F, 1F);

                                    //}
                                }
                            }

                            listParticlesCloud.add(particle);
                        }
                    }
                }
            }
        }

        //ground effects
        if (!ConfigCoroUtil.optimizedCloudRendering && levelCurIntensityStage >= STATE_HIGHWIND) {
            for (int i = 0; i < (stormType == TYPE_WATER ? 50 : 3)/*loopSize/2*/; i++) {
                if (listParticlesGround.size() < (stormType == TYPE_WATER ? 600 : 150)/*size + extraSpawning*/) {
                    double spawnRad = size / 4 * 3;

                    if (stormType == TYPE_WATER) {
                        spawnRad = size * 3;
                    }

                    //Weather.dbg("listParticlesCloud.size(): " + listParticlesCloud.size());

                    Vec3d tryPos = new Vec3d(pos.x + (rand.nextDouble() * spawnRad) - (rand.nextDouble() * spawnRad), posGround.y, pos.z + (rand.nextDouble() * spawnRad) - (rand.nextDouble() * spawnRad));
                    if (tryPos.distanceTo(playerAdjPos) < maxSpawnDistFromPlayer) {
                        int groundY = WeatherUtilBlock.getPrecipitationHeightSafe(manager.getWorld(), new BlockPos((int) tryPos.x, 0, (int) tryPos.z)).getY();
                        EntityRotFX particle;
                        if (WeatherUtil.isAprilFoolsDay()) {
                            particle = spawnFogParticle(tryPos.x, groundY + 3, tryPos.z, 0, ParticleRegistry.potato);
                        } else {
                            particle = spawnFogParticle(tryPos.x, groundY + 3, tryPos.z, 0);
                        }

                        particle.setScale(200);
                        particle.rotationYaw = rand.nextInt(360);
                        particle.rotationPitch = rand.nextInt(360);

                        listParticlesGround.add(particle);
                    }
                }
            }
        }

        delay = 1;
        loopSize = 2;

        double spawnRad = size / 48;

        if (levelCurIntensityStage >= STATE_STAGE5) {
            spawnRad = 200;
            loopSize = 10;
            sizeMaxFunnelParticles = 1200;
        } else if (levelCurIntensityStage >= STATE_STAGE4) {
            spawnRad = 150;
            loopSize = 8;
            sizeMaxFunnelParticles = 1000;
        } else if (levelCurIntensityStage >= STATE_STAGE3) {
            spawnRad = 100;
            loopSize = 6;
            sizeMaxFunnelParticles = 800;
        } else if (levelCurIntensityStage >= STATE_STAGE2) {
            spawnRad = 50;
            loopSize = 4;
            sizeMaxFunnelParticles = 600;
        } else {
            sizeMaxFunnelParticles = 600;
        }

        //spawn funnel
        if (isTornadoFormingOrGreater() || (attrib_waterSpout)) {
            if (this.manager.getWorld().getTime() % (delay + ConfigStorm.Storm_ParticleSpawnDelay) == 0) {
                for (int i = 0; i < loopSize; i++) {
                    //temp comment out
                    //if (attrib_tornado_severity > 0) {

                    //Weather.dbg("spawn");

                    //trim!
                    if (listParticlesFunnel.size() >= sizeMaxFunnelParticles) {
                        listParticlesFunnel.get(0).setExpired();
                        listParticlesFunnel.remove(0);
                    }

                    if (listParticlesFunnel.size() < sizeMaxFunnelParticles) {
                        Vec3d tryPos = new Vec3d(pos.x + (rand.nextDouble() * spawnRad) - (rand.nextDouble() * spawnRad), pos.y, pos.z + (rand.nextDouble() * spawnRad) - (rand.nextDouble() * spawnRad));
                        //int y = entP.world.getPrecipitationHeight((int)tryPos.xCoord, (int)tryPos.zCoord);

                        if (tryPos.distanceTo(playerAdjPos) < maxSpawnDistFromPlayer) {
                            EntityRotFX particle;
                            if (!isFirenado/* && false*/) {
                                if (WeatherUtil.isAprilFoolsDay()) {
                                    particle = spawnFogParticle(tryPos.x, posBaseFormationPos.y, tryPos.z, 1, ParticleRegistry.potato);
                                } else {
                                    particle = spawnFogParticle(tryPos.x, posBaseFormationPos.y, tryPos.z, 1);
                                }
                            } else {
                                particle = spawnFogParticle(tryPos.x, posBaseFormationPos.y, tryPos.z, 1, ParticleRegistry.cloud256_fire);

                            }

                            //move these to a damn profile damnit!
                            particle.setMaxAge(150 + ((levelCurIntensityStage - 1) * 100) + rand.nextInt(100));

                            float baseBright = 0.3F;
                            float randFloat = (rand.nextFloat() * 0.6F);

                            particle.rotationYaw = rand.nextInt(360);

                            float finalBright = Math.min(1F, baseBright + randFloat);

                            //highwind aka spout in this current code location
                            if (levelCurIntensityStage == STATE_HIGHWIND) {
                                particle.setScale(150);
                                particle.setRBGColorF(finalBright - 0.2F, finalBright - 0.2F, finalBright);
                            } else {
                                particle.setScale(250);
                                particle.setRBGColorF(finalBright, finalBright, finalBright);
                            }

                            if (isFirenado) {
                                particle.setRBGColorF(1F, 1F, 1F);
                                particle.setScale(particle.getScale() * 0.7F);
                            }

                            listParticlesFunnel.add(particle);

                            //System.out.println(listParticlesFunnel.size());
                        }
                    }
                }
            }
        }

        for (int i = 0; i < listParticlesFunnel.size(); i++) {
            EntityRotFX ent = listParticlesFunnel.get(i);
            //System.out.println(ent.getPosY());
            if (!ent.isAlive()) {
                listParticlesFunnel.remove(ent);
            } else if (ent.getPosY() > pos.y) {
                ent.setExpired();
                listParticlesFunnel.remove(ent);
                //System.out.println("asd");
            } else {
                double var16 = this.pos.z - ent.getPosX();
                double var18 = this.pos.z - ent.getPosZ();
                ent.rotationYaw = (float) (Math.atan2(var18, var16) * 180.0D / Math.PI) - 90.0F;
                ent.rotationYaw += ent.getEntityId() % 90;
                ent.rotationPitch = -30F;

                //fade spout blue to grey
                if (levelCurIntensityStage == STATE_HIGHWIND) {
                    int fadingDistStart = 30;
                    if (ent.getPosY() > posGround.y + fadingDistStart) {
                        float maxVal = ent.getBlueColorF();
                        float fadeRate = 0.002F;
                        ent.setRBGColorF(Math.min(maxVal, ent.getRedColorF() + fadeRate), Math.min(maxVal, ent.getGreenColorF() + fadeRate), maxVal);
                    }
                }

                spinEntity(ent);
            }
        }

        for (int i = 0; i < listParticlesCloud.size(); i++) {
            EntityRotFX ent = listParticlesCloud.get(i);
            if (!ent.isAlive()) {
                listParticlesCloud.remove(ent);
            } else {
                //ent.posX = pos.xCoord + i*10;

                double curSpeed = Math.sqrt(ent.getMotionX() * ent.getMotionX() + ent.getMotionY() * ent.getMotionY() + ent.getMotionZ() * ent.getMotionZ());

                double curDist = ent.getDistance(pos.x, ent.getPosY(), pos.z);

                float dropDownRange = 15F;

                float extraDropCalc = 0;
                if (curDist < 200 && ent.getEntityId() % 20 < 5) {
                    //cyclone and hurricane dropdown modifications here
                    extraDropCalc = ((ent.getEntityId() % 20) * dropDownRange);
                    if (isCycloneFormingOrGreater()) {
                        extraDropCalc = ((ent.getEntityId() % 20) * dropDownRange * 5F);
                        //Weather.dbg("extraDropCalc: " + extraDropCalc);
                    }
                }

                if (isSpinning()) {
                    double speed = spinSpeed + (rand.nextDouble() * 0.01D);
                    double distt = size;//300D;

                    double vecX = ent.getPosX() - pos.x;
                    double vecZ = ent.getPosZ() - pos.z;
                    float angle = (float) (Math.atan2(vecZ, vecX) * 180.0D / Math.PI);
                    //System.out.println("angle: " + angle);

                    //fix speed causing inner part of formation to have a gap
                    angle += speed * 50D;
                    //angle += 20;

                    angle -= (ent.getEntityId() % 10) * 3D;

                    //random addition
                    angle += rand.nextInt(10) - rand.nextInt(10);

                    if (curDist > distt) {
                        //System.out.println("curving");
                        angle += 40;
                        //speed = 1D;
                    }

                    //keep some near always - this is the lower formation part
                    if (ent.getEntityId() % 20 < 5) {
                        if (levelCurIntensityStage >= STATE_FORMING) {
                            if (stormType == TYPE_WATER) {
                                angle += 40 + ((ent.getEntityId() % 5) * 4);
                                if (curDist > 150 + ((levelCurIntensityStage - levelStormIntensityFormingStartVal + 1) * 30)) {
                                    angle += 10;
                                }
                            } else {
                                angle += 30 + ((ent.getEntityId() % 5) * 4);
                            }

                        } else {
                            //make a wider spinning lower area of cloud, for high wind
                            if (curDist > 150) {
                                angle += 50 + ((ent.getEntityId() % 5) * 4);
                            }
                        }

                        double var16 = this.pos.x - ent.getPosX();
                        double var18 = this.pos.z - ent.getPosZ();
                        ent.rotationYaw = (float) (Math.atan2(var18, var16) * 180.0D / Math.PI) - 90.0F;
                        ent.rotationPitch = -20F - (ent.getEntityId() % 10);
                    }

                    if (curSpeed < speed * 20D) {
                        ent.setMotionX(ent.getMotionX() + -Math.sin(Math.toRadians(angle)) * speed);
                        ent.setMotionZ(ent.getMotionZ() + Math.cos(Math.toRadians(angle)) * speed);
                    }
                } else {
                    float cloudMoveAmp = 0.2F * (1 + layer);

                    float speed = getAdjustedSpeed() * cloudMoveAmp;
                    float angle = getAdjustedAngle();

                    //TODO: prevent new particles spawning inside or near solid blocks

                    if ((manager.getWorld().getTime() + this.id) % 40 == 0) {
                        ent.avoidTerrainAngle = getAvoidAngleIfTerrainAtOrAheadOfPosition(angle, ent.getPos());
                    }

                    angle += ent.avoidTerrainAngle;

                    if (ent.avoidTerrainAngle != 0) {
                        speed *= 0.5D;
                    }

                    dropDownRange = 5;
                    if (/*curDist < 200 && */ent.getEntityId() % 20 < 5) {
                        extraDropCalc = ((ent.getEntityId() % 20) * dropDownRange);
                    }

                    if (curSpeed < speed * 1D) {
                        ent.setMotionX(ent.getMotionX() + -Math.sin(Math.toRadians(angle)) * speed);
                        ent.setMotionZ(ent.getMotionZ() + Math.cos(Math.toRadians(angle)) * speed);
                    }
                }

                if (Math.abs(ent.getPosY() - (pos.y - extraDropCalc)) > 2F) {
                    if (ent.getPosY() < pos.y - extraDropCalc) {
                        ent.setMotionY(ent.getMotionY() + 0.1D);
                    } else {
                        ent.setMotionY(ent.getMotionY() - 0.1D);
                    }
                }

                float dropDownSpeedMax = 0.15F;

                if (isCycloneFormingOrGreater()) {
                    dropDownSpeedMax = 0.9F;
                }

                if (ent.getMotionY() < -dropDownSpeedMax) {
                    ent.setMotionY(-dropDownSpeedMax);
                }

                if (ent.getMotionY() > dropDownSpeedMax) {
                    ent.setMotionY(dropDownSpeedMax);
                }
            }
        }

        for (int i = 0; i < listParticlesGround.size(); i++) {
            EntityRotFX ent = listParticlesGround.get(i);

            double curDist = ent.getDistance(pos.x, ent.getPosY(), pos.z);

            if (!ent.isAlive()) {
                listParticlesGround.remove(ent);
            } else {
                double curSpeed = Math.sqrt(ent.getMotionX() * ent.getMotionX() + ent.getMotionY() * ent.getMotionY() + ent.getMotionZ() * ent.getMotionZ());

                double speed = Math.max(0.2F, 5F * spinSpeed) + (rand.nextDouble() * 0.01D);

                double vecX = ent.getPosX() - pos.x;
                double vecZ = ent.getPosZ() - pos.z;
                float angle = (float) (Math.atan2(vecZ, vecX) * 180.0D / Math.PI);

                angle += 85;

                int maxParticleSize = 60;

                if (stormType == TYPE_WATER) {
                    maxParticleSize = 150;
                    speed /= 5D;
                }

                ent.setScale((float) Math.min(maxParticleSize, curDist * 2F));

                if (curDist < 20) {
                    ent.setExpired();
                }

                if (curSpeed < speed * 20D) {
                    ent.setMotionX(ent.getMotionX() + -Math.sin(Math.toRadians(angle)) * speed);
                    ent.setMotionZ(ent.getMotionZ() + Math.cos(Math.toRadians(angle)) * speed);
                }
            }
        }

        //System.out.println("size: " + listParticlesCloud.size());
    }

    public float getAdjustedSpeed() {
        return manager.windManager.getWindSpeedForClouds();
    }

    public float getAdjustedAngle() {
        float angle = manager.windManager.getWindAngleForClouds();

        float angleAdjust = Math.max(10, Math.min(45, 45F * levelTemperature * 0.2F));
        float targetYaw = 0;

        //coldfronts go south to 0, warmfronts go north to 180
        if (levelTemperature > 0) {
            //Weather.dbg("warmer!");
            targetYaw = 180;
        } else {
            //Weather.dbg("colder!");
            targetYaw = 0;
        }

        float bestMove = MathHelper.wrapDegrees(targetYaw - angle);

        if (Math.abs(bestMove) < 180/* - (angleAdjust * 2)*/) {
            if (bestMove > 0) angle -= angleAdjust;
            if (bestMove < 0) angle += angleAdjust;
        }

        //Weather.dbg("ID: " + ID + " - " + manager.windMan.getWindAngleForClouds() + " - final angle: " + angle);

        return angle;
    }

    public float getAvoidAngleIfTerrainAtOrAheadOfPosition(float angle, Vec3d pos) {
        double scanDistMax = 120;
        for (int scanAngle = -20; scanAngle < 20; scanAngle += 10) {
            for (double scanDistRange = 20; scanDistRange < scanDistMax; scanDistRange += 10) {
                double scanX = pos.x + (-Math.sin(Math.toRadians(angle + scanAngle)) * scanDistRange);
                double scanZ = pos.z + (Math.cos(Math.toRadians(angle + scanAngle)) * scanDistRange);

                int height = WeatherUtilBlock.getPrecipitationHeightSafe(this.manager.getWorld(), new BlockPos(scanX, 0, scanZ)).getY();

                if (pos.y < height) {
                    if (scanAngle <= 0) {
                        return 90;
                    } else {
                        return -90;
                    }
                }
            }
        }
        return 0;
    }

    public void spinEntity(Object entity1) {
        CloudStorm entT = this;
        CloudStorm entity = this;
        WeatherEntityConfig conf = getWeatherEntityConfigForStorm();//WeatherTypes.weatherEntTypes.get(curWeatherType);

        boolean forTornado = true;//entT != null;

        World world = WeatherUtilEntityOrParticle.getWorld(entity1);
        long worldTime = world.getTime();

        Entity ent = null;
        if (entity1 instanceof Entity) {
            ent = (Entity) entity1;
        }

        //ConfigTornado.Storm_Tornado_height;
        double radius = 10D;
        double scale = conf.tornadoWidthScale;
        double d1 = entity.pos.x - WeatherUtilEntityOrParticle.getPosX(entity1);
        double d2 = entity.pos.z - WeatherUtilEntityOrParticle.getPosZ(entity1);

        if (conf.type == WeatherEntityConfig.TYPE_SPOUT) {
            float range = 30F * (float) Math.sin((Math.toRadians(((worldTime * 0.5F) + (id * 50)) % 360)));
            float heightPercent = (float) (1F - ((WeatherUtilEntityOrParticle.getPosY(entity1) - posGround.y) / (pos.y - posGround.y)));
            float posOffsetX = (float) Math.sin((Math.toRadians(heightPercent * 360F)));
            float posOffsetZ = (float) -Math.cos((Math.toRadians(heightPercent * 360F)));
            d1 += range * posOffsetX;
            d2 += range * posOffsetZ;
        }

        float f = (float) ((Math.atan2(d2, d1) * 180D) / Math.PI) - 90F;
        float f1;

        for (f1 = f; f1 < -180F; f1 += 360F) {
        }

        for (; f1 >= 180F; f1 -= 360F) {
        }

        double distY;
        double distXZ = Math.sqrt(Math.abs(d1)) + Math.sqrt(Math.abs(d2));

        if (WeatherUtilEntityOrParticle.getPosY(entity1) - entity.pos.y < 0.0D) {
            distY = 1.0D;
        } else {
            distY = WeatherUtilEntityOrParticle.getPosY(entity1) - entity.pos.y;
        }

        if (distY > maxHeight) {
            distY = maxHeight;
        }

        float weight = WeatherUtilEntity.getWeight(entity1, forTornado);
        double grab = (10D / weight)/* / ((distY / maxHeight) * 1D)*/ * ((Math.abs((maxHeight - distY)) / maxHeight));
        float pullY = 0.0F;

        if (distXZ > 5D) {
            grab = grab * (radius / distXZ);
        }

        pullY += conf.tornadoLiftRate / (weight / 2F)/* * (Math.abs(radius - distXZ) / radius)*/;


        if (entity1 instanceof PlayerEntity) {
            double adjPull = 0.2D / ((weight * ((distXZ + 1D) / radius)));
            pullY += adjPull;
            double adjGrab = (10D * (((float) (((double) WeatherUtilEntity.playerInAirTime + 1D) / 400D))));

            if (adjGrab > 50) {
                adjGrab = 50D;
            }

            if (adjGrab < -50) {
                adjGrab = -50D;
            }

            grab = grab - adjGrab;

            if (WeatherUtilEntityOrParticle.getMotionY(entity1) > -0.8) {
                //System.out.println(entity1.motionY);
                ent.fallDistance = 0F;
            }
        } else if (entity1 instanceof LivingEntity) {
            double adjPull = 0.005D / ((weight * ((distXZ + 1D) / radius)));
            pullY += adjPull;
            int airTime = ent.getEntityData().getInteger("timeInAir");
            double adjGrab = (10D * (((float) (((double) (airTime) + 1D) / 400D))));

            if (adjGrab > 50) {
                adjGrab = 50D;
            }

            if (adjGrab < -50) {
                adjGrab = -50D;
            }

            grab = grab - adjGrab;

            if (ent.motionY > -1.5) {
                ent.fallDistance = 0F;
            }

            if (ent.motionY > 0.3F) ent.motionY = 0.3F;

            if (forTornado) ent.setOnGround(false);

            //its always raining during these, might as well extinguish them
            ent.extinguish();

            //System.out.println(adjPull);
        }

        grab += conf.relTornadoSize;

        double profileAngle = Math.max(1, (75D + grab - (10D * scale)));

        f1 = (float) ((double) f1 + profileAngle);

        if (entT.scale != 1F) f1 += 20 - (20 * entT.scale);

        float f3 = (float) Math.cos(-f1 * 0.01745329F - (float) Math.PI);
        float f4 = (float) Math.sin(-f1 * 0.01745329F - (float) Math.PI);
        float f5 = conf.tornadoPullRate * 1;

        if (entT.scale != 1F) f5 *= entT.scale * 1.2F;

        if (entity1 instanceof LivingEntity) {
            f5 /= (WeatherUtilEntity.getWeight(entity1, forTornado) * ((distXZ + 1D) / radius));
        }

        //if player and not spout
        if (entity1 instanceof PlayerEntity && conf.type != 0) {
            //System.out.println("grab: " + f5);
            if (ent.isOnGround()) {
                f5 *= 10.5F;
            } else {
                f5 *= 5F;
            }
            //if (entity1.world.rand.nextInt(2) == 0) entity1.onGround = false;
        } else if (entity1 instanceof LivingEntity && conf.type != 0) {
            f5 *= 1.5F;
        }

        if (conf.type == WeatherEntityConfig.TYPE_SPOUT && entity1 instanceof LivingEntity) {
            f5 *= 0.3F;
        }

        float moveX = f3 * f5;
        float moveZ = f4 * f5;
        //tornado strength changes
        float str;

        str = strength;

        if (conf.type == WeatherEntityConfig.TYPE_SPOUT && entity1 instanceof LivingEntity) {
            str *= 0.3F;
        }

        pullY *= str / 100F;

        if (entT.scale != 1F) {
            pullY *= entT.scale;
            pullY += 0.002F;
        }

        //prevent double+ pull on entities
        if (entity1 instanceof Entity) {
            long lastPullTime = ent.getEntityData().getLong("lastPullTime");
            if (lastPullTime == worldTime) {
                //System.out.println("preventing double pull");
                pullY = 0;
            }
            ent.getEntityData().setLong("lastPullTime", worldTime);
        }

        setVel(entity1, -moveX, pullY, moveZ);
    }

    public void setVel(Object entity, float f, float f1, float f2) {
        WeatherUtilEntityOrParticle.setMotionX(entity, WeatherUtilEntityOrParticle.getMotionX(entity) + f);
        WeatherUtilEntityOrParticle.setMotionY(entity, WeatherUtilEntityOrParticle.getMotionY(entity) + f1);
        WeatherUtilEntityOrParticle.setMotionZ(entity, WeatherUtilEntityOrParticle.getMotionZ(entity) + f2);

        if (entity instanceof SquidEntity) {
            Entity ent = (Entity) entity;
            ent.setPosition(ent.getX() + ent.motionX * 5F, ent.getY(), ent.getZ() + ent.motionZ * 5F);
        }
    }

    public EntityRotFX spawnFogParticle(double x, double y, double z, int parRenderOrder) {
        return spawnFogParticle(x, y, z, parRenderOrder, ParticleRegistry.cloud256);
    }

    public EntityRotFX spawnFogParticle(double x, double y, double z, int parRenderOrder, TextureAtlasSprite tex) {
        double speed = 0D;
        Random rand = new Random();
        EntityRotFX entityfx = particleBehaviorFog.spawnNewParticleIconFX(MinecraftClient.getInstance().world, tex, x, y, z, (rand.nextDouble() - rand.nextDouble()) * speed, 0.0D/*(rand.nextDouble() - rand.nextDouble()) * speed*/, (rand.nextDouble() - rand.nextDouble()) * speed, parRenderOrder);
        particleBehaviorFog.initParticle(entityfx);

        //potato
        //entityfx.setRBGColorF(1f, 1f, 1f);

        entityfx.setCanCollide(false);
        entityfx.callUpdatePB = false;

        if (levelCurIntensityStage == STATE_NORMAL) {
            entityfx.setMaxAge(300 + rand.nextInt(100));
        } else {
            entityfx.setMaxAge((size / 2) + rand.nextInt(100));
        }

        //pieces that move down with funnel need render order shift, also only for relevant storm formations
        if (entityfx.getEntityId() % 20 < 5 && isSpinning()) {
            entityfx.renderOrder = 1;

            entityfx.setMaxAge((size) + rand.nextInt(100));
        }

        //temp?
        if (ConfigCoroUtil.optimizedCloudRendering) {
            entityfx.setMaxAge(400);
        }

        float randFloat = (rand.nextFloat() * 0.6F);
        if (ConfigCoroUtil.optimizedCloudRendering) {
            randFloat = (rand.nextFloat() * 0.4F);
        }
        float baseBright = 0.7F;
        if (levelCurIntensityStage > STATE_NORMAL) {
            baseBright = 0.2F;
        } else if (attrib_precipitation) {
            baseBright = 0.2F;
        } else if (manager.vanillaRainActiveOnServer) {
            baseBright = 0.2F;
        } else {
            float adj = Math.min(1F, levelWater / levelWaterStartRaining) * 0.6F;
            baseBright -= adj;
        }

        float finalBright = Math.min(1F, baseBright + randFloat);

        entityfx.setRBGColorF(finalBright, finalBright, finalBright);

        //entityfx.setRBGColorF(1, 1, 1);

        ExtendedRenderer.rotEffRenderer.addEffect(entityfx);
        particleBehaviorFog.particles.add(entityfx);
        return entityfx;
    }

    @Override
    public void cleanup() {
        super.cleanup();
        if (tornadoHelper != null) {
            tornadoHelper.cleanup();
        }
        tornadoHelper = null;
    }

    @Override
    public void cleanupClient() {
        super.cleanupClient();
        listParticlesCloud.clear();
        listParticlesFunnel.clear();
        if (particleBehaviorFog != null && particleBehaviorFog.particles != null) particleBehaviorFog.particles.clear();
        particleBehaviorFog = null;
    }

    public float getTemperatureMCToWeatherSys(float parOrigVal) {
        //Weather.dbg("orig val: " + parOrigVal);
        //-0.7 to make 0 be the middle average
        parOrigVal -= 0.7;
        //multiply by 2 for an increased difference, for more to work with
        parOrigVal *= 2F;
        //Weather.dbg("final val: " + parOrigVal);
        return parOrigVal;
    }

    public void addWeatherEffectLightning(EntityLightningBolt parEnt, boolean custom) {
        //manager.getWorld().addWeatherEffect(parEnt);
        manager.getWorld().weatherEffects.add(parEnt);
        ((ServerWeatherManager) manager).syncLightningNew(parEnt, custom);
    }

    @Override
    public int getUpdateRateForNetwork() {
        if (levelCurIntensityStage >= CloudStorm.STATE_HIGHWIND) {
            return 2;
        } else {
            return super.getUpdateRateForNetwork();
        }
    }

    //notes moved to bottom\\

    //defaults are 0.5
	
	/*
	
	0.5  - ocean
	0.5  - river
	0.5  - sky (end)
	
	0.8  - plains
	2.0  - desert
	0.2  - extreme hills
	0.7  - forest
	0.05 - taiga
	0.8  - swampland
	2.0  - hell
	
	0.0  - frozen river
	0.0  - frozen ocean
	0.0  - ice plains
	0.0  - ice mountains
	0.2  - mushroom island
	0.9  - mushroom island shore
	
	0.8  - beach
	2.0  - desert hills
	0.7  - forest hills
	0.05 - taiga hills
	0.2  - extreme hills edge
	1.2  - jungle
	1.2  - jungle hills
	
	
	reorganized temperatures:
	
	0.0
	---
	frozen river
	frozen ocean
	ice plains
	ice mountains
	
	0.05
	---
	taiga
	taiga hills
	
	0.2
	---
	extreme hills
	extreme hills edge
	mushroom island
	
	0.5 (default val)
	---
	ocean
	river
	sky (end)
	
	0.7
	---
	forest
	forest hills
	
	0.8
	---
	plains
	swampland
	beach (we might not have to ignore beach, value seems sane)
	
	0.9
	---
	mushroom island shore
	
	1.2
	---
	jungle
	jungle hills
	
	2.0
	---
	desert
	desert hills
	hell
	
	*/
}
