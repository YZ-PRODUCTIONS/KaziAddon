package net.kazi.kazimod.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class KaziConfig {

    public static final ForgeConfigSpec SPEC;
    public static final KaziConfig INSTANCE;

    public final ForgeConfigSpec.BooleanValue disableFruitChanges;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        INSTANCE = new KaziConfig(builder);
        SPEC = builder.build();
    }

    private KaziConfig(ForgeConfigSpec.Builder builder) {
        builder.comment("KaziMod Configuration").push("kazimod");

        disableFruitChanges = builder
                .comment(
                        "Set to true to disable ALL fruit ability reworks and ALL custom fruits added by KaziMod.",
                        "Default: false (all changes are active).",
                        "Requires a restart to take effect."
                )
                .define("disableFruitChanges", false);

        builder.pop();
    }
}