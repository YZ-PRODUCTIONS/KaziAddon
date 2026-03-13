//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.KobuRework;

import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.TargetsPredicate;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class ShoureiRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "shourei", new Pair[]{ImmutablePair.of("Increases other people's fighting spirit and physical strength by simply encouraging them with words.", (Object)null)});
    private static final TargetsPredicate TARGETS_CHECK = (new TargetsPredicate()).testFriendlyFaction();
    private static final int COOLDOWN = 1200;
    private static final int RANGE = 20;
    public static final AbilityCore<ShoureiRework> INSTANCE;
    private final RangeComponent rangeComponent = new RangeComponent(this);

    public ShoureiRework(AbilityCore<ShoureiRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.rangeComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        for(LivingEntity target : this.rangeComponent.getTargetsInArea(entity, 20.0F, TARGETS_CHECK)) {
            target.addEffect(new EffectInstance(Effects.DAMAGE_BOOST, 900, 1, true, false, true));
            target.addEffect(new EffectInstance(Effects.MOVEMENT_SPEED, 900, 1, true, false, true));
            target.addEffect(new EffectInstance(Effects.DAMAGE_RESISTANCE, 900, 1, true, false, true));
            target.addEffect(new EffectInstance(Effects.REGENERATION, 900, 0, true, false, true));
            target.addEffect(new EffectInstance(KaziEffects.ENHANCED_MOVEMENT.get(), 900, 1, true, false, true));
            WyHelper.spawnParticleEffect((ParticleEffect)ModParticleEffects.SHOUREI.get(), entity, target.getX(), target.getY(), target.getZ());
        }

        super.cooldownComponent.startCooldown(entity, 1200.0F);
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Shourei", AbilityCategory.DEVIL_FRUITS, ShoureiRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(1200.0F), RangeComponent.getTooltip(20.0F, RangeType.AOE)}).build();
    }
}
