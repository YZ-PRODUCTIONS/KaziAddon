package net.kazi.kazimod.abilities.Nusu;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;

import java.util.List;
import java.util.UUID;

/**
 * /nusu_steal <1-N>
 *
 * Called when the player clicks a line in the Skill Hunter observation UI.
 * Reads the pending choices stored by SkillHunterAbility, executes the steal,
 * then clears pending state.
 */
public class NusuStealCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("nusu_steal")
                        .then(Commands.argument("choice", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getEntity() instanceof ServerPlayerEntity)) return 0;
                                    PlayerEntity player = (ServerPlayerEntity) ctx.getSource().getEntity();
                                    int choice = IntegerArgumentType.getInteger(ctx, "choice");
                                    executeSteal(player, choice);
                                    return 1;
                                })
                        )
        );
    }

    private static void executeSteal(PlayerEntity player, int choice) {
        List<AbilityCore<?>> pending = NusuStolenData.getPendingChoices(player);

        if (pending.isEmpty()) {
            send(player, "\u00a7cNo pending ability selection. Use Skill Hunter first.");
            return;
        }

        int index = choice - 1;
        if (index < 0 || index >= pending.size()) {
            send(player, "\u00a7cInvalid choice. Pick 1 to " + pending.size() + ".");
            return;
        }

        AbilityCore<?> chosen = pending.get(index);

        // Find the target among online players by UUID
        UUID targetUUID = NusuStolenData.getPendingStealTargetUUID(player);
        LivingEntity target = null;
        if (targetUUID != null && player.level != null) {
            for (PlayerEntity p : player.level.players()) {
                if (p.getUUID().equals(targetUUID)) {
                    target = p;
                    break;
                }
            }
        }

        NusuStolenData.clearPending(player);

        if (target == null) {
            send(player, "\u00a7cTarget is no longer online — steal cancelled");
            return;
        }

        if (NusuStolenData.heldCount(player) >= NusuStolenData.MAX_SLOTS) {
            send(player, "\u00a7cAll 4 stolen ability slots are full");
            return;
        }

        int slot = NusuEvents.stealAbility(player, target, chosen);
        if (slot == -1) {
            send(player, "\u00a7cCould not steal — slots full");
            return;
        }

        String name = chosen.getLocalizedName().getString();
        int total = NusuStolenData.heldCount(player);
        send(player, "\u00a7a" + name + " stolen! (" + total + "/4 slots used)");
        NusuEvents.sendMsg(target, "\u00a7c" + name + " was stolen by the Nusu user");
    }

    private static void send(PlayerEntity player, String text) {
        player.sendMessage(
                new net.minecraft.util.text.StringTextComponent(text),
                player.getUUID());
    }
}