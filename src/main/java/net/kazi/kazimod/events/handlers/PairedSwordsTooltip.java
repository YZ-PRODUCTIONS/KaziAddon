package net.kazi.kazimod.events.handlers;

import net.kazi.kazimod.abilities.SupaRework.EnhancementAbility;
import net.kazi.kazimod.items.PairedSwordsItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Correct the vanilla weapon stat after ItemStack has assembled its attribute tooltip. */
@Mod.EventBusSubscriber(modid = "kazimod", value = Dist.CLIENT)
public final class PairedSwordsTooltip {
    private PairedSwordsTooltip() { }

    @SubscribeEvent
    public static void updateDamage(ItemTooltipEvent event) {
        if (!PairedSwordsItem.isPair(event.getItemStack())
                || !PairedSwordsItem.isEnhancementTooltipActive(event.getItemStack())) return;
        for (int i = 0; i < event.getToolTip().size(); i++) {
            event.getToolTip().set(i, updateLine(event.getToolTip().get(i).copy()));
        }
    }

    private static ITextComponent updateLine(ITextComponent line) {
        // Vanilla prefixes the attribute line with a space component.
        for (int i = 0; i < line.getSiblings().size(); i++) {
            line.getSiblings().set(i, updateLine(line.getSiblings().get(i)));
        }
        if (line instanceof TranslationTextComponent) {
            TranslationTextComponent translated = (TranslationTextComponent) line;
            Object[] args = translated.getArgs();
            if (!"attribute.modifier.equals.0".equals(translated.getKey()) || args.length != 2
                    || !(args[1] instanceof TranslationTextComponent)
                    || !"attribute.name.generic.attack_damage".equals(((TranslationTextComponent) args[1]).getKey())) return line;
            try {
                // Preserve the existing enchantment and player-base contribution.
                double base = ItemStack.ATTRIBUTE_MODIFIER_FORMAT.parse(args[0].toString()).doubleValue();
                TranslationTextComponent updated = new TranslationTextComponent(translated.getKey(),
                        ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(base + EnhancementAbility.FIRST_DAMAGE_BONUS), args[1]);
                updated.setStyle(line.getStyle());
                for (ITextComponent sibling : line.getSiblings()) updated.append(sibling.copy());
                return updated;
            } catch (java.text.ParseException ignored) {
                // Leave another mod's non-numeric tooltip intact.
            }
        }
        return line;
    }
}
