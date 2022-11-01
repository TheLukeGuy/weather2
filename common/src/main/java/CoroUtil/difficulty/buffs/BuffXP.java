package CoroUtil.difficulty.buffs;

import CoroUtil.difficulty.UtilEntityBuffs;
import CoroUtil.difficulty.data.cmods.CmodXP;
import net.minecraft.entity.EntityCreature;

/**
 * Created by Corosus on 1/9/2017.
 */
public class BuffXP extends BuffBase {

    @Override
    public String getTagName() {
        return UtilEntityBuffs.dataEntityBuffed_XP;
    }

    @Override
    public boolean applyBuff(EntityCreature ent, float difficulty) {

        CmodXP cmod = (CmodXP) UtilEntityBuffs.getCmodData(ent, getTagName());

        if (cmod != null) {
            //set base value if we need to
            if (cmod.base_value != -1) {
                ent.experienceValue = (int) cmod.base_value;
            }
            double extraMultiplier = (/*1F + */difficulty * cmod.difficulty_multiplier);
            ent.experienceValue += (int) ((double) ent.experienceValue * extraMultiplier);
        }


        return super.applyBuff(ent, difficulty);
    }

    @Override
    public void applyBuffFromReload(EntityCreature ent, float difficulty) {
        applyBuff(ent, difficulty);
    }
}
