package net.kazi.kazimod.init;

import net.kazi.kazimod.animations.kama.SukunaDomainAnimation;
import net.kazi.kazimod.animations.kama.FugaSukunaAnimation;
import net.kazi.kazimod.animations.tenki.LightningFuryAnimation;
import net.kazi.kazimod.animations.toki.ChronostasisAnimation;
import net.kazi.kazimod.animations.toki.TimeTheftAnimation;
import net.minecraft.util.ResourceLocation;
import xyz.pixelatedw.mineminenomi.api.animations.Animation;
import xyz.pixelatedw.mineminenomi.api.animations.AnimationId;

public class KaziAnimations {

    public static final AnimationId<SukunaDomainAnimation>  SUKUNA_DOMAIN   = register("sukuna_domain");
    public static final AnimationId<FugaSukunaAnimation>    FUGA_SUKUNA     = register("fuga_sukuna");
    public static final AnimationId<LightningFuryAnimation> LIGHTNING_FURY  = register("lightning_fury");
    public static final AnimationId<ChronostasisAnimation>  CHRONOSTASIS    = register("chronostasis");
    public static final AnimationId<TimeTheftAnimation>     TIME_THEFT      = register("time_theft");

    public KaziAnimations() {
    }

    private static <A extends Animation<?, ?>> AnimationId<A> register(String name) {
        return new AnimationId(new ResourceLocation("kazimod", name));
    }

    public static void clientInit() {
        AnimationId.register(new SukunaDomainAnimation(SUKUNA_DOMAIN));
        AnimationId.register(new FugaSukunaAnimation(FUGA_SUKUNA));
        AnimationId.register(new LightningFuryAnimation(LIGHTNING_FURY));
        AnimationId.register(new ChronostasisAnimation(CHRONOSTASIS));
        AnimationId.register(new TimeTheftAnimation(TIME_THEFT));
    }
}