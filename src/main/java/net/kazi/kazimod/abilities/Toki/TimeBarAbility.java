package net.kazi.kazimod.abilities.Toki;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.text.DecimalFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityType;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.PassiveAbility2;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.GaugeComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.RendererHelper;
import xyz.pixelatedw.mineminenomi.init.ModResources;
import xyz.pixelatedw.mineminenomi.packets.server.ability.SSyncAbilityPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

public class TimeBarAbility extends PassiveAbility2 {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "time_bar",
            new Pair[]{
                    ImmutablePair.of(
                            "Passively accumulates time points, gaining 1 point every 10 seconds. " +
                                    "Points are capped at 2000.",
                            (Object) null
                    )
            }
    );

    private static final DecimalFormat FORMAT = new DecimalFormat("#0");

    /** Maximum time points that can be stored. */
    public static final float MAX_TIME_POINTS = 2000.0F;

    /** Ticks between each +1 point gain (10 seconds = 200 ticks). */
    private static final int GAIN_INTERVAL = 200;

    public static final AbilityCore<TimeBarAbility> INSTANCE;

    // ── Runtime state ─────────────────────────────────────────────────────────
    private float timePoints     = 0.0F;
    private int   tickAccumulator = 0;

    // ── Constructor ───────────────────────────────────────────────────────────
    public TimeBarAbility(AbilityCore<TimeBarAbility> core) {
        super(core);

        if (super.isClientSide()) {
            GaugeComponent gaugeComponent = new GaugeComponent(this, this::renderGauge);
            super.addComponents(new AbilityComponent[]{ gaugeComponent });
        }
    }

    // ── Passive tick — called every game tick by the ability system ───────────
    @Override
    public void tick(LivingEntity entity) {
        super.tick(entity);
        if (entity.level.isClientSide) return;

        // Stop accumulating ticks once the cap is reached
        if (this.timePoints >= MAX_TIME_POINTS) return;

        this.tickAccumulator++;
        if (this.tickAccumulator >= GAIN_INTERVAL) {
            this.tickAccumulator = 0;
            addTimePoints(entity, 1.0F);
        }
    }

    // ── API ───────────────────────────────────────────────────────────────────
    public void addTimePoints(LivingEntity entity, float amount) {
        this.timePoints = MathHelper.clamp(this.timePoints + amount, 0.0F, MAX_TIME_POINTS);
        sync(entity);
    }

    /**
     * Spend points. Returns true if there were enough points and they were deducted.
     */
    public boolean spendTimePoints(LivingEntity entity, float amount) {
        if (this.timePoints < amount) return false;
        this.timePoints = MathHelper.clamp(this.timePoints - amount, 0.0F, MAX_TIME_POINTS);
        sync(entity);
        return true;
    }

    public float getTimePoints() {
        return this.timePoints;
    }

    private void sync(LivingEntity entity) {
        if (entity instanceof PlayerEntity) {
            WyNetwork.sendTo(new SSyncAbilityPacket(entity.getId(), this), (PlayerEntity) entity);
        }
    }

    // ── NBT persistence ───────────────────────────────────────────────────────
    public CompoundNBT save(CompoundNBT nbt) {
        nbt.putFloat("timePoints", this.timePoints);
        nbt.putInt("tickAccumulator", this.tickAccumulator);
        return nbt;
    }

    public void load(CompoundNBT nbt) {
        this.timePoints     = MathHelper.clamp(nbt.getFloat("timePoints"), 0.0F, MAX_TIME_POINTS);
        this.tickAccumulator = nbt.getInt("tickAccumulator");
    }

    // ── Gauge renderer (client only) ──────────────────────────────────────────
    @OnlyIn(Dist.CLIENT)
    public void renderGauge(PlayerEntity player, MatrixStack matrixStack, int posX, int posY, TimeBarAbility ability) {
        RenderSystem.enableBlend();
        Minecraft mc = Minecraft.getInstance();
        mc.getTextureManager().bind(ModResources.WIDGETS);
        // Draw the ability icon in the gauge slot.
        RendererHelper.drawAbilityIcon(INSTANCE, matrixStack, (float) posX, (float) (posY - 38), 0, 32.0F, 32.0F);
        // Draw the point count centred under the icon.
        String label = FORMAT.format((double) ability.getTimePoints());
        WyHelper.drawStringWithBorder(
                Minecraft.getInstance().font, matrixStack,
                label,
                posX + 16 - mc.font.width(label) / 2,
                posY - 25,
                new Color(180, 230, 255).getRGB() // light-blue matching Toki's colour theme
        );
        RenderSystem.disableBlend();
    }

    // ── Static initialiser ────────────────────────────────────────────────────
    static {
        INSTANCE = (new AbilityCore.Builder<>(
                "Time Bar",
                AbilityCategory.DEVIL_FRUITS,
                AbilityType.PASSIVE,
                TimeBarAbility::new
        ))
                .addDescriptionLine(DESCRIPTION)
                .setHidden()
                .build();
    }
}