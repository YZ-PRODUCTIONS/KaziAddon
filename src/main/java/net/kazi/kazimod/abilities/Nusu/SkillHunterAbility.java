package net.kazi.kazimod.abilities.Nusu;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.items.AkumaNoMiItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class SkillHunterAbility extends PunchAbility2 {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "skill_hunter",
            new Pair[]{
                    ImmutablePair.of(
                            "Requires the Skill Book in hand. Punch a devil-fruit user with less HP than " +
                                    "you. Stay within 35 blocks for 30 seconds, then pick one of their " +
                                    "abilities to steal permanently (max 2 total).",
                            (Object) null)
            });

    public static final float  COOLDOWN      = 200.0F;
    private static final int   OBSERVE_TICKS = 600;  // 30 seconds
    private static final double OBSERVE_RANGE = 35.0D;

    public static final AbilityCore<SkillHunterAbility> INSTANCE;

    // ── Fruits that are entirely off-limits for Nusu stealing ─────────────────
    // Each entry is the display name as registered in the AkumaNoMiItem constructor
    // ("name" field), matched case-insensitively.
    private static final List<String> BLOCKED_FRUIT_NAMES = Arrays.asList(
            // Soru Soru no Mi — Cart addon
            "soru soru no mi",
            // Ope Ope no Mi — mine-mine-no-mi (base mod)
            "ope ope no mi",
            // Kake Kake no Mi — KaziMod
            "kake kake no mi",
            // Tenki Tenki no Mi — KaziMod
            "tenki tenki no mi",
            // Toki Toki no Mi — KaziMod
            "toki toki no mi",
            // Koku Koku no Mi — KaziMod (Gojo fruit, has Domain Expansion: Infinite Void)
            "koku koku no mi",
            // Kama Kama no Mi — KaziMod (Sukuna fruit, has Domain Expansion: Malevolent Shrine)
            "kama kama no mi"
    );

    private LivingEntity observeTarget = null;
    private int          ticksLeft     = 0;

    public SkillHunterAbility(AbilityCore<SkillHunterAbility> core) {
        super(core);
        this.isNew = true;
        this.addTickEvent(this::onTick);
    }

    @Override
    public boolean onHitEffect(LivingEntity user, LivingEntity target, ModDamageSource src) {
        if (!hasSkillBook(user)) {
            sendMsg(user, "\u00a7cYou need the Skill Book in your hand");
            return false;
        }

        if (this.observeTarget != null) {
            sendMsg(user, "\u00a7cAlready tracking a target");
            return false;
        }

        IDevilFruit fruit = DevilFruitCapability.get(target);
        if (fruit == null || !fruit.hasAnyDevilFruit()) return false;

        // ── Block entire protected fruits ──────────────────────────────────────
        if (isFruitBlocked(target)) {
            sendMsg(user, "\u00a7cThat devil fruit cannot be stolen");
            return false;
        }

        if (target.getHealth() >= user.getHealth()) {
            sendMsg(user, "\u00a7cTarget must have less HP than you");
            return false;
        }

        if (NusuStolenData.heldCount(user) >= NusuStolenData.MAX_SLOTS) {
            sendMsg(user, "\u00a7cAll " + NusuStolenData.MAX_SLOTS + " stolen ability slots are full");
            return false;
        }

        this.observeTarget = target;
        this.ticksLeft     = OBSERVE_TICKS;
        sendMsg(user, "\u00a7eTracking started — keep the Skill Book in hand and stay within 35 blocks for 30 seconds");
        return false;
    }

    // ── Returns true if the target's devil fruit is on the blocked list ────────
    private static boolean isFruitBlocked(LivingEntity target) {
        try {
            IDevilFruit devilFruit = DevilFruitCapability.get(target);
            if (devilFruit == null) return false;
            Item item = devilFruit.getDevilFruitItem();
            if (!(item instanceof AkumaNoMiItem)) return false;
            String fruitName = ((AkumaNoMiItem) item).getDevilFruitName();
            if (fruitName == null) return false;
            String lower = fruitName.toLowerCase(java.util.Locale.ROOT);
            for (String blocked : BLOCKED_FRUIT_NAMES) {
                if (lower.equals(blocked)) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void onTick(LivingEntity user, IAbility ability) {
        if (this.observeTarget == null) return;

        if (!hasSkillBook(user)) {
            sendMsg(user, "\u00a7cTracking failed — Skill Book must be in hand");
            resetWindow();
            return;
        }

        if (!this.observeTarget.isAlive()
                || user.distanceTo(this.observeTarget) > OBSERVE_RANGE) {
            sendMsg(user, "\u00a7cTracking failed — target left range");
            resetWindow();
            return;
        }

        this.ticksLeft--;
        if (this.ticksLeft == 300) sendMsg(user, "\u00a7e15 seconds remaining...");
        if (this.ticksLeft == 100) sendMsg(user, "\u00a7e5 seconds remaining...");
        if (this.ticksLeft <= 0) finishTracking(user);
    }

    private void finishTracking(LivingEntity user) {
        super.cooldownComponent.startCooldown(user, COOLDOWN);

        if (!(user instanceof PlayerEntity)) { resetWindow(); return; }
        PlayerEntity player = (PlayerEntity) user;

        // Collect all eligible devil fruit abilities from the target now
        IAbilityData targetData = AbilityDataCapability.get(this.observeTarget);
        if (targetData == null) {
            sendMsg(user, "\u00a7cFailed to read target abilities");
            resetWindow();
            return;
        }

        List<AbilityCore<?>> choices = new ArrayList<>();
        for (IAbility a : targetData.getEquippedAndPassiveAbilities()) {
            // Must be a devil fruit ability
            if (!AbilityCategory.DEVIL_FRUITS.isAbilityPartofCategory().test(a)) continue;
            // Skip morph abilities (transformation points etc.)
            if (a instanceof MorphAbility2) continue;
            AbilityCore<?> core = a.getCore();
            if (NusuStolenData.isHeld(user, core)) continue;
            choices.add(core);
        }

        if (choices.isEmpty()) {
            sendMsg(user, "\u00a7cTarget has no eligible abilities to steal");
            resetWindow();
            return;
        }

        NusuStolenData.setPendingChoices(user, choices);
        NusuStolenData.setPendingStealTarget(user, this.observeTarget);

        player.sendMessage(new StringTextComponent(
                        "\u00a76=== Skill Hunter — Choose ability to steal ==="),
                player.getUUID());

        for (int i = 0; i < choices.size(); i++) {
            String name = choices.get(i).getLocalizedName().getString();
            int num = i + 1;
            StringTextComponent line = new StringTextComponent(
                    "\u00a7e[" + num + "] \u00a7b" + name + " \u00a77(click to steal)");
            line.withStyle(style -> style
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/nusu_steal " + num))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new StringTextComponent("Permanently steal " + name))));
            player.sendMessage(line, player.getUUID());
        }

        player.sendMessage(new StringTextComponent(
                        "\u00a76==============================================="),
                player.getUUID());

        resetWindow();
    }

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
    }

    @Override public Predicate<LivingEntity> canActivate() { return e -> true; }
    @Override public int   getUseLimit()      { return 1; }
    @Override public float getPunchCooldown() { return COOLDOWN; }

    static {
        INSTANCE = new AbilityCore.Builder<SkillHunterAbility>(
                "Skill Hunter", AbilityCategory.DEVIL_FRUITS, SkillHunterAbility::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN)
                })
                .setSourceType(new SourceType[]{ SourceType.FIST })
                .build();
    }
}