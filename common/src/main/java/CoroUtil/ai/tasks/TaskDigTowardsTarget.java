package CoroUtil.ai.tasks;

import java.util.ArrayDeque;

import CoroUtil.ai.IInvasionControlledTask;
import CoroUtil.block.TileEntityRepairingBlock;
import CoroUtil.config.ConfigCoroUtilAdvanced;
import CoroUtil.config.ConfigDynamicDifficulty;
import CoroUtil.forge.CULog;
import CoroUtil.packet.PacketHelper;
import CoroUtil.util.CoroUtilEntity;
import CoroUtil.util.UtilMining;
import CoroUtil.world.WorldDirectorManager;
import CoroUtil.world.grid.block.BlockDataPoint;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import CoroUtil.ai.ITaskInitializer;
import CoroUtil.util.BlockCoord;
import net.minecraft.util.math.Vec3d;

public class TaskDigTowardsTarget extends EntityAIBase implements ITaskInitializer, IInvasionControlledTask
{

	//set from ConfigInvasion
	public static boolean convertMinedBlocksToRepairingBlocksDuringInvasions = true;
	public static boolean preventMinedTileEntitiesDuringInvasions = true;

    private EntityCreature entity = null;
	private IBlockState stateCurMining = null;
    private BlockCoord posCurMining = null;
    private EntityLivingBase targetLastTracked = null;
    private int digTimeCur = 0;
    private int digTimeMax = 15*20;
    //private double curBlockDamage = 0D;
    private int noMoveTicks = 0;
    private ArrayDeque<BlockPos> listPillarToMine = new ArrayDeque<>();

	private Vec3d posLastTracked = null;

    public boolean debug = false;

	/**
	 * Fields used when task is added via invasions, but not via hwmonsters, or do we want that too?
	 */
	public static String dataUseInvasionRules = "HW_Inv_UseInvasionRules";
	public static String dataUsePlayerList = "HW_Inv_UsePlayerList";
	public static String dataWhitelistMode = "HW_Inv_WhitelistMode";
	public static String dataListPlayers = "HW_Inv_ListPlayers";
	/*public static String dataListPlayers = "HW_Inv_ActiveTimeStart";
	public static String dataListPlayers = "HW_Inv_ActiveTimeEnd";*/

	//needed for generic instantiation
    public TaskDigTowardsTarget()
    {
        this.setMutexBits(3);
    }
    
    @Override
    public void setEntity(EntityCreature creature) {
    	this.entity = creature;
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    @Override
	public boolean shouldExecute()
    {
    	//dbg("should?");
    	/**
    	 * Zombies wouldnt try to mine if they are bunched up behind others, as they are still technically pathfinding, this helps resolve that issue, and maybe water related issues
    	 */
    	double movementThreshold = 0.05D;
    	int noMoveThreshold = 5;
    	/*if (posCurMining == null && entity.motionX < movementThreshold && entity.motionX > -movementThreshold &&
    			entity.motionZ < movementThreshold && entity.motionZ > -movementThreshold) {
    		
    		noMoveTicks++;
    		
    	} else {
    		noMoveTicks = 0;
    	}*/

		if (posLastTracked == null) {
			posLastTracked = entity.getPositionVector();
		} else {
			if (posLastTracked.distanceTo(entity.getPositionVector()) < 2) {
				noMoveTicks++;
			} else {
				posLastTracked = entity.getPositionVector();
				noMoveTicks = 0;
			}
		}
    	
    	//System.out.println("noMoveTicks: " + noMoveTicks);
    	/*if (noMoveTicks > noMoveThreshold) {
    		System.out.println("ent not moving enough, try to mine!? " + noMoveTicks + " ent: " + entity.getEntityId());
    	}*/
    	
    	if (!entity.onGround && !entity.isInWater()) return false;
    	//return true if not pathing, has target
    	if (entity.getAttackTarget() != null || targetLastTracked != null) {
    		if (entity.getAttackTarget() == null) {
    			//System.out.println("forcing reset of target2");
    			entity.setAttackTarget(targetLastTracked);
				//fix for scenario where setAttackTarget calls forge event and someone undoes target setting
				if (entity.getAttackTarget() == null) {
					return false;
				}
    		} else {
    			targetLastTracked = entity.getAttackTarget();
    		}

    		//prevent invasion spawned diggers to not dig for players with invasions off
			if (entity.getEntityData().getBoolean(dataUsePlayerList)) {
				String playerName = CoroUtilEntity.getName(entity.getAttackTarget());
				boolean whitelistMode = entity.getEntityData().getBoolean(dataWhitelistMode);
				String listPlayers = entity.getEntityData().getString(dataListPlayers);

				if (whitelistMode) {
					if (!listPlayers.contains(playerName)) {
						return false;
					}
				} else {
					if (listPlayers.contains(playerName)) {
						return false;
					}
				}
			}

    		//if (!entity.getNavigator().noPath()) System.out.println("path size: " + entity.getNavigator().getPath().getCurrentPathLength());
    		if (entity.getNavigator().noPath() || entity.getNavigator().getPath().getCurrentPathLength() == 1 || noMoveTicks > noMoveThreshold) {
    		//if (entity.motionX < 0.1D && entity.motionZ < 0.1D) {
    			if (updateBlockToMine()) {
    				//System.out.println("should!");
    				return true;
    			}
    		} else {
    			//clause for if stuck trying to path
    			
    		}
    	}
    	
        return false;
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
	@Override
    public boolean shouldContinueExecuting()
    {
		//dbg("continue?");
    	if (posCurMining == null) {
			dbg("shouldContinueExecuting fail because posCurMining == null");
    		return false;
		}
        BlockPos pos = new BlockPos(posCurMining.posX, posCurMining.posY, posCurMining.posZ);
        //IBlockState state = entity.world.getBlockState(pos);
    	if (UtilMining.canMineBlockNew(entity.world, pos)/*!entity.world.isAirBlock(pos) && UtilMining.canMineBlock(entity.world, pos, entity.world.getBlockState(pos).getBlock())*/) {
    		return true;
    	} else {
			setMiningBlock(null, null);
			dbg("shouldContinueExecuting fail because not air");
    		//System.out.println("ending execute");
    		return false;
    	}
    }

    /**
     * Execute a one shot task or start executing a continuous task
     */
	@Override
    public void startExecuting()
    {
    	dbg("start mining task");
    	//System.out.println("start!");
    }

    /**
     * Resets the task
     */
	@Override
    public void resetTask()
    {
    	//System.out.println("reset!");
		//Minecraft.getMinecraft().mouseHelper.ungrabMouseCursor();
    	digTimeCur = 0;
    	//curBlockDamage = 0;
    	listPillarToMine.clear();
		setMiningBlock(null, null);
    }

    /**
     * Updates the task
     */

    @Override
    public void updateTask()
    {
    	//System.out.println("running!");
		entity.idleTime = 0;
    	
    	if (entity.getAttackTarget() != null) {
    		targetLastTracked = entity.getAttackTarget();
    	} else {
    		if (targetLastTracked != null) {
    			//System.out.println("forcing reset of target");
    			entity.setAttackTarget(targetLastTracked);
    		}
    	}
    	
    	tickMineBlock();
    }
    
    public boolean updateBlockToMine() {

		//fix for scenario where setAttackTarget calls forge event and someone undoes target setting
		if (entity.getAttackTarget() == null) {
			return false;
		}

		setMiningBlock(null, null);

		double entPosX = Math.floor(entity.posX) + 0.5F;
		double entPosZ = Math.floor(entity.posZ) + 0.5F;

		//hmm trying to fix a weird diagonal bug:
		/*entPosX = entity.posX;
		entPosZ = entity.posZ;*/
    	
    	double vecX = entity.getAttackTarget().posX - entPosX;
    	//feet
    	double vecY = entity.getAttackTarget().getEntityBoundingBox().minY - entity.getEntityBoundingBox().minY;
    	double vecZ = entity.getAttackTarget().posZ - entPosZ;

    	//get angle, snap it to 90, then reconvert back to scalar which is now locked to best 90 degree option
		double angle = Math.atan2(vecZ, vecX);
		//i think angle snapping is having issues, doing weird things to xz diagonal digging
		//more random!
		//if (entity.world.rand.nextBoolean()) {
			angle = Math.round(Math.toDegrees(angle) / 90D) * 90;
		//}
		//verified this matched the original scalar version of vecX / Z vars before it was snapped to 90
		double relX = Math.cos(angle);
		double relZ = Math.sin(angle);
    	
    	double scanX = entPosX + relX;
    	double scanZ = entPosZ + relZ;

		double distHoriz = Math.sqrt(vecX * vecX + vecZ * vecZ);
		if (distHoriz < 0) distHoriz = 1;

		double distVert = vecY;

		double factor = distVert / distHoriz;

		/**
		 * 0 = even
		 * - = down
		 * + = up
		 *
		 * use 0.3 has threshold for digging up or down?
		 */

		dbg("factor: " + factor);
    	
    	/*if (rand.nextBoolean()) {
    		//scanX = entity.posX;
        	scanZ = entity.posZ + 0;
    	} else {
    		scanX = entity.posX + 0;
        	//scanZ = entity.posZ;
    	}*/

        listPillarToMine.clear();

        //account for being directly above or directly below target
        if (distHoriz <= 1) {
            scanX = entPosX;
            scanZ = entPosZ;
        }

        //i think the y is the block under feet, not where feet occupy
        //actually, must be where feet occupy...
        BlockPos posFrontFeet = new BlockPos(MathHelper.floor(scanX), MathHelper.floor(entity.getEntityBoundingBox().minY), MathHelper.floor(scanZ));


        BlockPos posFeetCheck = new BlockPos(MathHelper.floor(entPosX), MathHelper.floor(entity.getEntityBoundingBox().minY), MathHelper.floor(entPosZ));

        /**
         * when digging up, or down, need to make sure theres space above to jump up to next pillar
         * - just up?
         */

        boolean wasDiggingStrait = false;

        if (factor <= -0.3F) {

            //down
            dbg("Digging Down");
            listPillarToMine.add(posFrontFeet.up(1));
            listPillarToMine.add(posFrontFeet);
            listPillarToMine.add(posFrontFeet.down(1));


        } else if (factor >= 0.1F) {

            //if (!entity.world.isAirBlock(posFeetCheck.up(2)) && UtilMining.canMineBlock(entity.world, posFeetCheck.up(2), entity.world.getBlockState(posFeetCheck.up(2)).getBlock())) {
			if (UtilMining.canMineBlockNew(entity.world, posFeetCheck.up(2))) {
                dbg("Detected block above head, dig it out");
                listPillarToMine.add(posFeetCheck.up(2));
            } else {
                //up
                dbg("Digging Up");
                listPillarToMine.add(posFrontFeet.up(1));
                listPillarToMine.add(posFrontFeet.up(2));
                listPillarToMine.add(posFrontFeet.up(3));
            }



        } else {

            wasDiggingStrait = true;
            //strait
            dbg("Digging Strait");
            listPillarToMine.add(posFrontFeet.up(1));
            listPillarToMine.add(posFrontFeet);
        }

        boolean fail = false;
        boolean oneMinable = false;

        for (BlockPos pos : listPillarToMine) {
            IBlockState state = entity.world.getBlockState(pos);
            dbg("set: " + pos + " - " + state.getBlock());
        }

        for (BlockPos pos : listPillarToMine) {
            //allow for air for now
            if (UtilMining.canMineBlockNew(entity.world, pos)) {
                oneMinable = true;
                break;
            }
        }

        if (!oneMinable) {
            dbg("All air blocks or unmineable, trying to fallback to alternate");
            listPillarToMine.clear();

            //second try, to try to fix an AI loop maybe caused by aquard angles or the repairing block, a bit of a hack fix, and messy duplicated code
            //TODO: also consider trying the alternate too, if up or down is failing, dig strait instead, both should realign eachother?
            //we are now

            //weird issues, lets try random again
            wasDiggingStrait = entity.world.rand.nextBoolean();
            if (wasDiggingStrait) {
                if (factor < 0F) {
                    //down
                    dbg("Digging Down Fallback try, but not infront of feet, instead directly under");
                    listPillarToMine.add(posFeetCheck.up(1));
                    listPillarToMine.add(posFeetCheck);
                    listPillarToMine.add(posFeetCheck.down(1));
                } else if (factor >= 0F) {
                    //if (!entity.world.isAirBlock(posFeetCheck.up(2)) && UtilMining.canMineBlock(entity.world, posFeetCheck.up(2), entity.world.getBlockState(posFeetCheck.up(2)).getBlock())) {
					if (UtilMining.canMineBlockNew(entity.world, posFeetCheck.up(2))) {
                        IBlockState check = entity.world.getBlockState(posFeetCheck.up(2));
                        dbg("Digging Up Fallback try, Detected block above head, dig it out Fallback try, block was: " + check);
                        listPillarToMine.add(posFeetCheck.up(2));
                    } else {
                        //up
                        dbg("Digging Up Fallback try");
                        listPillarToMine.add(posFrontFeet.up(1));
                        listPillarToMine.add(posFrontFeet.up(2));
                        listPillarToMine.add(posFrontFeet.up(3));
                    }
                }
            } else {
                dbg("Digging Strait Fallback try");
                listPillarToMine.add(posFrontFeet.up(1));
                listPillarToMine.add(posFrontFeet);
            }


            for (BlockPos pos : listPillarToMine) {
                IBlockState state = entity.world.getBlockState(pos);
                dbg("set try2: " + pos + " - " + state.getBlock());
            }

            for (BlockPos pos : listPillarToMine) {
                //allow for air for now
                //if (!entity.world.isAirBlock(pos) && UtilMining.canMineBlock(entity.world, pos, entity.world.getBlockState(pos).getBlock())) {
				if (UtilMining.canMineBlockNew(entity.world, pos)) {
                    oneMinable = true;
                    break;
                }
            }

            if (!oneMinable) {
                dbg("All air blocks or unmineable, fallback failed, cancelling");
                listPillarToMine.clear();
                return false;
            }

            //return false;
        }

		if (ConfigCoroUtilAdvanced.enableDebugRenderer) {
			for (BlockPos pos : listPillarToMine) {
				PacketHelper.spawnDebugRender(entity.world.provider.getDimension(), pos, 40, 0x0000FF, 0);
			}
		}

        setMiningBlock(entity.world.getBlockState(listPillarToMine.getFirst()), new BlockCoord(listPillarToMine.getFirst()));

        return true;

    }

    public void setMiningBlock(IBlockState state, BlockCoord pos) {
		dbg("setMiningBlock: " + pos + (state != null ? " - " + state.getBlock() : ""));
		this.posCurMining = pos;
		this.stateCurMining = state;
	}
    
    public void tickMineBlock() {
    	if (posCurMining == null) return;

		IBlockState state = entity.world.getBlockState(posCurMining.toBlockPos());
		Block block = state.getBlock();

		//while (entity.world.isAirBlock(posCurMining.toBlockPos()) || !UtilMining.canMineBlock(entity.world, posCurMining.toBlockPos(), entity.world.getBlockState(posCurMining.toBlockPos()).getBlock())) {
		while (!UtilMining.canMineBlockNew(entity.world, posCurMining.toBlockPos())) {
			dbg("Detected air or unmineable block, moving to next block in list, cur size: " + listPillarToMine.size());
			if (listPillarToMine.size() > 1) {
				listPillarToMine.removeFirst();
				BlockPos pos = listPillarToMine.getFirst();
				setMiningBlock(entity.world.getBlockState(pos), new BlockCoord(pos));
				//return;
			} else {
				//try to move into the spot, sometimes vanilla/my other pathing wont move them into it
				//nm might have been cause testing while spectating in a wall
				//entity.getNavigator().tryMoveToXYZ(posCurMining.posX, posCurMining.posY, posCurMining.posZ, 1);
				resetTask();
				return;
			}

			state = entity.world.getBlockState(posCurMining.toBlockPos());
			block = state.getBlock();
		}
    	
    	//force stop mining if pushed away, or if block changed
    	if (stateCurMining != state || entity.getDistance(posCurMining.posX, posCurMining.posY, posCurMining.posZ) > 6) {
			dbg("too far or block changed state");
    		//entity.world.destroyBlockInWorldPartially(entity.getEntityId(), posCurMining.posX, posCurMining.posY, posCurMining.posZ, 0);
			entity.world.sendBlockBreakProgress(Integer.MAX_VALUE - 50, posCurMining.toBlockPos(), 0);
			setMiningBlock(null, null);
    		return;
    	}
    	
    	//entity.getNavigator().clearPathEntity();
    	
    	//Block block = entity.world.getBlock(posCurMining.posX, posCurMining.posY, posCurMining.posZ);
    	//double blockStrength = block.getBlockHardness(entity.world, posCurMining.posX, posCurMining.posY, posCurMining.posZ);
    	//Block block = state.getBlock();

    	
    	double blockStrength = state.getBlockHardness(entity.world, posCurMining.toBlockPos());
    	
    	if (blockStrength == -1) {
			setMiningBlock(null, null);
    		return;
    	}


		if (entity.world.getTotalWorldTime() % 10 == 0) {
			//entity.swingItem();
			entity.swingArm(EnumHand.MAIN_HAND);
			//System.out.println("swing!");

			entity.world.playSound(null, posCurMining.toBlockPos(), block.getSoundType(state, entity.world, posCurMining.toBlockPos(), entity).getBreakSound(), SoundCategory.HOSTILE, 0.5F, 1F);
			//entity.world.playSoundEffect(posCurMining.getX(), posCurMining.getY(), posCurMining.getZ(), block.stepSound.getBreakSound(), 0.5F, 1F);
		}
    	
    	//curBlockDamage += 0.01D / blockStrength;
		BlockDataPoint bdp = WorldDirectorManager.instance().getBlockDataGrid(entity.world).getBlockData(posCurMining.getX(), posCurMining.getY(), posCurMining.getZ());// ServerTickHandler.wd.getBlockDataGrid(world).getBlockData(newX, newY, newZ);
		//only allow continual progress if was dug at within last 30 seconds
		int maxTimeBetweenDigProgress = 20*30;
		if (bdp.lastTickTimeDig + maxTimeBetweenDigProgress > entity.world.getTotalWorldTime()) {
			//do nothing?
		} else {
			//reset it
			bdp.digDamage = 0;
		}
		bdp.lastTickTimeDig = entity.world.getTotalWorldTime();
		double digSpeed = ConfigDynamicDifficulty.digSpeed;
		bdp.digDamage += digSpeed / blockStrength;
    	
    	if (bdp.digDamage > 1D) {
    		entity.world.sendBlockBreakProgress(Integer.MAX_VALUE - 50, posCurMining.toBlockPos(), 0);
    		//added tile entity check due to new config support to not use repairing block system
			//since repairing blocks dont support tile entities, need to prevent turning them into one, and instead dropping as normal item
            //if (convertMinedBlocksToRepairingBlocksDuringInvasions && UtilMining.canConvertToRepairingBlock(entity.world, state) && entity.world.getTileEntity(posCurMining.toBlockPos()) == null) {
			if (convertMinedBlocksToRepairingBlocksDuringInvasions && UtilMining.canConvertToRepairingBlockNew(entity.world, posCurMining.toBlockPos(), false)) {
				if (UtilMining.canGrabEventCheck(entity.world, state, posCurMining.toBlockPos())) {
					TileEntityRepairingBlock.replaceBlockAndBackup(entity.world, posCurMining.toBlockPos());
				}
            } else {
            	boolean useFakePlayer = true;
            	if (useFakePlayer) {
            		UtilMining.tryRemoveBlockWithFakePlayer(entity.world, posCurMining.toBlockPos());
				} else {
            		//this method has issues and shouldnt be used without more care, eg itemizing things that shouldnt exist like top/bottom piece of double grass, causes missing tex items
					Block.spawnAsEntity(entity.world, posCurMining.toBlockPos(), new ItemStack(state.getBlock(), 1, state.getBlock().getMetaFromState(state)));
					entity.world.setBlockToAir(posCurMining.toBlockPos());
				}
            }

            if (listPillarToMine.size() > 1) {
				listPillarToMine.removeFirst();
            	BlockPos pos = listPillarToMine.getFirst();
				setMiningBlock(entity.world.getBlockState(pos), new BlockCoord(pos));
			} else {
            	listPillarToMine.clear();
				setMiningBlock(null, null);
			}


			bdp.digDamage = 0;
    		
    	} else {
    		//entity.world.destroyBlockInWorldPartially(entity.getEntityId(), posCurMining.posX, posCurMining.posY, posCurMining.posZ, (int)(curBlockDamage * 10D));
			//entity.world.sendBlockBreakProgress(Integer.MAX_VALUE - 50, posCurMining.toBlockPos(), 0);
    		entity.world.sendBlockBreakProgress(Integer.MAX_VALUE - 50, posCurMining.toBlockPos(), (int)(bdp.digDamage * 10D));
    	}
    }

	@Override
	public boolean shouldBeRemoved() {
		boolean forInvasion = entity.getEntityData().getBoolean(dataUseInvasionRules);

		if (forInvasion && ConfigCoroUtilAdvanced.removeInvasionAIWhenInvasionDone) {
			//once its day, disable forever
			if (this.entity.world.isDaytime()) {
				CULog.dbg("removing digging from " + this.entity.getName());
				return true;
				//taskActive = false;
			}
		}

		return false;
	}

	public void dbg(String str) {
		debug = false;
    	if (debug) {
    	    CULog.dbg(str);
			//System.out.println(str);
		}
	}
}
