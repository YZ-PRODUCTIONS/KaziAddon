package local.autointentdodge;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import org.apache.commons.lang3.tuple.Pair;

@Mod(AutoIntentDodge.ID)
public final class AutoIntentDodge {
    static final String ID = "autointentdodge";
    static final ForgeConfigSpec.BooleanValue SINGLEPLAYER;
    static final ForgeConfigSpec.BooleanValue MULTIPLAYER;
    static final ForgeConfigSpec.BooleanValue PROJECTILES;
    static final ForgeConfigSpec.BooleanValue CHARGED_ATTACKS;
    static final ForgeConfigSpec.IntValue LOOKAHEAD;
    private static final ForgeConfigSpec CONFIG;

    static {
        ForgeConfigSpec.Builder config = new ForgeConfigSpec.Builder();
        SINGLEPLAYER = config.comment("Use Intent Sense for eligible incoming hits in private worlds.")
                .define("singleplayer", true);
        MULTIPLAYER = config.comment("Enable automatic Intent Sense Dodge on servers and LAN worlds, including the after-hit fallback.")
                .define("multiplayer", true);
        PROJECTILES = config.comment("Anticipate arrows, tridents and Mine Mine no Mi ability projectiles on a collision course.")
                .define("anticipateProjectiles", true);
        CHARGED_ATTACKS = config.comment("Anticipate synced damaging abilities nearing full charge within 6 blocks and aimed at you. Feints can waste a dodge.")
                .define("anticipateChargedAttacks", true);
        LOOKAHEAD = config.comment("Ticks to look ahead (20 ticks = 1 second). Prediction is approximate and cannot prevent every hit.")
                .defineInRange("predictionTicks", 5, 1, 6);
        CONFIG = config.build();
    }

    public AutoIntentDodge() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CONFIG);
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.DISPLAYTEST,
                () -> Pair.of(() -> FMLNetworkConstants.IGNORESERVERONLY, (version, remoteIsServer) -> true));
    }
}
