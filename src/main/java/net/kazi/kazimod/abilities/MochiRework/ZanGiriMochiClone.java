package net.kazi.kazimod.abilities.MochiRework;

import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.init.CartAbilityPools;
import net.MrMagicalCart.cartaddon.init.CartMorphs;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.HakiHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.api.protection.BlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.block.AirBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.block.CoreBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.block.FoliageBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.block.OreBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.entities.LightningDischargeEntity;
import xyz.pixelatedw.mineminenomi.data.entity.haki.HakiDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.haki.IHakiData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.effects.CommonExplosionParticleEffect;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

import javax.annotation.Nullable;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/** Local, independently editable clone of CartAddon's Zan Giri Mochi. */
public class ZanGiriMochiClone extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "zan_giri_mochi",
            new Pair[]{ImmutablePair.of(
                    "The user grabs their foe and spins them around until they slam them into the ground, causing immense damage. (You can turn terrain damage on or off)",
                    null)});

    public static final AbilityCore<ZanGiriMochiClone> INSTANCE;
    private static final BlockProtectionRule GRIEF_RULE = new BlockProtectionRule.Builder(
            AirBlockProtectionRule.INSTANCE,
            CoreBlockProtectionRule.INSTANCE,
            FoliageBlockProtectionRule.INSTANCE,
            OreBlockProtectionRule.INSTANCE).build();

    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final ChargeComponent chargeComponent = new ChargeComponent(this)
            .addStartEvent(100, this::onChargeStart)
            .addTickEvent(this::onChargeTick)
            .addEndEvent(this::onChargeEnd);
    private final ContinuousComponent continuousComponent = new ContinuousComponent(this, true)
            .addTickEvent(this::onContinuityTick)
            .addEndEvent(this::onContinuityEnd);
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final PoolComponent poolComponent = new PoolComponent(
            this, CartAbilityPools.MOCHI_ABILITY, new AbilityPool2[]{ModAbilityPools.GRAB_ABILITY});
    private final MorphComponent morphComponent = new MorphComponent(this);
    private final AltModeComponent<Mode> altModeComponent =
            new AltModeComponent<>(this, Mode.class, Mode.EXPLOSIVE);
    private final GrabEntityComponent grabComponent =
            new GrabEntityComponent(this, true, true, false, 27.0F);
    private final List<LivingEntity> grabbedEntities = new ArrayList<>();
    private float yaw;

    public ZanGiriMochiClone(AbilityCore<ZanGiriMochiClone> core) {
        super(core);
        this.isNew = true;
        this.addComponents(grabComponent, rangeComponent, dealDamageComponent, chargeComponent,
                continuousComponent, poolComponent, morphComponent, altModeComponent);
        this.addUseEvent(AbilityLimits::usingBrawler);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide && !chargeComponent.isCharging() && !continuousComponent.isContinuous()) {
            grabbedEntities.clear();
            chargeComponent.startCharging(entity, 100.0F);
        }
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        morphComponent.startMorph(entity, (MorphInfo) CartMorphs.ZAN_GIRI_MOCHI_NEW.get());
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        if (chargeComponent.getChargeTime() < 40.0F) {
            entity.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(), 2, 2, false, false));
            return;
        }
        if (entity.level.isClientSide) return;

        yaw += 30.0F;
        double angle = Math.toRadians(yaw);
        double posX = entity.getX() + Math.cos(angle) * 27.0D;
        double posY = entity.getY() + entity.getEyeHeight() / 2.0F;
        double posZ = entity.getZ() + Math.sin(angle) * 27.0D;

        List<LivingEntity> targets = WyHelper.getNearbyLiving(entity.position(), entity.level,
                32.0D, 4.0D, 32.0D, ModEntityPredicates.getEnemyFactions(entity));
        for (LivingEntity target : targets) {
            target.addEffect(new EffectInstance(ModEffects.GRABBED.get(), 30, 0, false, false));
            target.teleportTo(posX, posY, posZ);
            if (!grabbedEntities.contains(target)) grabbedEntities.add(target);
        }
        entity.addEffect(new EffectInstance(Effects.DIG_SPEED, 2, 2, false, false));
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        continuousComponent.startContinuity(entity, 40.0F);
    }

    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;
        for (LivingEntity target : grabbedEntities) {
            if (target.isAlive()) {
                target.teleportTo(entity.getX(), entity.getY() + 18.0D, entity.getZ());
                target.addEffect(new EffectInstance(Effects.LEVITATION, 2, 0));
            }
        }
        WyHelper.spawnParticleEffect((ParticleEffect<?>) CartParticleEffects.ZAN_GIRI.get(),
                entity, entity.getX(), entity.getY() + 3.0D, entity.getZ());
        entity.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(), 2, 2, false, false));
    }

    private void onContinuityEnd(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) return;

        float damage = 180.0F;
        float explosionDamage = 30.0F;
        if (grabbedEntities.size() > 3) {
            damage *= 0.6F;
            explosionDamage *= 0.5F;
        } else if (grabbedEntities.size() > 2) {
            damage *= 0.8F;
            explosionDamage *= 0.75F;
        }

        for (LivingEntity target : grabbedEntities) {
            if (!target.isAlive()) continue;
            target.teleportTo(entity.getX(), entity.getY(), entity.getZ());
            if (dealDamageComponent.hurtTarget(entity, target, damage)) {
                target.addEffect(new EffectInstance(Effects.BAD_OMEN, 10, 0, false, false));
                target.teleportTo(entity.getX(), entity.getY() - 1.0D, entity.getZ());
                target.addEffect(new EffectInstance(ModEffects.DIZZY.get(), 90, 0));
                target.removeEffect(ModEffects.GRABBED.get());
                target.verticalCollision = true;
            }
        }

        ExplosionAbility explosion = AbilityHelper.newExplosion(
                entity, entity.level, entity.getX(), entity.getY(), entity.getZ(), 22.0F);
        explosion.setExplosionSound(true);
        explosion.setDamageOwner(false);
        explosion.setDestroyBlocks(false);
        if (altModeComponent.getCurrentMode() == Mode.EXPLOSIVE) {
            AbilityHelper.createSphere(entity.level, entity.blockPosition(), 27, 27,
                    false, Blocks.AIR, 2, GRIEF_RULE);
        }
        explosion.setSmokeParticles(new CommonExplosionParticleEffect(25));
        explosion.setDamageEntities(false);
        explosion.setStaticDamage(explosionDamage);
        explosion.doExplosion();

        entity.level.playSound(null, entity.blockPosition(), ModSounds.HAKI_RELEASE_SFX.get(),
                SoundCategory.PLAYERS, 4.0F, 0.85F);
        spawnHakiLightning(entity);
        entity.teleportTo(entity.getX(), entity.getY() + 14.0D, entity.getZ());
        morphComponent.stopMorph(entity);
        grabbedEntities.clear();
        cooldownComponent.startCooldown(entity, 1200.0F);
    }

    private void spawnHakiLightning(LivingEntity entity) {
        IHakiData haki = HakiDataCapability.get(entity);
        float hakiLevel = haki.getTotalHakiExp() / 100.0F;
        int radius = hakiLevel <= 1.0F ? 10 : hakiLevel <= 1.75F ? 25 : 40;
        int mastery = hakiLevel <= 1.0F ? 0 : hakiLevel <= 1.75F ? 1 : 2;

        LightningDischargeEntity discharge = new LightningDischargeEntity(
                entity, entity.getX(), entity.getY() + 1.5D, entity.getZ(), entity.yRot, entity.xRot);
        discharge.setAliveTicks(30);
        discharge.setUpdateRate(8);
        discharge.setLightningLength(radius * 2.0F);
        discharge.setColor(new Color(0, 0, 0, 100));
        discharge.setOutlineColor(new Color(HakiHelper.getHaoshokuColour(entity)));
        discharge.setRenderTransparent();
        discharge.setDetails(16);
        discharge.setDensity(mastery == 2 ? 32 : 16);
        discharge.setSize(1.0F);
        discharge.setSkipSegments(1);
        if (mastery == 0) discharge.setSplit();
        entity.level.addFreshEntity(discharge);
    }

    public ContinuousComponent getContinuousComponent(LivingEntity entity) {
        return continuousComponent;
    }

    public ChargeComponent getChargeComponent(LivingEntity entity) {
        return chargeComponent;
    }

    static {
        INSTANCE = new AbilityCore.Builder<>(
                "Zan Giri Mochi", AbilityCategory.DEVIL_FRUITS, ZanGiriMochiClone::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(AbilityDescriptionLine.NEW_LINE,
                        DealDamageComponent.getTooltip(260.0F),
                        ChargeComponent.getTooltip(100.0F),
                        CooldownComponent.getTooltip(1000.0F),
                        RangeComponent.getTooltip(32.0F, RangeType.AOE))
                .setIcon(new ResourceLocation("cartaddon", "textures/abilities/zan_giri_mochi.png"))
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .build();
    }

    public enum Mode {
        EXPLOSIVE,
        NONE
    }
}
