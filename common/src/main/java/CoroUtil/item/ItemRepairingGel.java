package CoroUtil.item;

import CoroUtil.block.TileEntityRepairingBlock;
import CoroUtil.util.UtilMining;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemRepairingGel extends Item {

    public ItemRepairingGel() {

    }

    /**
     * Called when a Block is right-clicked with this Item
     */
    public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack itemstack = player.getHeldItem(hand);

        if (!player.canPlayerEdit(pos.offset(facing), facing, itemstack)) {
            return EnumActionResult.FAIL;
        } else {
            if (!worldIn.isRemote) {
                if (player.isCreative() && player.isSneaking()) {
                    IBlockState state = worldIn.getBlockState(pos);
                    if (UtilMining.canMineBlock(worldIn, pos, state.getBlock())/* &&
                            UtilMining.canConvertToRepairingBlock(worldIn, state)*/) {
                        TileEntityRepairingBlock.replaceBlockAndBackup(worldIn, pos);
                    }
                } else {
                    TileEntity tEnt = worldIn.getTileEntity(pos);
                    if (tEnt instanceof TileEntityRepairingBlock) {

                        ((TileEntityRepairingBlock) tEnt).restoreBlock();

                        if (!player.capabilities.isCreativeMode) {
                            itemstack.shrink(1);
                        }

                        return EnumActionResult.SUCCESS;
                    } else {
                        return EnumActionResult.PASS;
                    }
                }
            }
        }

        return EnumActionResult.PASS;
    }
}
