package net.kazi.kazimod.jerome;

import net.minecraft.entity.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.*;

public final class JeromeFeatures {
    private static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(ForgeRegistries.ENTITIES,"kazimod");
    public static final RegistryObject<EntityType<JeromeEntity>> BOSS=TYPES.register("jerome",()->
            EntityType.Builder.<JeromeEntity>of(JeromeEntity::new,EntityClassification.MONSTER)
                    .sized(1.65F,3.4F).clientTrackingRange(16).updateInterval(2).build("jerome"));
    public static final RegistryObject<EntityType<JeromeProjectile>> SPHERE=TYPES.register("jerome_power_impact",()->
            EntityType.Builder.<JeromeProjectile>of(JeromeProjectile::new,EntityClassification.MISC)
                    .sized(.8F,.8F).clientTrackingRange(16).updateInterval(1).setShouldReceiveVelocityUpdates(true).noSave().build("jerome_power_impact"));
    public static final RegistryObject<EntityType<JeromeEffect>> EFFECT=TYPES.register("jerome_effect",()->
            EntityType.Builder.<JeromeEffect>of(JeromeEffect::new,EntityClassification.MISC)
                    .sized(.1F,.1F).clientTrackingRange(16).updateInterval(20).noSave().build("jerome_effect"));
    public static void init(IEventBus bus){
        TYPES.register(bus);
        bus.addListener((EntityAttributeCreationEvent e)->e.put(BOSS.get(),JeromeEntity.createAttributes().build()));
        MinecraftForge.EVENT_BUS.addListener(JeromeSpawning::tick);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->JeromeClient.init(bus));
    }
    private JeromeFeatures(){}
}


