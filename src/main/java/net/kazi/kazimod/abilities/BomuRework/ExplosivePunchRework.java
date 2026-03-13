//
// Reworked to follow PunchAbility2 pattern (like MagmaCoatingAbility)
//

package net.kazi.kazimod.abilities.BomuRework;

import java.awt.Color;
import java.util.function.Predicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityOverlay;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityOverlay.OverlayPart;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.PunchAbility2;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.SkinOverlayComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModResources;
import xyz.pixelatedw.mineminenomi.particles.effects.CommonExplosionParticleEffect;

public class ExplosivePunchRework extends PunchAbility2 {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "explosive_punch", new Pair[]{ImmutablePair.of("The user coats their fist with explosives and punches, creating an explosion on hit", (Object)null)});
    private static final float THRESHOLD = 600.0F;
    private static final float COOLDOWN = 200.0F;
    public static final AbilityCore<ExplosivePunchRework> INSTANCE;

    public ExplosivePunchRework(AbilityCore<ExplosivePunchRework> core) {
        super(core);
        super.isNew = true;
        super.continuousComponent.addStartEvent(this::onContinuityStart).addEndEvent(this::onContinuityEnd);
        this.addCanUseCheck(AbilityHelper::canUseBrawlerAbilities);
    }

    private void onContinuityStart(LivingEntity entity, IAbility ability) {

    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
    }

    @Override
    public float getPunchDamage() {
        return 18.0F;
    }

    @Override
    public float getPunchCooldown() {
        return 200.0F;
    }

    @Override
    public boolean onHitEffect(LivingEntity entity, LivingEntity target, ModDamageSource source) {
        ExplosionAbility explosion = AbilityHelper.newExplosion(entity, entity.level, target.getX(), target.getY(), target.getZ(), 4.0F);
        explosion.setStaticDamage(35.0F);
        explosion.setSmokeParticles(new CommonExplosionParticleEffect(3));
        explosion.doExplosion();
        return true;
    }

    @Override
    public float getPunchHoldTime() {
        return 600.0F;
    }

    @Override
    public Predicate<LivingEntity> canActivate() {
        return (entity) -> super.continuousComponent.isContinuous() && entity.getMainHandItem().isEmpty();
    }

    @Override
    public int getUseLimit() {
        return -1;
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Explosive Punch", AbilityCategory.DEVIL_FRUITS, ExplosivePunchRework::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(200.0F),
                        ContinuousComponent.getTooltip(600.0F),
                        ChangeStatsComponent.getTooltip()
                })
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .setSourceType(new SourceType[]{SourceType.FIST})
                .setSourceElement(SourceElement.EXPLOSION)
                .build();


    }
}