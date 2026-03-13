package net.kazi.kazimod.init;

import net.kazi.kazimod.api.KaziRegistry;
import net.kazi.kazimod.effects.*;
import net.minecraft.potion.Effect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;

public class KaziEffects {

    public static final RegistryObject<Effect> WEAKENED_MOVEMENT  = KaziRegistry.EFFECTS.register("weakened_movement",  WeakenedMovement::new);
    public static final RegistryObject<Effect> ENHANCED_MOVEMENT  = KaziRegistry.EFFECTS.register("enhanced_movement",  EnhancedMovement::new);
    public static final RegistryObject<Effect> TIRED              = KaziRegistry.EFFECTS.register("tired",              TiredEffect::new);
    public static final RegistryObject<Effect> FLAMING_ROT        = KaziRegistry.EFFECTS.register("flaming_rot",        FlamingRotEffect::new);
    public static final RegistryObject<Effect> BOUNCY             = KaziRegistry.EFFECTS.register("bouncy",             BouncyEffect::new);
    public static final RegistryObject<Effect> MINIATURIZED       = KaziRegistry.EFFECTS.register("miniaturized",       MiniaturizedEffect::new);

    public KaziEffects() {
    }

    public static void register(IEventBus eventBus) {
        KaziRegistry.EFFECTS.register(eventBus);
    }
}