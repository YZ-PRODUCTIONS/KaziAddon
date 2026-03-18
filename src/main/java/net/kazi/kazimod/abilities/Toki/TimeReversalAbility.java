package net.kazi.kazimod.abilities.Toki;

import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.GrabEntityComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import net.kazi.kazimod.abilities.Toki.TimeBarAbility;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public class TimeReversalAbility extends Ability {

    // ── Description ──────────────────────────────────────────────────────────
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "time_reversal",
            new Pair[]{
                    ImmutablePair.of(
                            "Passively records your position and HP every tick. " +
                                    "On activation, reverts your position and HP back to " +
                                    "what they were 2 seconds ago.",
                            (Object) null
                    )
            }
    );

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final float COOLDOWN      = 1200.0F; // 10 s × 20 ticks/s
    private static final int   HISTORY_TICKS = 40;    // 5 s × 20 ticks/s
    private static final float TIME_COST     = 200.0F;

    // ── Static instance ───────────────────────────────────────────────────────
    public static final AbilityCore<TimeReversalAbility> INSTANCE;

    // ── Runtime state ─────────────────────────────────────────────────────────
    /**
     * Rolling snapshot buffer. Each entry holds [x, y, z, hp].
     * Oldest snapshot sits at the head; newest at the tail.
     */
    private final Deque<double[]> history = new ArrayDeque<>();
    private int recordTick = 0;

    // ── Constructor ───────────────────────────────────────────────────────────
    public TimeReversalAbility(AbilityCore<TimeReversalAbility> core) {
        super(core);
        this.isNew = true;
        // No extra components needed — we hook directly into the ability's
        // built-in tick via addTickEvent, which fires every game tick
        // regardless of cooldown or activation state.
        this.addComponents(new AbilityComponent[]{});
        this.addTickEvent(this::onPassiveTick);
        this.addUseEvent(this::onUseEvent);
    }

    // ── Passive tick — record snapshot every tick ─────────────────────────────
    private void onPassiveTick(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) {
            return;
        }

        history.addLast(new double[]{
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                entity.getHealth()
        });

        // Keep only the most recent HISTORY_TICKS snapshots.
        while (history.size() > HISTORY_TICKS) {
            history.pollFirst();
        }
    }

    // ── Activation — revert to oldest snapshot ────────────────────────────────
    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) {
            return;
        }

        if (history.isEmpty()) {
            return;
        }

        TimeBarAbility bar = getTimeBar(entity);
        if (bar == null || !bar.spendTimePoints(entity, 50F)) {
            if (entity instanceof PlayerEntity) {
                ((PlayerEntity) entity).displayClientMessage(
                        new net.minecraft.util.text.TranslationTextComponent("Not Enough Time Points"),
                        true
                );
            }
            return;
        }

        // Oldest snapshot = up to 5 seconds ago.
        double[] snapshot = history.peekFirst();

        double oldX  = snapshot[0];
        double oldY  = snapshot[1];
        double oldZ  = snapshot[2];
        float  oldHp = (float) Math.min(snapshot[3], entity.getMaxHealth());

        // ── Escape any active grab before reverting ───────────────────────────
        // Scan all nearby entities for one that currently has us grabbed,
        // then force-release us via their GrabEntityComponent.
        breakGrabIfHeld(entity);

        // Teleport back to the recorded position.
        entity.teleportToWithTicket(oldX, oldY, oldZ);

        // Restore recorded HP.
        entity.setHealth(oldHp);

        entity.level.playSound(
                (PlayerEntity) null,
                entity.blockPosition(),
                (SoundEvent) ModSounds.ROOM_EXPAND_SFX.get(),
                SoundCategory.PLAYERS,
                3.0F, 1.5F
        );

        // Clear history so the next cycle starts fresh from this point.
        history.clear();

        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }


    // ── Time Bar helper ───────────────────────────────────────────────────────
    private static TimeBarAbility getTimeBar(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) return null;
        xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData data =
                xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability
                        .getLazy(entity).orElse(null);
        if (data == null) return null;
        for (IAbility abl : data.getEquippedAndPassiveAbilities()) {
            if (abl instanceof TimeBarAbility) return (TimeBarAbility) abl;
        }
        return null;
    }

    /**
     * Searches every nearby living entity for a GrabEntityComponent that is
     * currently holding {@code victim}, and forces a release.
     * This mirrors the pattern used in GrabEntityComponent.hasGrabbed() to
     * iterate ability data, but we additionally call release() when found.
     */
    private static void breakGrabIfHeld(LivingEntity victim) {
        // Check entities within a generous radius — grab range is at most ~5.5 blocks.
        for (LivingEntity nearby : victim.level.getEntitiesOfClass(
                LivingEntity.class,
                victim.getBoundingBox().inflate(8.0))) {

            if (nearby == victim) {
                continue;
            }

            IAbilityData data = AbilityDataCapability.getLazy(nearby).orElse(null);
            if (data == null) {
                continue;
            }

            for (IAbility abl : data.getEquippedAndPassiveAbilities()) {
                if (!abl.hasComponent(ModAbilityKeys.GRAB)) {
                    continue;
                }

                abl.getComponent(ModAbilityKeys.GRAB).ifPresent(comp -> {
                    GrabEntityComponent grabComp = (GrabEntityComponent) comp;
                    // Only release if this grabber is actually holding our victim.
                    if (grabComp.hasGrabbedEntity() && grabComp.getGrabbedEntity() == victim) {
                        grabComp.release(nearby);
                    }
                });
            }
        }
    }

    // ── Static initialiser ────────────────────────────────────────────────────
    static {
        INSTANCE = (new AbilityCore.Builder<>(
                "Time Reversal",
                AbilityCategory.DEVIL_FRUITS,
                TimeReversalAbility::new
        ))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN)
                })
                .setSourceHakiNature(SourceHakiNature.SPECIAL)
                .setSourceType(new SourceType[]{ SourceType.FIST })
                .build();
    }
}