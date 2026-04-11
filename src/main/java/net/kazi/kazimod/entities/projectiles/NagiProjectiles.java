package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.EntityType;
import net.minecraftforge.fml.RegistryObject;

public class NagiProjectiles {

    public static RegistryObject<EntityType<SilentSliceProjectile>> SILENT_SLICE =
            KaziEntities.SILENT_SLICE;

    public static RegistryObject<EntityType<SilentDeathProjectile>> SILENT_DEATH =
            KaziEntities.SILENT_DEATH;
}
