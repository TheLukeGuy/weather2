package CoroUtil.bt.actions;

import CoroUtil.bt.Behavior;
import CoroUtil.bt.EnumBehaviorState;
import CoroUtil.bt.leaf.LeafAction;

import java.util.Random;

public class RandomChance extends LeafAction {

    public RandomChance(Behavior parParent) {
        super(parParent);
    }

    @Override
    public EnumBehaviorState tick() {
        Random rand = new Random();
        Boolean bool = rand.nextBoolean();
        //bool = false;
        dbg("Leaf Rand Tick - " + bool);
        return bool ? EnumBehaviorState.SUCCESS : EnumBehaviorState.FAILURE;
    }

}
