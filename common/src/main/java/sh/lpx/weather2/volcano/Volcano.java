package sh.lpx.weather2.volcano;

import sh.lpx.weather2.Weather;
import sh.lpx.weather2.client.render.ExtendedRenderer;
import sh.lpx.weather2.client.render.particle.ParticleRegistry;
import sh.lpx.weather2.client.render.particle.behavior.ParticleBehaviors;
import sh.lpx.weather2.client.render.particle.entity.EntityRotFX;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.Material;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import sh.lpx.weather2.util.WeatherUtilBlock;
import sh.lpx.weather2.weather.WeatherManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Volcano {
    //used on both server and client side, mark things SideOnly where needed

    //management stuff
    public static long lastUsedID = 0; //ID starts from 0, set on nbt load, who would max this out for volcanos? surely this will never fail...
    public long ID;
    public WeatherManager manager;

    public List<EntityRotFX> listParticlesSmoke = new ArrayList<>();
    public ParticleBehaviors particleBehaviors;

    //basic info
    public static int staticYPos = 200;
    public Vec3d pos = new Vec3d(0, staticYPos, 0);

    //public boolean isGrowing = true;
    public int processRateDelay = 20; //make configurable
    public Block topBlockID = Blocks.AIR;
    public int startYPos = -1;
    public int curRadius = 5;
    public int curHeight = 3;

    //growth / progression info:
    public int state = 0;

    //state 0 = initial gen
    //state 1 = grow volcano to max
    //state 2 = leak lava up to top
    //state 3 = build pressure
    //state 4 = explode top and PARTICLES
    //state 5 = cool off and harden top, make sure ring forms
    //state 6 = non state, repeat 2-6

    //state 1
    public int size = 0;
    public int maxSize = 20; //make configurable

    //state 2
    public int step = 0;

    //state 3
    public int stepsBuildupMax = 20;

    //state 4
    public int ticksToErupt = 20 * 30;//20*60*2;
    public int ticksPerformedErupt = 0;

    //state 4
    public int ticksToCooldown = 20 * 30;//20*60*5; //this should ideally be larger than maxSize so it can full cooldown (does 1 y layer per 1 processRateDelay)
    public int ticksPerformedCooldown = 0;

    public Volcano(WeatherManager parManager) {
        manager = parManager;
    }

    public void initFirstTime() {
        ID = Volcano.lastUsedID++;
    }

    public void initPost() {
    }

    public void resetEruption() {
        step = 0;
        state = 2;
        ticksPerformedErupt = 0;
        ticksPerformedCooldown = 0;
    }

    public void readFromNBT(NbtCompound data) {
        ID = data.getLong("ID");

        pos = new Vec3d(data.getInt("posX"), data.getInt("posY"), data.getInt("posZ"));
        size = data.getInt("size");
        maxSize = data.getInt("maxSize");

        state = data.getInt("state");
        //isGrowing = data.getBoolean("isGrowing");

        curRadius = data.getInt("curRadius");
        curHeight = data.getInt("curHeight");
        topBlockID = Registry.BLOCK.get(new Identifier(data.getString("topBlockID")));
        //topBlockID = data.getInteger("topBlockID");
        startYPos = data.getInt("startYPos");

        step = data.getInt("step");
        ticksPerformedErupt = data.getInt("ticksPerformedErupt");
        ticksPerformedCooldown = data.getInt("ticksPerformedCooldown");
    }

    public void writeToNBT(NbtCompound data) {
        data.putLong("ID", ID);

        data.putInt("posX", (int) pos.x);
        data.putInt("posY", (int) pos.y);
        data.putInt("posZ", (int) pos.z);

        data.putInt("size", size);
        data.putInt("maxSize", maxSize);

        data.putInt("state", state);
        //data.setBoolean("isGrowing", isGrowing);

        data.putInt("curRadius", curRadius);
        data.putInt("curHeight", curHeight);
        data.putString("topBlockID", Registry.BLOCK.getId(topBlockID).toString());
        //data.setInteger("topBlockID", topBlockID);
        data.putInt("startYPos", startYPos);

        data.putInt("step", step);
        data.putInt("ticksPerformedErupt", ticksPerformedErupt);
        data.putInt("ticksPerformedCooldown", ticksPerformedCooldown);
    }

    //receiver method
    public void nbtSyncFromServer(NbtCompound parNBT) {
        ID = parNBT.getLong("ID");
        Weather.LOGGER.debug("VolcanoObject " + ID + " receiving sync");

        pos = new Vec3d(parNBT.getInt("posX"), parNBT.getInt("posY"), parNBT.getInt("posZ"));
        size = parNBT.getInt("size");
        maxSize = parNBT.getInt("maxSize");

        state = parNBT.getInt("state");
    }

    //compose nbt data for packet (and serialization in future)
    public NbtCompound nbtSyncForClient() {
        NbtCompound data = new NbtCompound();

        data.putInt("posX", (int) pos.x);
        data.putInt("posY", (int) pos.y);
        data.putInt("posZ", (int) pos.z);

        data.putLong("ID", ID);
        data.putInt("size", size);
        data.putInt("maxSize", maxSize);

        data.putInt("state", state);

        return data;
    }

    public void tick() {
        processRateDelay = 10;

        World world = manager.getWorld();
        if (world.isClient) {
            if (!MinecraftClient.getInstance().isPaused()) {
                tickClient();
            }
        } else {
            float res = 5;

            if (state == 0) {
                //quantify and ground level coords
                pos.x = Math.floor(pos.x);
                pos.z = Math.floor(pos.z);

                pos.y = WeatherUtilBlock.getPrecipitationHeightSafe(world, new BlockPos((int) pos.x, 0, (int) pos.z)).getY();
                startYPos = (int) pos.y;

                BlockState statez = world.getBlockState(new BlockPos(MathHelper.floor(pos.x), MathHelper.floor(pos.y - 1), MathHelper.floor(pos.z)));
                if (statez.getMaterial() == Material.AIR || !statez.getMaterial().isSolid()) {
                    topBlockID = world.getBlockState(new BlockPos((int) pos.x, (int) pos.y - 1, (int) pos.z)).getBlock();
                }

                for (int yy = startYPos + curHeight; yy > 2; yy--) {
                    for (int dist = 0; dist <= curRadius; dist++) {
                        double vecX = dist;
                        double vecZ = 0;

                        if (yy > startYPos) {
                            vecX = dist + (startYPos - yy);
                        }

                        for (double angle = 0; angle <= 360; angle += res) {
                            Vec3d vec = new Vec3d(vecX, 0, vecZ);
                            vec.rotateY((float) angle);

                            int posX = (int) Math.floor((pos.x) + vec.x + 0.5);
                            int posZ = (int) Math.floor((pos.z) + vec.z + 0.5);

                            Block blockID = Blocks.OBSIDIAN;

                            if (yy >= startYPos) {
                                blockID = topBlockID;
                            } else if (dist < curRadius) {
                                blockID = Blocks.LAVA;
                            }

                            //skip derpy top layer
                            if (yy != startYPos + curHeight) {
                                BlockState idScan = world.getBlockState(new BlockPos(posX, yy, posZ));
                                if (idScan.getMaterial() == Material.AIR || idScan.getMaterial() == Material.WATER) {
                                    world.setBlockState(new BlockPos(posX, yy, posZ), blockID.getDefaultState());
                                }
                            }
                        }
                    }
                }

                state++;

                System.out.println("initial volcano created");
            } else if (state == 1) {
                if (this.manager.getWorld().getTime() % processRateDelay == 0) {
                    //if (isGrowing) {
                    size++;
                    curHeight++;
                    curRadius++;
                    if (size >= maxSize) {
                        //isGrowing = false;
                        state++;
                    }

                    res = 1;

                    for (int yy = 0; yy <= curHeight; yy++) {
                        //System.out.println("rad: " + radiusForLayer);

                        double vecX = Math.max(0, curRadius - yy - 2);
                        double vecZ = 0;

                        for (double angle = 0; angle <= 360; angle += res) {
                            Vec3d vec = new Vec3d(vecX, 0, vecZ);
                            vec.rotateY((float) angle);

                            int posX = (int) Math.floor((pos.x) + vec.x + 0.5);
                            int posZ = (int) Math.floor((pos.z) + vec.z + 0.5);

                            Block blockID = topBlockID;

                            Random rand = new Random();

                            //some random chance of placing a block here
                            if (rand.nextInt(4) == 0) {

                                //skip top layers
                                if (yy != curHeight) {
                                    if (world.getBlockState(new BlockPos(posX, startYPos + yy, posZ)).getMaterial() == Material.AIR) {
                                        world.setBlockState(new BlockPos(posX, startYPos + yy, posZ), blockID.getDefaultState());
                                    }
                                }

                                //handle growth under expanded area
                                int underY = startYPos + yy - 1;
                                BlockState underBlockState = world.getBlockState(new BlockPos(posX, underY, posZ));
                                while ((underBlockState.getMaterial() == Material.AIR || underBlockState.getMaterial() == Material.WATER) && underY > 1) {
                                    world.setBlockState(new BlockPos(posX, underY, posZ), Blocks.DIRT.getDefaultState());
                                    underY--;
                                    underBlockState = world.getBlockState(new BlockPos(posX, underY, posZ));
                                }
                            }
                        }
                    }
                    //}

                    System.out.println("cur size: " + size + " - " + curHeight + " - " + curRadius);
                }
            } else if (state == 2) {
                //build up pressure, somehow, just a timer and increasing particle effects?
                //occasional rumble and shake
                //buildup lava through center, once it hits top, thats when actual pressure builds

                //temp remove self - this might have a bug, make sure it works properly
                if (this.manager.getWorld().getTime() % processRateDelay == 0) {
                    if (step <= maxSize) {
                        int posX = (int) Math.floor((pos.x));
                        int posY = (int) Math.floor((startYPos)) + step;
                        int posZ = (int) Math.floor((pos.z));

                        world.setBlockState(new BlockPos(posX, posY, posZ), Blocks.LAVA.getDefaultState());
                        world.setBlockState(new BlockPos(posX + 1, posY, posZ), Blocks.LAVA.getDefaultState());
                        world.setBlockState(new BlockPos(posX - 1, posY, posZ), Blocks.LAVA.getDefaultState());
                        world.setBlockState(new BlockPos(posX, posY, posZ + 1), Blocks.LAVA.getDefaultState());
                        world.setBlockState(new BlockPos(posX, posY, posZ - 1), Blocks.LAVA.getDefaultState());
                    } else {
                        step = 0;
                        state++;
                    }

                    step++;
                }
            } else if (state == 3) {
                //slowly increase smoking particles here
                if (this.manager.getWorld().getTime() % processRateDelay == 0) {
                    step++;
                    if (step > stepsBuildupMax) {
                        step = 0;
                        state++;
                    }
                }
            } else if (state == 4) {
                if (ticksPerformedErupt == 0) {
                    Weather.LOGGER.debug("volcano " + ID + " is erupting");

                    for (int i = 0; i < 3; i++) {
                        int posX = (int) Math.floor((pos.x));
                        int posY = (int) Math.floor((startYPos)) + maxSize + i;
                        int posZ = (int) Math.floor((pos.z));

                        Block blockID = Blocks.LAVA;

                        world.setBlockState(new BlockPos(posX, posY, posZ), blockID.getDefaultState());
                        world.setBlockState(new BlockPos(posX + 1, posY, posZ), blockID.getDefaultState());
                        world.setBlockState(new BlockPos(posX - 1, posY, posZ), blockID.getDefaultState());
                        world.setBlockState(new BlockPos(posX, posY, posZ + 1), blockID.getDefaultState());
                        world.setBlockState(new BlockPos(posX, posY, posZ - 1), blockID.getDefaultState());
                        world.setBlockState(new BlockPos(posX + 1, posY, posZ + 1), blockID.getDefaultState());
                        world.setBlockState(new BlockPos(posX - 1, posY, posZ - 1), blockID.getDefaultState());
                        world.setBlockState(new BlockPos(posX - 1, posY, posZ + 1), blockID.getDefaultState());
                        world.setBlockState(new BlockPos(posX + 1, posY, posZ - 1), blockID.getDefaultState());
                    }
                }

                ticksPerformedErupt++;
                if (ticksPerformedErupt > ticksToErupt) {
                    state++;
                }
            } else if (state == 5) {
                if (ticksPerformedCooldown == 0) {
                    Weather.LOGGER.debug("volcano " + ID + " is cooling");
                }

                if (ticksPerformedCooldown % processRateDelay == 0) {
                    int posX = (int) Math.floor((pos.x));
                    int posY = (int) Math.floor((startYPos)) + maxSize - step + 2;
                    int posZ = (int) Math.floor((pos.z));

                    Block blockID = Blocks.STONE;

                    world.setBlockState(new BlockPos(posX, posY, posZ), blockID.getDefaultState());
                    world.setBlockState(new BlockPos(posX + 1, posY, posZ), blockID.getDefaultState());
                    world.setBlockState(new BlockPos(posX - 1, posY, posZ), blockID.getDefaultState());
                    world.setBlockState(new BlockPos(posX, posY, posZ + 1), blockID.getDefaultState());
                    world.setBlockState(new BlockPos(posX, posY, posZ - 1), blockID.getDefaultState());
                    world.setBlockState(new BlockPos(posX + 1, posY, posZ + 1), blockID.getDefaultState());
                    world.setBlockState(new BlockPos(posX - 1, posY, posZ - 1), blockID.getDefaultState());
                    world.setBlockState(new BlockPos(posX - 1, posY, posZ + 1), blockID.getDefaultState());
                    world.setBlockState(new BlockPos(posX + 1, posY, posZ - 1), blockID.getDefaultState());

                    step++;
                }

                ticksPerformedCooldown++;
                if (ticksPerformedCooldown > ticksToCooldown) {
                    state++;
                }

            } else if (state == 6) {
                Weather.LOGGER.debug("volcano " + ID + " has reset!");

                //go to step 2
                resetEruption();
                //manager.removeVolcanoObject(ID);
            }

            //manager.getVolcanoObjects().clear();
        }
    }

    public void tickClient() {
        //Weather.dbg("ticking client volcano " + ID + " - " + state);

        if (particleBehaviors == null) {
            particleBehaviors = new ParticleBehaviors(pos);
            //particleBehaviorFog.sourceEntity = this;
        } else {
            if (!MinecraftClient.getInstance().isInSingleplayer() || !(MinecraftClient.getInstance().currentScreen instanceof GameMenuScreen)) {
                particleBehaviors.tickUpdateList();
            }
        }

        int loopSize = 1;
        Random rand = new Random();

        this.manager.getWorld().getTime();
        for (int i = 0; i < loopSize; i++) {
            if (listParticlesSmoke.size() < 500) {
                double spawnRad = size / 48;
                EntityRotFX particle = spawnSmokeParticle(pos.x + (rand.nextDouble() * spawnRad) - (rand.nextDouble() * spawnRad), pos.y + size + 2, pos.z + (rand.nextDouble() * spawnRad) - (rand.nextDouble() * spawnRad));
                listParticlesSmoke.add(particle);
            }
        }

        for (int i = 0; i < listParticlesSmoke.size(); i++) {
            EntityRotFX ent = listParticlesSmoke.get(i);
            if (!ent.isAlive()) {
                listParticlesSmoke.remove(ent);
            }
        }
    }

    public EntityRotFX spawnSmokeParticle(double x, double y, double z) {
        double speed = 0D;
        Random rand = new Random();
        EntityRotFX entityfx = particleBehaviors.spawnNewParticleIconFX(MinecraftClient.getInstance().world, ParticleRegistry.cloud256, x, y, z, (rand.nextDouble() - rand.nextDouble()) * speed, 0.0D/*(rand.nextDouble() - rand.nextDouble()) * speed*/, (rand.nextDouble() - rand.nextDouble()) * speed);
        particleBehaviors.initParticle(entityfx);
        ParticleBehaviors.setParticleRandoms(entityfx, true, true);
        ParticleBehaviors.setParticleFire(entityfx);
        entityfx.setCanCollide(false);
        entityfx.callUpdatePB = false;
        entityfx.setMaxAge(400 + rand.nextInt(200));
        entityfx.setScale(50);

        float randFloat = (rand.nextFloat() * 0.6F);
        float baseBright = 0.1F;
        float finalBright = Math.min(1F, baseBright + randFloat);
        entityfx.setRBGColorF(finalBright, finalBright, finalBright);

        ExtendedRenderer.rotEffRenderer.addEffect(entityfx);
        //entityfx.spawnAsWeatherEffect();
        particleBehaviors.particles.add(entityfx);
        return entityfx;
    }

    public void reset() {
        setDead();
    }

    public void setDead() {
        Weather.LOGGER.debug("volcano... killed? NO ONE KILLS A VOLCANO!");
    }
}
