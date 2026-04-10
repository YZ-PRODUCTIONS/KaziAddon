package net.kazi.kazimod.abilities.AkumaRework;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.text.DecimalFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.DamageSource;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent.DamageState;
import xyz.pixelatedw.mineminenomi.api.abilities.components.GaugeComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.RendererHelper;
import xyz.pixelatedw.mineminenomi.init.ModResources;
import xyz.pixelatedw.mineminenomi.packets.server.ability.SSyncAbilityPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

public class RevengePassiveAbility extends PassiveAbility2 {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "revenge_passive",
            new Pair[]{ImmutablePair.of(
                    "Passively stores damage taken up to a maximum of 500. Used by Revenge Counter.", null)});

    private static final DecimalFormat FORMAT = new DecimalFormat("#0");

    public static final float MAX_STORED_DAMAGE = 500.0F;

    public static final AbilityCore<RevengePassiveAbility> INSTANCE;

    private float storedDamage = 0.0F;

    private final DamageTakenComponent damageTakenComponent;

    public RevengePassiveAbility(AbilityCore<RevengePassiveAbility> core) {
        super(core);
        this.damageTakenComponent = new DamageTakenComponent(this, this::onDamageTaken, DamageState.ATTACK);

        if (super.isClientSide()) {
            GaugeComponent gaugeComponent = new GaugeComponent(this, this::renderGauge);
            super.addComponents(new AbilityComponent[]{this.damageTakenComponent, gaugeComponent});
        } else {
            super.addComponents(new AbilityComponent[]{this.damageTakenComponent});
        }
    }

    private static final int DECAY_INTERVAL = 40; // 2 seconds
    private static final float DECAY_AMOUNT = 5.0F;
    private int decayTicker = 0;

    @Override
    public void tick(LivingEntity entity) {
        super.tick(entity);
        if (entity.level.isClientSide) return;
        if (this.storedDamage <= 0) {
            this.decayTicker = 0;
            return;
        }
        this.decayTicker++;
        if (this.decayTicker >= DECAY_INTERVAL) {
            this.decayTicker = 0;
            this.storedDamage = Math.max(0.0F, this.storedDamage - DECAY_AMOUNT);
            sync(entity);
        }
    }

    private float onDamageTaken(LivingEntity entity, IAbility ability, DamageSource source, float damage) {
        this.storedDamage = Math.min(this.storedDamage + damage, MAX_STORED_DAMAGE);
        sync(entity);
        return damage;
    }

    public float getStoredDamage() {
        return this.storedDamage;
    }

    public void resetStoredDamage(LivingEntity entity) {
        this.storedDamage = 0.0F;
        sync(entity);
    }

    private void sync(LivingEntity entity) {
        if (entity instanceof PlayerEntity) {
            WyNetwork.sendTo(new SSyncAbilityPacket(entity.getId(), this), (PlayerEntity) entity);
        }
    }

    // ── NBT persistence ───────────────────────────────────────────────────────
    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        nbt.putFloat("storedDamage", this.storedDamage);
        return nbt;
    }

    @Override
    public void load(CompoundNBT nbt) {
        this.storedDamage = MathHelper.clamp(nbt.getFloat("storedDamage"), 0.0F, MAX_STORED_DAMAGE);
    }

    // ── Gauge renderer (client only) ──────────────────────────────────────────
    @OnlyIn(Dist.CLIENT)
    public void renderGauge(PlayerEntity player, MatrixStack matrixStack, int posX, int posY, RevengePassiveAbility ability) {
        RenderSystem.enableBlend();
        Minecraft mc = Minecraft.getInstance();
        mc.getTextureManager().bind(ModResources.WIDGETS);
        RendererHelper.drawAbilityIcon(INSTANCE, matrixStack, (float) posX, (float) (posY - 38), 0, 32.0F, 32.0F);
        String label = FORMAT.format((double) ability.getStoredDamage());
        // Color shifts from gray to red as damage fills up
        float ratio = ability.getStoredDamage() / MAX_STORED_DAMAGE;
        int r = (int) (100 + 155 * ratio);
        int g = (int) (100 * (1 - ratio));
        int b = (int) (100 * (1 - ratio));
        WyHelper.drawStringWithBorder(
                mc.font, matrixStack,
                label,
                posX + 16 - mc.font.width(label) / 2,
                posY - 25,
                new Color(r, g, b).getRGB()
        );
        RenderSystem.disableBlend();
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>(
                "Revenge Passive",
                AbilityCategory.DEVIL_FRUITS,
                AbilityType.PASSIVE,
                RevengePassiveAbility::new
        ))
                .addDescriptionLine(DESCRIPTION)
                .setHidden()
                .build();
    }
}
