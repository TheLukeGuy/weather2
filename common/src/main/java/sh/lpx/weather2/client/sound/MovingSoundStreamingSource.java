package sh.lpx.weather2.client.sound;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import sh.lpx.weather2.weather.storm.CloudStorm;

public class MovingSoundStreamingSource extends MovingSoundInstance {
    private CloudStorm storm = null;
    public float cutOffRange = 128;
    public Vec3d realSource = null;
    public boolean lockToPlayer = false;

    public MovingSoundStreamingSource(Vec3d parPos, SoundEvent event, SoundCategory category, float parVolume, float parPitch, boolean lockToPlayer) {
        super(event, category);
        this.repeat = false;
        this.volume = parVolume;
        this.pitch = parPitch;
        this.realSource = parPos;

        this.lockToPlayer = lockToPlayer;

        tick();
    }

    //constructor for non moving sounds
    public MovingSoundStreamingSource(Vec3d parPos, SoundEvent event, SoundCategory category, float parVolume, float parPitch, float parCutOffRange) {
        super(event, category);
        this.repeat = false;
        this.volume = parVolume;
        this.pitch = parPitch;
        cutOffRange = parCutOffRange;
        realSource = parPos;

        //sync position
        tick();
    }

    //constructor for moving sounds
    public MovingSoundStreamingSource(CloudStorm parStorm, SoundEvent event, SoundCategory category, float parVolume, float parPitch, float parCutOffRange) {
        super(event, category);
        this.storm = parStorm;
        this.repeat = false;
        this.volume = parVolume;
        this.pitch = parPitch;
        cutOffRange = parCutOffRange;

        //sync position
        tick();
    }

    @Override
    public void tick() {
        PlayerEntity entP = MinecraftClient.getInstance().player;

        if (entP != null) {
            this.x = (float) entP.getX();
            this.y = (float) entP.getY();
            this.z = (float) entP.getZ();
        }

        if (storm != null) {
            realSource = new Vec3d(this.storm.posGround.x, this.storm.posGround.y, this.storm.posGround.z);
        }

        //if locked to player, don't dynamically adjust volume
        if (!lockToPlayer) {
            float var3 = (float) ((cutOffRange - (double) MathHelper.sqrt(getDistanceFrom(realSource, entP.getPos()))) / cutOffRange);

            if (var3 < 0.0F) {
                var3 = 0.0F;
            }

            volume = var3;
        }
    }

    public double getDistanceFrom(Vec3d source, Vec3d targ) {
        double d3 = source.x - targ.x;
        double d4 = source.y - targ.y;
        double d5 = source.z - targ.z;
        return d3 * d3 + d4 * d4 + d5 * d5;
    }
}
