//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.OpeRework;

import net.MrMagicalCart.cartaddon.abilities.opeextra.ReworkedOpeHelper;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.RayTraceResult;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class RadioKnifeRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "radio_knife", new Pair[]{ImmutablePair.of("The user slashes the air rapidly, dealing damage to targets in front of them.", (Object)null)});
    private static final float COOLDOWN = 300.0F;
    private static final int CHARGE_TIME = 40;
    private static final float DAMAGE = 5.0F;
    private static final float RANGE = 4.0F;
    private final Interval damageInterval = new Interval(4);
    public static final AbilityCore<RadioKnifeRework> INSTANCE;
    private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::startChargeEvent).addTickEvent(this::duringChargeEvent).addEndEvent(this::endChargeEvent);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent(this);

    public RadioKnifeRework(AbilityCore<RadioKnifeRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.chargeComponent, this.dealDamageComponent, this.rangeComponent, this.animationComponent, this.hitTrackerComponent});
        super.addCanUseCheck(ReworkedOpeHelper::hasRoomActive);
        super.addCanUseCheck(AbilityHelper::canUseSwordsmanAbilities);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        this.chargeComponent.startCharging(entity, 40.0F);
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide && (ReworkedOpeHelper.hasRoomActive(entity, this).isFail() || !AbilityHelper.canUseSwordsmanAbilities(entity))) {
            this.chargeComponent.stopCharging(entity);
        }

        entity.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 5, 1, false, false));

        for(LivingEntity target : this.rangeComponent.getTargetsInLine(entity, 4.0F, 4.0F)) {
            if (this.hitTrackerComponent.canHit(target) && this.damageInterval.canTick() && this.dealDamageComponent.hurtTarget(entity, target, 5.0F)) {
                target.addEffect(new EffectInstance((Effect)ModEffects.ANTI_KNOCKBACK.get(), 2, 0, false, false));
                target.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(), 2, 1, false, false));
            }

            this.hitTrackerComponent.clearHits();
        }

        RayTraceResult mop = WyHelper.rayTraceBlocksAndEntities(entity, (double)3.0F);
        double x = mop.getLocation().x + WyHelper.randomDouble();
        double z = mop.getLocation().z + WyHelper.randomDouble();
        double y = mop.getLocation().y + WyHelper.randomDouble();
        WyHelper.spawnParticleEffect((ParticleEffect)CartParticleEffects.RADIO_KNIFE.get(), entity, x, y, z);
        if (!entity.level.isClientSide) {
            ((ServerWorld)entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
        }

        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.LIGHTNING_TELEPORT.get(), SoundCategory.PLAYERS, 3.0F, 1.45F);
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
        this.cooldownComponent.startCooldown(entity, 240.0F);
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Radio Knife", AbilityCategory.DEVIL_FRUITS, RadioKnifeRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, DealDamageComponent.getTooltip(50.0F), ChargeComponent.getTooltip(40.0F), CooldownComponent.getTooltip(240.0F), RangeComponent.getTooltip(4.0F, RangeType.LINE)}).setSourceHakiNature(SourceHakiNature.SPECIAL).setSourceType(new SourceType[]{SourceType.SLASH}).build();
    }
}
