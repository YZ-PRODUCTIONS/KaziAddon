package net.kazi.kazimod.effects;

import java.util.ArrayList;
import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.effects.ModEffect;
import xyz.pixelatedw.mineminenomi.api.events.ability.AbilityUseEvent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;

/** A synchronized target lock that expires independently of the marked target's sequence. */
@Mod.EventBusSubscriber(modid = "kazimod")
public final class LostWorksFruitLockEffect extends ModEffect {
    public static final int DURATION_TICKS = 20 * 20;

    public LostWorksFruitLockEffect() {
        super(EffectType.HARMFUL, 0x292020);
    }

    public static void applyToTarget(LivingEntity target) {
        if (target.level.isClientSide || !target.isAlive()) return;
        target.addEffect(new EffectInstance(KaziEffects.LOST_WORKS_FRUIT_LOCK.get(), DURATION_TICKS, 0, false, false));
        if (AbilityDataCapability.get(target) == null) return;
        for (IAbility ability : new ArrayList<>(AbilityDataCapability.get(target).getEquippedAndPassiveAbilities())) {
            if (isFruitMove(ability)) {
                AbilityHelper.emergencyStopAbility(target, ability);
            }
        }
    }

    private static boolean isFruitMove(IAbility ability) {
        return ability != null && ability.getCore().getCategory() == AbilityCategory.DEVIL_FRUITS;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void beforeUse(AbilityUseEvent.Pre event) {
        LivingEntity caster = event.getEntityLiving();
        if (!caster.hasEffect(KaziEffects.LOST_WORKS_FRUIT_LOCK.get()) || !isFruitMove(event.getAbility())) return;
        event.setCanceled(true);
        if (!caster.level.isClientSide && caster instanceof PlayerEntity) {
            ((PlayerEntity) caster).displayClientMessage(new TranslationTextComponent(
                    "ability.kazimod.unlimited_lost_works.fruit_locked"), true);
        }
    }

    @Override public boolean shouldRender(EffectInstance effect) { return false; }
    @Override public boolean shouldRenderHUD(EffectInstance effect) { return false; }
    @Override public boolean isRemoveable() { return false; }
}
