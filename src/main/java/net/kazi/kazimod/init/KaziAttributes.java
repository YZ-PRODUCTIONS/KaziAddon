package net.kazi.kazimod.init;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class KaziAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, "kazimod");

    public static final RegistryObject<Attribute> SIZE =
            ATTRIBUTES.register("size",
                    () -> new RangedAttribute("attribute.name.kazimod.size", 1.0, 0.0, 256.0)
                            .setSyncable(true));

    @Mod.EventBusSubscriber(modid = "kazimod", bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class Setup {
        @SubscribeEvent
        public static void onEntityConstruct(EntityAttributeModificationEvent event) {
            for (EntityType<? extends LivingEntity> type : event.getTypes()) {
                event.add(type, KaziAttributes.SIZE.get());
            }
        }
    }
}