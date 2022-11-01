package CoroUtil.util;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.InvalidBlockStateException;
import net.minecraft.command.NumberInvalidException;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CoroUtilBlockState {

    private static final Splitter COMMA_SPLITTER = Splitter.on(',').trimResults();
    private static final Splitter EQUAL_SPLITTER = Splitter.on('=').limit(2).trimResults();

    public static class StateImplementationImpl extends BlockStateContainer.StateImplementation {
        protected StateImplementationImpl(Block blockIn, ImmutableMap<IProperty<?>, Comparable<?>> propertiesIn) {
            super(blockIn, propertiesIn);
        }
    }

    public static IBlockState getStatelessBlock(Block block) {
        ImmutableMap.Builder<IProperty<?>, Comparable<?>> builder = ImmutableMap.builder();
        return new CoroUtilBlockState.StateImplementationImpl(block, builder.build());
    }

    public static boolean partialStateInListMatchesFullState(IBlockState fullState, List<IBlockState> listStates) {
        for (IBlockState partialState : listStates) {
            if (partialStateMatchesFullState(partialState, fullState)) {
                return true;
            }
        }
        return false;
    }

    public static boolean partialStateMatchesFullState(IBlockState partialState, IBlockState fullState) {
        if (partialState.getBlock() != fullState.getBlock()) return false;
        //ImmutableSet<IProperty<?>> temp = partialState.getProperties().keySet();
        for (IProperty<?> prop : partialState.getProperties().keySet()) {
            if (partialState.getValue(prop) != fullState.getValue(prop)) {
                return false;
            }
        }
        return true;
    }

    public static IBlockState convertArgToBlockState(Block p_190794_0_, String p_190794_1_) throws NumberInvalidException, InvalidBlockStateException {
        return CommandBase.convertArgToBlockState(p_190794_0_, p_190794_1_);
    }

    /**
     * This and methods below it are copied from CommandBase, with the key modification to not use getDefaultState
     *
     * @param p_190794_0_
     * @param p_190794_1_
     * @return
     * @throws NumberInvalidException
     * @throws InvalidBlockStateException
     */
    public static IBlockState convertArgToPartialBlockState(Block p_190794_0_, String p_190794_1_) throws NumberInvalidException, InvalidBlockStateException {
        try {
            int i = Integer.parseInt(p_190794_1_);

            if (i < 0) {
                throw new NumberInvalidException("commands.generic.num.tooSmall", new Object[]{i, Integer.valueOf(0)});
            } else if (i > 15) {
                throw new NumberInvalidException("commands.generic.num.tooBig", new Object[]{i, Integer.valueOf(15)});
            } else {
                return p_190794_0_.getStateFromMeta(Integer.parseInt(p_190794_1_));
            }
        } catch (RuntimeException var7) {
            try {
                Map<IProperty<?>, Comparable<?>> map = getBlockStatePropertyValueMap(p_190794_0_, p_190794_1_);

                /**
                 * MAIN CHANGES
                 */
                ImmutableMap.Builder<IProperty<?>, Comparable<?>> builder = ImmutableMap.builder();
                //IBlockState iblockstate = p_190794_0_.getDefaultState();

                //builder.put(BlockDoublePlant.VARIANT, BlockDoublePlant.EnumPlantType.SUNFLOWER);

                //need to convert from Immutable
                for (Map.Entry<IProperty<?>, Comparable<?>> entry : map.entrySet()) {
                    //iblockstate = getBlockState(iblockstate, entry.getKey(), entry.getValue());
                    builder.put(entry.getKey(), entry.getValue());
                }

                IBlockState iblockstate = new CoroUtilBlockState.StateImplementationImpl(p_190794_0_, builder.build());

                //IBlockState statePartial = new CoroUtilBlockState.StateImplementationImpl(p_190794_0_, map);

                return iblockstate;
                //return statePartial;
            } catch (RuntimeException var6) {
                throw new InvalidBlockStateException("commands.generic.blockstate.invalid", new Object[]{p_190794_1_, Block.REGISTRY.getNameForObject(p_190794_0_)});
            }
        }
    }

    private static Map<IProperty<?>, Comparable<?>> getBlockStatePropertyValueMap(Block p_190795_0_, String p_190795_1_) throws InvalidBlockStateException {
        Map<IProperty<?>, Comparable<?>> map = Maps.<IProperty<?>, Comparable<?>>newHashMap();

        if ("default".equals(p_190795_1_)) {
            return p_190795_0_.getDefaultState().getProperties();
        } else {
            BlockStateContainer blockstatecontainer = p_190795_0_.getBlockState();
            Iterator iterator = COMMA_SPLITTER.split(p_190795_1_).iterator();

            while (true) {
                if (!iterator.hasNext()) {
                    return map;
                }

                String s = (String) iterator.next();
                Iterator<String> iterator1 = EQUAL_SPLITTER.split(s).iterator();

                if (!iterator1.hasNext()) {
                    break;
                }

                IProperty<?> iproperty = blockstatecontainer.getProperty(iterator1.next());

                if (iproperty == null || !iterator1.hasNext()) {
                    break;
                }

                Comparable<?> comparable = getValueHelper(iproperty, iterator1.next());

                if (comparable == null) {
                    break;
                }

                map.put(iproperty, comparable);
            }

            throw new InvalidBlockStateException("commands.generic.blockstate.invalid", new Object[]{p_190795_1_, Block.REGISTRY.getNameForObject(p_190795_0_)});
        }
    }

    private static <T extends Comparable<T>> IBlockState getBlockState(IBlockState p_190793_0_, IProperty<T> p_190793_1_, Comparable<?> p_190793_2_) {
        return p_190793_0_.withProperty(p_190793_1_, (T) p_190793_2_);
    }

    @Nullable
    private static <T extends Comparable<T>> T getValueHelper(IProperty<T> p_190792_0_, String p_190792_1_) {
        return (T) (p_190792_0_.parseValue(p_190792_1_).orNull());
    }
}
