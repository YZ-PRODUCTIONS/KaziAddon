package net.kazi.kazimod.abilities.KachiRework;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.kazi.kazimod.init.KaziAttributes;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityAttributeModifier;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityType;
import xyz.pixelatedw.mineminenomi.api.abilities.PassiveAbility2;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent.DamageState;
import xyz.pixelatedw.mineminenomi.api.abilities.components.GaugeComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.RendererHelper;
import xyz.pixelatedw.mineminenomi.init.ModResources;
import xyz.pixelatedw.mineminenomi.packets.server.ability.SSyncAbilityPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

import java.awt.Color;
import java.text.DecimalFormat;
import java.util.UUID;

public class SunshineAbility extends PassiveAbility2 {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "sunshine",
            new Pair[]{ImmutablePair.of(
                    "The power of Sunshine. Grows stronger as the sun rises (6:00-19:00). " +
                            "Each point reduces damage taken and increases size by a percentage.",
                    (Object) null)}
    );

    private static final DecimalFormat FORMAT = new DecimalFormat("#0");

    // ── Time of day constants ─────────────────────────────────────────────────
    // Minecraft day ticks: 0 = 6:00, 6000 = 12:00 (noon), 13000 = 19:00, 18000 = 0:00
    // Each in-game hour = 1000 ticks
    // 6:00  = tick 0     (start of day)
    // 12:00 = tick 6000  (noon / peak)
    // 19:00 = tick 13000 (end of day window)
    private static final long DAY_START_TICK  = 0L;     // 6:00
    private static final long NOON_TICK       = 6000L;  // 12:00
    private static final long DAY_END_TICK    = 13000L; // 19:00

    // ── Point constants ───────────────────────────────────────────────────────
    // 1 point per 2 in-game hours = per 2000 ticks, max 7 points
    public static final int MAX_POINTS = 7;

    // ── Modifier UUIDs ────────────────────────────────────────────────────────
    private static final UUID ARMOR_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567801");
    private static final UUID SIZE_UUID  = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567802");

    // ── Per-point scaling ─────────────────────────────────────────────────────
    // Damage reduction: 5% per point (via armor equivalent)
    // Size: 10% per point (multiplicative)
    private static final double ARMOR_PER_POINT = 4.0; // ~5% DR per point in vanilla armor formula
    private static final double SIZE_PER_POINT  = 0.10; // 10% size increase per point

    public static final AbilityCore<SunshineAbility> INSTANCE;

    // ── Runtime state ─────────────────────────────────────────────────────────
    private int   currentPoints   = 0;
    private int   lastPoints      = -1; // tracks when modifiers need updating
    private final DamageTakenComponent damageTakenComponent;

    // ── Constructor ───────────────────────────────────────────────────────────
    public SunshineAbility(AbilityCore<SunshineAbility> core) {
        super(core);
        this.damageTakenComponent = new DamageTakenComponent(this, this::onDamageTaken, DamageState.ATTACK);
        super.addComponents(new AbilityComponent[]{ this.damageTakenComponent });

        if (super.isClientSide()) {
            GaugeComponent gaugeComponent = new GaugeComponent(this, this::renderGauge);
            super.addComponents(new AbilityComponent[]{ gaugeComponent });
        }
    }

    // ── Passive tick ──────────────────────────────────────────────────────────
    @Override
    public void tick(LivingEntity entity) {
        super.tick(entity);
        if (entity.level.isClientSide) return;

        int newPoints = calculatePoints(entity);

        if (newPoints != currentPoints || newPoints != lastPoints) {
            currentPoints = newPoints;
            updateModifiers(entity);
            lastPoints = newPoints;
            sync(entity);
        }
    }

    // ── Time of day → points ──────────────────────────────────────────────────
    private int calculatePoints(LivingEntity entity) {
        long dayTime = entity.level.getDayTime() % 24000L;

        // Outside 6:00–19:00 window → 0
        if (dayTime < DAY_START_TICK || dayTime >= DAY_END_TICK) return 0;

        long timeInWindow = dayTime - DAY_START_TICK; // 0–13000
        long windowLength = DAY_END_TICK - DAY_START_TICK; // 13000

        // Map position in window to points 1–7
        // Each point covers 2 in-game hours = 2000 ticks
        // Window: 6:00(0)→8:00(1)→10:00(2)→12:00(3)→... peaks at noon then descends
        // Symmetrical bell: rises 6:00–12:00, falls 12:00–19:00
        // Distance from noon (tick 6000 from window start)
        long noonInWindow = NOON_TICK - DAY_START_TICK; // 6000
        long distFromNoon = Math.abs(timeInWindow - noonInWindow);

        // Max distance is 6000 (at 6:00 or 19:00 edge)
        // Invert so noon = highest
        // Scale to 1–7: at noon dist=0→7, at edge dist=6000→1
        // Each 2000 tick step = 1 point drop from noon
        int dropFromMax = (int) (distFromNoon / 2000L);
        int points = MAX_POINTS - dropFromMax;

        return MathHelper.clamp(points, 1, MAX_POINTS);
    }

    // ── Apply/remove attribute modifiers based on current points ──────────────
    private void updateModifiers(LivingEntity entity) {
        // Remove old modifiers
        entity.getAttribute(Attributes.ARMOR)
                .removeModifier(ARMOR_UUID);
        if (entity.getAttribute(KaziAttributes.SIZE.get()) != null) {
            entity.getAttribute(KaziAttributes.SIZE.get())
                    .removeModifier(SIZE_UUID);
        }

        if (currentPoints <= 0) return;

        // Add new modifiers scaled to current points
        double armorBonus = ARMOR_PER_POINT * currentPoints;
        double sizeBonus  = SIZE_PER_POINT  * currentPoints; // e.g. 7 points = 70% bigger

        entity.getAttribute(Attributes.ARMOR).addPermanentModifier(
                new AbilityAttributeModifier(ARMOR_UUID, INSTANCE,
                        "Sunshine Armor", armorBonus, Operation.ADDITION));

        if (entity.getAttribute(KaziAttributes.SIZE.get()) != null) {
            entity.getAttribute(KaziAttributes.SIZE.get()).addPermanentModifier(
                    new AbilityAttributeModifier(SIZE_UUID, INSTANCE,
                            "Sunshine Size", sizeBonus, Operation.MULTIPLY_BASE));
        }

        // Refresh entity size
        entity.refreshDimensions();
    }

    // ── Damage reduction via DamageTakenComponent ─────────────────────────────
    private float onDamageTaken(LivingEntity entity, xyz.pixelatedw.mineminenomi.api.abilities.IAbility ability,
                                DamageSource source, float damage) {
        if (currentPoints <= 0) return damage;
        // 5% reduction per point, max 35% at 7 points
        float reduction = currentPoints * 0.05F;
        return damage * (1.0F - reduction);
    }

    // ── API ───────────────────────────────────────────────────────────────────
    public int getCurrentPoints() {
        return this.currentPoints;
    }

    private void sync(LivingEntity entity) {
        if (entity instanceof PlayerEntity) {
            WyNetwork.sendTo(new SSyncAbilityPacket(entity.getId(), this), (PlayerEntity) entity);
        }
    }

    // ── NBT persistence ───────────────────────────────────────────────────────
    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        nbt.putInt("sunshinePoints", this.currentPoints);
        return nbt;
    }

    @Override
    public void load(CompoundNBT nbt) {
        this.currentPoints = MathHelper.clamp(nbt.getInt("sunshinePoints"), 0, MAX_POINTS);
    }

    // ── Gauge renderer ────────────────────────────────────────────────────────
    @OnlyIn(Dist.CLIENT)
    public void renderGauge(PlayerEntity player, MatrixStack matrixStack, int posX, int posY, SunshineAbility ability) {
        RenderSystem.enableBlend();
        Minecraft mc = Minecraft.getInstance();
        mc.getTextureManager().bind(ModResources.WIDGETS);
        RendererHelper.drawAbilityIcon(INSTANCE, matrixStack, (float) posX, (float) (posY - 38), 0, 32.0F, 32.0F);

        int pts = ability.getCurrentPoints();
        // Label: e.g. "7 ☀" or "0" at night
        String label = pts > 0 ? FORMAT.format(pts) : "0";
        // Golden/orange colour when active, grey at night
        int colour = pts > 0
                ? new Color(255, 200, 50).getRGB()   // warm gold
                : new Color(150, 150, 150).getRGB();  // grey at night

        WyHelper.drawStringWithBorder(
                mc.font, matrixStack,
                label,
                posX + 16 - mc.font.width(label) / 2,
                posY - 25,
                colour
        );
        RenderSystem.disableBlend();
    }

    // ── Static initialiser ────────────────────────────────────────────────────
    static {
        INSTANCE = (new AbilityCore.Builder<>(
                "Sunshine",
                AbilityCategory.DEVIL_FRUITS,
                AbilityType.PASSIVE,
                SunshineAbility::new
        ))
                .addDescriptionLine(DESCRIPTION)
                .setHidden()
                .build();
    }
}