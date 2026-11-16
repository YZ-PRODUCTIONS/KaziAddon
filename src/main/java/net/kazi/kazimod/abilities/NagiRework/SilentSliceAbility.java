package net.kazi.kazimod.abilities.NagiRework;

import net.kazi.kazimod.entities.projectiles.SilentSliceProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.vector.Vector3d;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.init.ModSounds;

public class SilentSliceAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION =
            AbilityHelper.registerDescriptionText("kazimod", "silent_slice",
                    new Pair[]{ImmutablePair.of(
                            "The user fires an invisible air slash that cuts through enemies unseen.", null)});

    private static final ResourceLocation ICON = new ResourceLocation("kazimod", "textures/abilities/silent_slice.png");
    public static final float DAMAGE_VALUE = 18.0F;
    private static final float COOLDOWN = 300.0F; // 15 seconds
    private static final float RANGE = 20.0F;

    public static final AbilityCore<SilentSliceAbility> INSTANCE;

    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);

    public SilentSliceAbility(AbilityCore<SilentSliceAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.dealDamageComponent,
                this.rangeComponent
        });
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (!entity.level.isClientSide) {
            Vector3d look = entity.getLookAngle();
            SilentSliceProjectile slash = new SilentSliceProjectile(entity.level, entity);
            slash.setPos(
                    entity.getX() + look.x * 1.5,
                    entity.getY() + 1.0 + look.y * 1.5,
                    entity.getZ() + look.z * 1.5
            );
            slash.setDeltaMovement(look.x * 2.5, look.y * 2.5, look.z * 2.5);
            entity.level.addFreshEntity(slash);
        }

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(),
                (SoundEvent) ModSounds.DASH_ABILITY_SWOOSH_SFX.get(),
                SoundCategory.PLAYERS, 0.5F, 1.8F);

        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    private static boolean canUnlock(LivingEntity user) {
        return DevilFruitCapability.get(user).hasAwakenedFruit();
    }

    static {
        INSTANCE = (new AbilityCore.Builder<>("Silent Slice", AbilityCategory.DEVIL_FRUITS, SilentSliceAbility::new))
                .setIcon(ICON)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN),
                        DealDamageComponent.getTooltip(DAMAGE_VALUE),
                        RangeComponent.getTooltip(RANGE, RangeType.LINE)
                })
                .setUnlockCheck(SilentSliceAbility::canUnlock)
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .build();
    }
}
