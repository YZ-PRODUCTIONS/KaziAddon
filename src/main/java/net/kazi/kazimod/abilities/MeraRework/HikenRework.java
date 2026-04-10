package net.kazi.kazimod.abilities.MeraRework;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import net.kazi.kazimod.entities.projectiles.ReworkedHikenProjectile;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

public class HikenRework extends Ability {
    private static final ResourceLocation ICON = new ResourceLocation("mineminenomi", "textures/abilities/hiken.png");
    private static final TranslationTextComponent NAME =
            new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.hiken", "Hiken"));

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "mineminenomi", "hiken",
            new Pair[]{ImmutablePair.of("Turns the user's fist into flames and launches it towards the target", null)}
    );

    public static final AbilityCore<HikenRework> INSTANCE =
            new AbilityCore.Builder<>("Hiken", AbilityCategory.DEVIL_FRUITS, HikenRework::new)
                    .addDescriptionLine(DESCRIPTION)
                    .addAdvancedDescriptionLine(
                            AbilityDescriptionLine.NEW_LINE,
                            CooldownComponent.getTooltip(360.0F)
                    )
                    .addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips())
                    .setSourceHakiNature(SourceHakiNature.IMBUING)
                    .setSourceElement(SourceElement.FIRE)
                    .setIcon(ICON)
                    .build();

    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final ChargeComponent chargeComponent = new ChargeComponent(this).addEndEvent(this::onChargeEnd);

    public HikenRework(AbilityCore<HikenRework> core) {
        super(core);
        this.isNew = true;
        this.setDisplayName(NAME);
        this.addComponents(new AbilityComponent[]{this.projectileComponent, this.chargeComponent, this.animationComponent});
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (this.chargeComponent.isCharging()) {
            this.chargeComponent.stopCharging(entity);
            return;
        }

        this.chargeComponent.startCharging(entity, 5.0F);
        this.animationComponent.start(entity, ModAnimations.CHARGE_PUNCH);
    }

    private void onChargeEnd(LivingEntity entity, IAbility ability) {
        this.projectileComponent.shoot(entity, 2.0F, 1.0F);
        entity.level.playSound(null, entity.blockPosition(), ModSounds.MERA_SFX.get(), SoundCategory.PLAYERS, 3.0F, 1.0F);
        this.cooldownComponent.startCooldown(entity, 360.0F);
        this.animationComponent.stop(entity);
    }

    private ReworkedHikenProjectile createProjectile(LivingEntity entity) {
        return new ReworkedHikenProjectile(entity.level, entity);
    }
}
