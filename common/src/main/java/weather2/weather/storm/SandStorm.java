package weather2.weather.storm;

import CoroUtil.util.Vec3;
import extendedrenderer.particle.ParticleRegistry;
import extendedrenderer.particle.behavior.ParticleBehaviorSandstorm;
import extendedrenderer.particle.entity.EntityRotFX;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import weather2.CommonProxy;
import weather2.client.entity.particle.ParticleSandstorm;
import weather2.config.ConfigParticle;
import weather2.config.ConfigSand;
import weather2.util.CachedNbtCompound;
import weather2.util.WeatherUtil;
import weather2.util.WeatherUtilBlock;
import weather2.weather.WeatherManager;
import weather2.weather.wind.WindManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * spawns in sandy biomes
 * needs high wind event
 * starts small size grows up to something like 80 height
 * needs to sorda stay near sand, to be fed
 * where should position be? stay in sand biome? travel outside it?
 * - main position is moving, where the front of the storm is
 * - store original spawn position, spawn particles of increasing height from spawn to current pos
 * <p>
 * build up sand like snow?
 * usual crazy storm sounds
 * hurt plantlife leafyness
 * <p>
 * take sand and relocate it forward in direction storm is pushing, near center of where stormfront is
 *
 * @author Corosus
 */
public class SandStorm extends Storm {
    public int height = 0;

    public Vec3 posSpawn = new Vec3(0, 0, 0);

    public List<EntityRotFX> listParticlesCloud;

    public ParticleBehaviorSandstorm particleBehavior;

    public int age = 0;
    //public int maxAge = 20*20;

    public int sizePeak = 1;

    public int ageFadeout = 0;
    public int ageFadeoutMax = 20 * 60 * 5;

    //public boolean dying = false;
    public boolean isFrontGrowing = true;

    public Random rand = new Random();

    public SandStorm(WeatherManager parManager) {
        super(parManager);

        this.weatherObjectType = StormType.SAND;

        if (parManager.getWorld().isClient) {
            listParticlesCloud = new ArrayList<>();

        }
    }

    public void initSandstormSpawn(Vec3 pos) {
        this.pos = new Vec3(pos);

        size = 1;
        sizePeak = 1;
        maxSize = 100;

        World world = manager.getWorld();
        pos.yCoord = WeatherUtilBlock.getPrecipitationHeightSafe(world, new BlockPos(pos.xCoord, 0, pos.zCoord)).getY();

        posGround = new Vec3(pos);

        this.posSpawn = new Vec3(this.pos);
    }

    public float getSandstormScale() {
        if (isFrontGrowing) {
            return (float) size / (float) maxSize;
        } else {
            return 1F - ((float) ageFadeout / (float) ageFadeoutMax);
        }
    }

    public static boolean isDesert(Biome biome) {
        return isDesert(biome, false);
    }

    /**
     * prevent rivers from killing sandstorm if its just passing over from desert to more desert
     */
    public static boolean isDesert(Biome biome, boolean forSpawn) {
        return biome.getCategory() == Biome.Category.DESERT || (!forSpawn && biome.getCategory() == Biome.Category.RIVER);
    }

    /**
     * - size of storm determined by how long it was in desert
     * - front of storm dies down once it exits desert
     * - stops moving once fully dies down
     * <p>
     * - storm continues for minutes even after front has exited desert
     */
    public void tickProgressionAndMovement() {
        World world = manager.getWorld();
        WindManager windMan = manager.getWindManager();

        float angle = windMan.getWindAngleForClouds();
        float speedWind = windMan.getWindSpeedForClouds();

        /*
          Progression
         */

        if (!world.isClient) {
            age++;

            //boolean isGrowing = true;

            BlockPos posBlock = pos.toBlockPos();

            //only grow if in loaded area and in desert, also prevent it from growing again for some reason if it started dying already
            if (isFrontGrowing && world.canSetBlock(posBlock)) {
                Biome biomeIn = world.getBiome(posBlock);
                isFrontGrowing = isDesert(biomeIn);
            } else {
                isFrontGrowing = false;
            }

            int sizeAdjRate = 10;

            if (isFrontGrowing) {
                if (world.getTime() % sizeAdjRate == 0) {
                    if (size < maxSize) {
                        size++;
                        //System.out.println("size: " + size);
                    }
                }
            } else {
                if (world.getTime() % sizeAdjRate == 0) {
                    if (size > 0) {
                        size--;
                        //System.out.println("size: " + size);
                    }
                }

                //fadeout till death
                if (ageFadeout < ageFadeoutMax) {
                    ageFadeout++;
                } else {
                    //System.out.println("sandstorm died");
                    this.setDead();
                }
            }

            if (size > sizePeak) {
                sizePeak = size;
            }

            //keep high wind active incase it dies off during storm
            if (windMan.highWindTimer < 100) {
                windMan.highWindTimer = 100;
            }
        }

        /*
          Movement
         */

        //clouds move at 0.2 amp of actual wind speed

        double vecX = -Math.sin(Math.toRadians(angle));
        double vecZ = Math.cos(Math.toRadians(angle));
        double speed = speedWind * 0.3D;//0.2D;

        //prevent it from moving if its died down to nothing
        if (size > 0) {
            this.pos.xCoord += vecX * speed;
            this.pos.zCoord += vecZ * speed;
        }

        int yy = WeatherUtilBlock.getPrecipitationHeightSafe(world, new BlockPos(pos.xCoord, 0, pos.zCoord)).getY();

        this.pos.yCoord = yy + 1;
    }

    public void tickBlockSandBuildup() {
        World world = manager.getWorld();
        WindManager windMan = manager.getWindManager();

        float angle = windMan.getWindAngleForClouds();

        //keep it set to do a lot of work only occasionally, prevents chunk render update spam for client which kills fps
        int delay = ConfigSand.Sandstorm_Sand_Buildup_TickRate;
        int loop = (int) ((float) ConfigSand.Sandstorm_Sand_Buildup_LoopAmountBase * getSandstormScale());

        //sand block buildup
        if (!world.isClient) {
            if (world.getTime() % delay == 0) {
                for (int i = 0; i < loop; i++) {
                    //rate of placement based on storm intensity
                    if (rand.nextDouble() >= getSandstormScale()) continue;

                    Vec3 vecPos = getRandomPosInSandstorm();

                    int y = WeatherUtilBlock.getPrecipitationHeightSafe(world, new BlockPos(vecPos.xCoord, 0, vecPos.zCoord)).getY();
                    vecPos.yCoord = y;

                    //avoid unloaded areas
                    if (!world.canSetBlock(vecPos.toBlockPos())) continue;

                    Biome biomeIn = world.getBiome(vecPos.toBlockPos());

                    if (ConfigSand.Sandstorm_Sand_Buildup_AllowOutsideDesert || isDesert(biomeIn)) {
                        WeatherUtilBlock.fillAgainstWallSmoothly(world, vecPos, angle/* + angleRand*/, 15, 2, CommonProxy.blockSandLayer);
                    }
                }
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (manager == null) {
            System.out.println("WeatherManager is null for " + this + ", why!!!");
            return;
        }

        World world = manager.getWorld();

        if (world == null) {
            System.out.println("world is null for " + this + ", why!!!");
            return;
        }

        if (WeatherUtil.isPausedSideSafe(world)) return;

        tickProgressionAndMovement();

        int yy = WeatherUtilBlock.getPrecipitationHeightSafe(world, new BlockPos(pos.xCoord, 0, pos.zCoord)).getY();

        if (world.isClient) {
            tickClient();
        }

        //if (size >= 2) {
        if (getSandstormScale() > 0.2D) {
            tickBlockSandBuildup();
        }

        this.posGround.xCoord = pos.xCoord;
        this.posGround.yCoord = yy;
        this.posGround.zCoord = pos.zCoord;
    }

    public void tickClient() {
        MinecraftClient mc = MinecraftClient.getInstance();
        World world = manager.getWorld();
        WindManager windMan = manager.getWindManager();

        if (particleBehavior == null) {
            particleBehavior = new ParticleBehaviorSandstorm(pos);
        }

        //double scale = 10;
        double distBetweenParticles = 3;

        //double circScale = circ / distBetweenParticles;

        Random rand = mc.world.random;

        this.height = this.size / 4;
        int heightLayers = Math.max(1, this.height / (int) distBetweenParticles);

        double distFromSpawn = this.posSpawn.distanceTo(this.pos);

        double xVec = this.posSpawn.xCoord - this.pos.xCoord;
        double zVec = this.posSpawn.zCoord - this.pos.zCoord;

        double directionAngle = Math.atan2(zVec, xVec);

        double directionAngleDeg = Math.toDegrees(directionAngle);

        int spawnedThisTick = 0;

        /*
          stormfront wall
         */
        float sandstormScale = getSandstormScale();

        double sandstormParticleRateDust = ConfigParticle.Sandstorm_Particle_Dust_effect_rate;
        if (size > 0/*isFrontGrowing || sandstormScale > 0.5F*/) {
            for (int heightLayer = 0; heightLayer < heightLayers && spawnedThisTick < 500; heightLayer++) {
                //youd think this should be angle - 90 to angle + 90, but minecraft / bad math
                //for (double i = directionAngleDeg; i < directionAngleDeg + (180); i += degRate) {
                double i = directionAngleDeg + (rand.nextDouble() * 180D);
                if ((mc.world.getTime()) % 2 == 0) {

                    if (rand.nextDouble() >= sandstormParticleRateDust) continue;

                    double sizeSub = heightLayer * 2D;
                    double sizeDyn = size - sizeSub;
                    double inwardsAdj = rand.nextDouble() * 5D;//(sizeDyn * 0.75D);

                    double sizeRand = (sizeDyn - /*rand.nextDouble() * 30D*/ inwardsAdj/*30D*/)/* / (double)heightLayer*/;
                    double x = pos.xCoord + (-Math.sin(Math.toRadians(i)) * (sizeRand));
                    double z = pos.zCoord + (Math.cos(Math.toRadians(i)) * (sizeRand));
                    double y = pos.yCoord + (heightLayer * distBetweenParticles * 2);

                    TextureAtlasSprite sprite = ParticleRegistry.cloud256;
                    if (WeatherUtil.isAprilFoolsDay()) {
                        sprite = ParticleRegistry.chicken;
                    }

                    ParticleSandstorm part = new ParticleSandstorm(mc.world, x, y, z
                            , 0, 0, 0, sprite);
                    particleBehavior.initParticle(part);

                    part.angleToStorm = i;
                    part.distAdj = sizeRand;
                    part.heightLayer = heightLayer;
                    part.lockPosition = true;

                    part.setFacePlayer(false);
                    part.isTransparent = true;
                    part.rotationYaw = (float) i + rand.nextInt(20) - 10;//Math.toDegrees(Math.cos(Math.toRadians(i)) * 2D);
                    part.rotationPitch = 0;
                    part.setMaxAge(300);
                    part.setGravity(0.09F);
                    part.setAlphaF(1F);
                    float brightnessMulti = 1F - (rand.nextFloat() * 0.5F);
                    part.setRBGColorF(0.65F * brightnessMulti, 0.6F * brightnessMulti, 0.3F * brightnessMulti);
                    part.setScale(100);

                    //part.windWeight = 5F;

                    part.setKillOnCollide(true);

                    particleBehavior.particles.add(part);
                    part.spawnAsWeatherEffect();

                    spawnedThisTick++;
                }
                //}
            }
        }


        if (spawnedThisTick > 0) {
            //System.out.println("spawnedThisTickv1: " + spawnedThisTick);
            spawnedThisTick = 0;
        }

        //half of the angle (?)
        double spawnAngle = Math.atan2(this.sizePeak/*this.size*//* / 2D*/, distFromSpawn);

        //tweaking for visual due to it moving, etc
        spawnAngle *= 1.2D;

        double spawnDistInc = 10;

        double extraDistSpawnIntoWall = sizePeak / 2D;

        // Spawn particles between spawn pos and current pos, cone shaped
        if ((mc.world.getTime()) % 3 == 0) {

            //System.out.println(this.particleBehavior.particles.size());

            for (double spawnDistTick = 0; spawnDistTick < distFromSpawn + (extraDistSpawnIntoWall) && spawnedThisTick < 500; spawnDistTick += spawnDistInc) {
                //rate of spawn based on storm intensity
                if (rand.nextDouble() >= sandstormScale) continue;

                if (rand.nextDouble() >= sandstormParticleRateDust) continue;

                //add 1/4 PI for some reason, converting math to mc I guess
                double randAngle = directionAngle + (Math.PI / 2D) - (spawnAngle) + (rand.nextDouble() * spawnAngle * 2D);

                double randHeight = (spawnDistTick / distFromSpawn) * height * 1.2D * rand.nextDouble();

                //project out from spawn point, towards a point within acceptable angle
                double x = posSpawn.xCoord + (-Math.sin(/*Math.toRadians(*/randAngle/*)*/) * (spawnDistTick));
                double z = posSpawn.zCoord + (Math.cos(/*Math.toRadians(*/randAngle/*)*/) * (spawnDistTick));

                //attempt to widen start, might mess with spawn positions further towards front
                x += (rand.nextDouble() - rand.nextDouble()) * 30D;
                z += (rand.nextDouble() - rand.nextDouble()) * 30D;

                int yy = WeatherUtilBlock.getPrecipitationHeightSafe(world, new BlockPos(x, 0, z)).getY();
                double y = yy/*posSpawn.yCoord*/ + 2 + randHeight;

                TextureAtlasSprite sprite = ParticleRegistry.cloud256;
                if (WeatherUtil.isAprilFoolsDay()) {
                    sprite = ParticleRegistry.chicken;
                }

                ParticleSandstorm part = new ParticleSandstorm(mc.world, x, y, z
                        , 0, 0, 0, sprite);
                particleBehavior.initParticle(part);

                part.setFacePlayer(false);
                part.isTransparent = true;
                part.rotationYaw = (float) rand.nextInt(360);
                part.rotationPitch = (float) rand.nextInt(360);
                part.setMaxAge(100);
                part.setGravity(0.09F);
                part.setAlphaF(1F);
                float brightnessMulti = 1F - (rand.nextFloat() * 0.5F);
                part.setRBGColorF(0.65F * brightnessMulti, 0.6F * brightnessMulti, 0.3F * brightnessMulti);
                part.setScale(100);

                part.setKillOnCollide(true);

                part.windWeight = 1F;

                particleBehavior.particles.add(part);
                //ClientTickHandler.weatherManager.addWeatheredParticle(part);
                part.spawnAsWeatherEffect();

                spawnedThisTick++;
            }

            //System.out.println("age: " + age + " - SCALE: " + getSandstormScale());
        }

        float angle = windMan.getWindAngleForClouds();
        float speedWind = windMan.getWindSpeedForClouds();

        double vecX = -Math.sin(Math.toRadians(angle));
        double vecZ = Math.cos(Math.toRadians(angle));
        double speed = 0.8D;


        particleBehavior.coordSource = pos;
        particleBehavior.tickUpdateList();

        //System.out.println("client side size: " + size);

        // keep sandstorm front in position
        for (int i = 0; i < particleBehavior.particles.size(); i++) {
            ParticleSandstorm particle = (ParticleSandstorm) particleBehavior.particles.get(i);

            // lock to position while sandstorm is in first size using phase, otherwise just let them fly without lock
            if (particle.lockPosition) {
                if (size > 0) {
                    double x = pos.xCoord + (-Math.sin(Math.toRadians(particle.angleToStorm)) * (particle.distAdj));
                    double z = pos.zCoord + (Math.cos(Math.toRadians(particle.angleToStorm)) * (particle.distAdj));
                    double y = pos.yCoord + (particle.heightLayer * distBetweenParticles);

                    moveToPosition(particle, x, y, z, 0.01D);
                } else {
                    //should be same formula actual storm object uses for speed
                    particle.setMotionX((vecX * speedWind * 0.3F));
                    particle.setMotionZ((vecZ * speedWind * 0.3F));
                }
            } else {
                particle.setMotionX(/*particle.getMotionX() + */(vecX * speed));
                particle.setMotionZ(/*particle.getMotionZ() + */(vecZ * speed));
            }
            //windMan.applyWindForceNew(particle);
        }

        //System.out.println("spawn particles at: " + pos);
    }

    public Vec3 getRandomPosInSandstorm() {

        double extraDistSpawnIntoWall = sizePeak / 2D;
        double distFromSpawn = this.posSpawn.distanceTo(this.pos);

        double randDist = rand.nextDouble() * (distFromSpawn + extraDistSpawnIntoWall);

        double xVec = this.posSpawn.xCoord - this.pos.xCoord;
        double zVec = this.posSpawn.zCoord - this.pos.zCoord;

        double spawnAngle = Math.atan2(this.sizePeak, distFromSpawn);

        //tweaking for visual due to it moving, etc
        //spawnAngle *= 1.2D;

        double directionAngle = Math.atan2(zVec, xVec);

        double randAngle = directionAngle + (Math.PI / 2D) - (spawnAngle) + (rand.nextDouble() * spawnAngle * 2D);

        double x = posSpawn.xCoord + (-Math.sin(/*Math.toRadians(*/randAngle/*)*/) * (randDist));
        double z = posSpawn.zCoord + (Math.cos(/*Math.toRadians(*/randAngle/*)*/) * (randDist));

        return new Vec3(x, 0, z);
    }

    public List<Vec3> getSandstormAsShape() {
        List<Vec3> listPoints = new ArrayList<>();

        double extraDistSpawnIntoWall = sizePeak / 2D;
        double distFromSpawn = this.posSpawn.distanceTo(this.pos);

        //for triangle shape
        listPoints.add(new Vec3(this.posSpawn.xCoord, 0, this.posSpawn.zCoord));

        double xVec = this.posSpawn.xCoord - this.pos.xCoord;
        double zVec = this.posSpawn.zCoord - this.pos.zCoord;

        double spawnAngle = Math.atan2(this.sizePeak, distFromSpawn);

        double directionAngle = Math.atan2(zVec, xVec);

        double angleLeft = directionAngle + (Math.PI / 2D) - (spawnAngle);
        double angleRight = directionAngle + (Math.PI / 2D) - (spawnAngle) + (/*rand.nextDouble() * */spawnAngle * 2D);

        double xLeft = posSpawn.xCoord + (-Math.sin(/*Math.toRadians(*/angleLeft/*)*/) * (distFromSpawn + extraDistSpawnIntoWall));
        double zLeft = posSpawn.zCoord + (Math.cos(/*Math.toRadians(*/angleLeft/*)*/) * (distFromSpawn + extraDistSpawnIntoWall));

        double xRight = posSpawn.xCoord + (-Math.sin(/*Math.toRadians(*/angleRight/*)*/) * (distFromSpawn + extraDistSpawnIntoWall));
        double zRight = posSpawn.zCoord + (Math.cos(/*Math.toRadians(*/angleRight/*)*/) * (distFromSpawn + extraDistSpawnIntoWall));

        listPoints.add(new Vec3(xLeft, 0, zLeft));
        listPoints.add(new Vec3(xRight, 0, zRight));

        return listPoints;
    }

    public void moveToPosition(ParticleSandstorm particle, double x, double y, double z, double maxSpeed) {
        if (particle.getPosX() > x) {
            particle.setMotionX(particle.getMotionX() - maxSpeed);
        } else {
            particle.setMotionX(particle.getMotionX() + maxSpeed);
        }

        if (particle.getPosZ() > z) {
            particle.setMotionZ(particle.getMotionZ() + -maxSpeed);
        } else {
            particle.setMotionZ(particle.getMotionZ() + maxSpeed);
        }

        double distXZ = Math.sqrt((particle.getPosX() - x) * 2 + (particle.getPosZ() - z) * 2);
        if (distXZ < 5D) {
            particle.setMotionX(particle.getMotionX() * 0.8D);
            particle.setMotionZ(particle.getMotionZ() * 0.8D);
        }
    }

    @Override
    public int getUpdateRateForNetwork() {
        return 1;
    }

    @Override
    public void nbtSyncForClient() {
        super.nbtSyncForClient();

        CachedNbtCompound data = this.getNbtCache();

        data.putDouble("posSpawnX", posSpawn.xCoord);
        data.putDouble("posSpawnY", posSpawn.yCoord);
        data.putDouble("posSpawnZ", posSpawn.zCoord);

        data.setInt("ageFadeout", this.ageFadeout);
        data.setInt("ageFadeoutMax", this.ageFadeoutMax);

        data.setInt("sizePeak", sizePeak);
        data.setInt("age", age);

        data.putBoolean("isFrontGrowing", isFrontGrowing);
    }

    @Override
    public void nbtSyncFromServer() {
        super.nbtSyncFromServer();

        CachedNbtCompound parNBT = this.getNbtCache();

        posSpawn = new Vec3(parNBT.getDouble("posSpawnX"), parNBT.getDouble("posSpawnY"), parNBT.getDouble("posSpawnZ"));

        this.ageFadeout = parNBT.getInt("ageFadeout");
        this.ageFadeoutMax = parNBT.getInt("ageFadeoutMax");

        this.sizePeak = parNBT.getInt("sizePeak");
        this.age = parNBT.getInt("age");

        this.isFrontGrowing = parNBT.getBoolean("isFrontGrowing");
    }

    @Override
    public void readFromNBT() {
        super.readFromNBT();
        nbtSyncFromServer();

        CachedNbtCompound var1 = this.getNbtCache();

        motion = new Vec3(var1.getDouble("vecX"), var1.getDouble("vecY"), var1.getDouble("vecZ"));
    }

    @Override
    public void writeToNBT() {
        super.writeToNBT();
        nbtSyncForClient();

        CachedNbtCompound nbt = this.getNbtCache();

        nbt.putDouble("vecX", motion.xCoord);
        nbt.putDouble("vecY", motion.yCoord);
        nbt.putDouble("vecZ", motion.zCoord);

    }

    @Override
    public void cleanup() {
        super.cleanup();
    }

    @Override
    public void cleanupClient() {
        super.cleanupClient();
        listParticlesCloud.clear();
        if (particleBehavior != null && particleBehavior.particles != null) particleBehavior.particles.clear();
        particleBehavior = null;
    }
}
