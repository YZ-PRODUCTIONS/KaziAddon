package net.kazi.kazimod.init;

import net.minecraft.util.ResourceLocation;

public class KaziResources {

    public static final ResourceLocation Dismantle;
    public static final ResourceLocation Wind;
    public static final ResourceLocation SpiderwebCleave;
    public static final ResourceLocation Bakugo;
    public static final ResourceLocation Fuga;
    public static final ResourceLocation GreenSweep;
    public static final ResourceLocation GreenTornado;
    public static final ResourceLocation GojoRed;
    public static final ResourceLocation GojoBlue;
    public static final ResourceLocation GojoPurple;
    public static final ResourceLocation InfiniteVoid;

    /** Texture for the domain opening red streak particle. */
    public static final ResourceLocation InfiniteVoidStreak;
    /** Texture for the domain opening pink/magenta streak particle. */
    public static final ResourceLocation InfiniteVoidStreakPink;

    public static final ResourceLocation LuckySlot0;
    public static final ResourceLocation LuckySlot1;
    public static final ResourceLocation LuckySlot2;
    public static final ResourceLocation LuckySlot3;
    public static final ResourceLocation LuckySlot4;
    public static final ResourceLocation LuckySlot5;
    public static final ResourceLocation LuckySlot6;
    public static final ResourceLocation LuckySlot7;
    public static final ResourceLocation LuckySlot8;
    public static final ResourceLocation LuckySlot9;
    public static final ResourceLocation Coin;
    public static final ResourceLocation CasinoChip;
    public static final ResourceLocation PlayingCard;
    public static final ResourceLocation PetalBlade;

    static {
        Dismantle              = new ResourceLocation("kazimod", "textures/particle/dismantle.png");
        Wind                   = new ResourceLocation("kazimod", "textures/particle/wind.png");
        SpiderwebCleave        = new ResourceLocation("kazimod", "textures/particle/spiderweb_cleave.png");
        Bakugo                 = new ResourceLocation("kazimod", "textures/particle/bakugo.png");
        Fuga                   = new ResourceLocation("kazimod", "textures/particle/fuga_particle.png");
        GreenSweep             = new ResourceLocation("kazimod", "textures/particle/green_sweep_particle.png");
        GreenTornado           = new ResourceLocation("kazimod", "textures/particle/green_tornado_particle.png");  // note: was missing from original static block but field exists
        GojoRed                = new ResourceLocation("kazimod", "textures/particle/gojored.png");
        GojoBlue               = new ResourceLocation("kazimod", "textures/particle/gojo_blue.png");
        GojoPurple             = new ResourceLocation("kazimod", "textures/particle/gojopurple.png");
        InfiniteVoid           = new ResourceLocation("kazimod", "textures/particle/infinite_void.png");
        // New textures for domain streak effect
        InfiniteVoidStreak     = new ResourceLocation("kazimod", "textures/particle/infinite_void_streak_red.png");
        InfiniteVoidStreakPink = new ResourceLocation("kazimod", "textures/particle/gojopink.png");
        LuckySlot0             = new ResourceLocation("kazimod", "textures/particle/lucky_slot_0.png");
        LuckySlot1             = new ResourceLocation("kazimod", "textures/particle/lucky_slot_1.png");
        LuckySlot2             = new ResourceLocation("kazimod", "textures/particle/lucky_slot_2.png");
        LuckySlot3             = new ResourceLocation("kazimod", "textures/particle/lucky_slot_3.png");
        LuckySlot4             = new ResourceLocation("kazimod", "textures/particle/lucky_slot_4.png");
        LuckySlot5             = new ResourceLocation("kazimod", "textures/particle/lucky_slot_5.png");
        LuckySlot6             = new ResourceLocation("kazimod", "textures/particle/lucky_slot_6.png");
        LuckySlot7             = new ResourceLocation("kazimod", "textures/particle/lucky_slot_7.png");
        LuckySlot8             = new ResourceLocation("kazimod", "textures/particle/lucky_slot_8.png");
        LuckySlot9             = new ResourceLocation("kazimod", "textures/particle/lucky_slot_9.png");
        Coin                   = new ResourceLocation("kazimod", "textures/particle/coin.png");
        CasinoChip             = new ResourceLocation("kazimod", "textures/particle/casino_chip.png");
        PlayingCard            = new ResourceLocation("kazimod", "textures/particle/playing_card.png");
        PetalBlade             = new ResourceLocation("kazimod", "textures/particle/petal_blade.png");
    }
}
