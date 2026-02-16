//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.ToriNueRework;

import java.util.Iterator;
import java.util.List;
import net.MrMagicalCart.cartaddon.init.CartEffects;
import net.MrMagicalCart.cartaddon.init.CartMorphs;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.RequireMorphComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.api.util.TargetsPredicate;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class FlameBlessingRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "flame_blessing", new Pair[]{ImmutablePair.of("The user blesses their allies around them for 30 seconds.", (Object)null)});
    private static final TargetsPredicate TARGETS_CHECK = (new TargetsPredicate()).testFriendlyFaction();
    private static final int COOLDOWN = 1200;
    private static final int RANGE = 20;
    public static final AbilityCore<FlameBlessingRework> INSTANCE;
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final RequireMorphComponent requireMorphComponent;

    public FlameBlessingRework(AbilityCore<FlameBlessingRework> core) {
        super(core);
        this.requireMorphComponent = new RequireMorphComponent(this, (MorphInfo)CartMorphs.NUE_FLY.get(), new MorphInfo[0]);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.rangeComponent, this.requireMorphComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        WyHelper.spawnParticleEffect((ParticleEffect)CartParticleEffects.NUE_FLAMES_RING.get(), entity, entity.getX(), entity.getY() + (double)entity.getBbHeight(), entity.getZ());
        List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(entity, 20.0F, TARGETS_CHECK);
        Iterator var4 = targets.iterator();
        entity.addEffect(new EffectInstance((Effect)CartEffects.FLAME_BLESSING.get(), 300, 0, true, false, true));

        while(var4.hasNext()) {
            LivingEntity target = (LivingEntity)var4.next();
            WyHelper.spawnParticleEffect((ParticleEffect)CartParticleEffects.NUE_FLAMES_RING.get(), target, target.getX(), target.getY() + (double)target.getBbHeight(), target.getZ());
            entity.addEffect(new EffectInstance((Effect)CartEffects.FLAME_BLESSING.get(), 300, 0, true, false, true));
        }

        super.cooldownComponent.startCooldown(entity, 1200.0F);
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Flame Blessing", AbilityCategory.DEVIL_FRUITS, FlameBlessingRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(1200.0F), RangeComponent.getTooltip(20.0F, RangeType.AOE)}).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, RequireMorphComponent.getTooltip()}).build();
    }
}
