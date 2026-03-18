package net.kazi.kazimod.abilities.Nusu;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Nusu Nusu no Mi - Skill Hunter
 *
 * Punch a devil-fruit user with less HP than you to begin a 30-second
 * observation window. Stay within 20 blocks of them. During this time,
 * any devil-fruit ability they actively use (continuous or charging) is
 * logged. Morphs (MorphAbility2) are excluded.
 *
 * After 30 seconds, a clickable chat menu lists all observed abilities.
 * Click one to steal it permanently (max 4 total stolen).
 */
public class SkillHunterAbility extends PunchAbility2 {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "skill_hunter",
            new Pair[]{
                    ImmutablePair.of(
                            "Punch a devil-fruit user with less HP than you. Stay within 20 blocks for 30 " +
                                    "seconds while observing their abilities, then pick one to steal permanently.",
                            (Object) null)
            });

    public static final float   COOLDOWN      = 200.0F;
    private static final int    OBSERVE_TICKS = 600;    // 30 seconds
    private static final double OBSERVE_RANGE = 20.0D;

    public static final AbilityCore<SkillHunterAbility> INSTANCE;

    private LivingEntity              observeTarget = null;
    private int                       ticksLeft     = 0;
    private final Set<AbilityCore<?>> observed      = new LinkedHashSet<>();

    public SkillHunterAbility(AbilityCore<SkillHunterAbility> core) {
        super(core);
        this.isNew = true;
        this.addTickEvent(this::onTick);
    }

    // ── Hit handler: start observation ───────────────────────────────────────

    @Override
    public boolean onHitEffect(LivingEntity user, LivingEntity target, ModDamageSource src) {
        if (this.observeTarget != null) {
            sendMsg(user, "\u00a7cAlready observing a target");
            return false;
        }


        IDevilFruit fruit = DevilFruitCapability.get(target);
        if (fruit == null || !fruit.hasAnyDevilFruit()) return false;

        if (target.getHealth() >= user.getHealth()) {
            sendMsg(user, "\u00a7cTarget must have less HP than you");
            return false;
        }

        if (NusuStolenData.heldCount(user) >= NusuStolenData.MAX_SLOTS) {
            sendMsg(user, "\u00a7cAll 4 stolen ability slots are full");
            return false;
        }

        this.observeTarget = target;
        this.ticksLeft     = OBSERVE_TICKS;
        this.observed.clear();
        sendMsg(user, "\u00a7eObservation started — stay within 20 blocks for 30 seconds");
        return false;
    }

    // ── Tick: watch, log, countdown ───────────────────────────────────────────

    private void onTick(LivingEntity user, IAbility ability) {
        if (this.observeTarget == null) return;

        // Fail if target dies or leaves range
        if (!this.observeTarget.isAlive()
                || user.distanceTo(this.observeTarget) > OBSERVE_RANGE) {
            sendMsg(user, "\u00a7cObservation failed — target left range");
            resetWindow();
            return;
        }

        // Log any active devil-fruit ability the target is using this tick
        IAbilityData targetData = AbilityDataCapability.get(this.observeTarget);
        if (targetData != null) {
            for (IAbility a : targetData.getEquippedAndPassiveAbilities()) {
                if (!AbilityCategory.DEVIL_FRUITS.isAbilityPartofCategory().test(a)) continue;
                if (a instanceof MorphAbility2) continue;
                if (isActive(a) && this.observed.add(a.getCore())) {
                    sendMsg(user, "\u00a7bObserved: " + a.getCore().getLocalizedName().getString());
                }
            }
        }

        this.ticksLeft--;

        if (this.ticksLeft == 300) sendMsg(user, "\u00a7e15 seconds remaining...");
        if (this.ticksLeft == 100) sendMsg(user, "\u00a7e5 seconds remaining...");

        if (this.ticksLeft <= 0) {
            finishObservation(user);
        }
    }

    // ── Finish: open pick UI ──────────────────────────────────────────────────

    private void finishObservation(LivingEntity user) {
        super.cooldownComponent.startCooldown(user, COOLDOWN);

        if (!(user instanceof PlayerEntity)) { resetWindow(); return; }
        PlayerEntity player = (PlayerEntity) user;

        if (this.observed.isEmpty()) {
            sendMsg(user, "\u00a7cObservation complete — target used no abilities");
            resetWindow();
            return;
        }

        // Filter out abilities already stolen
        List<AbilityCore<?>> choices = new ArrayList<>();
        for (AbilityCore<?> core : this.observed) {
            if (!NusuStolenData.isHeld(user, core)) choices.add(core);
        }

        if (choices.isEmpty()) {
            sendMsg(user, "\u00a7cAll observed abilities are already stolen");
            resetWindow();
            return;
        }

        // Save choices and target so /nusu_steal can act on them
        NusuStolenData.setPendingChoices(user, choices);
        NusuStolenData.setPendingStealTarget(user, this.observeTarget);

        // Show the pick menu
        player.sendMessage(new StringTextComponent(
                        "\u00a76=== Skill Hunter — Choose ability to steal ==="),
                player.getUUID());

        for (int i = 0; i < choices.size(); i++) {
            String name = choices.get(i).getLocalizedName().getString();
            int num = i + 1;
            StringTextComponent line = new StringTextComponent(
                    "\u00a7e[" + num + "] \u00a7b" + name + " \u00a77(click to steal)");
            line.withStyle(style -> style
                    .withClickEvent(new ClickEvent(
                            ClickEvent.Action.RUN_COMMAND, "/nusu_steal " + num))
                    .withHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_TEXT,
                            new StringTextComponent("Permanently steal " + name))));
            player.sendMessage(line, player.getUUID());
        }

        player.sendMessage(new StringTextComponent(
                        "\u00a76==============================================="),
                player.getUUID());

        resetWindow();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static boolean isActive(IAbility ability) {
        boolean cont = ability.getComponent(ModAbilityKeys.CONTINUOUS)
                .map(c -> ((ContinuousComponent) c).isContinuous()).orElse(false);
        boolean chg  = ability.getComponent(ModAbilityKeys.CHARGE)
                .map(c -> ((ChargeComponent) c).isCharging()).orElse(false);
        return cont || chg;
    }

    /** Returns true if the user has the Skill Book in their main hand or offhand. */
    public static boolean hasSkillBook(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) return false;
        PlayerEntity player = (PlayerEntity) entity;
        ItemStack main    = player.getMainHandItem();
        ItemStack offhand = player.getOffhandItem();
        return main.getItem() instanceof net.kazi.kazimod.items.SkillBookItem
                || offhand.getItem() instanceof net.kazi.kazimod.items.SkillBookItem;
    }

    static void sendMsg(LivingEntity entity, String text) {
        if (entity instanceof PlayerEntity) {
            ((PlayerEntity) entity).sendMessage(
                    new StringTextComponent(text), entity.getUUID());
        }
    }

    private void resetWindow() {
        this.observeTarget = null;
        this.ticksLeft     = 0;
        this.observed.clear();
    }

    @Override public Predicate<LivingEntity> canActivate() { return e -> true; }
    @Override public int   getUseLimit()      { return 1; }
    @Override public float getPunchCooldown() { return COOLDOWN; }

    static {
        INSTANCE = (new AbilityCore.Builder<SkillHunterAbility>(
                "Skill Hunter", AbilityCategory.DEVIL_FRUITS, SkillHunterAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN)
                })
                .setSourceType(new SourceType[]{ SourceType.FIST })
                .build();
    }
}