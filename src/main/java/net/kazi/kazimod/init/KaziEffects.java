package net.kazi.kazimod.init;

import net.kazi.kazimod.api.KaziRegistry;
import net.kazi.kazimod.effects.*;
import net.minecraft.potion.Effect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;

public class KaziEffects {
<<<<<<< HEAD
    public static final RegistryObject<Effect> BLOOD_POISON = KaziRegistry.EFFECTS.register("blood_poison", BloodPoisonEffect::new);
=======
    public static final RegistryObject<Effect> IMAGINE_BROKEN = KaziRegistry.EFFECTS.register(
            "imagine_broken", net.kazi.kazimod.abilities.NagiAwakening.ImagineBrokenEffect::new);
>>>>>>> 25f114e1b87ab4440534026453f3d39ec350d021
    public static final RegistryObject<Effect> HEAVEN_BOUND = KaziRegistry.EFFECTS.register("heaven_bound", HeavenBoundEffect::new);

    public static final RegistryObject<Effect> KIRIN_SLEEP = KaziRegistry.EFFECTS.register("kirin_sleep", KirinSleepEffect::new);

    public static final RegistryObject<Effect> LOST_WORKS_FRUIT_LOCK = KaziRegistry.EFFECTS.register("lost_works_fruit_lock", LostWorksFruitLockEffect::new);
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
