package sh.lpx.weather2.util;

import net.minecraft.client.particle.Particle;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import sh.lpx.weather2.mixin.client.ParticleAccessor;

public class WeatherUtilEntityOrParticle {
    public static double getPosX(Object obj) {
        if (obj instanceof Entity) {
            return ((Entity) obj).getX();
        } else {
            return ((ParticleAccessor) obj).getX();
        }
    }

    public static double getPosY(Object obj) {
        if (obj instanceof Entity) {
            return ((Entity) obj).getY();
        } else {
            return ((ParticleAccessor) obj).getY();
        }
    }

    public static double getPosZ(Object obj) {
        if (obj instanceof Entity) {
            return ((Entity) obj).getZ();
        } else {
            return ((ParticleAccessor) obj).getZ();
        }
    }

    public static double getMotionX(Object obj) {
        if (obj instanceof Entity) {
            return ((Entity) obj).motionX;
        } else {
            return ((Particle) obj).motionX;
        }
    }

    public static double getMotionY(Object obj) {
        if (obj instanceof Entity) {
            return ((Entity) obj).motionY;
        } else {
            return ((Particle) obj).motionY;
        }
    }

    public static double getMotionZ(Object obj) {
        if (obj instanceof Entity) {
            return ((Entity) obj).motionZ;
        } else {
            return ((Particle) obj).motionZ;
        }
    }

    public static void setMotionX(Object obj, double val) {
        if (obj instanceof Entity) {
            ((Entity) obj).motionX = val;
        } else {
            ((Particle) obj).motionX = val;
        }
    }

    public static void setMotionY(Object obj, double val) {
        if (obj instanceof Entity) {
            ((Entity) obj).motionY = val;
        } else {
            ((Particle) obj).motionY = val;
        }
    }

    public static void setMotionZ(Object obj, double val) {
        if (obj instanceof Entity) {
            ((Entity) obj).motionZ = val;
        } else {
            ((Particle) obj).motionZ = val;
        }
    }

    public static World getWorld(Object obj) {
        if (obj instanceof Entity) {
            return ((Entity) obj).world;
        } else {
            return ((ParticleAccessor) obj).getWorld();
        }
    }
}
