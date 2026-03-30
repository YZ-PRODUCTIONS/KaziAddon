package net.kazi.kazimod.abilities.HakiRework;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.CombatRules;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.abilities.LogiaInvulnerabilityAbility;
import xyz.pixelatedw.mineminenomi.abilities.haki.KenbunshokuHakiAuraAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityOverlay;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityPool2;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.SkinOverlayComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.StackComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.HakiHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.haki.HakiDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.haki.IHakiData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.init.ModResources;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.packets.server.ability.SUpdateEquippedAbilityPacket;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

public class KenbunshokuHakiFutureSightRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "kenbunshoku_haki_future_sight", new Pair[]{ImmutablePair.of("Using Observation Haki allows the user to see a short period into the future to avoid attacks.", null)});
    private static final int MIN_COOLDOWN = 100;
    public static final AbilityCore<KenbunshokuHakiFutureSightRework> INSTANCE;
    public static final AbilityOverlay OVERLAY;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this, true)).addStartEvent(this::startContinuityEvent).addTickEvent(this::duringContinuityEvent).addEndEvent(this::endContinuityEvent);
    private final DamageTakenComponent damageTakenComponent = (new DamageTakenComponent(this)).addOnAttackEvent(this::damageTakenEvent);
    private final SkinOverlayComponent skinOverlayComponent;
    private final PoolComponent poolComponent;
    private final StackComponent stackComponent;
    private boolean hasDodged;
    private int protTimer;
    private int invulnerabilityTimer;

    public KenbunshokuHakiFutureSightRework(AbilityCore<KenbunshokuHakiFutureSightRework> core) {
        super(core);
        this.skinOverlayComponent = new SkinOverlayComponent(this, OVERLAY, new AbilityOverlay[0]);
        this.poolComponent = new PoolComponent(this, ModAbilityPools.DODGE_ABILITY, new AbilityPool2[0]);
        this.stackComponent = (new StackComponent(this)).addStackChangeEvent(this::changeStackEvent);
        this.invulnerabilityTimer = 0;
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.damageTakenComponent, this.skinOverlayComponent, this.poolComponent, this.stackComponent});
        this.addCanUseCheck(HakiHelper::canEnableHaki);
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addCanUseCheck(AbilityHelper::requiresFocus);
        this.addUseEvent(this::useEvent);
        this.addEquipEvent(this::equipEvent);
    }

    private void equipEvent(LivingEntity entity, Ability ability) {
        this.stackComponent.setDefaultStacks(this.calculateMaxProtection(entity));
        this.stackComponent.revertStacksToDefault(entity, ability);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity);
    }

    private void changeStackEvent(LivingEntity entity, IAbility ability, int stacks) {
        if (stacks <= 0) {
            WyHelper.spawnParticleEffect((ParticleEffect) ModParticleEffects.HAKI_GUARD.get(), entity, entity.getX(), entity.getY(), entity.getZ());
            this.continuousComponent.stopContinuity(entity);
        }
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.stackComponent.setDefaultStacks(this.calculateMaxProtection(entity));
        this.stackComponent.revertStacksToDefault(entity, ability);
        this.invulnerabilityTimer = 0;
        entity.level.playSound((PlayerEntity) null, entity.blockPosition(), (SoundEvent) ModSounds.KENBUNSHOKU_HAKI_ON_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.0F);
    }

    private void duringContinuityEvent(LivingEntity entity, IAbility ability) {
        boolean isOnMaxOveruse = HakiHelper.checkForHakiOveruse(entity, 2);
        if (isOnMaxOveruse) {
            WyHelper.spawnParticleEffect((ParticleEffect) ModParticleEffects.HAKI_GUARD.get(), entity, entity.getX(), entity.getY(), entity.getZ());
            this.continuousComponent.stopContinuity(entity);
        }

        if (this.protTimer > 0) {
            --this.protTimer;
        } else if (this.hasDodged) {
            this.skinOverlayComponent.hideAll(entity);
            this.hasDodged = false;
        }

        if (this.invulnerabilityTimer > 0) {
            --this.invulnerabilityTimer;
        }
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        entity.level.playSound((PlayerEntity) null, entity.blockPosition(), (SoundEvent) ModSounds.KENBUNSHOKU_HAKI_OFF.get(), SoundCategory.PLAYERS, 2.0F, 1.0F);
        this.skinOverlayComponent.hideAll(entity);
        float protLost = (float) ((this.stackComponent.getDefaultStacks() - this.stackComponent.getStacks()) * 6);
        float cooldown = 100.0F + WyHelper.secondsToTicks(protLost);
        this.cooldownComponent.startCooldown(entity, cooldown);
        this.stackComponent.revertStacksToDefault(entity, this);
    }

    private int calculateMaxProtection(LivingEntity entity) {
        IEntityStats sprops = EntityStatsCapability.get(entity);
        IHakiData hakiProps = HakiDataCapability.get(entity);
        double dorikiPower = sprops.getDoriki() / 5000.0D;
        double hakiPower = hakiProps.getKenbunshokuHakiExp() / 12.0F;
        return (int) Math.max(1L, Math.round(dorikiPower + hakiPower));
    }

    public void reduceProtection(LivingEntity entity, float damage) {
        if (this.invulnerabilityTimer == 0) {
            entity.level.playSound((PlayerEntity) null, entity.blockPosition(), (SoundEvent) ModSounds.FUTURE_SIGHT_HIT.get(), SoundCategory.PLAYERS, 2.0F, 0.75F + entity.getRandom().nextFloat() / 2.0F);
            int stacks = Math.max(1, Math.round(damage / 15.0F));
            this.stackComponent.addStacks(entity, this, -stacks);
            this.protTimer = 4;
            this.invulnerabilityTimer = 10;
            this.hasDodged = true;
        }

        WyNetwork.sendToAllTrackingAndSelf(new SUpdateEquippedAbilityPacket(entity, this), entity);
    }

    public CompoundNBT save(CompoundNBT nbt) {
        nbt = super.save(nbt);
        nbt.putInt("protectionTimer", this.protTimer);
        nbt.putInt("invulnerabilityTimer", this.invulnerabilityTimer);
        return nbt;
    }

    public void load(CompoundNBT nbt) {
        super.load(nbt);
        this.protTimer = nbt.getInt("protectionTimer");
        this.invulnerabilityTimer = nbt.getInt("invulnerabilityTimer");
    }

    public float damageTakenEvent(LivingEntity entity, IAbility ability, DamageSource source, float damage) {
        if (!super.isContinuous() || !AbilityHelper.canUseMomentumAbilities(entity) || AbilityHelper.isGrabbing(entity)) {
            return damage;
        }

        IHakiData hakiProps = HakiDataCapability.get(entity);
        int hakiOveruse = 10 + hakiProps.getMaxOveruse() / 1180;
        boolean isLogia = DevilFruitCapability.get(entity).isLogia();
        if (isLogia) {
            IAbilityData abilityProps = AbilityDataCapability.get(entity);
            for (IAbility otherAbility : abilityProps.getPassiveAbilities(AbilityCategory.DEVIL_FRUITS.isAbilityPartofCategory())) {
                if (otherAbility instanceof LogiaInvulnerabilityAbility) {
                    break;
                }
            }
            hakiOveruse /= 3;
        } else if (source.isExplosion()) {
            return damage;
        }

        ArrayList<DamageSource> damageableSources = new ArrayList<>(Arrays.asList(DamageSource.LAVA, DamageSource.IN_WALL, DamageSource.CACTUS, DamageSource.SWEET_BERRY_BUSH, DamageSource.STARVE, DamageSource.ANVIL, DamageSource.FLY_INTO_WALL, DamageSource.FALL, DamageSource.FALLING_BLOCK, DamageSource.OUT_OF_WORLD, DamageSource.WITHER, DamageSource.MAGIC, DamageSource.IN_FIRE, DamageSource.ON_FIRE, DamageSource.LIGHTNING_BOLT));
        if (this.getInvulnerabilityTimer() > 0 && !damageableSources.contains(source) && !source.getMsgId().equals("special") && !isLogia) {
            return 0.0F;
        }

        if (!source.isBypassArmor()) {
            damage = CombatRules.getDamageAfterAbsorb(damage, (float) entity.getArmorValue(), (float) entity.getAttribute(Attributes.ARMOR_TOUGHNESS).getValue());
        }

        int absorbed = EnchantmentHelper.getDamageProtection(entity.getArmorSlots(), source);
        if (absorbed > 0) {
            damage = CombatRules.getDamageAfterMagicAbsorb(damage, (float) absorbed);
        }

        if (damage < 0.0F) {
            return 0.0F;
        }

        boolean baseCondition = damageableSources.stream().noneMatch((s) -> source.getMsgId().equals(s.getMsgId()));
        boolean isUnavoidable = source instanceof ModDamageSource && ((ModDamageSource) source).isUnavoidable() || source instanceof EntityDamageSource && ((EntityDamageSource) source).isThorns();
        if (this.getInvulnerabilityTimer() > 0) {
            return 0.0F;
        }

        if (baseCondition && !isUnavoidable) {
            this.skinOverlayComponent.showAll(entity);
            this.reduceProtection(entity, damage);
            return 0.0F;
        }

        return damage;
    }

    private static boolean canUnlock(LivingEntity user) {
        IAbilityData abilityProps = AbilityDataCapability.get(user);
        IHakiData props = HakiDataCapability.get(user);
        IEntityStats statsProps = EntityStatsCapability.get(user);
        boolean hasAuraUnlocked = abilityProps.hasUnlockedAbility(KenbunshokuHakiAuraAbility.INSTANCE);
        return hasAuraUnlocked && statsProps.getDoriki() > 6000.0D && props.getKenbunshokuHakiExp() > HakiHelper.getKenbunshokuFutureSightExpNeeded(user);
    }

    public int getInvulnerabilityTimer() {
        return this.invulnerabilityTimer;
    }

    public int getProtectionStacks() {
        return this.stackComponent.getStacks();
    }

    public int getMaxProtectionStacks() {
        return this.stackComponent.getDefaultStacks();
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Kenbunshoku Haki: Future Sight", AbilityCategory.HAKI, KenbunshokuHakiFutureSightRework::new)).addDescriptionLine(DESCRIPTION).setUnlockCheck(KenbunshokuHakiFutureSightRework::canUnlock).build();
        OVERLAY = (new AbilityOverlay.Builder()).setTexture(ModResources.BUSOSHOKU_HAKI_ARM).setColor(new Color(255, 100, 200, 100)).build();
    }
}
