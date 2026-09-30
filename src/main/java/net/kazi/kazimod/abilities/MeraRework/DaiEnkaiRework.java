package net.kazi.kazimod.abilities.MeraRework;

import net.kazi.kazimod.entities.EnteiGeometry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.abilities.mera.DaiEnkaiAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.mera.DaiEnkaiEnteiProjectile;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

/** Dai Enkai with the growing Entei's overhead clearance handled by the ability. */
public class DaiEnkaiRework extends DaiEnkaiAbility {
    public static final AbilityCore<DaiEnkaiRework> INSTANCE =
            new AbilityCore.Builder<>("Dai Enkai: Entei", AbilityCategory.DEVIL_FRUITS,
                    DaiEnkaiRework::new)
                    .setIcon(new ResourceLocation("mineminenomi", "textures/abilities/dai_enkai_entei.png"))
                    .addDescriptionLine(
                            new StringTextComponent("Amasses the user's flames into a gigantic fireball that the user hurls at the opponent."),
                            new StringTextComponent("Creates a giant fire vortex around the user and fires three flame dragons at the enemy."))
                    .setSourceHakiNature(SourceHakiNature.SPECIAL)
                    .setSourceElement(SourceElement.FIRE)
                    .build();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public DaiEnkaiRework(AbilityCore<DaiEnkaiRework> core) {
        super((AbilityCore) core);
        this.addTickEvent(this::updateEnteiChargePosition);
    }

    private void updateEnteiChargePosition(LivingEntity user, IAbility ability) {
        if (!this.isCharging() && !this.isContinuous()) return;

        ProjectileComponent projectileComponent = this
                .<ProjectileComponent>getComponent(ModAbilityKeys.PROJECTILE)
                .orElse(null);
        if (projectileComponent == null) return;

        AbilityProjectileEntity cachedProjectile = projectileComponent.getCachedProjectile(user);
        if (!(cachedProjectile instanceof DaiEnkaiEnteiProjectile) || !cachedProjectile.isAlive()) return;

        DaiEnkaiEnteiProjectile entei = (DaiEnkaiEnteiProjectile) cachedProjectile;
        double overheadOffset = EnteiGeometry.overheadOffset(entei.getSize());
        entei.setPos(user.getX(), user.getY() + user.getEyeHeight() + overheadOffset, user.getZ());
    }
}
