package net.kazi.kazimod.longbob;

import net.minecraft.entity.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.*;

public final class LongbobFeatures {
    private static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(ForgeRegistries.ENTITIES,"kazimod");
    public static final RegistryObject<EntityType<LongbobEntity>> BOSS=TYPES.register("longbobthegod",()->
            EntityType.Builder.<LongbobEntity>of(LongbobEntity::new,EntityClassification.MONSTER).sized(1.15F,2.9F).clientTrackingRange(16).updateInterval(2).build("longbobthegod"));
    public static final RegistryObject<EntityType<LongbobProjectile>> PROJECTILE=TYPES.register("longbob_poison",()->
            EntityType.Builder.<LongbobProjectile>of(LongbobProjectile::new,EntityClassification.MISC).sized(.7F,.7F).clientTrackingRange(16).updateInterval(1).setShouldReceiveVelocityUpdates(true).noSave().build("longbob_poison"));
    public static final RegistryObject<EntityType<LongbobCloud>> CLOUD=TYPES.register("longbob_poison_cloud",()->
            EntityType.Builder.<LongbobCloud>of(LongbobCloud::new,EntityClassification.MISC).sized(.1F,.1F).clientTrackingRange(16).updateInterval(20).noSave().build("longbob_poison_cloud"));
    public static void init(IEventBus bus){
        TYPES.register(bus);bus.addListener((EntityAttributeCreationEvent e)->e.put(BOSS.get(),LongbobEntity.createAttributes().build()));
        MinecraftForge.EVENT_BUS.addListener(LongbobSpawning::tick);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->LongbobClient.init(bus));
    }
    private LongbobFeatures(){}
}
