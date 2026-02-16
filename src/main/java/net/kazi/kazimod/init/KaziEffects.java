package net.kazi.kazimod.init;

import net.kazi.kazimod.api.KaziRegistry;
import net.kazi.kazimod.effects.FlamingRotEffect;
import net.kazi.kazimod.effects.TiredEffect;
import net.kazi.kazimod.effects.WeakenedMovement;
import net.minecraft.potion.Effect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;

public class KaziEffects {

    public static final RegistryObject<Effect> WEAKENED_MOVEMENT = KaziRegistry.EFFECTS.register("weakened_movement", WeakenedMovement::new);
    public static final RegistryObject<Effect> TIRED = KaziRegistry.EFFECTS.register("tired", TiredEffect::new);
    public static final RegistryObject<Effect> FLAMING_ROT = KaziRegistry.EFFECTS.register("flaming_rot", FlamingRotEffect::new);

    public KaziEffects() {
    }

    public static void register(IEventBus eventBus) {
        KaziRegistry.EFFECTS.register(eventBus);
    }
}