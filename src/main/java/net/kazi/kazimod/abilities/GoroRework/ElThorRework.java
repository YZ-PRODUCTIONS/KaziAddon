//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.GoroRework;

import java.awt.Color;
import javax.annotation.Nullable;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceContext.BlockMode;
import net.minecraft.util.math.RayTraceContext.FluidMode;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
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
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.api.util.Interval;
import xyz.pixelatedw.mineminenomi.config.ClientConfig;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModMorphs;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class ElThorRework extends Ability {
    private static final ResourceLocation DEFAULT_ICON = new ResourceLocation("mineminenomi", "textures/abilities/el_thor.png");
    private static final ResourceLocation ALT_ICON = new ResourceLocation("mineminenomi", "textures/abilities/alts/el_thor.png");
    public static final Color YELLOW_THUNDER = new Color(-135916, true);
    public static final Color BLUE_THUNDER = WyHelper.hexToRGB("#70EAFF22");
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "el_thor", new Pair[]{ImmutablePair.of("Focuses a large cluster of electricity above the target, then sends a powerful lightning bolt crashing down from the sky", (Object)null)});
    private static final int CHARGE_TIME = 80;
    private static final int COOLDOWN = 360;
    public static final AbilityCore<ElThorRework> INSTANCE;
    private final ChargeComponent chargeComponent = (new ChargeComponent(this, (component) -> component.getChargeTime() >= 10.0F)).addStartEvent(this::onChargeStart).addTickEvent(this::onChargeTick).addEndEvent(this::onChargeEnd);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final Interval particleInterval = new Interval(2);

    public ElThorRework(AbilityCore<ElThorRework> core) {
        super(core);
        super.setDisplayIcon(DEFAULT_ICON);
        if (ClientConfig.INSTANCE.isGoroBlue()) {
            super.setDisplayIcon(ALT_ICON);
        }

        super.isNew = true;
        super.addComponents(new AbilityComponent[]{this.chargeComponent, this.animationComponent});
        super.addUseEvent(this::onUseEvent);
        super.addEquipEvent(this::equipEvent);
    }

    public void equipEvent(LivingEntity entity, Ability ability) {
        super.setDisplayIcon(DEFAULT_ICON);
        if (ClientConfig.INSTANCE.isGoroBlue()) {
            super.setDisplayIcon(ALT_ICON);
        }

    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        this.chargeComponent.startCharging(entity, 80.0F);
    }

    private void onChargeStart(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            this.particleInterval.restartIntervalToZero();
            this.animationComponent.start(entity, ModAnimations.RAISE_RIGHT_ARM);
        }
    }

    private void onChargeTick(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            AbilityHelper.slowEntityFall(entity);
            if (this.particleInterval.canTick()) {
                RayTraceResult mop = WyHelper.rayTraceBlocksAndEntities(entity, (double)256.0F, 0.4F);
                double i = mop.getLocation().x;
                double j = mop.getLocation().y;
                double k = mop.getLocation().z;
                double particleAmount = (double)this.chargeComponent.getChargeTime();

                for(int n = 0; (double)n < particleAmount; ++n) {
                    double offsetX = WyHelper.randomDouble() * (double)n * 0.225;
                    double offsetZ = WyHelper.randomDouble() * (double)n * 0.225;
                    if (entity instanceof PlayerEntity) {
                        WyHelper.spawnParticleEffectForOwner((ParticleEffect)ModParticleEffects.EL_THOR_AIM.get(), (PlayerEntity)entity, i + offsetX, j, k + offsetZ, (ParticleEffect.Details)null);
                    }
                }

            }
        }
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            RayTraceResult mop = WyHelper.rayTraceBlocksAndEntities(entity);
            double time = (double)this.chargeComponent.getChargePercentage();
            float multi = (float)((double)0.4F + time * (double)0.6F);
            if (((MorphInfo)ModMorphs.VOLT_AMARU.get()).isActive(entity)) {
                multi += 0.25F;
            }

            Vector3d mopPos = mop.getLocation();
            BlockRayTraceResult hitResult = entity.level.clip(new RayTraceContext(mopPos, mopPos.add((double)0.0F, (double)128.0F, (double)0.0F), BlockMode.COLLIDER, FluidMode.ANY, entity));
            float targetY = hitResult.getType().equals(Type.BLOCK) ? (float)hitResult.getLocation().y : 128.0F;
            float travelLength = targetY + 16.0F * multi;
            Vector3d pos = new Vector3d(mopPos.x, (double)targetY, mopPos.z);
            LightningEntity boltInner = new LightningEntity(entity, pos.x, pos.y, pos.z, 0.0F, 90.0F, travelLength, 24.0F, this.getCore());
            LightningEntity boltOuter = new LightningEntity(entity, pos.x, pos.y, pos.z, 0.0F, 90.0F, travelLength, 24.0F, this.getCore());
            this.setBoltPropieties(boltInner, 2.0F, 0.0F, 90, 40, false, Color.WHITE, multi);
            this.setBoltPropieties(boltOuter, 2.5F, 50.0F, 100, 9999, true, ClientConfig.INSTANCE.isGoroBlue() ? BLUE_THUNDER : YELLOW_THUNDER, multi);
            boltOuter.seed = boltInner.seed;
            entity.level.addFreshEntity(boltInner);
            entity.level.addFreshEntity(boltOuter);
            entity.level.playSound((PlayerEntity)null, new BlockPos(mopPos), (SoundEvent)ModSounds.EL_THOR_SFX.get(), SoundCategory.PLAYERS, 20.0F, 1.0F);
            this.animationComponent.stop(entity);
            super.cooldownComponent.startCooldown(entity, 360.0F);
        }
    }

    public void setBoltPropieties(LightningEntity bolt, float size, float damage, int timeAlive, int resetTime, boolean explodes, @Nullable Color color, float multiplier) {
        bolt.setBlocksAffectedLimit(8150);
        bolt.setAngle(160);
        bolt.setBranches(1);
        bolt.setSegments(1);
        bolt.setSize(size * multiplier);
        bolt.setBoxSizeDivision((double)0.225F);
        bolt.setLightningMovement(false);
        bolt.setExplosion(explodes ? (int)(10.0F * multiplier) : 0, true, 0.25F);
        if (color != null) {
            bolt.setColor(color);
        }

        bolt.setMaxLife(timeAlive);
        bolt.setDamage(damage * multiplier);
        bolt.setTargetTimeToReset(resetTime);
    }

    static {
        INSTANCE = (new AbilityCore.Builder("El Thor", AbilityCategory.DEVIL_FRUITS, ElThorRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(360.0F), ChargeComponent.getTooltip(80.0F)}).setSourceElement(SourceElement.LIGHTNING).setSourceHakiNature(SourceHakiNature.SPECIAL).setIcon(DEFAULT_ICON).build();
    }
}
