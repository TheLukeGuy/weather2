package weather2.util;

import extendedrenderer.particle.entity.EntityRotFX;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import weather2.entity.EntityMovingBlock;
import weather2.weather.wind.WindAffected;

public class WeatherUtilEntity {
    //old non multiplayer friendly var, needs resdesign where this is used
    public static int playerInAirTime = 0;

    public static float getWeight(Object entity1) {
        return getWeight(entity1, false);
    }

    public static float getWeight(Object entity1, boolean forTornado) {
        World world = WeatherUtilEntityOrParticle.getWorld(entity1);

        //fixes issue #270
        if (world == null) {
            return 1F;
        }

        if (entity1 instanceof WindAffected) {
            return ((WindAffected) entity1).getWindWeight();
        }

        if (entity1 instanceof EntityMovingBlock) {
            return 1F + ((float) ((EntityMovingBlock) entity1).age / 200);
        }

        if (entity1 instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity1;
            if (player.isOnGround() || player.isTouchingWater()) {
                playerInAirTime = 0;
            } else {
                //System.out.println(playerInAirTime);
                playerInAirTime++;
            }

            if (player.isCreative()) return 99999999F;

            int extraWeight = 0;

            if (player.inventory != null && !(player.inventory.armor.get(2).isEmpty())
                    && player.inventory.armor.get(2).getItem() == Items.IRON_CHESTPLATE) {
                extraWeight = 2;
            }

            if (player.inventory != null && !(player.inventory.armor.get(2).isEmpty())
                    && player.inventory.armor.get(2).getItem() == Items.DIAMOND_CHESTPLATE) {
                extraWeight = 4;
            }

            if (forTornado) {
                return 4.5F + extraWeight + ((float) (playerInAirTime / 400));
            } else {
                return 5.0F + extraWeight + ((float) (playerInAirTime / 400));
            }
        }

        if (isParticleRotServerSafe(world, entity1)) {
            float var = WeatherUtilParticle.getParticleWeight((EntityRotFX) entity1);

            if (var != -1) {
                return var;
            }
        }

        if (entity1 instanceof SquidEntity) {
            return 400F;
        }

        if (entity1 instanceof LivingEntity) {
            LivingEntity livingEnt = (LivingEntity) entity1;
            int airTime = livingEnt.getEntityData().getInteger("timeInAir");
            if (livingEnt.isOnGround() || livingEnt.isTouchingWater()) {
                airTime = 0;
            } else {
                airTime++;
            }

            livingEnt.getEntityData().setInteger("timeInAir", airTime);
        }

        if (entity1 instanceof Entity) {
            Entity ent = (Entity) entity1;
            if (WeatherUtilData.isWindWeightSet(ent) && (forTornado || WeatherUtilData.isWindAffected(ent))) {
                return WeatherUtilData.getWindWeight(ent);
            }
        }

        if (entity1 instanceof LivingEntity) {
            LivingEntity livingEnt = (LivingEntity) entity1;
            int airTime = livingEnt.getEntityData().getInteger("timeInAir");
            if (forTornado) {
                return 0.5F + (((float) airTime) / 800F);
            } else {
                return 500.0F + (livingEnt.isOnGround() ? 2.0F : 0.0F) + ((airTime) / 400);
            }
        }

        if (/*entity1 instanceof EntitySurfboard || */entity1 instanceof BoatEntity || entity1 instanceof ItemEntity/* || entity1 instanceof EntityTropicalFishHook*/ || entity1 instanceof FishingBobberEntity) {
            return 4000F;
        }

        if (entity1 instanceof MinecartEntity) {
            return 80F;
        }

        return 1F;
    }

    public static boolean isParticleRotServerSafe(World world, Object obj) {
        if (!world.isClient) return false;
        return isParticleRotClientCheck(obj);
    }

    public static boolean isParticleRotClientCheck(Object obj) {
        return obj instanceof EntityRotFX;
    }

    public static boolean isEntityOutside(Entity parEnt) {
        return isEntityOutside(parEnt, false);
    }

    public static boolean isEntityOutside(Entity parEnt, boolean cheapCheck) {
        return isPosOutside(parEnt.world, parEnt.getPos(), cheapCheck);
    }

    public static boolean isPosOutside(World parWorld, Vec3d parPos) {
        return isPosOutside(parWorld, parPos, false);
    }

    public static boolean isPosOutside(World parWorld, Vec3d parPos, boolean cheapCheck) {
        int rangeCheck = 5;
        int yOffset = 1;

        if (WeatherUtilBlock.getPrecipitationHeightSafe(parWorld, new BlockPos(MathHelper.floor(parPos.x), 0, MathHelper.floor(parPos.z))).getY() < parPos.y + 1)
            return true;

        if (cheapCheck) return false;

        Vec3d vecTry = new Vec3d(parPos.x + Direction.NORTH.getOffsetX() * rangeCheck, parPos.y + yOffset, parPos.z + Direction.NORTH.getOffsetZ() * rangeCheck);
        if (checkVecOutside(parWorld, parPos, vecTry)) return true;

        vecTry = new Vec3d(parPos.x + Direction.SOUTH.getOffsetX() * rangeCheck, parPos.y + yOffset, parPos.z + Direction.SOUTH.getOffsetZ() * rangeCheck);
        if (checkVecOutside(parWorld, parPos, vecTry)) return true;

        vecTry = new Vec3d(parPos.x + Direction.EAST.getOffsetX() * rangeCheck, parPos.y + yOffset, parPos.z + Direction.EAST.getOffsetZ() * rangeCheck);
        if (checkVecOutside(parWorld, parPos, vecTry)) return true;

        vecTry = new Vec3d(parPos.x + Direction.WEST.getOffsetX() * rangeCheck, parPos.y + yOffset, parPos.z + Direction.WEST.getOffsetZ() * rangeCheck);
        if (checkVecOutside(parWorld, parPos, vecTry)) return true;

        return false;
    }

    public static boolean checkVecOutside(World parWorld, Vec3d parPos, Vec3d parCheckPos) {
        boolean dirNorth = parWorld.rayTraceBlocks(parPos, parCheckPos) == null;
        if (dirNorth) {
            return WeatherUtilBlock.getPrecipitationHeightSafe(parWorld, new BlockPos(MathHelper.floor(parCheckPos.x), 0, MathHelper.floor(parCheckPos.z))).getY() < parCheckPos.y;
        }
        return false;
    }

    public static PlayerEntity getClosestPlayerAny(World world, double posX, double posY, double posZ, double distance) {
        double d0 = -1.0D;
        PlayerEntity entityplayer = null;

        for (int i = 0; i < world.getPlayers().size(); ++i) {
            PlayerEntity entityplayer1 = world.getPlayers().get(i);

            //if ((EntitySelectors.CAN_AI_TARGET.apply(entityplayer1) || !spectator) && (EntitySelectors.NOT_SPECTATING.apply(entityplayer1) || spectator))
            //{
            double d1 = entityplayer1.squaredDistanceTo(posX, posY, posZ);

            if ((distance < 0.0D || d1 < distance * distance) && (d0 == -1.0D || d1 < d0)) {
                d0 = d1;
                entityplayer = entityplayer1;
            }
            //}
        }

        return entityplayer;
    }
}
