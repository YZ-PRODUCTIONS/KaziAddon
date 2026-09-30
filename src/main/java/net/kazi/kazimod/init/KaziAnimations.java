package net.kazi.kazimod.init;

import net.kazi.kazimod.animations.gojo.GojoBlueAnimation;
import net.kazi.kazimod.animations.gojo.GojoDomainAnimation;
import net.kazi.kazimod.animations.gojo.GojoHollowPurpleAnimation;
import net.kazi.kazimod.animations.gojo.GojoRedAnimation;
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
    public static final AnimationId<GojoBlueAnimation>      GOJO_BLUE       = register("gojo_blue");
    public static final AnimationId<GojoRedAnimation>       GOJO_RED        = register("gojo_red");
    public static final AnimationId<GojoDomainAnimation>    GOJO_DOMAIN     = register("gojo_domain");
    public static final AnimationId<GojoHollowPurpleAnimation> GOJO_HOLLOW_PURPLE = register("gojo_hollow_purple");


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
        AnimationId.register(new GojoBlueAnimation(GOJO_BLUE));
        AnimationId.register(new GojoRedAnimation(GOJO_RED));
        AnimationId.register(new GojoDomainAnimation(GOJO_DOMAIN));
        AnimationId.register(new GojoHollowPurpleAnimation(GOJO_HOLLOW_PURPLE));

    }
}
