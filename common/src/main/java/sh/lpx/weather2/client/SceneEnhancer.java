package sh.lpx.weather2.client;

import CoroUtil.config.ConfigCoroUtil;
import CoroUtil.util.ChunkCoordinatesBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFlame;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.util.vector.*;
import sh.lpx.weather2.ClientTickHandler;
import sh.lpx.weather2.Weather;
import sh.lpx.weather2.client.entity.particle.EntityWaterfallFX;
import sh.lpx.weather2.client.entity.particle.ParticleSandstorm;
import sh.lpx.weather2.client.foliage.FoliageEnhancerShader;
import sh.lpx.weather2.client.render.EventHandler;
import sh.lpx.weather2.client.render.particle.ParticleRegistry;
import sh.lpx.weather2.client.render.particle.behavior.ParticleBehaviorSandstorm;
import sh.lpx.weather2.client.render.particle.behavior.ParticleBehaviors;
import sh.lpx.weather2.client.render.particle.entity.EntityRotFX;
import sh.lpx.weather2.client.render.particle.entity.ParticleTexExtraRender;
import sh.lpx.weather2.client.render.particle.entity.ParticleTexFX;
import sh.lpx.weather2.client.render.particle.entity.ParticleTexLeafColor;
import sh.lpx.weather2.client.render.render.RotatingParticleManager;
import sh.lpx.weather2.client.render.shader.Matrix4fe;
import sh.lpx.weather2.client.tornado.TornadoFunnel;
import sh.lpx.weather2.config.ConfigMisc;
import sh.lpx.weather2.config.ConfigParticle;
import sh.lpx.weather2.config.ConfigStorm;
import sh.lpx.weather2.registry.WeatherSounds;
import sh.lpx.weather2.util.*;
import sh.lpx.weather2.weather.ClientWeatherManager;
import sh.lpx.weather2.weather.storm.CloudStorm;
import sh.lpx.weather2.weather.storm.SandStorm;
import sh.lpx.weather2.weather.wind.WindAffected;
import sh.lpx.weather2.weather.wind.WindManager;
import sh.lpx.weather2.weather.wind.WindReader;

import java.lang.reflect.Field;
import java.util.*;

public class SceneEnhancer implements Runnable {
    //this is for the thread we make
    public World lastWorldDetected = null;

    //used for acting on fire/smoke
    public static ParticleBehaviors pm;

    public static List<Particle> spawnQueueNormal = new ArrayList<>();
    public static List<Particle> spawnQueue = new ArrayList<>();

    public static long threadLastWorldTickTime;
    public static int lastTickFoundBlocks;
    public static long lastTickAmbient;
    public static long lastTickAmbientThreaded;

    //consider caching somehow without desyncing or overflowing
    //WE USE 0 TO MARK WATER, 1 TO MARK LEAVES
    public static ArrayList<ChunkCoordinatesBlock> soundLocations = new ArrayList<>();
    public static HashMap<ChunkCoordinatesBlock, Long> soundTimeLocations = new HashMap<>();

    public static Block SOUNDMARKER_WATER = Blocks.WATER;
    public static Block SOUNDMARKER_LEAVES = Blocks.LEAVES;

    public static float curPrecipStr = 0F;
    public static float curPrecipStrTarget = 0F;

    public static float curOvercastStr = 0F;
    public static float curOvercastStrTarget = 0F;

    //sandstorm fog state
    public static double distToStormThreshold = 100;
    public static double distToStorm = distToStormThreshold + 50;
    public static float stormFogRed = 0;
    public static float stormFogGreen = 0;
    public static float stormFogBlue = 0;
    public static float stormFogRedOrig = 0;
    public static float stormFogGreenOrig = 0;
    public static float stormFogBlueOrig = 0;
    public static float stormFogDensity = 0;
    public static float stormFogDensityOrig = 0;

    public static float stormFogStart = 0;
    public static float stormFogEnd = 0;
    public static float stormFogStartOrig = 0;
    public static float stormFogEndOrig = 0;

    public static float stormFogStartClouds = 0;
    public static float stormFogEndClouds = 0;
    public static float stormFogStartCloudsOrig = 0;
    public static float stormFogEndCloudsOrig = 0;

    public static boolean needFogState = true;

    public static float scaleIntensitySmooth = 0F;

    public static float adjustAmountTarget = 0F;
    public static float adjustAmountSmooth = 0F;

    public static float adjustAmountTargetPocketSandOverride = 0F;

    public static boolean isPlayerOutside = true;

    public static ParticleBehaviorSandstorm particleBehavior;

    public static ParticleTexExtraRender testParticle;

    public static EntityRotFX testParticle2;
    private int rainSoundCounter;

    private static final List<BlockPos> listPosRandom = new ArrayList<>();

    public static Matrix4fe matrix = new Matrix4fe();
    public static Matrix4fe matrix2 = new Matrix4fe();

    public static Vec3f vec = new Vec3f();

    public static TornadoFunnel funnel;

    public SceneEnhancer() {
        pm = new ParticleBehaviors(null);

        listPosRandom.clear();
        listPosRandom.add(new BlockPos(0, -1, 0));
        listPosRandom.add(new BlockPos(1, 0, 0));
        listPosRandom.add(new BlockPos(-1, 0, 0));
        listPosRandom.add(new BlockPos(0, 0, 1));
        listPosRandom.add(new BlockPos(0, 0, -1));
    }

    @Override
    public void run() {
        while (true) {
            try {
                tickClientThreaded();
                Thread.sleep(ConfigMisc.Thread_Particle_Process_Delay);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    //run from client side _mc_ thread
    public void tickClient() {
        if (!MinecraftClient.getInstance().isPaused() && !ConfigMisc.Client_PotatoPC_Mode) {
            tryParticleSpawning();
            tickRainRates();
            tickParticlePrecipitation();
            trySoundPlaying();

            MinecraftClient mc = MinecraftClient.getInstance();

            if (mc.world != null && lastWorldDetected != mc.world) {
                lastWorldDetected = mc.world;
                reset();
            }

            tryWind(mc.world);

            tickSandstorm();

            if (particleBehavior == null) {
                particleBehavior = new ParticleBehaviorSandstorm(null);
            }
            particleBehavior.tickUpdateList();

            if (ConfigCoroUtil.foliageShaders && EventHandler.queryUseOfShaders()) {
                if (!FoliageEnhancerShader.useThread) {
                    if (mc.world.getTime() % 40 == 0) {
                        FoliageEnhancerShader.tickClientThreaded();
                    }
                }

                if (mc.world.getTime() % 5 == 0) {
                    FoliageEnhancerShader.tickClientCloseToPlayer();
                }
            }
        }
    }

    //run from our newly created thread
    public void tickClientThreaded() {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.world != null && mc.player != null && WeatherUtilConfig.listDimensionsWindEffects.contains(mc.world.getDimension())) {
            profileSurroundings();
            tryAmbientSounds();
        }
    }

    public synchronized void trySoundPlaying() {
        try {
            if (lastTickAmbient < System.currentTimeMillis()) {
                lastTickAmbient = System.currentTimeMillis() + 500;

                MinecraftClient mc = MinecraftClient.getInstance();

                World worldRef = mc.world;
                PlayerEntity player = mc.player;

                int size = 32;
                int curX = (int) player.getX();
                int curY = (int) player.getY();
                int curZ = (int) player.getZ();

                Random rand = new Random();

                //trim out distant sound locations, also update last time played
                for (int i = 0; i < soundLocations.size(); i++) {
                    ChunkCoordinatesBlock cCor = soundLocations.get(i);

                    if (Math.sqrt(cCor.getDistanceSquared(curX, curY, curZ)) > size) {
                        soundLocations.remove(i--);
                        soundTimeLocations.remove(cCor);
                        //System.out.println("trim out soundlocation");
                    } else {

                        Block block = getBlock(worldRef, cCor.posX, cCor.posY, cCor.posZ);//Block.blocksList[id];

                        if (block == null || (block.getDefaultState().getMaterial() != Material.WATER && block.getDefaultState().getMaterial() != Material.LEAVES)) {
                            soundLocations.remove(i);
                            soundTimeLocations.remove(cCor);
                        } else {

                            long lastPlayTime = 0;


                            if (soundTimeLocations.containsKey(cCor)) {
                                lastPlayTime = soundTimeLocations.get(cCor);
                            }

                            //System.out.println(Math.sqrt(cCor.getDistanceSquared(curX, curY, curZ)));
                            if (lastPlayTime < System.currentTimeMillis()) {
                                if (cCor.block == SOUNDMARKER_WATER) {
                                    soundTimeLocations.put(cCor, System.currentTimeMillis() + 2500 + rand.nextInt(50));
                                    mc.world.playSound(cCor.toBlockPos(), WeatherSounds.WATERFALL, SoundCategory.AMBIENT, (float) ConfigMisc.volWaterfallScale, 0.75F + (rand.nextFloat() * 0.05F), false);
                                } else if (cCor.block == SOUNDMARKER_LEAVES) {
                                    float windSpeed = WindReader.getWindSpeed(mc.world, new Vec3d(cCor.posX, cCor.posY, cCor.posZ), WindReader.WindType.EVENT);
                                    if (windSpeed > 0.2F) {
                                        soundTimeLocations.put(cCor, System.currentTimeMillis() + 12000 + rand.nextInt(50));
                                        mc.world.playSound(cCor.toBlockPos(), WeatherSounds.WIND_CALM_FADE, SoundCategory.AMBIENT, (float) (windSpeed * 4F * ConfigMisc.volWindTreesScale), 0.70F + (rand.nextFloat() * 0.1F), false);
                                    } else {
                                        windSpeed = WindReader.getWindSpeed(mc.world, new Vec3d(cCor.posX, cCor.posY, cCor.posZ));
                                        //if (windSpeed > 0.3F) {
                                        if (mc.world.random.nextInt(15) == 0) {
                                            soundTimeLocations.put(cCor, System.currentTimeMillis() + 12000 + rand.nextInt(50));
                                            mc.world.playSound(cCor.toBlockPos(), WeatherSounds.WIND_CALM_FADE, SoundCategory.AMBIENT, (float) (windSpeed * 2F * ConfigMisc.volWindTreesScale), 0.70F + (rand.nextFloat() * 0.1F), false);
                                        }
                                        //System.out.println("play leaves sound at: " + cCor.posX + " - " + cCor.posY + " - " + cCor.posZ + " - windSpeed: " + windSpeed);
                                        //}
                                    }
                                }
                            }
                        }
                    }
                }
            }

            MinecraftClient mc = MinecraftClient.getInstance();

            float vanillaCutoff = 0.2F;
            float precipStrength = Math.abs(getRainStrengthAndControlVisuals(mc.player, ClientTickHandler.clientConfigData.overcastMode));

            //if less than vanilla sound playing amount
            if (precipStrength <= vanillaCutoff) {
                float volAmp = 0.2F + ((precipStrength / vanillaCutoff) * 0.8F);

                Random random = new Random();

                float f = mc.world.getRainGradient(1.0F);

                if (mc.options.graphicsMode == GraphicsMode.FAST) {
                    f /= 2.0F;
                }

                if (f != 0.0F) {
                    //random.setSeed((long)this.rendererUpdateCount * 312987231L);
                    Entity entity = mc.getCameraEntity();
                    World world = mc.world;
                    BlockPos blockpos = entity.getBlockPos();
                    double d0 = 0.0D;
                    double d1 = 0.0D;
                    double d2 = 0.0D;
                    int j = 0;
                    int k = 3;//(int) (400.0F * f * f);

                    if (mc.options.particles == ParticlesMode.DECREASED) {
                        k >>= 1;
                    } else if (mc.options.particles == ParticlesMode.MINIMAL) {
                        k = 0;
                    }

                    for (int l = 0; l < k; ++l) {
                        BlockPos blockpos1 = world.getPrecipitationHeight(blockpos.add(random.nextInt(10) - random.nextInt(10), 0, random.nextInt(10) - random.nextInt(10)));
                        Biome biome = world.getBiome(blockpos1);
                        BlockPos blockpos2 = blockpos1.down();
                        BlockState iblockstate = world.getBlockState(blockpos2);

                        if (blockpos1.getY() <= blockpos.getY() + 10 && blockpos1.getY() >= blockpos.getY() - 10 && biome.getPrecipitation() == Biome.Precipitation.RAIN && biome.getTemperature(blockpos1) >= 0.15F) {
                            double d3 = random.nextDouble();
                            double d4 = random.nextDouble();
                            AxisAlignedBB axisalignedbb = iblockstate.getBoundingBox(world, blockpos2);

                            if (iblockstate.getMaterial() != Material.LAVA && iblockstate.getBlock() != Blocks.MAGMA_BLOCK) {
                                if (iblockstate.getMaterial() != Material.AIR) {
                                    ++j;

                                    if (random.nextInt(j) == 0) {
                                        d0 = (double) blockpos2.getX() + d3;
                                        d1 = (double) ((float) blockpos2.getY() + 0.1F) + axisalignedbb.maxY - 1.0D;
                                        d2 = (double) blockpos2.getZ() + d4;
                                    }

                                    mc.world.addParticle(ParticleTypes.RAIN, (double) blockpos2.getX() + d3, (double) ((float) blockpos2.getY() + 0.1F) + axisalignedbb.maxY, (double) blockpos2.getZ() + d4, 0.0D, 0.0D, 0.0D, new int[0]);
                                }
                            } else {
                                mc.world.addParticle(ParticleTypes.SMOKE, (double) blockpos1.getX() + d3, (double) ((float) blockpos1.getY() + 0.1F) - axisalignedbb.minY, (double) blockpos1.getZ() + d4, 0.0D, 0.0D, 0.0D, new int[0]);
                            }
                        }
                    }

                    if (j > 0 && random.nextInt(3) < this.rainSoundCounter++) {
                        this.rainSoundCounter = 0;

                        if (d1 > (double) (blockpos.getY() + 1) && world.getPrecipitationHeight(blockpos).getY() > MathHelper.floor((float) blockpos.getY())) {
                            mc.world.playSound(d0, d1, d2, SoundEvents.WEATHER_RAIN_ABOVE, SoundCategory.WEATHER, 0.1F * volAmp, 0.5F, false);
                        } else {
                            mc.world.playSound(d0, d1, d2, SoundEvents.WEATHER_RAIN, SoundCategory.WEATHER, 0.2F * volAmp, 1.0F, false);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            System.out.println("Weather2: Error handling sound play queue: ");
            ex.printStackTrace();
        }
    }

    //Threaded function
    public static void tryAmbientSounds() {
        MinecraftClient mc = MinecraftClient.getInstance();

        World worldRef = mc.world;
        PlayerEntity player = mc.player;

        if (lastTickAmbientThreaded < System.currentTimeMillis()) {
            lastTickAmbientThreaded = System.currentTimeMillis() + 500;

            int size = 32;
            int hsize = size / 2;
            int curX = (int) player.getX();
            int curY = (int) player.getY();
            int curZ = (int) player.getZ();

            //soundLocations.clear();

            for (int xx = curX - hsize; xx < curX + hsize; xx++) {
                for (int yy = curY - (hsize / 2); yy < curY + hsize; yy++) {
                    for (int zz = curZ - hsize; zz < curZ + hsize; zz++) {
                        Block block = getBlock(worldRef, xx, yy, zz);

                        if (block != null) {
                            //Waterfall
                            if (ConfigParticle.Wind_Particle_waterfall && ((block.getDefaultState().getMaterial() == Material.WATER))) {
                                int meta = getBlockMetadata(worldRef, xx, yy, zz);
                                if ((meta & 8) != 0) {

                                    int bottomY = yy;
                                    int index = 0;

                                    //this scans to bottom till not water, kinda overkill? owell lets keep it, and also add rule if index > 4 (waterfall height of 4)
                                    while (yy - index > 0) {
                                        Block id2 = getBlock(worldRef, xx, yy - index, zz);
                                        if (id2 != null && !(id2.getDefaultState().getMaterial() == Material.WATER)) {
                                            break;
                                        }
                                        index++;
                                    }

                                    bottomY = yy - index + 1;

                                    //check if +10 from here is water with right meta too
                                    int meta2 = getBlockMetadata(worldRef, xx, bottomY + 10, zz);
                                    Block block2 = getBlock(worldRef, xx, bottomY + 10, zz);

                                    if (index >= 4 && (block2 != null && block2.getDefaultState().getMaterial() == Material.WATER && (meta2 & 8) != 0)) {
                                        boolean proxFail = false;
                                        for (int j = 0; j < soundLocations.size(); j++) {
                                            if (Math.sqrt(soundLocations.get(j).getDistanceSquared(xx, bottomY, zz)) < 5) {
                                                proxFail = true;
                                                break;
                                            }
                                        }

                                        if (!proxFail) {
                                            soundLocations.add(new ChunkCoordinatesBlock(xx, bottomY, zz, SOUNDMARKER_WATER));
                                            //System.out.println("add waterfall");
                                        }
                                    }
                                }
                            } else if (ConfigMisc.volWindTreesScale > 0 && ((block.getDefaultState().getMaterial() == Material.LEAVES))) {
                                boolean proxFail = false;
                                for (ChunkCoordinatesBlock soundLocation : soundLocations) {
                                    if (Math.sqrt(soundLocation.getDistanceSquared(xx, yy, zz)) < 15) {
                                        proxFail = true;
                                        break;
                                    }
                                }

                                if (!proxFail) {
                                    soundLocations.add(new ChunkCoordinatesBlock(xx, yy, zz, SOUNDMARKER_LEAVES));
                                    //System.out.println("add leaves sound location");
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void reset() {
        //reset particle data, discard dead ones as that was a bug from weather1

        lastWorldDetected.weatherEffects.clear();

        if (WeatherUtilParticle.fxLayers == null) {
            WeatherUtilParticle.getFXLayers();
        }
        //WeatherUtilSound.getSoundSystem();
    }

    public void tickParticlePrecipitation() {

        //if (true) return;

        if (ConfigParticle.Particle_RainSnow) {
            PlayerEntity entP = MinecraftClient.getInstance().player;

            if (entP.getY() >= CloudStorm.static_YPos_layer0) return;

            ClientWeatherManager weatherMan = ClientTickHandler.weatherManager;
            if (weatherMan == null) return;
            WindManager windMan = weatherMan.getWindManager();
            if (windMan == null) return;

            float curPrecipVal = getRainStrengthAndControlVisuals(entP);

            float maxPrecip = 0.5F;
			
			/*if (entP.world.getTotalWorldTime() % 20 == 0) {
				Weather.dbg("curRainStr: " + curRainStr);
			}*/

            //Weather.dbg("curPrecipVal: " + curPrecipVal * 100F);


            int precipitationHeight = entP.world.getPrecipitationHeight(new BlockPos(MathHelper.floor(entP.posX), 0, MathHelper.floor(entP.getZ()))).getY();

            Biome biomegenbase = entP.world.getBiome(new BlockPos(MathHelper.floor(entP.getX()), 0, MathHelper.floor(entP.getZ())));

            World world = entP.world;
            Random rand = entP.world.random;

            //System.out.println("ClientTickEvent time: " + world.getTotalWorldTime());

            double particleAmp = 1F;
            if (RotatingParticleManager.useShaders && ConfigCoroUtil.particleShaders) {
                particleAmp = ConfigMisc.shaderParticleRateAmplifier;
            }

            if (funnel == null) {
                funnel = new TornadoFunnel();
                funnel.pos = new Vec3d(entP.getX(), entP.getY(), entP.getZ());
            }

            //funnel.tickGame();

            //check rules same way vanilla texture precip does
            if (biomegenbase != null && (biomegenbase.getPrecipitation() != Biome.Precipitation.NONE)) {
                //biomegenbase.getFloatTemperature(new BlockPos(MathHelper.floor(entP.posX), MathHelper.floor(entP.posY), MathHelper.floor(entP.posZ)));
                float temperature = WeatherUtilCompatibility.getAdjustedTemperature(world, biomegenbase, entP.getPos());

                //now absolute it for ez math
                curPrecipVal = Math.min(maxPrecip, Math.abs(curPrecipVal));

                curPrecipVal *= 1F;

                if (curPrecipVal > 0) {
                    //particleAmp = 1;

                    int spawnCount;
                    int spawnNeed = (int) (curPrecipVal * 40F * ConfigParticle.Precipitation_Particle_effect_rate * particleAmp);
                    int safetyCutout = 100;

                    int extraRenderCount = 15;

                    //attempt to fix the cluttering issue more noticable when barely anything spawning
                    if (curPrecipVal < 0.1 && ConfigParticle.Precipitation_Particle_effect_rate > 0) {
                        //swap rates
                        int oldVal = extraRenderCount;
                        extraRenderCount = spawnNeed;
                        spawnNeed = oldVal;
                    }

                    //rain
                    if (entP.world.getBiomeProvider().getTemperatureAtHeight(temperature, precipitationHeight) >= 0.15F) {

                        //Weather.dbg("precip: " + curPrecipVal);

                        spawnCount = 0;
                        int spawnAreaSize = 20;

                        if (spawnNeed > 0) {
                            for (int i = 0; i < safetyCutout; i++) {
                                BlockPos pos = new BlockPos(
                                        entP.getX() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2),
                                        entP.getY() - 5 + rand.nextInt(25),
                                        entP.getZ() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2));

                                //EntityRenderer.addRainParticles doesnt actually use isRainingAt,
                                //switching to match what that method does to improve consistancy and tough as nails compat
                                if (canPrecipitateAt(world, pos)/*world.isRainingAt(pos)*/) {
                                    ParticleTexExtraRender rain = new ParticleTexExtraRender(entP.world,
                                            pos.getX(),
                                            pos.getY(),
                                            pos.getZ(),
                                            0D, 0D, 0D, ParticleRegistry.rain_white);
                                    rain.setKillWhenUnderTopmostBlock(true);
                                    rain.setCanCollide(false);
                                    rain.killWhenUnderCameraAtLeast = 5;
                                    rain.setTicksFadeOutMaxOnDeath(5);
                                    rain.setDontRenderUnderTopmostBlock(true);
                                    rain.setExtraParticlesBaseAmount(extraRenderCount);
                                    rain.fastLight = true;
                                    rain.setSlantParticleToWind(true);
                                    rain.windWeight = 1F;

                                    if (!RotatingParticleManager.useShaders || !ConfigCoroUtil.particleShaders) {
                                        //old slanty rain way
                                        rain.setFacePlayer(true);
                                        rain.setSlantParticleToWind(true);
                                    } else {
                                        //new slanty rain way
                                        rain.setFacePlayer(false);
                                        rain.extraYRotation = rain.getWorld().random.nextInt(360) - 180F;
                                    }

                                    //rain.setFacePlayer(true);
                                    rain.setScale(2F);
                                    rain.isTransparent = true;
                                    rain.setGravity(2.5F);
                                    //rain.isTransparent = true;
                                    rain.setMaxAge(50);
                                    //opted to leave the popin for rain, its not as bad as snow, and using fade in causes less rain visual overall
                                    rain.setTicksFadeInMax(5);
                                    rain.setAlphaF(0);
                                    rain.rotationYaw = rain.getWorld().random.nextInt(360) - 180F;
                                    rain.setMotionY(-0.5D/*-5D - (entP.world.rand.nextInt(5) * -1D)*/);
                                    rain.spawnAsWeatherEffect();
                                    ClientTickHandler.weatherManager.addWeatheredParticle(rain);

                                    spawnCount++;
                                    if (spawnCount >= spawnNeed) {
                                        break;
                                    }
                                }
                            }
                        }

                        boolean groundSplash = ConfigParticle.Particle_Rain_GroundSplash;
                        boolean downfall = ConfigParticle.Particle_Rain_DownfallSheet;

                        //TODO: make ground splash and downfall use spawnNeed var style design

                        spawnAreaSize = 40;
                        //ground splash
                        if (groundSplash && curPrecipVal > 0.15) {
                            for (int i = 0; i < 30F * curPrecipVal * ConfigParticle.Precipitation_Particle_effect_rate * particleAmp * 4F; i++) {
                                BlockPos pos = new BlockPos(
                                        entP.getX() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2),
                                        entP.getY() - 5 + rand.nextInt(15),
                                        entP.getZ() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2));


                                //get the block on the topmost ground
                                pos = world.getPrecipitationHeight(pos).down()/*.add(0, 1, 0)*/;

                                BlockState state = world.getBlockState(pos);
                                AxisAlignedBB axisalignedbb = state.getBoundingBox(world, pos);

                                if (pos.getSquaredDistance(MathHelper.floor(entP.getX()), MathHelper.floor(entP.getY()), MathHelper.floor(entP.getZ()), false) > spawnAreaSize / 2)
                                    continue;

                                //block above topmost ground
                                if (canPrecipitateAt(world, pos.up())/*world.isRainingAt(pos)*/) {
                                    ParticleTexFX rain = new ParticleTexFX(entP.world,
                                            pos.getX() + rand.nextFloat(),
                                            pos.getY() + 0.01D + axisalignedbb.maxY,
                                            pos.getZ() + rand.nextFloat(),
                                            0D, 0D, 0D, ParticleRegistry.cloud256_6);
                                    rain.setKillWhenUnderTopmostBlock(true);
                                    rain.setCanCollide(false);
                                    rain.killWhenUnderCameraAtLeast = 5;

                                    boolean upward = rand.nextBoolean();

                                    rain.windWeight = 20F;
                                    rain.setFacePlayer(upward);
                                    //SHADER COMPARE TEST
                                    //rain.setFacePlayer(false);

                                    rain.setScale(3F + (rand.nextFloat() * 3F));
                                    rain.setMaxAge(15);
                                    rain.setGravity(-0.0F);
                                    //opted to leave the popin for rain, its not as bad as snow, and using fade in causes less rain visual overall
                                    rain.setTicksFadeInMax(0);
                                    rain.setAlphaF(0);
                                    rain.setTicksFadeOutMax(4);
                                    rain.renderOrder = 2;

                                    rain.rotationYaw = rain.getWorld().rand.nextInt(360) - 180F;
                                    rain.rotationPitch = 90;
                                    rain.setMotionY(0D);
                                    rain.setMotionX((rand.nextFloat() - 0.5F) * 0.01F);
                                    rain.setMotionZ((rand.nextFloat() - 0.5F) * 0.01F);
                                    rain.spawnAsWeatherEffect();
                                    ClientTickHandler.weatherManager.addWeatheredParticle(rain);
                                }
                            }
                        }

                        //if (true) return;

                        spawnAreaSize = 20;
                        //downfall - at just above 0.3 cause rainstorms lock at 0.3 but flicker a bit above and below
                        if (downfall && curPrecipVal > 0.32) {
                            int scanAheadRange;
                            //quick is outside check, prevent them spawning right near ground
                            //and especially right above the roof so they have enough space to fade out
                            //results in not seeing them through roofs
                            if (entP.world.canBlockSeeSky(entP.getPos())) {
                                scanAheadRange = 3;
                            } else {
                                scanAheadRange = 10;
                            }

                            for (int i = 0; i < 2F * curPrecipVal * ConfigParticle.Precipitation_Particle_effect_rate; i++) {
                                BlockPos pos = new BlockPos(
                                        entP.getX() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2),
                                        entP.getY() + 5 + rand.nextInt(15),
                                        entP.getZ() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2));

                                if (entP.squaredDistanceTo(pos) < 10D * 10D) continue;

                                //pos = world.getPrecipitationHeight(pos).add(0, 1, 0);

                                if (canPrecipitateAt(world, pos.up(-scanAheadRange))/*world.isRainingAt(pos)*/) {
                                    ParticleTexExtraRender rain = new ParticleTexExtraRender(entP.world,
                                            pos.getX() + rand.nextFloat(),
                                            pos.getY() - 1 + 0.01D,
                                            pos.getZ() + rand.nextFloat(),
                                            0D, 0D, 0D, ParticleRegistry.downfall3);
                                    rain.setCanCollide(false);
                                    rain.killWhenUnderCameraAtLeast = 5;
                                    rain.setKillWhenUnderTopmostBlock(true);
                                    rain.setKillWhenUnderTopmostBlock_ScanAheadRange(scanAheadRange);
                                    rain.setTicksFadeOutMaxOnDeath(10);

                                    rain.noExtraParticles = true;

                                    rain.windWeight = 8F;
                                    rain.setFacePlayer(true);
                                    //SHADER COMPARE TEST
                                    rain.setFacePlayer(false);
                                    rain.facePlayerYaw = true;

                                    rain.setScale(90F + (rand.nextFloat() * 3F));
                                    //rain.setScale(25F);
                                    rain.setMaxAge(60);
                                    rain.setGravity(0.35F);
                                    //opted to leave the popin for rain, its not as bad as snow, and using fade in causes less rain visual overall
                                    rain.setTicksFadeInMax(20);
                                    rain.setAlphaF(0);
                                    rain.setTicksFadeOutMax(20);

                                    rain.rotationYaw = rain.getWorld().rand.nextInt(360) - 180F;
                                    //SHADER COMPARE TEST
                                    rain.rotationPitch = 0;
                                    rain.setMotionY(-0.3D);
                                    rain.setMotionX((rand.nextFloat() - 0.5F) * 0.01F);
                                    rain.setMotionZ((rand.nextFloat() - 0.5F) * 0.01F);
                                    rain.spawnAsWeatherEffect();
                                    ClientTickHandler.weatherManager.addWeatheredParticle(rain);
                                }
                            }
                        }
                        //snow
                    } else {
                        //Weather.dbg("rate: " + curPrecipVal * 5F * ConfigMisc.Particle_Precipitation_effect_rate);

                        spawnCount = 0;
                        //less for snow, since it falls slower so more is on screen longer
                        spawnNeed = (int) (curPrecipVal * 40F * ConfigParticle.Precipitation_Particle_effect_rate * particleAmp);

                        int spawnAreaSize = 50;

                        if (spawnNeed > 0) {
                            for (int i = 0; i < safetyCutout/*curPrecipVal * 20F * ConfigParticle.Precipitation_Particle_effect_rate*/; i++) {
                                BlockPos pos = new BlockPos(
                                        entP.getX() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2),
                                        entP.getY() - 5 + rand.nextInt(25),
                                        entP.getZ() + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2));

                                if (canPrecipitateAt(world, pos)) {
                                    ParticleTexExtraRender snow = new ParticleTexExtraRender(entP.world, pos.getX(), pos.getY(), pos.getZ(),
                                            0D, 0D, 0D, ParticleRegistry.snow);

                                    snow.setCanCollide(false);
                                    snow.setKillWhenUnderTopmostBlock(true);
                                    snow.setTicksFadeOutMaxOnDeath(5);
                                    snow.setDontRenderUnderTopmostBlock(true);
                                    snow.setExtraParticlesBaseAmount(10);
                                    snow.killWhenFarFromCameraAtLeast = 20;

                                    snow.setMotionY(-0.1D);
                                    snow.setScale(1.3F);
                                    snow.setGravity(0.1F);
                                    snow.windWeight = 0.2F;
                                    snow.setMaxAge(40);
                                    snow.setFacePlayer(false);
                                    snow.setTicksFadeInMax(5);
                                    snow.setAlphaF(0);
                                    snow.setTicksFadeOutMax(5);
                                    snow.rotationYaw = snow.getWorld().random.nextInt(360) - 180F;
                                    snow.spawnAsWeatherEffect();
                                    ClientTickHandler.weatherManager.addWeatheredParticle(snow);

                                    spawnCount++;
                                    if (spawnCount >= spawnNeed) {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static boolean canPrecipitateAt(World world, BlockPos strikePosition) {
        return world.getPrecipitationHeight(strikePosition).getY() <= strikePosition.getY();
    }

    public static float getRainStrengthAndControlVisuals(PlayerEntity entP) {
        return getRainStrengthAndControlVisuals(entP, false);
    }

    /**
     * Returns value between -1 to 1
     * -1 is full on snow
     * 1 is full on rain
     * 0 is no precipitation
     * <p>
     * also controls the client side raining and thundering values for vanilla
     */
    public static float getRainStrengthAndControlVisuals(PlayerEntity entP, boolean forOvercast) {
        MinecraftClient mc = MinecraftClient.getInstance();

        double maxStormDist = 512 / 4 * 3;
        Vec3d plPos = new Vec3d(entP.getX(), CloudStorm.static_YPos_layer0, entP.getZ());
        CloudStorm storm;

        ClientTickHandler.checkClientWeather();

        storm = ClientTickHandler.weatherManager.getClosestCloudStorm(plPos, maxStormDist, CloudStorm.STATE_FORMING, true);

        boolean closeEnough = false;
        double stormDist = 9999;
        float tempAdj = 1F;

        float sizeToUse = 0;

        float overcastModeMinPrecip;
        //overcastModeMinPrecip = 0.16F;
        overcastModeMinPrecip = (float) ConfigStorm.Storm_Rain_Overcast_Amount;

        //evaluate if storms size is big enough to be over player
        if (storm != null) {
            sizeToUse = storm.size;
            //extend overcast effect, using x2 for now since we cant cancel sound and ground particles, originally was 4x, then 3x, change to that for 1.7 if lex made change
            if (forOvercast) {
                sizeToUse *= 1F;
            }

            stormDist = storm.pos.distanceTo(plPos);
            //System.out.println("storm dist: " + stormDist);
            if (sizeToUse > stormDist) {
                closeEnough = true;
            }
        }

        if (closeEnough) {
            double stormIntensity = (sizeToUse - stormDist) / sizeToUse;

            tempAdj = storm.levelTemperature > 0 ? 1F : -1F;

            //limit plain rain clouds to light intensity
            if (storm.levelCurIntensityStage == CloudStorm.STATE_NORMAL) {
                if (stormIntensity > 0.3) stormIntensity = 0.3;
            }

            if (ConfigStorm.Storm_NoRainVisual) {
                stormIntensity = 0;
            }

            if (stormIntensity < overcastModeMinPrecip) {
                stormIntensity = overcastModeMinPrecip;
            }

            //System.out.println("intensity: " + stormIntensity);
            mc.world.getLevelProperties().setRaining(true);
            mc.world.getLevelProperties().setThundering(true);
            if (forOvercast) {
                curOvercastStrTarget = (float) stormIntensity;
            } else {
                curPrecipStrTarget = (float) stormIntensity;
            }
            //mc.world.thunderingStrength = (float) stormIntensity;
        } else {
            if (!ClientTickHandler.clientConfigData.overcastMode) {
                mc.world.getLevelProperties().setRaining(false);
                mc.world.getLevelProperties().setThundering(false);

                if (forOvercast) {
                    curOvercastStrTarget = 0;
                } else {
                    curPrecipStrTarget = 0;
                }
            } else {
                if (ClientTickHandler.weatherManager.vanillaRainActiveOnServer) {
                    mc.world.getLevelProperties().setRaining(true);
                    mc.world.getLevelProperties().setThundering(true);

                    if (forOvercast) {
                        curOvercastStrTarget = overcastModeMinPrecip;
                    } else {
                        curPrecipStrTarget = overcastModeMinPrecip;
                    }
                } else {
                    if (forOvercast) {
                        curOvercastStrTarget = 0;
                    } else {
                        curPrecipStrTarget = 0;
                    }
                }
            }
        }

        if (forOvercast) {
            if (curOvercastStr < 0.001 && curOvercastStr > -0.001F) {
                return 0;
            } else {
                return curOvercastStr * tempAdj;
            }
        } else {
            if (curPrecipStr < 0.001 && curPrecipStr > -0.001F) {
                return 0;
            } else {
                return curPrecipStr * tempAdj;
            }
        }
    }

    public static void tickRainRates() {
        float rateChange = 0.0005F;

        if (curOvercastStr > curOvercastStrTarget) {
            curOvercastStr -= rateChange;
        } else if (curOvercastStr < curOvercastStrTarget) {
            curOvercastStr += rateChange;
        }

        if (curPrecipStr > curPrecipStrTarget) {
            curPrecipStr -= rateChange;
        } else if (curPrecipStr < curPrecipStrTarget) {
            curPrecipStr += rateChange;
        }
    }

    public synchronized void tryParticleSpawning() {
        try {
            for (Particle ent : spawnQueue) {
                if (ent != null/* && ent.world != null*/) {

                    if (ent instanceof EntityRotFX) {
                        ((EntityRotFX) ent).spawnAsWeatherEffect();
                    }
                    ClientTickHandler.weatherManager.addWeatheredParticle(ent);
                }
            }
            for (Particle ent : spawnQueueNormal) {
                if (ent != null/* && ent.world != null*/) {
                    MinecraftClient.getInstance().effectRenderer.addEffect(ent);
                }
            }
        } catch (Exception ex) {
            Weather.LOGGER.error("Weather2: Error handling particle spawn queue: ");
            ex.printStackTrace();
        }

        spawnQueue.clear();
        spawnQueueNormal.clear();
    }

    public void profileSurroundings() {
        //tryClouds();

        Minecraft mc = FMLClientHandler.instance().getClient();
        World worldRef = lastWorldDetected;
        EntityPlayer player = FMLClientHandler.instance().getClient().player;
        ClientWeatherManager manager = ClientTickHandler.weatherManager;

        if (worldRef == null || player == null || manager == null || manager.windManager == null) {
            try {
                Thread.sleep(1000L);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return;
        }

        if (threadLastWorldTickTime == worldRef.getTotalWorldTime()) {
            return;
        }

        threadLastWorldTickTime = worldRef.getTotalWorldTime();

        Random rand = new Random();

        //mining a tree causes leaves to fall
        int size = 40;
        int hsize = size / 2;
        int curX = (int) player.posX;
        int curY = (int) player.posY;
        int curZ = (int) player.posZ;
        //if (true) return;

        float windStr = manager.windManager.getWindSpeedForPriority();//(weatherMan.wind.strength <= 1F ? weatherMan.wind.strength : 1F);

        /*if (mc.objectMouseOver != null && mc.objectMouseOver.getBlockPos() != null) {
        	Block id = mc.world.getBlockState(new BlockPos(mc.objectMouseOver.getBlockPos().getX(), mc.objectMouseOver.getBlockPos().getY(), mc.objectMouseOver.getBlockPos().getZ())).getBlock();
        	//System.out.println(mc.world.getBlockStateId(mc.objectMouseOver.blockX,mc.objectMouseOver.blockY,mc.objectMouseOver.blockZ));
        	if (CoroUtilBlock.isAir(id) && id.getMaterial() == Material.wood) {
        		float var5 = 0;

        		var5 = (Float)OldUtil.getPrivateValueSRGMCP(PlayerControllerMP.class, (PlayerControllerMP)mc.playerController, OldUtil.refl_curBlockDamageMP_obf, OldUtil.refl_curBlockDamageMP_mcp);

                if (var5 > 0) {
                	//weather2 disabled for now
                	//shakeTrees(8);
                }
        	}
        }*/

        //FoliageEnhancerShader.tickThreaded();


        if ((!ConfigParticle.Wind_Particle_leafs && !ConfigParticle.Wind_Particle_waterfall)/* || weatherMan.wind.strength < 0.10*/) {
            return;
        }

        //Wind requiring code goes below
        int spawnRate = (int) (30 / (windStr + 0.001));


        float lastBlockCount = lastTickFoundBlocks;

        float particleCreationRate = (float) ConfigParticle.Wind_Particle_effect_rate;

        //TEST OVERRIDE
        //uh = (lastBlockCount / 30) + 1;
        float maxScaleSample = 15000;
        if (lastBlockCount > maxScaleSample) lastBlockCount = maxScaleSample - 1;
        float scaleRate = (maxScaleSample - lastBlockCount) / maxScaleSample;

        spawnRate = (int) ((spawnRate / (scaleRate + 0.001F)) / (particleCreationRate + 0.001F));

        int BlockCountRate = (int) (((300 / scaleRate + 0.001F)) / (particleCreationRate + 0.001F));

        spawnRate *= (mc.gameSettings.particleSetting + 1);
        BlockCountRate *= (mc.gameSettings.particleSetting + 1);

        //since reducing threaded ticking to 200ms sleep, 1/4 rate, must decrease rand size
        spawnRate /= 2;

        //performance fix
        if (spawnRate < 40) {
            spawnRate = 40;
        }

        //performance fix
        if (BlockCountRate < 80) BlockCountRate = 80;
        //patch for block counts over 15000
        if (BlockCountRate > 5000) BlockCountRate = 5000;

        //TEMP!!!
        //uh = 10;

        //System.out.println("lastTickFoundBlocks: " + lastTickFoundBlocks + " - rand size: " + uh + " - " + BlockCountRate);

        lastTickFoundBlocks = 0;

        //Wind_Particle_waterfall = true;
        //Wind_Particle_leafs = true;
        //debug = true;
        //if (true) return;

        double particleAmp = 1F;
        if (RotatingParticleManager.useShaders && ConfigCoroUtil.particleShaders) {
            particleAmp = ConfigMisc.shaderParticleRateAmplifier * 2D;
            //ConfigCoroAI.optimizedCloudRendering = true;
        } else {
            //ConfigCoroAI.optimizedCloudRendering = false;
        }

        spawnRate = (int) ((double) spawnRate / particleAmp);

        //if (debug) System.out.println("windStr: " + windStr + " chance: " + uh);
        //Semi intensive area scanning code
        for (int xx = curX - hsize; xx < curX + hsize; xx++) {
            for (int yy = curY - (hsize / 2); yy < curY + hsize; yy++) {
                for (int zz = curZ - hsize; zz < curZ + hsize; zz++) {
                    //for (int i = 0; i < p_blocks_leaf.size(); i++)
                    //{
                    Block block = getBlock(worldRef, xx, yy, zz);

                    //if (block != null && block.getMaterial() == Material.leaves)

                    if (block != null && (block.getMaterial(block.getDefaultState()) == Material.LEAVES
                            || block.getMaterial(block.getDefaultState()) == Material.VINE ||
                            block.getMaterial(block.getDefaultState()) == Material.PLANTS)) {

                        lastTickFoundBlocks++;

                        if (worldRef.rand.nextInt(spawnRate) == 0) {
                            //bottom of tree check || air beside vine check
                            if (ConfigParticle.Wind_Particle_leafs) {

                                //far out enough to avoid having the AABB already inside the block letting it phase through more
                                //close in as much as we can to make it look like it came from the block
                                double relAdj = 0.70D;

                                BlockPos pos = getRandomWorkingPos(worldRef, new BlockPos(xx, yy, zz));
                                double xRand = 0;
                                double yRand = 0;
                                double zRand = 0;

                                if (pos != null) {

                                    //further limit the spawn position along the face side to prevent it clipping into perpendicular blocks
                                    float particleAABB = 0.1F;
                                    float particleAABBAndBuffer = particleAABB + 0.05F;
                                    float invert = 1F - (particleAABBAndBuffer * 2F);

                                    if (pos.getY() != 0) {
                                        xRand = particleAABBAndBuffer + (rand.nextDouble() - 0.5D) * invert;
                                        zRand = particleAABBAndBuffer + (rand.nextDouble() - 0.5D) * invert;
                                    } else if (pos.getX() != 0) {
                                        yRand = particleAABBAndBuffer + (rand.nextDouble() - 0.5D) * invert;
                                        zRand = particleAABBAndBuffer + (rand.nextDouble() - 0.5D) * invert;
                                    } else if (pos.getZ() != 0) {
                                        yRand = particleAABBAndBuffer + (rand.nextDouble() - 0.5D) * invert;
                                        xRand = particleAABBAndBuffer + (rand.nextDouble() - 0.5D) * invert;
                                    }

                                    EntityRotFX var31 = new ParticleTexLeafColor(worldRef, xx, yy, zz, 0D, 0D, 0D, ParticleRegistry.leaf);
                                    var31.setPosition(xx + 0.5D + (pos.getX() * relAdj) + xRand,
                                            yy + 0.5D + (pos.getY() * relAdj) + yRand,
                                            zz + 0.5D + (pos.getZ() * relAdj) + zRand);
                                    var31.setPrevPosX(var31.posX);
                                    var31.setPrevPosY(var31.posY);
                                    var31.setPrevPosZ(var31.posZ);
                                    var31.setMotionX(0);
                                    var31.setMotionY(0);
                                    var31.setMotionZ(0);
                                    var31.setSize(particleAABB, particleAABB);
                                    //ParticleBreakingTemp test = new ParticleBreakingTemp(worldRef, (double)xx, (double)yy - 0.5, (double)zz, ParticleRegistry.leaf);
                                    var31.setGravity(0.05F);
                                    var31.setCanCollide(true);
                                    var31.setKillOnCollide(false);
                                    var31.collisionSpeedDampen = false;
                                    var31.killWhenUnderCameraAtLeast = 20;
                                    var31.killWhenFarFromCameraAtLeast = 20;
                                    var31.isTransparent = false;
                                    //var31.setSize(1, 1);
                                    //var31.setKillWhenUnderTopmostBlock(true);

                                    //System.out.println("add particle");
                                    //Minecraft.getMinecraft().effectRenderer.addEffect(var31);
                                    //ExtendedRenderer.rotEffRenderer.addEffect(test);
                                    //ExtendedRenderer.rotEffRenderer.addEffect(var31);
                                    //WeatherUtil.setParticleGravity((EntityFX)var31, 0.1F);

                                    //worldRef.spawnParticle(EnumParticleTypes.FALLING_DUST, (double)xx, (double)yy, (double)zz, 0.0D, 0.0D, 0.0D, 0);

											/*for (int ii = 0; ii < 10; ii++)
											{
												applyWindForce(var31);
											}*/

                                    var31.rotationYaw = rand.nextInt(360);
                                    var31.rotationPitch = rand.nextInt(360);
                                    var31.updateQuaternion(null);

                                    spawnQueue.add(var31);
                                }

                            } else {
	                                    /*if (Wind_Particle_leafs)
	                                    {
	                                        //This is non leaves, as in wildgrass or wahtever is in the p_blocks_leaf list (no special rules)
	                                        EntityRotFX var31 = new EntityTexFX(worldRef, (double)xx, (double)yy + 0.5, (double)zz, 0D, 0D, 0D, 10D, 0, effLeafID);
	                                        c_CoroWeatherUtil.setParticleGravity((EntityFX)var31, 0.1F);
	                                        var31.rotationYaw = rand.nextInt(360);
	                                        //var31.spawnAsWeatherEffect();
	                                        spawnQueue.add(var31);
	                                        //mc.effectRenderer.addEffect(var31);
	                                        
	                                        //System.out.println("leaf spawn!");
	                                    }*/
                            }
                        }
                    } else if (ConfigParticle.Wind_Particle_waterfall && player.getDistance(xx, yy, zz) < 16 && (block != null && block.getMaterial(block.getDefaultState()) == Material.WATER)) {

                        int meta = getBlockMetadata(worldRef, xx, yy, zz);
                        if ((meta & 8) != 0) {
                            lastTickFoundBlocks += 70; //adding more to adjust for the rate 1 waterfall block spits out particles
                            int chance = (int) (1 + (((float) BlockCountRate) / 120F));

                            Block block2 = getBlock(worldRef, xx, yy - 1, zz);
                            int meta2 = getBlockMetadata(worldRef, xx, yy - 1, zz);
                            Block block3 = getBlock(worldRef, xx, yy + 10, zz);
                            //Block block2 = Block.blocksList[id2];
                            //Block block3 = Block.blocksList[id3];

                            //if ((block2 == null || block2.getMaterial() != Material.water) && (block3 != null && block3.getMaterial() == Material.water)) {
                            //chance /= 3;

                            //}
                            //System.out.println("woot! " + chance);
                            if ((((block2 == null || block2.getMaterial(block2.getDefaultState()) != Material.WATER) || (meta2 & 8) == 0) && (block3 != null && block3.getMaterial(block3.getDefaultState()) == Material.WATER)) || worldRef.rand.nextInt(chance) == 0) {

                                float range = 0.5F;

                                EntityRotFX waterP;
                                //if (rand.nextInt(10) == 0) {
                                //waterP = new EntityBubbleFX(worldRef, (double)xx + 0.5F + ((rand.nextFloat() * range) - (range/2)), (double)yy + 0.5F + ((rand.nextFloat() * range) - (range/2)), (double)zz + 0.5F + ((rand.nextFloat() * range) - (range/2)), 0D, 0D, 0D);
                                //} else {
                                waterP = new EntityWaterfallFX(worldRef, (double) xx + 0.5F + ((rand.nextFloat() * range) - (range / 2)), (double) yy + 0.5F + ((rand.nextFloat() * range) - (range / 2)), (double) zz + 0.5F + ((rand.nextFloat() * range) - (range / 2)), 0D, 0D, 0D, 6D, 2);
                                //}


                                if (((block2 == null || block2.getMaterial(block2.getDefaultState()) != Material.WATER) || (meta2 & 8) == 0) && (block3 != null && block3.getMaterial(block3.getDefaultState()) == Material.WATER)) {

                                    range = 2F;
                                    float speed = 0.2F;

                                    for (int i = 0; i < 10; i++) {
                                        if (worldRef.rand.nextInt(chance / 2) == 0) {
                                            waterP = new EntityWaterfallFX(worldRef,
                                                    (double) xx + 0.5F + ((rand.nextFloat() * range) - (range / 2)),
                                                    (double) yy + 0.7F + ((rand.nextFloat() * range) - (range / 2)),
                                                    (double) zz + 0.5F + ((rand.nextFloat() * range) - (range / 2)),
                                                    ((rand.nextFloat() * speed) - (speed / 2)),
                                                    ((rand.nextFloat() * speed) - (speed / 2)),
                                                    ((rand.nextFloat() * speed) - (speed / 2)),
                                                    2D, 3);
                                            //waterP.motionX = -1.5F;
                                            waterP.setMotionY(4.5F);
                                            //System.out.println("woot! " + chance);
                                            spawnQueueNormal.add(waterP);
                                        }

                                    }
                                } else {
                                    waterP = new EntityWaterfallFX(worldRef,
                                            (double) xx + 0.5F + ((rand.nextFloat() * range) - (range / 2)),
                                            (double) yy + 0.5F + ((rand.nextFloat() * range) - (range / 2)),
                                            (double) zz + 0.5F + ((rand.nextFloat() * range) - (range / 2)), 0D, 0D, 0D, 6D, 2);

                                    waterP.setMotionY(0.5F);

                                    spawnQueueNormal.add(waterP);
                                }


                                //waterP.rotationYaw = rand.nextInt(360);

                            }
                        }

                    } else if (ConfigParticle.Wind_Particle_fire && (block != null && block == Blocks.FIRE/*block.getMaterial() == Material.fire*/)) {
                        lastTickFoundBlocks++;

                        //
                        if (worldRef.rand.nextInt(Math.max(1, (spawnRate / 100))) == 0) {
                            double speed = 0.15D;
                            //System.out.println("xx:" + xx);
                            EntityRotFX entityfx = pm.spawnNewParticleIconFX(worldRef, ParticleRegistry.smoke, xx + rand.nextDouble(), yy + 0.2D + rand.nextDouble() * 0.2D, zz + rand.nextDouble(), (rand.nextDouble() - rand.nextDouble()) * speed, 0.03D, (rand.nextDouble() - rand.nextDouble()) * speed);//pm.spawnNewParticleWindFX(worldRef, ParticleRegistry.smoke, xx + rand.nextDouble(), yy + 0.2D + rand.nextDouble() * 0.2D, zz + rand.nextDouble(), (rand.nextDouble() - rand.nextDouble()) * speed, 0.03D, (rand.nextDouble() - rand.nextDouble()) * speed);
                            ParticleBehaviors.setParticleRandoms(entityfx, true, true);
                            ParticleBehaviors.setParticleFire(entityfx);
                            entityfx.setMaxAge(100 + rand.nextInt(300));
                            spawnQueueNormal.add(entityfx);
                            //entityfx.spawnAsWeatherEffect();
                            //pm.particles.add(entityfx);
                        }
                    }
                    //}


                }
            }
        }
    }

    /**
     * Returns the successful relative position
     *
     * @param world
     * @param posOrigin
     * @return
     */
    public static BlockPos getRandomWorkingPos(World world, BlockPos posOrigin) {
        Collections.shuffle(listPosRandom);
        for (BlockPos posRel : listPosRandom) {
            Block blockCheck = getBlock(world, posOrigin.add(posRel));

            if (blockCheck != null && blockCheck.getDefaultState().getMaterial() == Material.AIR) {
                return posRel;
            }
        }

        return null;
    }

    @SideOnly(Side.CLIENT)
    public static void tryWind(World world) {

        Minecraft mc = FMLClientHandler.instance().getClient();
        EntityPlayer player = mc.player;

        if (player == null) {
            return;
        }

        int dist = 60;

        List list = world.loadedEntityList;

        ClientWeatherManager weatherMan = ClientTickHandler.weatherManager;
        if (weatherMan == null) return;
        WindManager windMan = weatherMan.getWindManager();
        if (windMan == null) return;

        //Chunk Entities
        //we're not moving chunk entities with wind this way ....... i think, only weather events like spinning etc
        /*if (list != null)
        {
            for (int i = 0; i < list.size(); i++)
            {
                Entity entity1 = (Entity)list.get(i);

                if (canPushEntity(entity1) && !(entity1 instanceof EntityPlayer))
                {
                    applyWindForce(entity1, 1F);
                }
            }
        }*/

        Random rand = new Random();

        int handleCount = 0;

        if (world.getTotalWorldTime() % 60 == 0) {
            //System.out.println("weather particles: " + ClientTickHandler.weatherManager.listWeatherEffectedParticles.size());
        }

        //Weather Effects
        for (int i = 0; i < ClientTickHandler.weatherManager.listWeatherEffectedParticles.size(); i++) {

            Particle particle = ClientTickHandler.weatherManager.listWeatherEffectedParticles.get(i);

            if (!particle.isAlive()) {
                ClientTickHandler.weatherManager.listWeatherEffectedParticles.remove(i--);
                continue;
            }

            if (ClientTickHandler.weatherManager.windManager.getWindSpeedForPriority() >= 0.10) {

                handleCount++;

                if (particle instanceof EntityRotFX) {

                    EntityRotFX entity1 = (EntityRotFX) particle;

                    if (entity1 == null) {
                        continue;
                    }

                    if ((WeatherUtilBlock.getPrecipitationHeightSafe(world, new BlockPos(MathHelper.floor(entity1.getPosX()), 0, MathHelper.floor(entity1.getPosZ()))).getY() - 1 < (int) entity1.getPosY() + 1) || (entity1 instanceof ParticleTexFX)) {
                        if (entity1 instanceof WindAffected) {
                            if (((WindAffected) entity1).getParticleDecayExtra() > 0 && WeatherUtilParticle.getParticleAge(entity1) % 2 == 0) {
                                WeatherUtilParticle.setParticleAge(entity1, WeatherUtilParticle.getParticleAge(entity1) + ((WindAffected) entity1).getParticleDecayExtra());
                            }
                        } else if (WeatherUtilParticle.getParticleAge(entity1) % 2 == 0) {
                            WeatherUtilParticle.setParticleAge(entity1, WeatherUtilParticle.getParticleAge(entity1) + 1);
                        }

                        if ((entity1 instanceof ParticleTexFX) && ((ParticleTexFX) entity1).getParticleTexture() == ParticleRegistry.leaf/*((ParticleTexFX)entity1).getParticleTextureIndex() == WeatherUtilParticle.effLeafID*/) {
                            if (entity1.getMotionX() < 0.01F && entity1.getMotionZ() < 0.01F) {
                                entity1.setMotionY(entity1.getMotionY() + rand.nextDouble() * 0.02 * ((ParticleTexFX) entity1).particleGravity);
                            }

                            entity1.setMotionY(entity1.getMotionY() - 0.01F * ((ParticleTexFX) entity1).particleGravity);

                        }
                    }

                    windMan.applyWindForceNew(entity1, 1F / 20F, 0.5F);
                }
            }
        }

        //System.out.println("particles moved: " + handleCount);

        //WindManager windMan = ClientTickHandler.weatherManager.windMan;

        //Particles
        if (WeatherUtilParticle.fxLayers != null && windMan.getWindSpeedForPriority() >= 0.10) {
            //Built in particles
            for (int layer = 0; layer < WeatherUtilParticle.fxLayers.length; layer++) {
                for (int i = 0; i < WeatherUtilParticle.fxLayers[layer].length; i++) {
                    //for (int j = 0; j < WeatherUtilParticle.fxLayers[layer][i].size(); j++)
                    for (Particle entity1 : WeatherUtilParticle.fxLayers[layer][i]) {

                        //Particle entity1 = WeatherUtilParticle.fxLayers[layer][i].get(j);

                        if (ConfigParticle.Particle_VanillaAndWeatherOnly) {
                            String className = entity1.getClass().getName();
                            if (className.contains("net.minecraft.") || className.contains("weather2.")) {

                            } else {
                                continue;
                            }

                            //Weather.dbg("process: " + className);
                        }

                        if ((WeatherUtilBlock.getPrecipitationHeightSafe(world, new BlockPos(MathHelper.floor(WeatherUtilEntityOrParticle.getPosX(entity1)), 0, MathHelper.floor(WeatherUtilEntityOrParticle.getPosZ(entity1)))).getY() - 1 < (int) WeatherUtilEntityOrParticle.getPosY(entity1) + 1) || (entity1 instanceof ParticleTexFX)) {
                            if ((entity1 instanceof ParticleFlame)) {
                                if (windMan.getWindSpeedForPriority() >= 0.20) {
                                    entity1.particleAge += 1;
                                }
                            } else if (entity1 instanceof WindAffected) {
                                if (((WindAffected) entity1).getParticleDecayExtra() > 0 && WeatherUtilParticle.getParticleAge(entity1) % 2 == 0) {
                                    entity1.particleAge += ((WindAffected) entity1).getParticleDecayExtra();
                                }
                            }/*
	                        else if (WeatherUtilParticle.getParticleAge(entity1) % 2 == 0)
	                        {
								entity1.particleAge += 1;
	                        }*/

                            //rustle!
                            if (!(entity1 instanceof EntityWaterfallFX)) {
                                //EntityWaterfallFX ent = (EntityWaterfallFX) entity1;
		                        /*if (entity1.onGround)
		                        {
		                            //entity1.onGround = false;
		                            entity1.motionY += rand.nextDouble() * entity1.getMotionX();
		                        }*/

                                if (WeatherUtilEntityOrParticle.getMotionX(entity1) < 0.01F && WeatherUtilEntityOrParticle.getMotionZ(entity1) < 0.01F) {
                                    //ent.setMotionY(ent.getMotionY() + rand.nextDouble() * 0.02);
                                    WeatherUtilEntityOrParticle.setMotionY(entity1, WeatherUtilEntityOrParticle.getMotionY(entity1) + rand.nextDouble() * 0.02);
                                }
                            }

                            //entity1.motionX += rand.nextDouble() * 0.03;
                            //entity1.motionZ += rand.nextDouble() * 0.03;
                            //entity1.motionY += -0.04 + rand.nextDouble() * 0.04;
                            //if (canPushEntity(entity1)) {
                            //if (!(entity1 instanceof EntityFlameFX)) {
                            //applyWindForce(entity1);
                            windMan.applyWindForceNew(entity1, 1F / 20F, 0.5F);
                        }
                    }
                }
            }

            //My particle renderer - actually, instead add ones you need to weatherEffects (add blank renderer file)
            /*for (int layer = 0; layer < ExtendedRenderer.rotEffRenderer.layers; layer++)
            {
                for (int i = 0; i < ExtendedRenderer.rotEffRenderer.fxLayers[layer].size(); i++)
                {
                    Entity entity1 = (Entity)ExtendedRenderer.rotEffRenderer.fxLayers[layer].get(i);
                }
            }*/
        }

        //this was code to push player around if really windy, lets not do this anymore, who slides around in wind IRL?
        //maybe maybe if a highwind/hurricane state is active, adjust their ACTIVE movement to adhere to wind vector
        /*if (windMan.getWindSpeedForPriority() >= 0.70)
        {
            if (WeatherUtilEntity.canPushEntity(player))
            {
                applyWindForce(player, 0.2F);
            }
        }*/

        //NEEEEEEEED TO STOP WIND WHEN UNDERGROUND!
        //we kinda did, is it good enough?
        float volScaleFar = windMan.getWindSpeedForPriority() * 1F;

        if (windMan.getWindSpeedForPriority() <= 0.07F) {
            volScaleFar = 0F;
        }

        volScaleFar *= ConfigMisc.volWindScale;

        //Sound whistling noise
        //First, use volume to represent intensity, maybe get different sound samples for higher level winds as they sound different
        //Second, when facing towards wind, you're ears hear it tearing by you more, when turned 90 degrees you do not, simulate this

        //weather2: commented out to test before sound code goes in!!!!!!!!!!!!!!!!!!!!!
        /*tryPlaySound(WeatherUtil.snd_wind_far, 2, mc.player, volScaleFar);

        if (lastSoundPositionUpdate < System.currentTimeMillis())
        {
            lastSoundPositionUpdate = System.currentTimeMillis() + 100;

            if (soundID[2] > -1 && soundTimer[2] < System.currentTimeMillis())
            {
                setVolume(new StringBuilder().append("sound_" + soundID[2]).toString(), volScaleFar);
            }
        }*/
    }

    //Thread safe functions

    @SideOnly(Side.CLIENT)
    private static Block getBlock(World parWorld, BlockPos pos) {
        return getBlock(parWorld, pos.getX(), pos.getY(), pos.getZ());
    }

    @SideOnly(Side.CLIENT)
    private static Block getBlock(World parWorld, int x, int y, int z) {
        try {
            if (!parWorld.isBlockLoaded(new BlockPos(x, 0, z))) {
                return null;
            }

            return parWorld.getBlockState(new BlockPos(x, y, z)).getBlock();
        } catch (Exception ex) {
            return null;
        }
    }

    @SideOnly(Side.CLIENT)
    private static int getBlockMetadata(World parWorld, int x, int y, int z) {
        if (!parWorld.isBlockLoaded(new BlockPos(x, 0, z))) {
            return 0;
        }

        IBlockState state = parWorld.getBlockState(new BlockPos(x, y, z));
        return state.getBlock().getMetaFromState(state);
    }

    /**
     * Manages transitioning fog densities and color from current vanilla settings to our desired settings, and vice versa
     */
    public static void tickSandstorm() {

        if (adjustAmountTargetPocketSandOverride > 0) {
            adjustAmountTargetPocketSandOverride -= 0.01F;
        }

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.player;
        World world = mc.world;
        Vec3 posPlayer = new Vec3(mc.player.posX, 0/*mc.player.posY*/, mc.player.posZ);
        SandStorm sandstorm = ClientTickHandler.weatherManager.getClosestSandStormByIntensity(posPlayer);
        WindManager windMan = ClientTickHandler.weatherManager.getWindManager();
        float scaleIntensityTarget = 0F;
        if (sandstorm != null) {

            if (mc.world.getTotalWorldTime() % 40 == 0) {
                isPlayerOutside = WeatherUtilEntity.isEntityOutside(mc.player);
                //System.out.println("isPlayerOutside: " + isPlayerOutside);
            }


            scaleIntensityTarget = sandstorm.getSandstormScale();
    		/*Vec3 posNoY = new Vec3(sandstorm.pos);
    		posNoY.yCoord = mc.player.posY;
    		distToStorm = posPlayer.distanceTo(posNoY);*/

            List<Vec3> points = sandstorm.getSandstormAsShape();
    		
    		/*for (Vec3 point : points) {
    			System.out.println("point: " + point.toString());
    		}*/

            boolean inStorm = WeatherUtilPhysics.isInConvexShape(posPlayer, points);
            if (inStorm) {
                //System.out.println("in storm");
                distToStorm = 0;
            } else {

                distToStorm = WeatherUtilPhysics.getDistanceToShape(posPlayer, points);
                //System.out.println("near storm: " + distToStorm);
            }
        } else {
            distToStorm = distToStormThreshold + 10;
        }

        scaleIntensitySmooth = adjVal(scaleIntensitySmooth, scaleIntensityTarget, 0.01F);

        //temp off
        //distToStorm = distToStormThreshold;

        //distToStorm = 0;

        /**
         * new way to detect in sandstorm
         * 1. use in convex shape method
         * 2. if not, get closest point of shape to player, use that for distance
         * -- note, add extra points to compare against so its hard to enter between points and have it think player is 50 blocks away still
         *
         * - better idea, do 1., then if not, do point vs "minimum distance from a point to a line segment."
         */


        //square shape test
    	/*points.add(new Vec3(-100, 0, -100));
    	points.add(new Vec3(-100, 0, 100));
    	points.add(new Vec3(100, 0, 100));
    	points.add(new Vec3(100, 0, -100));*/

        //triangle test
    	/*points.add(new Vec3(-100, 0, -100));
    	points.add(new Vec3(-100, 0, 100));
    	points.add(new Vec3(100, 0, 0));*/
        //points.add(new Vec3(100, 0, -100));

        //tested works well


        float fogColorChangeRate = 0.01F;
        float fogDistChangeRate = 2F;
        float fogDensityChangeRate = 0.01F;

        //make it be full intensity once storm is halfway there
        adjustAmountTarget = 1F - (float) ((distToStorm) / distToStormThreshold);
        adjustAmountTarget *= 2F * scaleIntensitySmooth * (isPlayerOutside ? 1F : 0.5F);

        //use override if needed
        boolean pocketSandOverride = false;
        if (adjustAmountTarget < adjustAmountTargetPocketSandOverride) {
            adjustAmountTarget = adjustAmountTargetPocketSandOverride;
            pocketSandOverride = true;
        }

        if (adjustAmountTarget < 0F) adjustAmountTarget = 0F;
        if (adjustAmountTarget > 1F) adjustAmountTarget = 1F;

        //test debug sandstorm fog
        //adjustAmountTarget = 0.95F;
        //adjustAmountTarget = 0F;


        float sunBrightness = mc.world.getSunBrightness(1F) * 1F;
        /*mc.world.rainingStrength = 1F;
        mc.world.thunderingStrength = 1F;*/

        //since size var adjusts by 10 every x seconds, transition is rough, try to make it smooth but keeps up
        if (!pocketSandOverride) {
            if (adjustAmountSmooth < adjustAmountTarget) {
                adjustAmountSmooth = CoroUtilMisc.adjVal(adjustAmountSmooth, adjustAmountTarget, 0.003F);
            } else {
                adjustAmountSmooth = CoroUtilMisc.adjVal(adjustAmountSmooth, adjustAmountTarget, 0.002F);
            }
        } else {
            adjustAmountSmooth = CoroUtilMisc.adjVal(adjustAmountSmooth, adjustAmountTarget, 0.02F);
        }

        //testing
        //adjustAmountSmooth = 1F;

        //update coroutil particle renderer fog state
        EventHandler.sandstormFogAmount = adjustAmountSmooth;

        if (mc.world.getTotalWorldTime() % 20 == 0) {
            //System.out.println(adjustAmount + " - " + distToStorm);
            if (adjustAmountSmooth > 0) {
                //System.out.println("adjustAmountTarget: " + adjustAmountTarget);
                //System.out.println("adjustAmountSmooth: " + adjustAmountSmooth);
            }

            //System.out.println("wut: " + mc.world.getCelestialAngle(1));
            //System.out.println("wutF: " + mc.world.getSunBrightnessFactor(1F));
            //System.out.println("wut: " + mc.world.getSunBrightness(1F));
        }

        if (adjustAmountSmooth > 0/*distToStorm < distToStormThreshold*/) {

            //TODO: remove fetching of colors from this now that we dynamically track that
            if (needFogState) {
                //System.out.println("getting fog state");

                try {
                    Object fogState = ObfuscationReflectionHelper.getPrivateValue(GlStateManager.class, null, "field_179155_g");
                    Class<?> innerClass = Class.forName("net.minecraft.client.renderer.GlStateManager$FogState");
                    Field fieldDensity = null;
                    Field fieldStart = null;
                    Field fieldEnd = null;
                    try {
                        fieldDensity = innerClass.getField("field_179048_c");
                        fieldDensity.setAccessible(true);
                        fieldStart = innerClass.getField("field_179045_d");
                        fieldStart.setAccessible(true);
                        fieldEnd = innerClass.getField("field_179046_e");
                        fieldEnd.setAccessible(true);
                    } catch (Exception e) {
                        //dev env mode
                        fieldDensity = innerClass.getField("density");
                        fieldDensity.setAccessible(true);
                        fieldStart = innerClass.getField("start");
                        fieldStart.setAccessible(true);
                        fieldEnd = innerClass.getField("end");
                        fieldEnd.setAccessible(true);
                    }
                    stormFogDensity = fieldDensity.getFloat(fogState);

                    stormFogStart = fieldStart.getFloat(fogState);
                    stormFogEnd = fieldEnd.getFloat(fogState);

                    stormFogStartClouds = 0;
                    stormFogEndClouds = 192;


                    stormFogStartOrig = stormFogStart;
                    stormFogEndOrig = stormFogEnd;
                    stormFogStartCloudsOrig = stormFogStartClouds;
                    stormFogEndCloudsOrig = stormFogEndClouds;

                    stormFogDensityOrig = stormFogDensity;

                } catch (Exception e) {
                    e.printStackTrace();
                }
                needFogState = false;
            }

            //new dynamic adjusting
            stormFogRed = stormFogRedOrig + (-(stormFogRedOrig - (0.7F * sunBrightness)) * adjustAmountSmooth);
            stormFogGreen = stormFogGreenOrig + (-(stormFogGreenOrig - (0.5F * sunBrightness)) * adjustAmountSmooth);
            stormFogBlue = stormFogBlueOrig + (-(stormFogBlueOrig - (0.25F * sunBrightness)) * adjustAmountSmooth);

            stormFogDensity = stormFogDensityOrig + (-(stormFogDensityOrig - 0.02F) * adjustAmountSmooth);

            stormFogStart = stormFogStartOrig + (-(stormFogStartOrig - 0F) * adjustAmountSmooth);
            stormFogEnd = stormFogEndOrig + (-(stormFogEndOrig - 7F) * adjustAmountSmooth);
            stormFogStartClouds = stormFogStartCloudsOrig + (-(stormFogStartCloudsOrig - 0F) * adjustAmountSmooth);
            stormFogEndClouds = stormFogEndCloudsOrig + (-(stormFogEndCloudsOrig - 20F) * adjustAmountSmooth);
        } else {
            if (!needFogState) {
                //System.out.println("resetting need for fog state");
            }
            needFogState = true;
        }

        //enhance the scene further with particles around player, check for sandstorm to account for pocket sand modifying adjustAmountTarget
        if (adjustAmountSmooth > 0.75F && sandstorm != null) {

            Vec3 windForce = windMan.getWindForce();

            Random rand = mc.world.rand;
            int spawnAreaSize = 80;

            double sandstormParticleRateDebris = ConfigParticle.Sandstorm_Particle_Debris_effect_rate;
            double sandstormParticleRateDust = ConfigParticle.Sandstorm_Particle_Dust_effect_rate;

            float adjustAmountSmooth75 = (adjustAmountSmooth * 8F) - 7F;

            //extra dust
            for (int i = 0; i < ((float) 30 * adjustAmountSmooth75 * sandstormParticleRateDust)/*adjustAmountSmooth * 20F * ConfigMisc.Particle_Precipitation_effect_rate*/; i++) {

                BlockPos pos = new BlockPos(
                        player.posX + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2),
                        player.posY - 2 + rand.nextInt(10),
                        player.posZ + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2));


                if (canPrecipitateAt(world, pos)) {
                    TextureAtlasSprite sprite = ParticleRegistry.cloud256;

                    ParticleSandstorm part = new ParticleSandstorm(world, pos.getX(),
                            pos.getY(),
                            pos.getZ(),
                            0, 0, 0, sprite);
                    particleBehavior.initParticle(part);

                    part.setMotionX(windForce.xCoord);
                    part.setMotionZ(windForce.zCoord);

                    part.setFacePlayer(false);
                    part.isTransparent = true;
                    part.rotationYaw = (float) rand.nextInt(360);
                    part.rotationPitch = (float) rand.nextInt(360);
                    part.setMaxAge(40);
                    part.setGravity(0.09F);
                    part.setAlphaF(0F);
                    float brightnessMulti = 1F - (rand.nextFloat() * 0.5F);
                    part.setRBGColorF(0.65F * brightnessMulti, 0.6F * brightnessMulti, 0.3F * brightnessMulti);
                    part.setScale(40);
                    part.aboveGroundHeight = 0.2D;

                    part.setKillOnCollide(true);

                    part.windWeight = 1F;

                    particleBehavior.particles.add(part);
                    ClientTickHandler.weatherManager.addWeatheredParticle(part);
                    part.spawnAsWeatherEffect();


                }
            }

            //tumbleweed
            for (int i = 0; i < ((float) 1 * adjustAmountSmooth75 * sandstormParticleRateDebris)/*adjustAmountSmooth * 20F * ConfigMisc.Particle_Precipitation_effect_rate*/; i++) {

                BlockPos pos = new BlockPos(
                        player.posX + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2),
                        player.posY - 2 + rand.nextInt(10),
                        player.posZ + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2));


                if (canPrecipitateAt(world, pos)) {
                    TextureAtlasSprite sprite = ParticleRegistry.tumbleweed;

                    ParticleSandstorm part = new ParticleSandstorm(world, pos.getX(),
                            pos.getY(),
                            pos.getZ(),
                            0, 0, 0, sprite);
                    particleBehavior.initParticle(part);

                    part.setMotionX(windForce.xCoord);
                    part.setMotionZ(windForce.zCoord);

                    part.setFacePlayer(true);
                    //part.spinFast = true;
                    part.isTransparent = true;
                    part.rotationYaw = (float) rand.nextInt(360);
                    part.rotationPitch = (float) rand.nextInt(360);
                    part.setMaxAge(80);
                    part.setGravity(0.3F);
                    part.setAlphaF(0F);
                    float brightnessMulti = 1F - (rand.nextFloat() * 0.2F);
                    //part.setRBGColorF(0.65F * brightnessMulti, 0.6F * brightnessMulti, 0.3F * brightnessMulti);
                    part.setRBGColorF(1F * brightnessMulti, 1F * brightnessMulti, 1F * brightnessMulti);
                    part.setScale(8);
                    part.aboveGroundHeight = 0.5D;
                    part.collisionSpeedDampen = false;
                    part.bounceSpeed = 0.03D;
                    part.bounceSpeedAhead = 0.03D;

                    part.setKillOnCollide(false);

                    part.windWeight = 1F;

                    particleBehavior.particles.add(part);
                    ClientTickHandler.weatherManager.addWeatheredParticle(part);
                    part.spawnAsWeatherEffect();


                }
            }

            //debris
            for (int i = 0; i < ((float) 8 * adjustAmountSmooth75 * sandstormParticleRateDebris)/*adjustAmountSmooth * 20F * ConfigMisc.Particle_Precipitation_effect_rate*/; i++) {
                BlockPos pos = new BlockPos(
                        player.posX + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2),
                        player.posY - 2 + rand.nextInt(10),
                        player.posZ + rand.nextInt(spawnAreaSize) - (spawnAreaSize / 2));


                if (canPrecipitateAt(world, pos)) {
                    TextureAtlasSprite sprite = null;
                    int tex = rand.nextInt(3);
                    if (tex == 0) {
                        sprite = ParticleRegistry.debris_1;
                    } else if (tex == 1) {
                        sprite = ParticleRegistry.debris_2;
                    } else if (tex == 2) {
                        sprite = ParticleRegistry.debris_3;
                    }

                    ParticleSandstorm part = new ParticleSandstorm(world, pos.getX(),
                            pos.getY(),
                            pos.getZ(),
                            0, 0, 0, sprite);
                    particleBehavior.initParticle(part);

                    part.setMotionX(windForce.xCoord);
                    part.setMotionZ(windForce.zCoord);

                    part.setFacePlayer(false);
                    part.spinFast = true;
                    part.isTransparent = true;
                    part.rotationYaw = (float) rand.nextInt(360);
                    part.rotationPitch = (float) rand.nextInt(360);

                    part.setMaxAge(80);
                    part.setGravity(0.3F);
                    part.setAlphaF(0F);
                    float brightnessMulti = 1F - (rand.nextFloat() * 0.5F);
                    //part.setRBGColorF(0.65F * brightnessMulti, 0.6F * brightnessMulti, 0.3F * brightnessMulti);
                    part.setRBGColorF(1F * brightnessMulti, 1F * brightnessMulti, 1F * brightnessMulti);
                    part.setScale(8);
                    part.aboveGroundHeight = 0.5D;
                    part.collisionSpeedDampen = false;
                    part.bounceSpeed = 0.03D;
                    part.bounceSpeedAhead = 0.03D;

                    part.setKillOnCollide(false);

                    part.windWeight = 1F;

                    particleBehavior.particles.add(part);
                    ClientTickHandler.weatherManager.addWeatheredParticle(part);
                    part.spawnAsWeatherEffect();


                }
            }
        }


        tickSandstormSound();
    }

    /**
     *
     */
    public static void tickSandstormSound() {
        /**
         * dist + storm intensity
         * 0F - 1F
         *
         * 0 = low
         * 0.33 = med
         * 0.66 = high
         *
         * static sound volume, keep at player
         */

        Minecraft mc = Minecraft.getMinecraft();
        if (adjustAmountSmooth > 0) {
            if (adjustAmountSmooth < 0.33F) {
                tryPlayPlayerLockedSound(WeatherUtilSound.snd_sandstorm_low, 5, mc.player, 1F);
            } else if (adjustAmountSmooth < 0.66F) {
                tryPlayPlayerLockedSound(WeatherUtilSound.snd_sandstorm_med, 4, mc.player, 1F);
            } else {
                tryPlayPlayerLockedSound(WeatherUtilSound.snd_sandstorm_high, 3, mc.player, 1F);
            }
        }
    }

    public static boolean tryPlayPlayerLockedSound(String[] sound, int arrIndex, Entity source, float vol) {
        Random rand = new Random();

        if (WeatherUtilSound.soundTimer[arrIndex] <= System.currentTimeMillis()) {

            String soundStr = sound[WeatherUtilSound.snd_rand[arrIndex]];

            WeatherUtilSound.playPlayerLockedSound(new Vec3(source.getPositionVector()), new StringBuilder().append("streaming." + soundStr).toString(), vol, 1.0F);

            int length = WeatherUtilSound.soundToLength.get(soundStr);
            //-500L, for blending
            WeatherUtilSound.soundTimer[arrIndex] = System.currentTimeMillis() + length - 500L;
            WeatherUtilSound.snd_rand[arrIndex] = rand.nextInt(sound.length);
        }

        return false;
    }

    public static boolean isFogOverridding() {
        Minecraft mc = Minecraft.getMinecraft();
        IBlockState iblockstate = ActiveRenderInfo.getBlockStateAtEntityViewpoint(mc.world, mc.getRenderViewEntity(), 1F);
        if (iblockstate.getMaterial().isLiquid()) return false;
        return adjustAmountSmooth > 0;
    }

    public static void renderWorldLast(RenderWorldLastEvent event) {

    }

    public static void renderTick(TickEvent.RenderTickEvent event) {

        if (ConfigMisc.Client_PotatoPC_Mode) return;

        if (event.phase == TickEvent.Phase.START) {
            Minecraft mc = FMLClientHandler.instance().getClient();
            EntityPlayer entP = mc.player;
            if (entP != null) {
                float curRainStr = SceneEnhancer.getRainStrengthAndControlVisuals(entP, true);
                curRainStr = Math.abs(curRainStr);
                mc.world.setRainStrength(curRainStr);
            }
        }
    }

    public static float adjVal(float source, float target, float adj) {
        if (source < target) {
            source += adj;
            //fix over adjust
            if (source > target) {
                source = target;
            }
        } else if (source > target) {
            source -= adj;
            //fix over adjust
            if (source < target) {
                source = target;
            }
        }
        return source;
    }
}
