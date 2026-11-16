package net.kazi.kazimod.abilities.NagiAwakening;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Operator utility: /nagi_cutscene reset [player] resets every awakening cutscene cooldown. */
@Mod.EventBusSubscriber(modid = "kazimod")
public final class InnerDragonsCutsceneCommand {
    private InnerDragonsCutsceneCommand() { }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("nagi_cutscene")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("reset")
                        .executes(context -> reset(context.getSource(), context.getSource().getPlayerOrException()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(context -> reset(context.getSource(), EntityArgument.getPlayer(context, "player"))))));
    }

    private static int reset(CommandSource source, ServerPlayerEntity player) {
        DragonStrikeAbility.resetCutsceneCooldown(player);
        net.kazi.kazimod.abilities.GoruRework.SeriousnessAbility.resetCutsceneCooldown(player);
        net.kazi.kazimod.abilities.YagiAwakening.HyperGonerCutscene.resetCutsceneCooldown(player);
        source.sendSuccess(new TranslationTextComponent("command.kazimod.nagi_cutscene.reset", player.getDisplayName()), true);
        return 1;
    }
}
