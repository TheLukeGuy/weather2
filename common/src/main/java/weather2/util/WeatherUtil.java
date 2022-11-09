package weather2.util;

import net.minecraft.block.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tag.BlockTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import weather2.CommonProxy;
import weather2.config.ConfigTornado;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Set;

public class WeatherUtil {
    public static HashMap<Block, Boolean> blockIDToUseMapping = new HashMap<>();

    //Terrain grabbing
    public static boolean shouldGrabBlock(World parWorld, BlockState state) {
        try {
            ItemStack itemStr = new ItemStack(Items.DIAMOND_AXE);

            Block block = state.getBlock();

            boolean result = true;

            if (ConfigTornado.Storm_Tornado_GrabCond_List) {
                try {

                    if (!ConfigTornado.Storm_Tornado_GrabListBlacklistMode) {
                        if (!blockIDToUseMapping.get(block)) {
                            result = false;
                        }
                    } else {
                        if (blockIDToUseMapping.get(block)) {
                            result = false;
                        }
                    }
                } catch (Exception e) {
                    //sometimes NPEs, just assume false if so
                    result = false;
                }
            } else {
                if (ConfigTornado.Storm_Tornado_GrabCond_StrengthGrabbing) {
                    float strMin = 0.0F;
                    float strMax = 0.74F;

                    if (block == null) {
                        return false; //force return false to prevent unchecked future code outside scope
                    } else {
                        float strVsBlock = block.getDefaultState().getHardness(parWorld, new BlockPos(0, 0, 0)) - (((itemStr.getMiningSpeedMultiplier(block.getDefaultState()) - 1) / 4F));

                        Material material = block.getDefaultState().getMaterial();
                        if ((strVsBlock <= strMax && strVsBlock >= strMin) ||
                                (material == Material.WOOD) ||
                                material == Material.WOOL ||
                                material == Material.PLANT ||
                                material == Material.REPLACEABLE_PLANT ||
                                block instanceof FernBlock) {
                            if (!safetyCheck(block)) {
                                result = false;
                            }
                        } else {
                            result = false;
                        }
                    }
                }

                if (ConfigTornado.Storm_Tornado_RefinedGrabRules) {
                    if (block == Blocks.DIRT || block == Blocks.GRASS || block == Blocks.SAND || block.isIn(BlockTags.LOGS)/* || block.blockMaterial == Material.wood*/) {
                        result = false;
                    }
                    if (!WeatherUtilCompatibility.canTornadoGrabBlockRefinedRules(state)) {
                        result = false;
                    }
                }
            }

            if (block == CommonProxy.blockWeatherMachine) {
                result = false;
            }

            return result;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public static boolean safetyCheck(Block id) {
        return id != Blocks.BEDROCK && !id.isIn(BlockTags.LOGS) && id != Blocks.CHEST && id != Blocks.JUKEBOX;
    }

    public static boolean shouldRemoveBlock(Block blockID) {
        return blockID.getDefaultState().getMaterial() != Material.WATER;
    }

    public static void doBlockList() {
        //System.out.println("1.8 TODO: verify block list lookup matching for exact comparions");

        blockIDToUseMapping.clear();
        //System.out.println("Blacklist: ");
        String[] splEnts = ConfigTornado.Storm_Tornado_GrabList.split(",");
        //int[] blocks = new int[splEnts.length];

        if (splEnts.length > 0) {
            for (int i = 0; i < splEnts.length; i++) {
                splEnts[i] = splEnts[i].trim();
            }
        }

        blockIDToUseMapping.put(Blocks.AIR, false);

        Set<Identifier> set = Registry.BLOCK.getIds();
        for (Identifier tagName : set) {
            Block block = Registry.BLOCK.get(tagName);
            //if (dbgShow) System.out.println("??? " + Block.REGISTRY.getNameForObject(block));

            boolean foundEnt = false;

            for (String splEnt : splEnts) {
                if (ConfigTornado.Storm_Tornado_GrabCond_List_PartialMatches) {
                    if (tagName.toString().contains(splEnt)) {
                        foundEnt = true;
                        break;
                    }
                } else {
                    Block blockEntry = Registry.BLOCK.get(new Identifier(splEnt));
                    if (block == blockEntry) {
                        foundEnt = true;
                        break;
                    }
                }
            }

            blockIDToUseMapping.put(block, foundEnt);
        }
    }

    public static boolean isAprilFoolsDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());

        //test
        //return calendar.get(Calendar.MONTH) == Calendar.MARCH && calendar.get(Calendar.DAY_OF_MONTH) == 25;

        return calendar.get(Calendar.MONTH) == Calendar.APRIL && calendar.get(Calendar.DAY_OF_MONTH) == 1;
    }
}
