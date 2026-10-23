package net.kazi.kazimod.mixin;

import java.util.Collection;
import com.mojang.brigadier.context.CommandContext;
import net.kazi.kazimod.saiyan.SaiyanFeatures;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.commands.AbilityCommand;

@Mixin(value=AbilityCommand.class,remap=false)
public abstract class SaiyanCooldownResetMixin {
    @Inject(method="resetCooldown",at=@At("TAIL"))
    private static void saiyan$refillKi(CommandContext<CommandSource> context,
            Collection<ServerPlayerEntity> targets,CallbackInfoReturnable<Integer> ci){
        for(ServerPlayerEntity player:targets)SaiyanFeatures.refillKi(player);
    }
}
