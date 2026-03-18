package net.kazi.kazimod.abilities.Nusu;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Registers the /nusu_remove command used by the Skill Remover clickable chat UI.
 *
 * Usage: /nusu_remove <1-4>
 *
 * Register via: MinecraftForge.EVENT_BUS.register(NusuRemoveCommand.class)
 * (done inside NusuEvents.register())
 */
public class NusuRemoveCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();
        dispatcher.register(
                Commands.literal("nusu_remove")
                        .then(Commands.argument("slot", IntegerArgumentType.integer(1, 4))
                                .executes(ctx -> {
                                    CommandSource source = ctx.getSource();
                                    int slot = IntegerArgumentType.getInteger(ctx, "slot");
                                    if (source.getEntity() instanceof ServerPlayerEntity) {
                                        PlayerEntity player = (ServerPlayerEntity) source.getEntity();
                                        SkillRemoverAbility.removeBySlot(player, slot);
                                    }
                                    return 1;
                                })
                        )
        );
    }
}