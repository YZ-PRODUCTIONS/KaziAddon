//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.KageRework;

import javax.annotation.Nullable;
import net.kazi.kazimod.entities.ShadowDoppelmanEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.StackComponent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;

public class DoppelmanRework extends Ability {
    private static final int HOLD_TIME = 12000;
    private static final int MIN_COOLDOWN = 600;
    private static final int MAX_COOLDOWN = 6000;
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "doppelman", new Pair[]{ImmutablePair.of("Creates a living version of the user's shadow to help them fight", (Object)null)});
    public static final AbilityCore<DoppelmanRework> INSTANCE;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this, true)).addStartEvent(100, this::startContinuityEvent).addTickEvent(100, this::onTickEvent).addEndEvent(100, this::stopContinuityEvent);
    private final StackComponent stackComponent = new StackComponent(this);
    private ShadowDoppelmanEntity doppelman = null;
    private int shadowsUsed = 0;
    private int prevShadowsUsed = 0;

    public DoppelmanRework(AbilityCore<DoppelmanRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.stackComponent});
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity);
    }

    private void startContinuityEvent(LivingEntity entity, IAbility ability) {
        this.doppelman = new ShadowDoppelmanEntity(entity.level, entity);
        this.doppelman.moveTo(entity.getX(), entity.getY(), entity.getZ(), entity.yRot, entity.xRot);
        if (!entity.level.isClientSide) {
            this.doppelman.setShadow(this.shadowsUsed);
            this.stackComponent.setStacks(entity, this, this.shadowsUsed);
        }

        entity.level.addFreshEntity(this.doppelman);
    }

    private void onTickEvent(LivingEntity entity, IAbility ability) {
        if (this.doppelman != null && this.doppelman.isAlive()) {
            this.shadowsUsed = this.doppelman.getShadows();
            if (this.shadowsUsed != this.prevShadowsUsed) {
                this.stackComponent.setStacks(entity, this, this.shadowsUsed);
                this.prevShadowsUsed = this.shadowsUsed;
            }

        } else {
            this.continuousComponent.stopContinuity(entity);
        }
    }

    private void stopContinuityEvent(LivingEntity entity, IAbility ability) {
        if (this.doppelman != null) {
            this.doppelman.remove();
        }

        this.prevShadowsUsed = 0;
        float cooldown = MathHelper.clamp(this.continuousComponent.getContinueTime(), 600.0F, 6000.0F);
        this.cooldownComponent.startCooldown(entity, cooldown);
    }

    @Nullable
    public ShadowDoppelmanEntity getDoppelman() {
        return this.doppelman;
    }

    public void doppelmanDeathTrigger(LivingEntity owner) {
        this.shadowsUsed = 0;
        this.stackComponent.setStacks(owner, this, 0);
    }

    public CompoundNBT save(CompoundNBT nbt) {
        nbt = super.save(nbt);
        nbt.putInt("shadows", this.shadowsUsed);
        return nbt;
    }

    public void load(CompoundNBT nbt) {
        super.load(nbt);
        this.shadowsUsed = nbt.getInt("shadows");
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Doppelman", AbilityCategory.DEVIL_FRUITS, DoppelmanRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(40.0F, 6000.0F), ContinuousComponent.getTooltip()}).build();
    }
}
