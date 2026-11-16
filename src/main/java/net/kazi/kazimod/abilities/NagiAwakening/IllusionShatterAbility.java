package net.kazi.kazimod.abilities.NagiAwakening;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kazi.kazimod.init.KaziEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.ISweep;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.events.WyLivingAttackEvent;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.protection.ProtectedArea;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.world.ProtectedAreasData;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

@Mod.EventBusSubscriber(modid = "kazimod")
public class IllusionShatterAbility extends PassiveAbility2 {
    public static final double REACH = 3.5D;
    public static final int SUPPRESSION_TICKS = 80;
    private final Set<UUID> suppressedThisTick = new HashSet<>();
    private long lastSuppressionTick = Long.MIN_VALUE;
    // Preserve saved passive ownership while changing its displayed name.
    public static final AbilityCore<IllusionShatterAbility> INSTANCE = new AbilityCore.Builder<>(
            "Imagine Breaker", "illusion_shatter", AbilityCategory.DEVIL_FRUITS, AbilityType.PASSIVE, IllusionShatterAbility::new)
            .setUnlockCheck(NagiAwakeningSupport::canUnlock)
            .setIcon(new ResourceLocation("mineminenomi", "textures/abilities/strong_right.png"))
            .setSourceType(SourceType.FIST)
            .addDescriptionLine(new TranslationTextComponent("ability.kazimod.illusion_shatter.description"))
            .addAdvancedDescriptionLine(AbilityDescriptionLine.NEW_LINE,
                    RangeComponent.getTooltip((float) REACH, RangeComponent.RangeType.LINE))
            .build();

    public IllusionShatterAbility(AbilityCore<IllusionShatterAbility> core) {
        super(core);
    }

    @Override public void tick(LivingEntity user) {
        super.tick(user);
        ImagineBreakerNegation.tick(user);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFruitAttack(WyLivingAttackEvent event) {
        // Cancel before MMNM applies damage, knockback or damage-triggered effects.
        if (blocksFruitAttack(event.getEntityLiving(), event.getSource())) {
            event.setCanceled(true);
            NagiAwakeningSupport.playNullificationSound(event.getEntityLiving(), event.getEntityLiving().position());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onForgeFruitAttack(LivingAttackEvent event) {
        if (blocksFruitAttack(event.getEntityLiving(), event.getSource())) {
            event.setCanceled(true);
            NagiAwakeningSupport.playNullificationSound(event.getEntityLiving(), event.getEntityLiving().position());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onFruitHurt(LivingHurtEvent event) {
        // Some melee abilities replace their damage source after the attack event.
        if (blocksFruitAttack(event.getEntityLiving(), event.getSource())) {
            event.setCanceled(true);
            NagiAwakeningSupport.playNullificationSound(event.getEntityLiving(), event.getEntityLiving().position());
        }
    }

    private static boolean blocksFruitAttack(LivingEntity user, DamageSource source) {
        // Ordinary melee from a fruit user remains ordinary damage.
        return getActiveNegation(user) != null && NagiAwakeningSupport.isDevilFruitAttack(source);
    }

    /** One live health/state gate for damage, projectiles and every beam implementation. */
    static IllusionShatterAbility getActiveNegation(LivingEntity user) {
        if (user == null || user.level.isClientSide || !user.isAlive() || user.isSpectator()
                || !IllusionShatterHealth.immune(user.getHealth(), user.getMaxHealth())
                || !NagiAwakeningSupport.canUnlock(user)) return null;
        IAbilityData data = AbilityDataCapability.get(user);
        IllusionShatterAbility passive = data == null ? null : data.getPassiveAbility(INSTANCE);
        // Passive protection does not require an active cast or an empty hand.
        if (passive == null || passive.disableComponent.isDisabled() || passive.isPaused()
                || user.hasEffect(KaziEffects.IMAGINE_BROKEN.get())
                || user.hasEffect(KaziEffects.LOST_WORKS_FRUIT_LOCK.get())
                || !allowsAbilityAt(user)) return null;
        return passive;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMeleeContact(AttackEntityEvent event) {
        if (event.getTarget() instanceof LivingEntity)
            meleeContact(event.getPlayer(), (LivingEntity) event.getTarget());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onNativeMeleeHit(WyLivingAttackEvent event) {
        confirmedMeleeHit(event.getEntityLiving(), event.getSource());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onMeleeHit(LivingAttackEvent event) {
        confirmedMeleeHit(event.getEntityLiving(), event.getSource());
    }

    private static void confirmedMeleeHit(LivingEntity target, DamageSource source) {
        // Weapon/item attacks can enter the damage path separately from the click event.
        // Only ordinary direct melee qualifies; thrown weapons and fruit moves do not.
        if (source == null || !"player".equals(source.getMsgId())
                || source instanceof AbilityDamageSource || source.isProjectile()
                || source.isExplosion() || source.isMagic()
                || !(source.getEntity() instanceof PlayerEntity)
                || source.getDirectEntity() != source.getEntity()) return;
        PlayerEntity user = (PlayerEntity) source.getEntity();
        if (user instanceof ISweep && ((ISweep) user).isSweeping()) return;
        meleeContact(user, target);
    }

    private static void meleeContact(PlayerEntity user, LivingEntity target) {
        if (user == null || user.level.isClientSide || !user.isAlive() || user.isSpectator()
                || target == null || target.level != user.level || !NagiAwakeningSupport.canUnlock(user)) return;
        IAbilityData data = AbilityDataCapability.get(user);
        IllusionShatterAbility passive = data == null ? null : data.getPassiveAbility(INSTANCE);
        if (passive != null) passive.trySuppress(user, target);
    }

    private void trySuppress(PlayerEntity user, LivingEntity target) {
        // Any held weapon or empty hand is allowed; passive state still applies.
        if (!canTriggerSuppression(user) || !target.isAlive() || !NagiAwakeningSupport.isEnemy(user, target)
                || !allowsAbilityAt(user) || !allowsAbilityAt(target)) return;
        long now = user.level.getGameTime();
        if (now != lastSuppressionTick) {
            lastSuppressionTick = now;
            suppressedThisTick.clear();
        }
        if (suppressedThisTick.contains(target.getUUID())) return;
        if (target instanceof PlayerEntity && (!user.canHarmPlayer((PlayerEntity) target)
                || user.getServer() == null || !user.getServer().isPvpAllowed())) return;
        // Validate the actual struck target: unrelated VFX entities must not swallow the proc or sound.
        Vector3d contact = contactPoint(user, target);
        if (contact != null
                && !target.isInvulnerableTo(ModDamageSource.causeAbilityDamage(user, this).bypassLogia())
                && !AbilityHelper.isDodging(target)) {
            // Click, MMNM and Forge callbacks can all describe the same armed hit.
            suppressedThisTick.add(target.getUUID());
            ImagineBrokenEffect.apply(target, SUPPRESSION_TICKS);
            NagiAwakeningSupport.shatterParticles(user, contact);
            // Every valid melee proc uses the intended recordings, including fruitless targets.
            NagiAwakeningSupport.playNullificationSound(user, contact);
        }
        // Do not deal damage or cancel the underlying ordinary melee attack.
    }

    private static Vector3d contactPoint(PlayerEntity user, LivingEntity target) {
        Vector3d eye = user.getEyePosition(1F);
        AxisAlignedBB bounds = target.getBoundingBox();
        Vector3d contact = new Vector3d(MathHelper.clamp(eye.x, bounds.minX, bounds.maxX),
                MathHelper.clamp(eye.y, bounds.minY, bounds.maxY),
                MathHelper.clamp(eye.z, bounds.minZ, bounds.maxZ));
        if (eye.distanceToSqr(contact) > REACH * REACH) return null;
        RayTraceResult obstruction = user.level.clip(new RayTraceContext(eye, contact,
                RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, user));
        return obstruction.getType() == RayTraceResult.Type.MISS ? contact : null;
    }

    private boolean canTriggerSuppression(PlayerEntity user) {
        // This is a passive contact effect, not an active ability cast. Cart's
        // AbilityUseEvent listener casts to Ability, which PassiveAbility2 does not
        // extend; posting that event here aborts PlayerEntity.attack before damage.
        // Keep the relevant suppression/hand-state checks local, without firing
        // active-use events or changing the underlying melee attack in any way.
        return IllusionShatterHealth.active(user.getHealth(), user.getMaxHealth())
                && canUse(user).isSuccess()
                && !user.hasEffect(KaziEffects.IMAGINE_BROKEN.get())
                && !user.hasEffect(KaziEffects.LOST_WORKS_FRUIT_LOCK.get())
                && !user.hasEffect(ModEffects.NO_HANDS.get())
                && !user.hasEffect(ModEffects.GUARDING.get());
    }

    private static boolean allowsAbilityAt(LivingEntity entity) {
        ProtectedArea area = ProtectedAreasData.get(entity.level).getProtectedArea(
                entity.blockPosition().getX(), entity.blockPosition().getY(), entity.blockPosition().getZ());
        return area == null || area.canUseAbility(INSTANCE);
    }

    /** Clear the former 12-second proc cooldown when an existing save is restored. */
    boolean clearLegacyCooldown(LivingEntity user) {
        if (!cooldownComponent.isOnCooldown()) return false;
        cooldownComponent.stopCooldown(user);
        return true;
    }
}
