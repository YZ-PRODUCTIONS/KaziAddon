package net.kazi.kazimod.abilities.NetsuRework;

import net.kazi.kazimod.entities.projectiles.HellfireBirdProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.IDevilFruit;
import xyz.pixelatedw.mineminenomi.init.ModAbilities;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

public class HellfireBirdAbility extends Ability {

    private static final ResourceLocation ICON =
            new ResourceLocation("kazimod", "textures/abilities/netsu_karasusu_red.png");
    private static final TranslationTextComponent NAME =
            new TranslationTextComponent(WyRegistry.registerName("ability.kazimod.hellfire_bird", "Hellfire Bird"));
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "hellfire_bird",
            new Pair[]{ImmutablePair.of("Launches a hellfire bird that weakly tracks enemies before erupting in flames.", null)}
    );

    private static final float COOLDOWN = 300.0F;
    private static final float DAMAGE = 35.0F;

    public static final AbilityCore<HellfireBirdAbility> INSTANCE =
            new AbilityCore.Builder<>("Hellfire Bird", AbilityCategory.DEVIL_FRUITS, HellfireBirdAbility::new)
                    .addDescriptionLine(DESCRIPTION)
                    .addAdvancedDescriptionLine(
                            AbilityDescriptionLine.NEW_LINE,
                            DealDamageComponent.getTooltip(DAMAGE),
                            CooldownComponent.getTooltip(COOLDOWN)
                    )
                    .addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips())
                    .setIcon(ICON)
                    .setSourceElement(SourceElement.FIRE)
                    .setSourceHakiNature(SourceHakiNature.IMBUING)
                    .setSourceType(new SourceType[]{SourceType.PROJECTILE, SourceType.INTERNAL})
                    .setUnlockCheck(HellfireBirdAbility::canUnlock)
                    .build();

    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);

    public HellfireBirdAbility(AbilityCore<HellfireBirdAbility> core) {
        super(core);
        this.isNew = true;
        this.setDisplayName(NAME);
        this.addComponents(new AbilityComponent[]{this.projectileComponent});
        this.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        if (entity.level.isClientSide) {
            return;
        }

        this.projectileComponent.shoot(entity, 2.2F, 0.0F);
        entity.level.playSound(null, entity.blockPosition(), ModSounds.MERA_SFX.get(), SoundCategory.PLAYERS, 3.0F, 0.85F);
        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    private HellfireBirdProjectile createProjectile(LivingEntity entity) {
        return new HellfireBirdProjectile(entity.level, entity);
    }

    private static boolean canUnlock(LivingEntity user) {
        IDevilFruit devilFruit = DevilFruitCapability.get(user);
        return devilFruit != null
                && devilFruit.hasDevilFruit(ModAbilities.NETSU_NETSU_NO_MI)
                && devilFruit.hasAwakenedFruit();
    }
}
