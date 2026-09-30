package net.kazi.kazimod.items;

import java.util.List;
import javax.annotation.Nullable;
import net.kazi.kazimod.abilities.SupaRework.EnhancementAbility;
import net.kazi.kazimod.renderers.items.PairedSwordsItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xyz.pixelatedw.mineminenomi.init.ModCreativeTabs;
import xyz.pixelatedw.mineminenomi.items.weapons.ModSwordItem;

/** One weapon stack: the companion blade is rendered, never put in the inventory. */
public final class PairedSwordsItem extends ModSwordItem {
    private static final int BASE_ATTACK_DAMAGE = 16;

    public PairedSwordsItem() {
        super(new Item.Properties().tab(ModCreativeTabs.WEAPONS).stacksTo(1)
                .setISTER(() -> PairedSwordsItemRenderer::new), BASE_ATTACK_DAMAGE - 1, -2.1F);
        // The player's base attack contributes 1; the weapon supplies the remainder.
        this.damage = BASE_ATTACK_DAMAGE - 1;
    }

    public static boolean isPair(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof PairedSwordsItem;
    }

    /** Match Ama no Murakumo: projected swords disappear when dropped. */
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (entity.isAlive()) {
            entity.remove();
        }
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable World world,
                                List<ITextComponent> tooltip, ITooltipFlag flag) {
        super.appendHoverText(stack, world, tooltip, flag);
        boolean enhanced = isEnhancementTooltipActive(stack);
        int displayedDamage = BASE_ATTACK_DAMAGE
                + (enhanced ? (int) EnhancementAbility.FIRST_DAMAGE_BONUS : 0);
        tooltip.add(new TranslationTextComponent(
                enhanced
                        ? "item.kazimod.kanshou_bakuya.damage.enhanced"
                        : "item.kazimod.kanshou_bakuya.damage",
                displayedDamage).withStyle(enhanced ? TextFormatting.GREEN : TextFormatting.DARK_GREEN));
        tooltip.add(new TranslationTextComponent("item.kazimod.kanshou_bakuya.hint")
                .withStyle(TextFormatting.GRAY));
    }

    // Strip this helper on dedicated servers before the JVM resolves client player types.
    @OnlyIn(Dist.CLIENT)
    public static boolean isEnhancementTooltipActive(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        PlayerEntity player = minecraft == null ? null : minecraft.player;
        return player != null && isPair(player.getMainHandItem())
                && ItemStack.matches(player.getMainHandItem(), stack)
                && EnhancementAbility.hasFirstSwordEnhancement(player);
    }
}
