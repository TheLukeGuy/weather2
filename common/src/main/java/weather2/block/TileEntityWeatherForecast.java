package weather2.block;

import CoroUtil.util.Vec3;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import weather2.ClientTickHandler;
import weather2.config.ConfigMisc;
import weather2.weathersystem.storm.CloudStorm;
import weather2.weathersystem.storm.Storm;

import java.util.ArrayList;
import java.util.List;

public class TileEntityWeatherForecast extends TileEntity implements ITickable {

    //since client receives data every couple seconds, we need to smooth out everything for best visual

    public float smoothAngle = 0;
    public float smoothSpeed = 0;

    public float smoothAngleRotationalVel = 0;
    public float smoothAngleRotationalVelAccel = 0;

    public float smoothAngleAdj = 0.1F;
    public float smoothSpeedAdj = 0.1F;

    public CloudStorm lastTickStormObject = null;

    public List<Storm> storms = new ArrayList<>();

    //public MapHandler mapHandler;

    @Override
    public void update() {
        if (world.isRemote) {
            if (world.getTotalWorldTime() % 200 == 0 || storms.size() == 0) {

                //catch race condition triggered by very slow computers
                ClientTickHandler.checkClientWeather();
                if (ClientTickHandler.weatherManager == null) return;

                lastTickStormObject = ClientTickHandler.weatherManager.getClosestCloudStorm(new Vec3(getPos().getX(), CloudStorm.layers.get(0), getPos().getZ()), 1024, CloudStorm.STATE_FORMING, true);

                if (ConfigMisc.radarCloudDebug) {
                    //storms.clear();
                    List<Storm> listAdd = new ArrayList<>();
                    for (Storm wo : ClientTickHandler.weatherManager.getStorms()) {
                        //if (wo instanceof StormObject && !((StormObject) wo).isCloudlessStorm()) {
                        listAdd.add(wo);
                        //}
                    }
                    storms = listAdd;
                } else {
                    storms = ClientTickHandler.weatherManager.getStormsAround(new Vec3(getPos().getX(), CloudStorm.layers.get(0), getPos().getZ()), 1024);
                }
            }
        } else {
    		/*if (mapHandler == null) {
    			mapHandler = new MapHandler(this);
    		}
    		mapHandler.tick();*/
        }
    }

    public NBTTagCompound writeToNBT(NBTTagCompound var1) {
        return super.writeToNBT(var1);
    }

    public void readFromNBT(NBTTagCompound var1) {
        super.readFromNBT(var1);

    }
}
