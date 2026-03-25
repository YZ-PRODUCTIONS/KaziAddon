package net.kazi.kazimod.abilities.Gojo;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.DamageTakenComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.PoolComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.StackComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModAbilityPools;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public class InfinityAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "infinity",
            new Pair[]{ImmutablePair.of("Activates Infinity, an infinite convergence that slows all matter to a halt before it can reach Gojo. Negates all incoming damage while active.", (Object) null)}
    );

    private static final float COOLDOWN_PER_STACK = 120.0F;
    private static final int   MAX_STACKS         = 6;

    public static final AbilityCore<InfinityAbility> INSTANCE;

    private final ContinuousComponent continuousComponent =
            (new ContinuousComponent(this, true))
                    .addStartEvent(this::onStartContinuity)
                    .addTickEvent(this::onTickContinuity)
                    .addEndEvent(this::onEndContinuity);

    private final DamageTakenComponent damageTakenComponent =
            (new DamageTakenComponent(this))
                    .addOnAttackEvent(this::onDamageTaken);

    private final StackComponent stackComponent;
    private final PoolComponent  poolComponent;

    private int stackInvulTimer = 0;
    private static final int STACK_INVUL_TICKS = 10;

    public InfinityAbility(AbilityCore<InfinityAbility> core) {
        super(core);
        this.stackComponent = (new StackComponent(this))
                .addStackChangeEvent(this::onStackChange);
        this.poolComponent = new PoolComponent(this, ModAbilityPools.DODGE_ABILITY);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.continuousComponent,
                this.damageTakenComponent,
                this.stackComponent,
                this.poolComponent
        });
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addUseEvent(this::onUseEvent);
        this.addEquipEvent(this::onEquip);
    }

    private void onEquip(LivingEntity entity, Ability ability) {
        this.stackComponent.setDefaultStacks(MAX_STACKS);
        this.stackComponent.revertStacksToDefault(entity, ability);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        this.continuousComponent.triggerContinuity(entity);
    }

    private void onStartContinuity(LivingEntity entity, IAbility ability) {
        this.stackComponent.setDefaultStacks(MAX_STACKS);
        this.stackComponent.revertStacksToDefault(entity, ability);
    }

    private void onTickContinuity(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            entity.addEffect(new EffectInstance(
                    Effects.DAMAGE_RESISTANCE, 10, 4, false, false));
            if (stackInvulTimer > 0) stackInvulTimer--;
        }
    }

    private void onEndContinuity(LivingEntity entity, IAbility ability) {
        int stacksUsed = MAX_STACKS - this.stackComponent.getStacks();
        float cooldown = Math.max(COOLDOWN_PER_STACK, stacksUsed * COOLDOWN_PER_STACK);
        super.cooldownComponent.startCooldown(entity, cooldown);
        this.stackComponent.revertStacksToDefault(entity, this);
        stackInvulTimer = 0;
    }

    private void onStackChange(LivingEntity entity, IAbility ability, int stacks) {
        if (stacks <= 0) {
            this.continuousComponent.stopContinuity(entity);
        }
    }

    public float onDamageTaken(LivingEntity entity, IAbility ability, DamageSource source, float damage) {
        if (!this.continuousComponent.isContinuous()) return damage;

        if (source.getEntity() == null || !(source.getEntity() instanceof LivingEntity)) {
            return 0.0F;
        }

        if (source.isBypassInvul()) return damage;

        if (stackInvulTimer > 0) return 0.0F;

        this.stackComponent.addStacks(entity, this, -1);
        stackInvulTimer = STACK_INVUL_TICKS;

        if (!entity.level.isClientSide) {
            entity.level.playSound(
                    (PlayerEntity) null,
                    entity.blockPosition(),
                    ModSounds.KENBUNSHOKU_HAKI_ON_SFX.get(),
                    SoundCategory.PLAYERS,
                    1.5F, 0.1F
            );
        }

        return 0.0F;
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Infinity", AbilityCategory.DEVIL_FRUITS, InfinityAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        StackComponent.getTooltip(MAX_STACKS),
                        CooldownComponent.getTooltip(COOLDOWN_PER_STACK * MAX_STACKS)
                })
                .setSourceType(new SourceType[]{SourceType.INDIRECT})
                .build();
    }
}