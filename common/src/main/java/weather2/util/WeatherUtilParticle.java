package weather2.util;

import extendedrenderer.particle.entity.EntityRotFX;
import extendedrenderer.particle.entity.ParticleTexFX;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraftforge.fml.client.FMLClientHandler;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Random;

public class WeatherUtilParticle {
    public static ArrayDeque<Particle>[][] fxLayers;

    public static Random rand = new Random();
    //public static int rainDrops = 20;

    //weather2: not sure what will happen to this in 1.7, copied over for convenience
    public static int getParticleAge(Particle ent) {
        return ent.particleAge;
        //return (Integer) OldUtil.getPrivateValueBoth(Particle.class, ent, "field_70546_d", "particleAge");
    }

    //weather2: not sure what will happen to this in 1.7, copied over for convenience
    public static void setParticleAge(Particle ent, int val) {
        ent.particleAge = val;
        //OldUtil.setPrivateValueBoth(Particle.class, ent, "field_70546_d", "particleAge", val);
    }

    public static void getFXLayers() {
        //fxLayers
        Field field;

        try {
            field = (ParticleManager.class).getDeclaredField("field_78876_b");//ObfuscationReflectionHelper.remapFieldNames("net.minecraft.client.particle.EffectRenderer", new String[] { "fxLayers" })[0]);
            field.setAccessible(true);
            fxLayers = (ArrayDeque<Particle>[][]) field.get(FMLClientHandler.instance().getClient().effectRenderer);
        } catch (Exception ex) {
            try {
                field = (ParticleManager.class).getDeclaredField("fxLayers");
                field.setAccessible(true);
                fxLayers = (ArrayDeque<Particle>[][]) field.get(FMLClientHandler.instance().getClient().effectRenderer);
            } catch (Exception ex2) {
                ex2.printStackTrace();
            }
        }
    }

    public static float getParticleWeight(EntityRotFX entity1) {
        if (entity1 instanceof ParticleTexFX) {
            return 5.0F + ((float) entity1.getAge() / 200);
        }

        if (entity1 instanceof Particle) {
            return 5.0F + ((float) entity1.getAge() / 200);
        }

        return -1;
    }
}
