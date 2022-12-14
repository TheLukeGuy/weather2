package sh.lpx.weather2.block;

import sh.lpx.weather2.ClientTickHandler;
import sh.lpx.weather2.CommonProxy;
import sh.lpx.weather2.config.ConfigMisc;
import sh.lpx.weather2.config.ConfigSand;
import sh.lpx.weather2.weather.storm.CloudStorm;
import sh.lpx.weather2.weather.storm.SandStorm;
import sh.lpx.weather2.util.WeatherUtilPhysics;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import sh.lpx.weather2.util.WeatherUtilSound;

import java.util.List;

public class TileEntityTSiren extends TileEntity implements ITickable {
    public long lastPlayTime = 0L;

    @Override
    public void update() {
        if (world.isRemote) {
            int meta = CommonProxy.blockTSiren.getMetaFromState(this.world.getBlockState(this.getPos()));
            if (BlockTSiren.isEnabled(meta)) {
                //System.out.println("enabled");
                tickClient();
            }

        }
    }

    @SideOnly(Side.CLIENT)
    public void tickClient() {

        if (this.lastPlayTime < System.currentTimeMillis()) {
            Vec3 pos = new Vec3(getPos().getX(), getPos().getY(), getPos().getZ());

            CloudStorm so = ClientTickHandler.weatherManager.getClosestCloudStorm(pos, ConfigMisc.sirenActivateDistance, CloudStorm.STATE_FORMING);

            if (so != null) {
                this.lastPlayTime = System.currentTimeMillis() + 13000L;
                WeatherUtilSound.playNonMovingSound(pos, "streaming.siren", 1.0F, 1.0F, 120);
            } else {
                if (!ConfigSand.Sandstorm_Siren_PleaseNoDarude) {
                    SandStorm sandstorm = ClientTickHandler.weatherManager.getClosestSandStormByIntensity(pos);

                    if (sandstorm != null) {
                        List<Vec3> points = sandstorm.getSandstormAsShape();

                        float distMax = 75F;

                        //double scale = sandstorm.getSandstormScale();
                        boolean inStorm = WeatherUtilPhysics.isInConvexShape(pos, points);
                        double dist = Math.min(distMax, WeatherUtilPhysics.getDistanceToShape(pos, points));

                        if (inStorm || dist < distMax) {
                            String soundToPlay = "siren_sandstorm_5_extra";
                            if (getWorld().rand.nextBoolean()) {
                                soundToPlay = "siren_sandstorm_6_extra";
                            }

                            float distScale = Math.max(0.1F, 1F - (float) ((dist) / distMax));
                            if (inStorm) distScale = 1F;

                            this.lastPlayTime = System.currentTimeMillis() + 15000L;//WeatherUtilSound.soundToLength.get(soundToPlay) - 500L;
                            WeatherUtilSound.playNonMovingSound(pos, "streaming." + soundToPlay, 1F, distScale, distMax);
                        }
                    }
                }
            }
        }
    }

}
