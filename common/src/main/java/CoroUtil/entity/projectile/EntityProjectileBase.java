package CoroUtil.entity.projectile;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import CoroUtil.entity.EntityThrowableUsefull;
import extendedrenderer.particle.behavior.ParticleBehaviors;

public class EntityProjectileBase extends EntityThrowableUsefull implements IEntityAdditionalSpawnData {

	public int projectileType = 0;
	
	public static int PRJTYPE_FIREBALL = 0;
	public static int PRJTYPE_ICEBALL = 1;
	public static int PRJTYPE_ = 2;
	
	@SideOnly(Side.CLIENT)
	public ParticleBehaviors particleBehavior;
	
	public boolean tickParticleBehaviorList = true;
	
	public EntityProjectileBase(World world)
	{
		super(world);
	}
	
	public EntityProjectileBase(World world, EntityLivingBase entityliving, double parSpeed)
	{
		super(world, entityliving, parSpeed);
	}

	public EntityProjectileBase(World par1World, EntityLivingBase par2EntityLivingBase, EntityLivingBase target, double parSpeed)
    {
		super(par1World, par2EntityLivingBase, target, parSpeed);
    }

	public EntityProjectileBase(World world, double d, double d1, double d2)
	{
		super(world, d, d1, d2);
	}

	@Override
	protected void onImpact(RayTraceResult movingobjectposition) {
		super.onImpact(movingobjectposition);

	}

	@Override
	public void writeSpawnData(ByteBuf data) {
		data.writeInt(projectileType);
	}

	@Override
	public void readSpawnData(ByteBuf data) {
		projectileType = data.readInt();
	}
	
	@Override
	public void onUpdate() {
		
		if (tickParticleBehaviorList) {
			if (particleBehavior != null) {
				particleBehavior.tickUpdateList();
			}
		}
		
		super.onUpdate();
	}
	
	@Override
	public void readEntityFromNBT(NBTTagCompound par1nbtTagCompound) {
		super.readEntityFromNBT(par1nbtTagCompound);
		
		//kill on reload
		setDead();
	}

}
