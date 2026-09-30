/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.LivingEntity
 *  net.minecraft.entity.player.PlayerEntity
 *  net.minecraft.entity.projectile.ProjectileEntity
 *  net.minecraft.inventory.EquipmentSlotType
 *  net.minecraft.item.ItemStack
 *  net.minecraft.network.IPacket
 *  net.minecraft.network.play.server.SAnimateHandPacket
 *  net.minecraft.particles.IParticleData
 *  net.minecraft.particles.ParticleTypes
 *  net.minecraft.potion.Effect
 *  net.minecraft.potion.EffectInstance
 *  net.minecraft.util.DamageSource
 *  net.minecraft.util.ResourceLocation
 *  net.minecraft.util.SoundCategory
 *  net.minecraft.util.SoundEvent
 *  net.minecraft.util.SoundEvents
 *  net.minecraft.util.math.BlockPos
 *  net.minecraft.util.math.MathHelper
 *  net.minecraft.util.math.vector.Vector3d
 *  net.minecraft.util.text.ITextComponent
 *  net.minecraft.util.text.StringTextComponent
 *  net.minecraft.util.text.Style
 *  net.minecraft.util.text.TextFormatting
 *  net.minecraft.util.text.TranslationTextComponent
 *  net.minecraft.world.IWorld
 *  net.minecraft.world.World
 *  net.minecraft.world.server.ServerWorld
 *  org.apache.commons.lang3.tuple.ImmutablePair
 *  org.apache.commons.lang3.tuple.Pair
 *  xyz.pixelatedw.mineminenomi.api.abilities.Ability
 *  xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory
 *  xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore
 *  xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore$Builder
 *  xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine
 *  xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine$IDescriptionLine
 *  xyz.pixelatedw.mineminenomi.api.abilities.AbilityPool2
 *  xyz.pixelatedw.mineminenomi.api.abilities.IAbility
 *  xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent
 *  xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent
 *  xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent
 *  xyz.pixelatedw.mineminenomi.api.abilities.components.ChangeStatsComponent
 *  xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent
 *  xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent
 *  xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent
 *  xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent
 *  xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent
 *  xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent
 *  xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent
 *  xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent
 *  xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent$RangeType
 *  xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource
 *  xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature
 *  xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper
 *  xyz.pixelatedw.mineminenomi.api.helpers.HakiHelper
 *  xyz.pixelatedw.mineminenomi.api.helpers.TargetHelper
 *  xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability
 *  xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats
 *  xyz.pixelatedw.mineminenomi.data.entity.haki.HakiDataCapability
 *  xyz.pixelatedw.mineminenomi.data.entity.haki.IHakiData
 *  xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData
 *  xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability
 *  xyz.pixelatedw.mineminenomi.entities.LightningDischargeEntity
 *  xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity
 *  xyz.pixelatedw.mineminenomi.init.ModAbilityPools
 *  xyz.pixelatedw.mineminenomi.init.ModAnimations
 *  xyz.pixelatedw.mineminenomi.init.ModDamageSource
 *  xyz.pixelatedw.mineminenomi.init.ModEffects
 *  xyz.pixelatedw.mineminenomi.init.ModI18n
 *  xyz.pixelatedw.mineminenomi.init.ModSounds
 *  xyz.pixelatedw.mineminenomi.wypi.WyHelper
 */
package net.kazi.kazimod.abilities.SaberRework;

import java.awt.Color;
import java.util.List;
import java.util.function.Predicate;
import net.MrMagicalCart.cartaddon.abilities.modifiedhuman.ModifiedHumanHelper;
import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.cartapi.CartRegistry;
import net.kazi.kazimod.entities.projectiles.DivineDepartureProjectileClone;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.HitTrackerComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.HakiHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.TargetHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.haki.HakiDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.haki.IHakiData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.entities.LightningDischargeEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModI18n;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class DivineDepartureClone
extends Ability {
    private static final TranslationTextComponent DIVINE_DEPARTURE = new TranslationTextComponent(CartRegistry.registerName("ability.cartaddon.divine_departure", "Divine Departure"));
    private static final TranslationTextComponent DIVINE_DEPARTURE_DASH = new TranslationTextComponent(CartRegistry.registerName("ability.cartaddon.divine_departure_dash", "Divine Departure: Dash"));
    private static final ResourceLocation PROJECTILE_ICON = new ResourceLocation("kazimod", "textures/abilities/divine_departure.png");
    private static final ResourceLocation DASH_ICON = new ResourceLocation("kazimod", "textures/abilities/divine_departure_dash.png");
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText((String)"cartaddon", (String)"divine_departure", (Pair[])new Pair[]{ImmutablePair.of((Object)"The user fires a haki infused slash that has immense knockback and drains their haki. (Fruitless)", null), ImmutablePair.of((Object)"The user dashes and fires a haki infused slash that drains an immense amount of haki. (Fruitless)", null)});
    private static final float COOLDOWN = 1500.0f;
    private static final int CHARGE_TIME = 20;
    private static final float DAMAGE = 60.0f;
    private static final float RANGE = 2.5f;
    private static final float MAX_TELEPORT_DISTANCE = 15.0f;
    public static final AbilityCore<DivineDepartureClone> INSTANCE;
    private final ChargeComponent chargeComponent = new ChargeComponent((IAbility)this).addStartEvent(this::startChargeEvent).addTickEvent(this::duringChargeEvent).addEndEvent(this::endChargeEvent);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent((IAbility)this);
    private final RangeComponent rangeComponent = new RangeComponent((IAbility)this);
    private final AnimationComponent animationComponent = new AnimationComponent((IAbility)this);
    private final HitTrackerComponent hitTrackerComponent = new HitTrackerComponent((IAbility)this);
    private final AltModeComponent<Mode> altModeComponent = new AltModeComponent<Mode>(this, Mode.class, Mode.PROJECTILE).addChangeModeEvent(this::onAltModeChange);
    private final ProjectileComponent projectileComponent = new ProjectileComponent((IAbility)this, this::createProjectile);
    public static int overuse;
    private LightningDischargeEntity discharge;
    private Color color = new Color(0xFF0000);
    private int radius = 0;
    private int haoMastery = 0;
    private final PoolComponent poolComponent = new PoolComponent((IAbility)this, ModAbilityPools.GRAB_ABILITY, new AbilityPool2[0]);

    public DivineDepartureClone(AbilityCore<DivineDepartureClone> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.poolComponent, this.altModeComponent, this.projectileComponent, this.chargeComponent, this.dealDamageComponent, this.rangeComponent, this.animationComponent, this.hitTrackerComponent});
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addCanUseCheck(AbilityHelper::canUseSwordsmanAbilities);
        super.addCanUseCheck(ModifiedHumanHelper::checkModifiedHuamn);
        this.addCanUseCheck(AbilityLimits::fruitless);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        LivingEntity player;
        IEntityStats props;
        boolean isOnMaxOveruse;
        if (!HakiHelper.hasInfusionActive((LivingEntity)entity) && entity instanceof PlayerEntity) {
            entity.sendMessage(new StringTextComponent("You need to activate Hao Infusion to use this move!"), entity.getUUID());
            return;
        }
        if (!WyHelper.isInChallengeDimension((World)entity.level) && (isOnMaxOveruse = HakiHelper.checkForHakiOveruse((LivingEntity)entity, (int)((props = EntityStatsCapability.get((LivingEntity)(player = entity))).isHuman() ? (int)((double)overuse * 0.7) : overuse)))) {
            return;
        }
        if (!AbilityHelper.canUseSwordsmanAbilities((LivingEntity)entity)) {
            entity.sendMessage(new TranslationTextComponent(ModI18n.ABILITY_MESSAGE_NEED_SWORD), entity.getUUID());
            return;
        }
        if (this.altModeComponent.getCurrentMode() == Mode.DASH) {
            this.chargeComponent.startCharging(entity, 20.0f);
        } else if (this.altModeComponent.getCurrentMode() == Mode.PROJECTILE) {
            this.chargeComponent.startCharging(entity, 10.0f);
        }
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.hitTrackerComponent.clearHits();
        this.animationComponent.start(entity, ModAnimations.BATTO_STRIKE);
        entity.level.playSound(null, entity.blockPosition(), ModSounds.HAKI_RELEASE_SFX.get(), SoundCategory.PLAYERS, 3.0f, 0.5f + entity.getRandom().nextFloat());
        IHakiData hakiProps = HakiDataCapability.get((LivingEntity)entity);
        float haoLevel = hakiProps.getTotalHakiExp() / 100.0f;
        if (haoLevel <= 1.0f) {
            this.radius = 10;
            this.haoMastery = 0;
        } else if (haoLevel > 1.0f && haoLevel <= 1.75f) {
            this.radius = 25;
            this.haoMastery = 1;
        } else if (haoLevel > 1.75f) {
            this.radius = 40;
            this.haoMastery = 2;
        }
        if (entity instanceof PlayerEntity) {
            this.color = new Color(HakiHelper.getHaoshokuColour((LivingEntity)entity));
        }
        this.discharge = new LightningDischargeEntity((Entity)entity, entity.getX(), entity.getY() + 1.5, entity.getZ(), entity.yRot, entity.xRot);
        this.discharge.setAliveTicks(-1);
        this.discharge.setUpdateRate(8);
        this.discharge.setLightningLength((float)(this.radius * 2));
        this.discharge.setColor(new Color(0, 0, 0, 100));
        this.discharge.setOutlineColor(this.color);
        this.discharge.setRenderTransparent();
        this.discharge.setDetails(16);
        int density = this.haoMastery == 2 ? 32 : 16;
        this.discharge.setDensity(density);
        this.discharge.setSize(1.0f);
        this.discharge.setSkipSegments(1);
        if (this.haoMastery == 0) {
            this.discharge.setSplit();
        }
        if (entity instanceof PlayerEntity) {
            entity.level.addFreshEntity((Entity)this.discharge);
            if (this.discharge != null) {
                this.discharge.setAliveTicks(40);
            }
        }
    }

    private void duringChargeEvent(LivingEntity entity, IAbility ability) {
        entity.addEffect(new EffectInstance((Effect)ModEffects.MOVEMENT_BLOCKED.get(), 5, 1, false, false));
        if (this.chargeComponent.getChargeTime() % 5.0f == 0.0f) {
            this.discharge.setPos(entity.getX(), entity.getY() + 1.0, entity.getZ());
        }
        if (this.chargeComponent.getChargeTime() % 10.0f == 0.0f) {
            entity.level.playSound(null, entity.blockPosition(), ModSounds.HAKI_RELEASE_SFX.get(), SoundCategory.PLAYERS, 3.0f, 0.5f + entity.getRandom().nextFloat());
        }
        if (this.discharge != null && !entity.isAlive()) {
            this.discharge.setAliveTicks(0);
        }
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        if (this.altModeComponent.getCurrentMode() == Mode.DASH) {
            ItemStack stack = entity.getMainHandItem();
            stack.hurtAndBreak(1, entity, user -> user.broadcastBreakEvent(EquipmentSlotType.MAINHAND));
            BlockPos blockpos = WyHelper.rayTraceBlockSafe((LivingEntity)entity, (float)15.0f);
            AbilityDamageSource source = (AbilityDamageSource)((ModDamageSource)this.dealDamageComponent.getDamageSource(entity)).setSlash();
            Vector3d startPos = entity.position();
            float actualTeleportDistance = 15.0f;
            for (double f = 0.0; f < 1.0; f += 0.13) {
                double z;
                double y;
                double x = MathHelper.lerp((double)f, (double)startPos.x, (double)blockpos.getX());
                Vector3d pos = new Vector3d(x, y = MathHelper.lerp((double)f, (double)startPos.y, (double)blockpos.getY()), z = MathHelper.lerp((double)f, (double)startPos.z, (double)blockpos.getZ()));
                List projectiles = WyHelper.getNearbyEntities((Vector3d)pos, (IWorld)entity.level, (double)entity.getBbWidth(), (double)entity.getBbHeight(), (double)entity.getBbWidth(), (Predicate)null, (Class[])new Class[]{ProjectileEntity.class});
                if (projectiles.isEmpty()) continue;
                projectiles.sort(TargetHelper.closestComparator((Vector3d)startPos));
                actualTeleportDistance = MathHelper.sqrt(((ProjectileEntity)projectiles.get(0)).distanceToSqr(startPos));
                break;
            }
            blockpos = WyHelper.rayTraceBlockSafe((LivingEntity)entity, (float)actualTeleportDistance);
            List<LivingEntity> targets = this.rangeComponent.getTargetsInLine(entity, actualTeleportDistance, 2.5f);
            for (LivingEntity target : targets) {
                boolean flag;
                if (!this.hitTrackerComponent.canHit((Entity)target) || !(flag = this.dealDamageComponent.hurtTarget(entity, target, 60.0f, (DamageSource)source)) || entity.level.isClientSide) continue;
                WyHelper.spawnParticles(ParticleTypes.SWEEP_ATTACK, (ServerWorld)entity.level, target.getX(), target.getY() + target.getEyeHeight(), target.getZ());
            }
            entity.unRide();
            entity.teleportTo((double)blockpos.getX(), (double)blockpos.getY(), (double)blockpos.getZ());
            entity.level.playSound(null, entity.blockPosition(), ModSounds.DASH_ABILITY_SWOOSH_SFX.get(), SoundCategory.PLAYERS, 2.0f, 1.0f);
        }
        if (!entity.level.isClientSide) {
            ((ServerWorld)entity.level).getChunkSource().broadcastAndSend((Entity)entity, (IPacket)new SAnimateHandPacket((Entity)entity, 0));
        }
        this.animationComponent.stop(entity);
        DivineDepartureProjectileClone proj = new DivineDepartureProjectileClone(entity.level, entity);
        if (this.altModeComponent.getCurrentMode() == Mode.DASH) {
            proj.setLife(20);
        }
        this.projectileComponent.shoot((AbilityProjectileEntity)proj, entity, 3.0f, 1.0f);
        this.cooldownComponent.startCooldown(entity, 1500.0f);
        entity.level.playSound(null, entity.blockPosition(), ModSounds.HAKI_RELEASE_SFX.get(), SoundCategory.PLAYERS, 1.0f, 1.0f);
        entity.level.playSound(null, entity.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0f, 1.0f);
        if (this.discharge != null) {
            this.discharge.setAliveTicks(30);
        }
    }

    private DivineDepartureProjectileClone createProjectile(LivingEntity entity) {
        DivineDepartureProjectileClone proj = new DivineDepartureProjectileClone(entity.level, entity);
        return proj;
    }

    private void onAltModeChange(LivingEntity entity, IAbility ability, Mode mode) {
        if (!this.chargeComponent.isCharging()) {
            if (mode == Mode.PROJECTILE) {
                this.setDisplayIcon(PROJECTILE_ICON);
            } else if (mode == Mode.DASH) {
                this.setDisplayIcon(DASH_ICON);
            }
        }
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        }
        PlayerEntity player = (PlayerEntity)entity;
        IEntityStats props = EntityStatsCapability.get((LivingEntity)player);
        IQuestData questProps = QuestDataCapability.get((PlayerEntity)player);
        return props.getFightingStyle().equals((Object)CartValues.SABER) && questProps.hasFinishedQuest(CartQuests.SABER_TRIAL_08);
    }

    static {
        overuse = 3000;
        INSTANCE = new AbilityCore.Builder("Divine Departure", AbilityCategory.STYLE, DivineDepartureClone::new).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{(e, a) -> DIVINE_DEPARTURE.copy().setStyle(Style.EMPTY.withColor(TextFormatting.GREEN)), (e, a) -> DESCRIPTION[0], ChargeComponent.getTooltip((float)10.0f), CooldownComponent.getTooltip((float)1500.0f)}).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, (e, a) -> DIVINE_DEPARTURE_DASH.copy().setStyle(Style.EMPTY.withColor(TextFormatting.GREEN)), (e, a) -> DESCRIPTION[1], DealDamageComponent.getTooltip((float)30.0f), ChargeComponent.getTooltip((float)20.0f), ContinuousComponent.getTooltip((float)80.0f), CooldownComponent.getTooltip((float)1500.0f), ChangeStatsComponent.getTooltip(), RangeComponent.getTooltip((float)15.0f, (RangeComponent.RangeType)RangeComponent.RangeType.LINE)}).setSourceHakiNature(SourceHakiNature.IMBUING).setUnlockCheck(DivineDepartureClone::canUnlock).build();
    }

    public static enum Mode {
        PROJECTILE,
        DASH;

    }
}



