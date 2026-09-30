package net.kazi.kazimod.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class KaziConfig {

    public static final ForgeConfigSpec SPEC;
    public static final KaziConfig INSTANCE;

    public final ForgeConfigSpec.BooleanValue disableFruitChanges;
    public final ForgeConfigSpec.IntValue gearFifthMaxHoldTime;
    public final ForgeConfigSpec.BooleanValue disableVegapunkSpawns;
    public final ForgeConfigSpec.BooleanValue awakeningEssenceRequirePlayerKills;
    public final ForgeConfigSpec.IntValue awakeningEssenceRequiredPlayerKills;
    public final ForgeConfigSpec.BooleanValue disableCustomAwakeningMessages;
    public final ForgeConfigSpec.IntValue hakiSenseRange;

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

        gearFifthMaxHoldTime = builder
                .comment(
                        "Maximum hold time for Gear Fifth Rework, in ticks.",
                        "20 ticks = 1 second.",
                        "Default: 1200 (60 seconds).",
                        "No longer even enabled this config is redundent"
                )
                .defineInRange("gearFifthMaxHoldTime", 1200, 20, 72000);

        disableVegapunkSpawns = builder
                .comment(
                        "Set to true to completely disable Vegapunk Trader spawning.",
                        "Default: false."
                )
                .define("disableVegapunkSpawns", false);

        awakeningEssenceRequirePlayerKills = builder
                .comment(
                        "Set to true to require player kills before Awakening Essence can be used.",
                        "Default: false."
                )
                .define("awakeningEssenceRequirePlayerKills", false);

        awakeningEssenceRequiredPlayerKills = builder
                .comment(
                        "How many player kills are required to use Awakening Essence when the requirement is enabled.",
                        "Default: 5."
                )
                .defineInRange("awakeningEssenceRequiredPlayerKills", 5, 0, 100000);

        builder.comment("Awakening announcement settings.").push("awakeningMessages");

        disableCustomAwakeningMessages = builder
                .comment(
                        "Set to true to disable fruit-specific awakening messages.",
                        "When disabled, every awakening uses: (PlayerName) Has awakened their devil fruit!",
                        "Default: false."
                )
                .define("disableCustomMessages", false);

        builder.pop();

        builder.comment("Haki Sense settings.").push("hakiSense");

        hakiSenseRange = builder
                .comment(
                        "Maximum distance, in blocks, at which Haki Sense can detect other players.",
                        "Default: 1000.",
                        "Changes apply the next time Haki Sense is used."
                )
                .defineInRange("range", 1000, 1, 100000);

        builder.pop();

        builder.pop();
    }
}
