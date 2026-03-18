package net.kazi.kazimod.abilities.Kake;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityType;
import xyz.pixelatedw.mineminenomi.api.abilities.PassiveAbility2;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.GaugeComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.RendererHelper;
import xyz.pixelatedw.mineminenomi.init.ModResources;
import xyz.pixelatedw.mineminenomi.packets.server.ability.SSyncAbilityPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

import java.awt.Color;

public class LuckySlotAbility extends PassiveAbility2 {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "lucky_slot",
            new Pair[]{
                    ImmutablePair.of(
                            "Displays the current slot number. Use Slot Spin to roll a new number from 0-9.",
                            (Object) null
                    )
            }
    );

    public static final AbilityCore<LuckySlotAbility> INSTANCE;

    // The current rolled number, -1 means not yet rolled
    private int slotNumber = -1;

    public LuckySlotAbility(AbilityCore<LuckySlotAbility> core) {
        super(core);
        if (super.isClientSide()) {
            GaugeComponent gaugeComponent = new GaugeComponent(this, this::renderGauge);
            super.addComponents(new AbilityComponent[]{ gaugeComponent });
        }
    }

    // ── API ───────────────────────────────────────────────────────────────────

    public void setSlotNumber(LivingEntity entity, int number) {
        this.slotNumber = Math.max(-1, Math.min(9, number));
        sync(entity);
    }

    public int getSlotNumber() {
        return this.slotNumber;
    }

    public boolean hasRolled() {
        return this.slotNumber >= 0;
    }

    private void sync(LivingEntity entity) {
        if (entity instanceof PlayerEntity) {
            WyNetwork.sendTo(new SSyncAbilityPacket(entity.getId(), this), (PlayerEntity) entity);
        }
    }

    // ── NBT persistence ───────────────────────────────────────────────────────

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        nbt.putInt("slotNumber", this.slotNumber);
        return nbt;
    }

    @Override
    public void load(CompoundNBT nbt) {
        this.slotNumber = nbt.getInt("slotNumber");
    }

    // ── Gauge renderer ────────────────────────────────────────────────────────

    @OnlyIn(Dist.CLIENT)
    public void renderGauge(PlayerEntity player, MatrixStack matrixStack, int posX, int posY, LuckySlotAbility ability) {
        RenderSystem.enableBlend();
        Minecraft mc = Minecraft.getInstance();
        mc.getTextureManager().bind(ModResources.WIDGETS);
        RendererHelper.drawAbilityIcon(INSTANCE, matrixStack, (float) posX, (float) (posY - 38), 0, 32.0F, 32.0F);

        String label = ability.hasRolled() ? String.valueOf(ability.getSlotNumber()) : "?";
        // Color changes based on the number — low = red, mid = yellow, high = green
        Color color;
        if (!ability.hasRolled()) {
            color = new Color(200, 200, 200); // grey for unrolled
        } else if (ability.getSlotNumber() <= 3) {
            color = new Color(255, 80, 80);   // red for low
        } else if (ability.getSlotNumber() <= 6) {
            color = new Color(255, 220, 50);  // yellow for mid
        } else {
            color = new Color(80, 255, 120);  // green for high
        }

        WyHelper.drawStringWithBorder(
                mc.font, matrixStack,
                label,
                posX + 16 - mc.font.width(label) / 2,
                posY - 25,
                color.getRGB()
        );
        RenderSystem.disableBlend();
    }

    // ── Static initialiser ────────────────────────────────────────────────────

    static {
        INSTANCE = (new AbilityCore.Builder<>(
                "Lucky Slot",
                AbilityCategory.DEVIL_FRUITS,
                AbilityType.PASSIVE,
                LuckySlotAbility::new
        ))
                .addDescriptionLine(new ITextComponent[]{ DESCRIPTION[0] })
                .setHidden()
                .build();
    }
}