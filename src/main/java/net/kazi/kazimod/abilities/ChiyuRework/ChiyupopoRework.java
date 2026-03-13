//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.ChiyuRework;

import java.util.List;
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
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class ChiyupopoRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "chiyupopo", new Pair[]{ImmutablePair.of("Releases dandelions made of tears that temporarily increase the healing rate of those around the user. This can only be applied once per person.", (Object)null)});
    private static final float COOLDOWN = 1800.0F;
    private static final float RANGE = 9.0F;
    public static final AbilityCore<ChiyupopoRework> INSTANCE;
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private TargetsPredicate predicate;

    public ChiyupopoRework(AbilityCore<ChiyupopoRework> core) {
        super(core);
        super.isNew = true;
        this.addComponents(new AbilityComponent[]{this.rangeComponent});
        super.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity player, IAbility ability) {
        EntityStatsCapability.get(player).setChiyuEffect(false);
        if (this.predicate == null) {
            this.predicate = (new TargetsPredicate()).testFriendlyFaction().selector((entityx) -> !EntityStatsCapability.get(entityx).hadChiyuEffect());
        }

        List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(player, 20.0F, this.predicate);
        if (ModEntityPredicates.getFriendlyFactions(player).test(player) && !EntityStatsCapability.get(player).hadChiyuEffect()) {
            targets.add(player);
        }

        for(LivingEntity entity : targets) {
            if (entity.addEffect(new EffectInstance(Effects.REGENERATION, 1200, 3))) {
                EntityStatsCapability.get(entity).setChiyuEffect(true);
            }
        }

        WyHelper.spawnParticleEffect((ParticleEffect)ModParticleEffects.CHIYUPOPO.get(), player, player.getX(), player.getY(), player.getZ());
        super.cooldownComponent.startCooldown(player, 1800.0F);
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Chiyupopo", AbilityCategory.DEVIL_FRUITS, ChiyupopoRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(1800.0F), RangeComponent.getTooltip(9.0F, RangeType.AOE)}).build();
    }
}
