package weather2.block;

import CoroUtil.util.Vec3;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import weather2.ServerTickHandler;
import weather2.config.ConfigStorm;
import weather2.weather.ServerWeatherManager;
import weather2.weather.storm.CloudStorm;
import weather2.weather.storm.Storm;

import java.util.List;

public class TileEntityWeatherDeflector extends TileEntity implements ITickable {

    //0 = kill storms, 1 = prevent block damage
    public int mode = 0;

    public static int MODE_KILLSTORMS = 0;
    public static int MODE_NOBLOCKDAMAGE = 1;

    @Override
    public void onLoad() {
        super.onLoad();
        maintainBlockDamageDeflect();
    }

    @Override
    public void update() {

        if (!world.isRemote) {

            if (mode == MODE_KILLSTORMS) {
                if (world.getTotalWorldTime() % 100 == 0) {
                    ServerWeatherManager wm = ServerTickHandler.lookupDimToWeatherMan.get(world.provider.getDimension());
                    if (wm != null) {
                        List<Storm> storms = wm.getStormsAround(new Vec3(getPos().getX(), CloudStorm.layers.get(0), getPos().getZ()), ConfigStorm.Storm_Deflector_RadiusOfStormRemoval, true);

                        for (int i = 0; i < storms.size(); i++) {
                            Storm storm = storms.get(i);

                            if (storm != null) {
                                wm.removeStorm(storm.id);
                                wm.syncStormRemove(storm);
                            }
                        }
                    }
                }
            }

            if (world.getTotalWorldTime() % 20 == 0) {
                maintainBlockDamageDeflect();
            }

        }
    }

    public void maintainBlockDamageDeflect() {
        ServerWeatherManager wm = ServerTickHandler.lookupDimToWeatherMan.get(world.provider.getDimension());
        if (wm != null) {
            if (mode == MODE_KILLSTORMS) {
                if (wm.getListWeatherBlockDamageDeflector().contains(getPos().toLong())) {
                    wm.getListWeatherBlockDamageDeflector().remove(getPos().toLong());
                }
            } else if (mode == MODE_NOBLOCKDAMAGE) {
                if (!wm.getListWeatherBlockDamageDeflector().contains(getPos().toLong())) {
                    wm.getListWeatherBlockDamageDeflector().add(getPos().toLong());
                }
            }
        }
    }

    public void rightClicked(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        cycleMode();

        String modeMsg = "";

        if (mode == MODE_KILLSTORMS) {
            modeMsg = "Kill thunderstorms and deadlier";
        } else if (mode == MODE_NOBLOCKDAMAGE) {
            modeMsg = "Prevent block damage only";
            maintainBlockDamageDeflect();
        }

        playerIn.sendMessage(new TextComponentString("Weather Deflector set to mode: " + modeMsg));
    }

    public void cycleMode() {
        mode++;

        if (mode > MODE_NOBLOCKDAMAGE) {
            mode = 0;
        }
    }

    public NBTTagCompound writeToNBT(NBTTagCompound var1) {
        var1.setInteger("mode", mode);
        return super.writeToNBT(var1);
    }

    public void readFromNBT(NBTTagCompound var1) {
        super.readFromNBT(var1);
        mode = var1.getInteger("mode");
    }

    @Override
    public void invalidate() {
        super.invalidate();

        if (!world.isRemote) {
            //always try to remove, incase they removed the block before the tick code could run after switching mode
            ServerWeatherManager wm = ServerTickHandler.lookupDimToWeatherMan.get(world.provider.getDimension());
            wm.getListWeatherBlockDamageDeflector().remove(getPos().toLong());
        }

    }
}
