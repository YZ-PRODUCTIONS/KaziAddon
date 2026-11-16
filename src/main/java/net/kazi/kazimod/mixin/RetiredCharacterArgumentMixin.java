package net.kazi.kazimod.mixin;

import java.util.concurrent.CompletableFuture;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.kazi.kazimod.events.RetiredCharacterOptions;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.pixelatedw.mineminenomi.api.ModRegistries;
import xyz.pixelatedw.mineminenomi.api.charactercreator.ICharacterCreatorEntry;
import xyz.pixelatedw.mineminenomi.api.commands.FightingStyleArgument;
import xyz.pixelatedw.mineminenomi.api.commands.RaceArgument;

@Mixin(value = {RaceArgument.class, FightingStyleArgument.class}, remap = false)
public abstract class RetiredCharacterArgumentMixin {
    @Inject(method = "parse", at = @At("RETURN"))
    private void kazi$rejectRetiredArgument(StringReader reader, CallbackInfoReturnable<ICharacterCreatorEntry> cir)
            throws CommandSyntaxException {
        ICharacterCreatorEntry entry = cir.getReturnValue();
        if (entry != null && RetiredCharacterOptions.retired(entry.getRegistryName()))
            throw new SimpleCommandExceptionType(new StringTextComponent("That race or fighting style has been retired.")).createWithContext(reader);
    }

    @Inject(method = "suggestAbility", at = @At("HEAD"), cancellable = true)
    private void kazi$hideRetiredSuggestions(SuggestionsBuilder builder, CallbackInfoReturnable<CompletableFuture<Suggestions>> cir) {
        java.util.Set<ResourceLocation> keys = (Object)this instanceof RaceArgument
                ? ModRegistries.RACES.getKeys() : ModRegistries.STYLES.getKeys();
        cir.setReturnValue(ISuggestionProvider.suggestResource(keys.stream()
                .filter(id -> !RetiredCharacterOptions.retired(id)), builder));
    }
}
