package net.kazi.kazimod.init;

import net.kazi.kazimod.entities.MalevolentShrineEntity;
import net.kazi.kazimod.entities.ShadowDoppelmanEntity;
import net.kazi.kazimod.entities.VegapunkTraderEntity;
import net.kazi.kazimod.entities.boss.luffy.LuffyBossEntity;
import net.kazi.kazimod.entities.boss.gojo.GojoBossEntity;
import net.kazi.kazimod.entities.boss.sukuna.SukunaBossEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;

public class KaziEntityAttributes {

    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(KaziEntities.MALEVOLENT_SHRINE.get(),
                MalevolentShrineEntity.createAttributes().build());

        event.put(KaziEntities.GOJO_BOSS.get(),
                GojoBossEntity.createAttributes().build());

        event.put(KaziEntities.SUKUNA_BOSS.get(),
                SukunaBossEntity.createAttributes().build());

        event.put(KaziEntities.LUFFY_BOSS.get(),
                LuffyBossEntity.createAttributes().build());

        event.put(KaziEntities.VEGAPUNK_TRADER.get(),
                VegapunkTraderEntity.createAttributes().build());

        event.put(KaziEntities.SHADOW_DOPPELMAN.get(),
                ShadowDoppelmanEntity.createAttributes().build());
    }
}
