package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.init.KaziEntities;
import net.minecraft.entity.EntityType;
import net.minecraftforge.fml.RegistryObject;

public class CasinoProjectiles {
    public static RegistryObject<EntityType<CasinoChipProjectile>> CHIP=KaziEntities.CASINO_CHIP;

    public static RegistryObject<EntityType<CoinProjectile>> COIN =
            KaziEntities.CASINO_COIN;

    public static RegistryObject<EntityType<DiceProjectile>> DICE =
            KaziEntities.CASINO_DICE;

    public static RegistryObject<EntityType<PlayingCardProjectile>> PLAYING_CARD =
            KaziEntities.CASINO_PLAYING_CARD;
}
