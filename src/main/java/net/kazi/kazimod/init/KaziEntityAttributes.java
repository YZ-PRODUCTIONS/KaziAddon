package net.kazi.kazimod.init;

import net.kazi.kazimod.entities.MalevolentShrineEntity;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "kazimod", bus = Mod.EventBusSubscriber.Bus.MOD)
public class
KaziEntityAttributes {

    @SubscribeEvent
    public static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(KaziEntities.MALEVOLENT_SHRINE.get(),
                MalevolentShrineEntity.createAttributes().build());
    }
}