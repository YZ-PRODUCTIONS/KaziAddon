//
// GalaxyImpactRework
// Changes: projectile now fires in the direction the player is looking
// instead of dropping straight down. Impact mode uses shootFromRotation
// with actual velocity, matching how UrsusShockRework fires its projectile.
//

package net.kazi.kazimod.abilities.BrawlerRework;

import java.awt.Color;
import java.util.UUID;
import net.MrMagicalCart.cartaddon.abilities.modifiedhuman.ModifiedHumanHelper;
import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.cartapi.CartRegistry;
import net.MrMagicalCart.cartaddon.entities.projectiles.brawlerextra.GalaxyDivideProjectile;
import net.MrMagicalCart.cartaddon.entities.projectiles.brawlerextra.GalaxyImpactProjectile;
import net.MrMagicalCart.cartaddon.init.CartAbilityPools;
import net.MrMagicalCart.cartaddon.init.CartParticleEffects;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.kazi.kazimod.entities.projectiles.GalaxyImpactReworkProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityAttributeModifier;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityPool2;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent.DamageState;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.DevilFruitHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.HakiHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.haki.HakiDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.haki.IHakiData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.entities.LightningDischargeEntity;
import xyz.pixelatedw.mineminenomi.entities.SphereEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class GalaxyImpactRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "galaxy_impact", new Pair[]{ImmutablePair.of("The user charges a massive shockwave of haki and fires it in any direction, bringing only destruction upon those it strikes.", (Object)null), ImmutablePair.of("The user charges a blast of haki and moves at immense speeds.", (Object)null)});
    private static final TranslationTextComponent IMPACT_NAME = new TranslationTextComponent(CartRegistry.registerName("ability.cartaddon.galaxy_impact", "Galaxy Impact"));
    private static final TranslationTextComponent DIVIDE_NAME = new TranslationTextComponent(CartRegistry.registerName("ability.cartaddon.galaxy_divide", "Galaxy Divide"));
    private static final ResourceLocation IMPACT_ICON = new ResourceLocation("cartaddon", "textures/abilities/galaxy_impact.png");
    private static final ResourceLocation DIVIDE_ICON = new ResourceLocation("cartaddon", "textures/abilities/galaxy_divide.png");
    private static final float G_IMPACT_COOLDOWN = 1800.0F;
    private static final float G_CHARGE = 50.0F;
    private static final float G_IMPACT_DURATION = 60.0F;
    private static final float G_DIVIDE_COOLDOWN = 1400.0F;
    private static final float G_DIVIDE_CHARGE = 60.0F;
    private static final float G_DIVIDE_DURATION = 80.0F;
    private static final float IMPACT_PROJECTILE_SPEED = 6.0F; // Increased from 4.0F
    public static final AbilityCore<GalaxyImpactRework> INSTANCE;
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::startChargeEvent).addTickEvent(this::tickChargeEvent).addEndEvent(this::endChargeEvent);
    boolean hasFallDamage;
    private SphereEntity galaxyImpactVisuals;
    private GalaxyImpactReworkProjectile proj;
    private BlockPos blockPos;
    private final DamageTakenComponent damageTakenComponent;
    private final ContinuousComponent continuousComponent;
    private final AltModeComponent<Mode> altModeComponent;
    private final ChangeStatsComponent statsComponent;
    private static final AbilityAttributeModifier SPEED_MODIFIER;
    private static final AbilityAttributeModifier JUMP_MODIFIER;
    private final PoolComponent poolComponent;
    private boolean prevSprintValue;
    public static int overuse = 1500;
    private LightningDischargeEntity discharge;
    private Color color;
    private int radius;
    private int haoMastery;

    public GalaxyImpactRework(AbilityCore<GalaxyImpactRework> core) {
        super(core);
        this.damageTakenComponent = new DamageTakenComponent(this, this::onDamageTaken, DamageState.ATTACK);
        this.continuousComponent = (new ContinuousComponent(this)).addStartEvent(this::startContinuityEvent).addTickEvent(this::tickContinuityEvent).addEndEvent(this::endContinuityEvent);
        this.altModeComponent = (new AltModeComponent<Mode>(this, Mode.class, GalaxyImpactRework.Mode.IMPACT)).addChangeModeEvent(this::onAltModeChange);
        this.statsComponent = new ChangeStatsComponent(this);
        this.poolComponent = new PoolComponent(this, CartAbilityPools.INIT_JUMP, new AbilityPool2[]{ModAbilityPools.GRAB_ABILITY});
        this.color = new Color(35, 175, 255, 200);
        this.radius = 0;
        this.haoMastery = 0;
        this.isNew = true;
        this.hasFallDamage = true;
        this.statsComponent.addAttributeModifier(Attributes.MOVEMENT_SPEED, SPEED_MODIFIER);
        this.statsComponent.addAttributeModifier((Attribute)ModAttributes.JUMP_HEIGHT.get(), JUMP_MODIFIER);
        this.addComponents(new AbilityComponent[]{this.poolComponent, this.statsComponent, this.damageTakenComponent, this.altModeComponent, this.continuousComponent, this.chargeComponent, this.projectileComponent, this.animationComponent});
        this.addCanUseCheck(AbilityHelper::canUseBrawlerAbilities);
        super.addCanUseCheck(ModifiedHumanHelper::checkModifiedHuamn);
        this.addCanUseCheck(AbilityLimits::fruitless);
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        if (!this.chargeComponent.isCharging() && !this.continuousComponent.isContinuous()) {
            if (!HakiHelper.hasInfusionActive(entity) && entity instanceof PlayerEntity) {
                entity.sendMessage(new StringTextComponent("You need to activate Hao Infusion to use this move!"), entity.getUUID());
                return;
            }

            if (!WyHelper.isInChallengeDimension(entity.level)) {
                boolean isOnMaxOveruse = HakiHelper.checkForHakiOveruse(entity, overuse);
                if (isOnMaxOveruse) {
                    return;
                }
            }

            if (this.altModeComponent.getCurrentMode() == GalaxyImpactRework.Mode.IMPACT) {
                if (entity instanceof PlayerEntity) {
                    this.chargeComponent.startCharging(entity, 50.0F);
                } else {
                    this.chargeComponent.startCharging(entity, 70.0F);
                }
            } else if (this.altModeComponent.getCurrentMode() == GalaxyImpactRework.Mode.DIVIDE) {
                this.chargeComponent.startCharging(entity, 60.0F);
            }
        }
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        if (this.altModeComponent.getCurrentMode() == GalaxyImpactRework.Mode.IMPACT) {
            this.hasFallDamage = false;
            this.galaxyImpactVisuals = null;
            this.proj = null;
            AbilityHelper.setDeltaMovement(entity, entity.getDeltaMovement().x, (double)4.0F, entity.getDeltaMovement().z);
            this.animationComponent.start(entity, ModAnimations.CHARGE_PUNCH);
            entity.addEffect(new EffectInstance((Effect)ModEffects.DIZZY.get(), 30, 0));
        }

        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.HAKI_RELEASE_SFX.get(), SoundCategory.PLAYERS, 3.0F, 0.5F + entity.getRandom().nextFloat());
        IHakiData hakiProps = HakiDataCapability.get(entity);
        float haoLevel = hakiProps.getTotalHakiExp() / 100.0F;
        if (haoLevel <= 1.0F) {
            this.radius = 10;
            this.haoMastery = 0;
        } else if (haoLevel > 1.0F && haoLevel <= 1.75F) {
            this.radius = 25;
            this.haoMastery = 1;
        } else if (haoLevel > 1.75F) {
            this.radius = 40;
            this.haoMastery = 2;
        }

        if (entity instanceof PlayerEntity) {
            this.color = new Color(HakiHelper.getHaoshokuColour(entity));
        }

        this.discharge = new LightningDischargeEntity(entity, entity.getX(), entity.getY() + (double)1.5F, entity.getZ(), entity.yRot, entity.xRot);
        this.discharge.setAliveTicks(-1);
        this.discharge.setUpdateRate(8);
        this.discharge.setLightningLength((float)(this.radius * 2));
        this.discharge.setColor(new Color(0, 0, 0, 100));
        this.discharge.setOutlineColor(this.color);
        this.discharge.setRenderTransparent();
        this.discharge.setDetails(16);
        int density = this.haoMastery == 2 ? 32 : 16;
        this.discharge.setDensity(density);
        this.discharge.setSize(1.0F);
        this.discharge.setSkipSegments(1);
        if (this.haoMastery == 0) {
            this.discharge.setSplit();
        }

        if (entity instanceof PlayerEntity) {
            entity.level.addFreshEntity(this.discharge);
            if (this.discharge != null) {
                this.discharge.setAliveTicks(80);
            }
        }
    }

    private void tickChargeEvent(LivingEntity entity, IAbility ability) {
        if (this.altModeComponent.getCurrentMode() == GalaxyImpactRework.Mode.IMPACT) {
            AbilityHelper.slowEntityFall(entity);
        }

        if (this.altModeComponent.getCurrentMode() == GalaxyImpactRework.Mode.DIVIDE) {
            entity.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN.getEffect(), 2, 2, false, false));
        }

        RayTraceResult mop = WyHelper.rayTraceBlocksAndEntities(entity, (double)-0.5F);
        double i = mop.getLocation().x;
        double j = mop.getLocation().y;
        double k = mop.getLocation().z;
        if (this.chargeComponent.getChargeTime() > 35.0F) {
            WyHelper.spawnParticleEffect((ParticleEffect)CartParticleEffects.GALAXY_IMPACT.get(), entity, i, j, k);
        }

        if (this.chargeComponent.getChargeTime() % 5.0F == 0.0F) {
            this.discharge.setPos(entity.getX(), entity.getY() + (double)1.0F, entity.getZ());
        }

        if (this.chargeComponent.getChargeTime() % 10.0F == 0.0F) {
            entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.HAKI_RELEASE_SFX.get(), SoundCategory.PLAYERS, 3.0F, 0.5F + entity.getRandom().nextFloat());
        }

        if (this.discharge != null && !entity.isAlive()) {
            this.discharge.setAliveTicks(0);
        }
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        if (this.altModeComponent.getCurrentMode() == GalaxyImpactRework.Mode.IMPACT) {
            if (this.discharge != null) {
                this.discharge.setAliveTicks(30);
            }

            this.proj = new GalaxyImpactReworkProjectile(entity.level, entity);
            entity.level.addFreshEntity(this.proj);
            this.proj.shootFromRotation(entity, entity.xRot, entity.yRot, 0.0F, IMPACT_PROJECTILE_SPEED, 0.0F);

            this.blockPos = new BlockPos(entity.getX(), entity.getY(), entity.getZ());

            if (!entity.level.isClientSide) {
                ((ServerWorld)entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
            }

            this.animationComponent.stop(entity);
            this.continuousComponent.startContinuity(entity, 60.0F);
        }

        if (this.altModeComponent.getCurrentMode() == GalaxyImpactRework.Mode.DIVIDE) {
            this.continuousComponent.startContinuity(entity, 60.0F);
        }
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        if (this.altModeComponent.getCurrentMode() == GalaxyImpactRework.Mode.DIVIDE) {
            this.statsComponent.applyModifiers(entity);
        }
    }

    private void tickContinuityEvent(LivingEntity entity, IAbility ability) {
        if (this.altModeComponent.getCurrentMode() == GalaxyImpactRework.Mode.IMPACT) {
            if (this.galaxyImpactVisuals == null && this.proj != null && this.proj.isFinished()) {
                this.blockPos = this.proj.blockPosition();

                this.galaxyImpactVisuals = new SphereEntity(entity.level, entity);
                Color col = WyHelper.intToRGB(HakiHelper.getHaoshokuColour(entity), 200);
                if (!(entity instanceof PlayerEntity)) {
                    col = new Color(35, 130, 255, 200);
                }

                this.galaxyImpactVisuals.setGlowing(true);
                this.galaxyImpactVisuals.setColor(col);
                this.galaxyImpactVisuals.setRadius(55.0F);
                this.galaxyImpactVisuals.setDetailLevel(32);
                this.galaxyImpactVisuals.setAnimationSpeed(1);
                this.galaxyImpactVisuals.setPos(
                        (double)this.blockPos.getX(),
                        (double)this.blockPos.getY(),
                        (double)this.blockPos.getZ());
                entity.level.addFreshEntity(this.galaxyImpactVisuals);
            }

            if (this.galaxyImpactVisuals != null) {
                this.galaxyImpactVisuals.setRadius(this.galaxyImpactVisuals.getRadius() + 0.15F);
            }

        } else if (this.altModeComponent.getCurrentMode() == GalaxyImpactRework.Mode.DIVIDE) {
            if (this.discharge != null && !entity.isAlive()) {
                this.discharge.setAliveTicks(0);
            }

            if (this.continuousComponent.getContinueTime() % 5.0F == 0.0F) {
                this.discharge.setPos(entity.getX(), entity.getY() + (double)1.0F, entity.getZ());
            }

            if (this.continuousComponent.getContinueTime() % 10.0F == 0.0F) {
                entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.HAKI_RELEASE_SFX.get(), SoundCategory.PLAYERS, 3.0F, 0.5F + entity.getRandom().nextFloat());
            }

            RayTraceResult mop = WyHelper.rayTraceBlocksAndEntities(entity, (double)-0.5F);
            double i = mop.getLocation().x;
            double j = mop.getLocation().y;
            double k = mop.getLocation().z;
            if (this.chargeComponent.getChargeTime() > 40.0F) {
                WyHelper.spawnParticleEffect((ParticleEffect)CartParticleEffects.GALAXY_IMPACT.get(), entity, i, j, k);
            }

            if (entity.swinging) {
                this.continuousComponent.stopContinuity(entity);
            }

            if (AbilityHelper.canUseMomentumAbilities(entity)) {
                if (entity.isSprinting()) {
                    if (!this.prevSprintValue) {
                        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.TELEPORT_SFX.get(), SoundCategory.PLAYERS, 2.0F, 0.8F);
                    }

                    float maxSpeed = 4.0F;
                    Vector3d vec = entity.getLookAngle();
                    if (entity.isOnGround()) {
                        double xDir = vec.x * (double)maxSpeed;
                        double zDir = vec.z * (double)maxSpeed;
                        AbilityHelper.setDeltaMovement(entity, xDir, entity.getDeltaMovement().y, zDir);
                    } else {
                        double xDir = vec.x * (double)maxSpeed * (double)0.5F;
                        double zDir = vec.z * (double)maxSpeed;
                        AbilityHelper.setDeltaMovement(entity, xDir, entity.getDeltaMovement().y, zDir * (double)0.5F);
                    }

                    this.prevSprintValue = entity.isSprinting();
                } else {
                    this.prevSprintValue = false;
                }
            }
        }
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        if (this.altModeComponent.getCurrentMode() == GalaxyImpactRework.Mode.IMPACT) {
            if (this.galaxyImpactVisuals != null) {
                this.galaxyImpactVisuals.remove();
            }

            this.cooldownComponent.startCooldown(entity, 1800.0F);
        } else if (this.altModeComponent.getCurrentMode() == GalaxyImpactRework.Mode.DIVIDE) {
            if (this.discharge != null) {
                this.discharge.setAliveTicks(30);
            }

            this.statsComponent.removeModifiers(entity);
            if (!entity.level.isClientSide) {
                ((ServerWorld)entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
            }

            this.projectileComponent.shoot(entity, 3.75F, 1.0F);
            this.cooldownComponent.startCooldown(entity, 1400.0F);
        }
    }

    private float onDamageTaken(LivingEntity entity, IAbility ability, DamageSource damageSource, float damage) {
        if (!this.hasFallDamage && damageSource == DamageSource.FALL) {
            this.hasFallDamage = true;
            return 0.0F;
        } else {
            return damage;
        }
    }

    private AbilityProjectileEntity createProjectile(LivingEntity entity) {
        if (this.altModeComponent.getCurrentMode() == GalaxyImpactRework.Mode.IMPACT) {
            return new GalaxyImpactReworkProjectile(entity.level, entity);
        } else {
            return new GalaxyDivideProjectile(entity.level, entity);
        }
    }

    private void onAltModeChange(LivingEntity entity, IAbility ability, Mode mode) {
        if (!this.chargeComponent.isCharging() && !this.continuousComponent.isContinuous()) {
            if (mode == GalaxyImpactRework.Mode.IMPACT) {
                this.setDisplayIcon(IMPACT_ICON);
                this.setDisplayName(IMPACT_NAME);
            } else if (mode == GalaxyImpactRework.Mode.DIVIDE) {
                this.setDisplayIcon(DIVIDE_ICON);
                this.setDisplayName(DIVIDE_NAME);
            }
        }
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.isBrawler() && questProps.hasFinishedQuest(CartQuests.BRAWLER_TRIAL_08);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Galaxy Impact", AbilityCategory.STYLE, GalaxyImpactRework::new)).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{(e, a) -> IMPACT_NAME.copy().setStyle(Style.EMPTY.withColor(TextFormatting.GREEN)), (e, a) -> DESCRIPTION[0], DealDamageComponent.getTooltip(80.0F), ChargeComponent.getTooltip(50.0F), CooldownComponent.getTooltip(1800.0F)}).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, (e, a) -> DIVIDE_NAME.copy().setStyle(Style.EMPTY.withColor(TextFormatting.GREEN)), (e, a) -> DESCRIPTION[1], DealDamageComponent.getTooltip(110.0F), ChargeComponent.getTooltip(60.0F), ContinuousComponent.getTooltip(80.0F), CooldownComponent.getTooltip(1400.0F), ChangeStatsComponent.getTooltip()}).setSourceHakiNature(SourceHakiNature.HARDENING).setUnlockCheck(GalaxyImpactRework::canUnlock).build();
        SPEED_MODIFIER = new AbilityAttributeModifier(UUID.fromString("83979d24-62a4-4014-b1db-44e22a641511"), INSTANCE, "Galaxy Divide Modifier", 0.02, Operation.ADDITION);
        JUMP_MODIFIER = new AbilityAttributeModifier(UUID.fromString("b07997f2-d0a2-4a98-a083-f2f237cb7b4e"), INSTANCE, "Galaxy Divide Jump Modifier", (double)3.0F, Operation.ADDITION);
    }

    public static enum Mode {
        IMPACT,
        DIVIDE;

        private Mode() {
        }
    }
}