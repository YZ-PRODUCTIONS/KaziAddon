package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.EntityType;
import net.minecraftforge.fml.RegistryObject;

public class AkumaProjectiles {

    public static RegistryObject<EntityType<HellblazeProjectile>> HELLBLAZE =
            KaziEntities.HELLBLAZE_PROJECTILE;

    public static RegistryObject<EntityType<SlashWaveProjectile>> SLASH_WAVE =
            KaziEntities.SLASH_WAVE;

    public static RegistryObject<EntityType<DarkSpearProjectile>> DARK_SPEAR =
            KaziEntities.DARK_SPEAR;
}
