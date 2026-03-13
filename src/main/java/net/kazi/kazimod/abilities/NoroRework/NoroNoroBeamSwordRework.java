//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.NoroRework;

import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.ItemAbility2;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTriggerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTriggerComponent.HitResult;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModWeapons;

public class NoroNoroBeamSwordRework extends ItemAbility2 {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "noro_noro_beam_sword", new Pair[]{ImmutablePair.of("Focuses photons inside a hilt to create a sword, which slows enemies upon hit", (Object)null)});
    private static final float COOLDOWN = 20.0F;
    public static final AbilityCore<NoroNoroBeamSwordRework> INSTANCE;
    private final HitTriggerComponent hitTriggerComponent = (new HitTriggerComponent(this)).addOnHitEvent(this::hitEvent).addTryHitEvent(this::tryHitEvent);

    public NoroNoroBeamSwordRework(AbilityCore<NoroNoroBeamSwordRework> core) {
        super(core);
        this.addComponents(new AbilityComponent[]{this.hitTriggerComponent});
        this.continuousComponent.addEndEvent(100, this::endContinuityEvent);
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.cooldownComponent.startCooldown(entity, 20.0F);
    }

    private HitTriggerComponent.HitResult tryHitEvent(LivingEntity entity, LivingEntity target, ModDamageSource source, IAbility ability) {
        return !this.continuousComponent.isContinuous() ? HitResult.PASS : HitResult.HIT;
    }

    public boolean hitEvent(LivingEntity entity, LivingEntity target, ModDamageSource source, IAbility ability) {
        target.addEffect(new EffectInstance((Effect)ModEffects.NORO_SLOWNESS.get(), 40, 1));
        target.addEffect(new EffectInstance((Effect) KaziEffects.WEAKENED_MOVEMENT.get(), 40, 1));
        return true;
    }

    public ItemStack createItemStack(LivingEntity entity) {
        return new ItemStack((IItemProvider)ModWeapons.NORO_NORO_BEAM_SWORD.get());
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Noro Noro Beam Sword", AbilityCategory.DEVIL_FRUITS, NoroNoroBeamSwordRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(20.0F), ContinuousComponent.getTooltip()}).setSourceHakiNature(SourceHakiNature.IMBUING).setSourceType(new SourceType[]{SourceType.FIST}).build();
    }
}
