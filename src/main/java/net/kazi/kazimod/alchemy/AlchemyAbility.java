package net.kazi.kazimod.alchemy;

import java.util.*;
import net.kazi.kazimod.petal.PetalFeatures;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.ForgeMod;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModAttributes;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

public final class AlchemyAbility extends Ability {
    public static final String[] IDS = {"automail_blade", "earth_fang", "alchemists_bulwark", "stone_prison", "fullmetal_combination", "grand_transmutation"};
    public static final String[] NAMES = {"Automail Blade", "Earth Fang", "Alchemist's Bulwark", "Stone Prison", "Fullmetal Combination", "Grand Transmutation"};
    public static final int[] COOLDOWN = {40, 180, 320, 400, 280, 600};
    public static final int GRAND_WINDUP = 10, PRISON_HOLD = 46;
    public static final int[] LIFE = {Integer.MAX_VALUE, 32, 160, 8, 72, GRAND_WINDUP + 4};
    public static final AbilityCore<AlchemyAbility>[] CORES = cores();
    public static final UUID BLADE_DAMAGE = UUID.fromString("22c9ddcb-19af-4589-94d7-cd7a48a7aad1");
    public static final UUID BLADE_REACH = UUID.fromString("22c9ddcb-19af-4589-94d7-cd7a48a7aad2");

    @SuppressWarnings("unchecked")
    private static AbilityCore<AlchemyAbility>[] cores() {
        AbilityCore<AlchemyAbility>[] result = new AbilityCore[6];
        for (int i = 0; i < result.length; i++) {
            final int move = i;
            result[i] = new AbilityCore.Builder<AlchemyAbility>(NAMES[i], "alchemy_" + IDS[i], AbilityCategory.DEVIL_FRUITS,
                    AbilityType.ACTION, core -> new AlchemyAbility(core, move))
                    .setUnlockCheck(AlchemyFeatures::awakened)
                    .setSourceType(i == 0 ? SourceType.SLASH : i == 4 ? SourceType.FIST : SourceType.INDIRECT)
                    .setSourceElement(SourceElement.NONE).setSourceHakiNature(SourceHakiNature.IMBUING)
                    .setIcon(new ResourceLocation("kazimod", "textures/abilities/alchemy/" + i + ".png"))
                    .addDescriptionLine(new TranslationTextComponent("ability.kazimod.alchemy_" + IDS[i] + ".description"))
                    .addAdvancedDescriptionLine(CooldownComponent.getTooltip(COOLDOWN[i])).build();
        }
        return result;
    }

    public final int move;
    private final ContinuousComponent continuous = new ContinuousComponent(this, true);
    private final DealDamageComponent damage = new DealDamageComponent(this);
    private final ChangeStatsComponent stats = new ChangeStatsComponent(this);
    private AlchemyField field;
    private LivingEntity victim;
    private int ticks, contact = -1;
    private Vector3d dash = Vector3d.ZERO;
    private boolean completed;

    public AlchemyAbility(AbilityCore<AlchemyAbility> core, int move) {
        super(core);
        this.move = move;
        isNew = true;
        addComponents(continuous, damage, stats);
        if (move == 0) {
            stats.addAttributeModifier(Attributes.ATTACK_DAMAGE, new AbilityAttributeModifier(BLADE_DAMAGE, this,
                    "Automail blade damage", 6, AttributeModifier.Operation.ADDITION), this::bladeActive);
            stats.addAttributeModifier(ForgeMod.REACH_DISTANCE, new AbilityAttributeModifier(BLADE_REACH, this,
                    "Automail blade reach", 1.25, AttributeModifier.Operation.ADDITION), this::bladeActive);
            stats.addAttributeModifier(ModAttributes.ATTACK_RANGE, new AbilityAttributeModifier(BLADE_REACH, this,
                    "Automail blade attack range", 1.25, AttributeModifier.Operation.ADDITION), this::bladeActive);
        }
        addCanUseCheck((p, ability) -> {
            if (continuous.isContinuous()) return AbilityUseResult.success();
            if (!AlchemyFeatures.usable(p)) return fail("locked");
            if (AlchemyFeatures.action(p) >= 0) return fail("busy");
            if ((move == 1 || move == 2 || move == 3 || move == 5) && !grounded(p)) return fail("ground");
            if (move == 3) {
                LivingEntity t = PetalFeatures.target(p, 18);
                if (t != null && PetalFeatures.bodyOwner(t) != t) return fail("target");
                if (t == null || AlchemyField.surface(p.level, t.blockPosition().below()) == null || t.getBbWidth() > 2.6F)
                    return fail("target");
            }
            if (move == 5 && aimGround(p) == null) return fail("aim_ground");
            return AbilityUseResult.success();
        });
        addUseEvent((p, ability) -> {
            if (p.level.isClientSide) return;
            if (continuous.isContinuous()) {
                boolean launched = move == 2 && field != null && field.isAlive() && ticks >= 12;
                if (launched) {
                    field.launch();
                    completed = true;
                }
                continuous.stopContinuity(p);
                if (launched) {
                    AlchemyFeatures.state(p).putLong("WallLaunch", p.level.getGameTime());
                    AlchemyFeatures.pose(p, 2, 12);
                }
                return;
            }
            ticks = 0;
            contact = -1;
            completed = false;
            victim = null;
            field = null;
            AlchemyFeatures.state(p).remove("ComboContact");
            AlchemyFeatures.state(p).remove("WallLaunch");
            AlchemyFeatures.pose(p, move, move == 0 ? 16 : move == 2 ? 20 : LIFE[move]);
            continuous.triggerContinuity(p, LIFE[move]);
            if (move == 0) {
                AlchemyFeatures.state(p).putBoolean("Blade", true);
                stats.applyModifiers(p);
                AlchemyFeatures.sync(p);
            } else if (move == 1) {
                field = AlchemyField.fangs(p);
            } else if (move == 2) {
                field = AlchemyField.wall(p);
            } else if (move == 3) {
                victim = PetalFeatures.target(p, 18);
                if (victim != null && PetalFeatures.bodyOwner(victim) == victim) field = AlchemyField.prison(p, victim);
                completed = field != null && field.isAlive();
            } else if (move == 4) {
                dash = p.getLookAngle().normalize();
                field = AlchemyField.spawn(p, 4, p.position(), 72, 1);
            } else {
                Vector3d at = aimGround(p);
                if (at != null) field = AlchemyField.grand(p, at);
            }
            if (move != 0 && field == null) {
                p.sendMessage(new TranslationTextComponent("message.kazimod.alchemy.space"), p.getUUID());
                continuous.stopContinuity(p);
            }
        });
        continuous.addTickEvent(this::during);
        continuous.addEndEvent(this::end);
        addRemoveEvent((p, ability) -> {
            if (continuous.isContinuous()) continuous.stopContinuity(p);
            if (move == 0) {
                stats.removeModifiers(p);
                if (!p.level.isClientSide) {
                    AlchemyFeatures.state(p).putBoolean("Blade", false);
                    AlchemyFeatures.sync(p);
                }
            }
        });
    }

    private boolean bladeActive(LivingEntity p) { return continuous.isContinuous() && AlchemyFeatures.usable(p); }
    public boolean active() { return continuous.isContinuous(); }
    public static boolean grounded(LivingEntity p) {
        // Movement locks can clear onGround even while the feet remain supported.
        return AlchemyField.surface(p.level, p.blockPosition().below()) != null
                && (p.isOnGround() || p.level.getBlockCollisions(p, p.getBoundingBox().move(0, -.08, 0))
                        .anyMatch(shape -> !shape.isEmpty()));
    }
    private static Vector3d aimGround(LivingEntity p) {
        return p.getLookAngle().y > .15 ? null : PetalFeatures.ground(p, 18);
    }
    private static AbilityUseResult fail(String key) { return AbilityUseResult.fail(new TranslationTextComponent("message.kazimod.alchemy." + key)); }

    private void during(LivingEntity p, IAbility ability) {
        if (p.level.isClientSide) return;
        ticks++;
        if (!AlchemyFeatures.usable(p)) { continuous.stopContinuity(p); return; }
        if (ticks == 6) sound(p, SoundEvents.PLAYER_ATTACK_STRONG, .75F, 1.6F);
        if (ticks == 8) sound(p, SoundEvents.BEACON_ACTIVATE, .35F, 1.8F);
        if (move == 0) return;
        if (move == 1 && ticks == 14) completed = true;
        if (move == 2) {
            if (field == null || !field.isAlive()) continuous.stopContinuity(p);
            return;
        }
        if ((move == 1 && ticks < 14) || (move == 5 && ticks < GRAND_WINDUP)) {
            if (!grounded(p)) { continuous.stopContinuity(p); return; }
            p.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(), 2, 0, false, false));
            AbilityHelper.setDeltaMovement(p, 0, Math.min(0, p.getDeltaMovement().y), 0);
        }
        if (move == 5 && ticks == GRAND_WINDUP) {
            completed = true;
            sound(p, SoundEvents.GENERIC_EXPLODE, 1.3F, .65F);
        }
        if (move == 4) combo(p);
    }

    private void combo(LivingEntity p) {
        if (contact < 0) {
            if (ticks < 7) return;
            if (ticks > 21 || p.horizontalCollision) { continuous.stopContinuity(p); return; }
            Vector3d from = p.getEyePosition(1), end = from.add(dash.scale(2.5));
            end = p.level.clip(new RayTraceContext(from, end, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, p)).getLocation();
            double nearest = Double.MAX_VALUE;
            for (LivingEntity candidate : p.level.getEntitiesOfClass(LivingEntity.class, new AxisAlignedBB(from, end).inflate(.8), t -> PetalFeatures.canHit(p, t))) {
                Optional<Vector3d> intercept = candidate.getBoundingBox().inflate(.45).clip(from, end);
                if ((intercept.isPresent() || candidate.getBoundingBox().inflate(.45).contains(from)) && p.canSee(candidate)) {
                    double d = intercept.isPresent() ? intercept.get().distanceToSqr(from) : 0;
                    if (d < nearest) { nearest = d; victim = PetalFeatures.bodyOwner(candidate); }
                }
            }
            if (victim != null) {
                if (!strike(p, victim, 6)) { continuous.stopContinuity(p); return; }
                contact = ticks;
                field.target(victim);
                AlchemyFeatures.state(p).putLong("ComboContact", p.level.getGameTime());
                AlchemyFeatures.sync(p);
                AbilityHelper.setDeltaMovement(p, Vector3d.ZERO);
            } else {
                Vector3d velocity = dash.scale(.95);
                if (!p.level.noCollision(p.getBoundingBox().move(velocity))) { continuous.stopContinuity(p); return; }
                AbilityHelper.setDeltaMovement(p, velocity);
                p.fallDistance = 0;
            }
            return;
        }
        if (victim == null || !victim.isAlive() || !PetalFeatures.canHit(p, victim) || !p.canSee(victim) || p.distanceToSqr(victim) > 36) {
            continuous.stopContinuity(p); return;
        }
        int beat = ticks - contact;
        if (beat < 29) {
            victim.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(), 2, 0, false, false));
            p.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(), 2, 0, false, false));
        }
        if (beat == 10 || beat == 20 || beat == 30) {
            if (strike(p, victim, beat == 30 ? 14 : 8)) {
                sound(p, beat == 10 ? SoundEvents.PLAYER_ATTACK_SWEEP : SoundEvents.STONE_BREAK, .8F, beat == 30 ? .7F : 1.1F);
                if (beat == 20) AbilityHelper.setDeltaMovement(victim, 0, .55, 0);
                if (beat == 30) {
                    AbilityHelper.setDeltaMovement(victim, dash.x * 1.1, -.65, dash.z * 1.1);
                    AlchemyField.spawn(p, 4, victim.position(), 16, 2);
                }
            }
        }
        if (beat >= 36) { completed = true; continuous.stopContinuity(p); }
    }

    private void end(LivingEntity p, IAbility ability) {
        if (p.level.isClientSide) return;
        if (move == 0) {
            stats.removeModifiers(p);
            AlchemyFeatures.state(p).putBoolean("Blade", false);
            AlchemyFeatures.sync(p);
        }
        if (field != null && (move == 4 || !completed)) field.remove();
        if (move == 4) AbilityHelper.setDeltaMovement(p, p.getDeltaMovement().scale(.2));
        if (AlchemyFeatures.action(p) == move) AlchemyFeatures.pose(p, -1, 0);
        cooldownComponent.startCooldown(p, COOLDOWN[move]);
        field = null;
        victim = null;
    }

    public boolean strike(LivingEntity p, LivingEntity part, float amount) {
        LivingEntity target = PetalFeatures.bodyOwner(part);
        return target != null && PetalFeatures.canHit(p, target) && damage.hurtTarget(p, target, amount);
    }
    static void sound(LivingEntity p, SoundEvent sound, float volume, float pitch) {
        p.level.playSound(null, p.blockPosition(), sound, SoundCategory.PLAYERS, volume, pitch);
    }
}
