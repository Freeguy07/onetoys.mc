package com.onetoys.client;

import com.onetoys.module.gameplay.mobs.goal.SlipperySlimeDashGoal;
import com.onetoys.registry.OnetoysAttachments;
import net.minecraft.world.entity.monster.Slime;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

public class SpecialMobClientHelper {

    public static void onClientEntityTick(EntityTickEvent.Pre event) {
        if (!event.getEntity().level().isClientSide()) {
            return;
        }

        if (event.getEntity() instanceof Slime slime) {
            int swell = slime.getData(OnetoysAttachments.SLIME_SWELL_TICKS);
            if (swell > 0) {
                if (swell < SlipperySlimeDashGoal.MAX_SWELL_TICKS) {
                    slime.setData(OnetoysAttachments.SLIME_SWELL_TICKS, swell + 1);
                } else if (swell > SlipperySlimeDashGoal.MAX_SWELL_TICKS + 15) {
                    slime.setData(OnetoysAttachments.SLIME_SWELL_TICKS, 0);
                }
            }
        }
    }
}
