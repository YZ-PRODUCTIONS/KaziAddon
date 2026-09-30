package net.kazi.kazimod.abilities.KamaRework;

import java.util.List;
import net.kazi.kazimod.init.KaziParticleEffects;
import net.kazi.kazimod.init.KaziSounds;
import net.kazi.kazimod.particles.SpiderwebCleaveParticleEffect;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class SpiderwebCleaveAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "spiderweb_cleave",
            new Pair[]{ImmutablePair.of("The user sends a web of slashes across a wide area.", (Object) null)}
    );

    private static final float DAMAGE      = 50.0F;
    private static final float COOLDOWN    = 400.0F;
    private static final int   CHARGE_TIME = 20;
    private static final float AOE_RANGE   = 20.0F;

    private static final SpiderwebCleaveParticleEffect PARTICLES = new SpiderwebCleaveParticleEffect();


    public static final AbilityCore INSTANCE;

    private final ChargeComponent chargeComponent = new ChargeComponent(this)
            .addStartEvent(this::startChargeEvent)
            .addTickEvent(this::tickChargeEvent)
            .addEndEvent(this::endChargeEvent);

    private final AnimationComponent  animationComponent  = new AnimationComponent(this);
    private final RangeComponent      rangeComponent      = new RangeComponent(this);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);

    public SpiderwebCleaveAbility(AbilityCore core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.chargeComponent,
                this.animationComponent,
                this.rangeComponent,
                this.dealDamageComponent
        });
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!chargeComponent.isCharging()) {
            chargeComponent.startCharging(entity, CHARGE_TIME);
        }
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, ModAnimations.RYU_NO_IBUKI);

    }

    private void tickChargeEvent(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance((Effect) ModEffects.MOVEMENT_BLOCKED.get(), 5, 0, false, false));


    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.stop(entity);
        net.kazi.kazimod.entities.KamaVfxEntity.spawn(entity,
                net.kazi.kazimod.entities.KamaVfxEntity.WEB, entity.position(), AOE_RANGE, 20);

        List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(entity, AOE_RANGE);

        for (LivingEntity target : targets) {
            if (target == null || !target.isAlive()) continue;
            if (target.hasEffect((Effect) ModEffects.SILENT.get())) continue;

            AbilityDamageSource source = (AbilityDamageSource) this.dealDamageComponent.getDamageSource(entity);
            source.setInternal();
            source.setSlash();
            source.markIndirectDamage();
            source.setUnavoidable();
            source.bypassArmor();

            if (this.dealDamageComponent.hurtTarget(entity, target, DAMAGE, source)) {
                if (!entity.level.isClientSide) {
                    ((ServerWorld) entity.level).playSound(null, target.blockPosition(),
                            KaziSounds.CLEAVE_HIT_SFX.get(), SoundCategory.PLAYERS, 4.0F, 1.0F);

                    net.kazi.kazimod.entities.KamaVfxEntity.slash(entity, target.getX(), target.getEyeY(), target.getZ());
                }
            }
        }

        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Spiderweb Cleave", AbilityCategory.DEVIL_FRUITS, SpiderwebCleaveAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN),
                        ChargeComponent.getTooltip(CHARGE_TIME),
                        DealDamageComponent.getTooltip(DAMAGE),
                        RangeComponent.getTooltip(AOE_RANGE, RangeType.AOE)
                })
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceType(new SourceType[]{SourceType.INTERNAL})
                .setSourceElement(SourceElement.SHOCKWAVE)
                .build();
    }
}
