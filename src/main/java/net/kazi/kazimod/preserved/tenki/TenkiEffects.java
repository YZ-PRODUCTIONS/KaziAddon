package net.kazi.kazimod.preserved.tenki;

import net.minecraft.entity.*;
import net.minecraftforge.registries.*;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.eventbus.api.IEventBus;

public final class TenkiEffects {
    private static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(ForgeRegistries.ENTITIES,"kazimod");
    public static final RegistryObject<EntityType<TenkiVisualEntity>> EFFECT=TYPES.register("tenki_weather_vfx",()->EntityType.Builder.<TenkiVisualEntity>of(TenkiVisualEntity::new,EntityClassification.MISC).sized(1,1).clientTrackingRange(16).updateInterval(2).noSave().build("tenki_weather_vfx"));
    public static void init(IEventBus bus){TYPES.register(bus);}
    public static void attach(Entity source,int kind,float radius){
        if(source.level.isClientSide||source.getPersistentData().getBoolean("KaziTenkiVfx"))return;
        source.getPersistentData().putBoolean("KaziTenkiVfx",true);
        TenkiVisualEntity.spawn(source,kind,radius,0);
    }
}
