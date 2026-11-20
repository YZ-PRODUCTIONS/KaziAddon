package net.kazi.kazimod.hermano;

import net.minecraft.entity.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.*;

public final class HermanoFeatures {
    private static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(ForgeRegistries.ENTITIES,"kazimod");
    public static final RegistryObject<EntityType<HermanoEntity>> BOSS=TYPES.register("el_hermano",()->
            EntityType.Builder.<HermanoEntity>of(HermanoEntity::new,EntityClassification.MONSTER)
                    .sized(1.25F,3.6F).clientTrackingRange(16).updateInterval(2).fireImmune().build("el_hermano"));
    public static final RegistryObject<EntityType<HermanoProjectile>> IMPACT=TYPES.register("hermano_power_impact",()->
            EntityType.Builder.<HermanoProjectile>of(HermanoProjectile::new,EntityClassification.MISC)
                    .sized(.8F,.8F).clientTrackingRange(16).updateInterval(1).setShouldReceiveVelocityUpdates(true).noSave().build("hermano_power_impact"));
    public static final RegistryObject<EntityType<HermanoEffect>> EFFECT=TYPES.register("hermano_effect",()->
            EntityType.Builder.<HermanoEffect>of(HermanoEffect::new,EntityClassification.MISC)
                    .sized(.1F,.1F).clientTrackingRange(16).updateInterval(20).noSave().build("hermano_effect"));
    public static void init(IEventBus bus){
        TYPES.register(bus);
        bus.addListener((EntityAttributeCreationEvent e)->e.put(BOSS.get(),HermanoEntity.createAttributes().build()));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->HermanoClient.init(bus));
    }
    private HermanoFeatures(){}
}
