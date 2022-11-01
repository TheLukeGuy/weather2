package CoroUtil.world.location;

import CoroUtil.util.BlockCoord;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public interface ISimulationTickable {

    public void init();

    public void initPost();

    public void tickUpdate();

    public void tickUpdateThreaded();

    public void readFromNBT(NBTTagCompound parData);

    public NBTTagCompound writeToNBT(NBTTagCompound parData);

    public void cleanup();

    public void setWorldID(int ID);

    public World getWorld();

    public BlockCoord getOrigin();

    public void setOrigin(BlockCoord coord);

    public boolean isThreaded();

    public String getSharedSimulationName();
}
