package net.kazi.kazimod.abilities.Kake;

import com.mojang.blaze3d.matrix.MatrixStack;
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
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.packets.server.ability.SSyncAbilityPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;


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
    private static final int ROLL_LIFETIME = 30 * 20;
    private int rollTicksRemaining;

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
        this.rollTicksRemaining = this.slotNumber >= 0 ? ROLL_LIFETIME : 0;
        sync(entity);
    }

    @Override
    public void tick(LivingEntity entity) {
        super.tick(entity);
        if (entity.level.isClientSide || !hasRolled()) {
            return;
        }
        if (--this.rollTicksRemaining <= 0) {
            setSlotNumber(entity, -1);
            IAbilityData data = AbilityDataCapability.get(entity);
            CasinoRollAbility casino = data == null ? null : data.getEquippedAbility(CasinoRollAbility.INSTANCE);
            if (casino != null) {
                casino.setModeForRoll(entity, -1);
                if (entity instanceof PlayerEntity) {
                    WyNetwork.sendTo(new SSyncAbilityPacket(entity.getId(), casino), (PlayerEntity) entity);
                }
            }
        }
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
        nbt.putInt("rollTicksRemaining", this.rollTicksRemaining);
        return nbt;
    }

    @Override
    public void load(CompoundNBT nbt) {
        this.slotNumber = nbt.contains("slotNumber") ? Math.max(-1, Math.min(9, nbt.getInt("slotNumber"))) : -1;
        this.rollTicksRemaining = hasRolled()
                ? Math.max(0, Math.min(ROLL_LIFETIME, nbt.contains("rollTicksRemaining")
                    ? nbt.getInt("rollTicksRemaining") : ROLL_LIFETIME)) : 0;
    }

    // ── Gauge renderer ────────────────────────────────────────────────────────

    @OnlyIn(Dist.CLIENT)
    public void renderGauge(PlayerEntity player, MatrixStack matrixStack, int posX, int posY, LuckySlotAbility ability) {
        net.kazi.kazimod.kake.KakeHud.render(player,matrixStack,posX,posY,ability.getSlotNumber());
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
                .setIcon(new net.minecraft.util.ResourceLocation("kazimod","textures/abilities/lucky_slot.png"))
                .setHidden()
                .build();
    }
}
