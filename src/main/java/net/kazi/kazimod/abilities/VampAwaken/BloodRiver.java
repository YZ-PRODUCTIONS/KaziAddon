//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.VampAwaken;

import net.kazi.kazimod.entities.projectiles.BloodRiverProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.ResourceLocation;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;

public class BloodRiver extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "blood_river",
            new Pair[]{ImmutablePair.of(
                    "Unleashes a surging river of blood that overwhelms enemies, leaving them disoriented, slowed, weakened, and briefly paralyzed.",
                    null)});
    private static final ResourceLocation ICON =
            new ResourceLocation("cartaddon", "textures/abilities/blood_burst.png");
    private static final float COOLDOWN = 500.0F;
    public static final AbilityCore<BloodRiver> INSTANCE;
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);

    public BloodRiver(AbilityCore<BloodRiver> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.projectileComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        entity.level.playSound((PlayerEntity)null, entity.blockPosition(),
                SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, SoundCategory.PLAYERS, 2.5F, 1.0F);
        entity.level.playSound((PlayerEntity)null, entity.blockPosition(),
                SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.PLAYERS, 1.6F, 0.95F);
        this.projectileComponent.shoot(entity, 3.0F, 0.0F);
        this.cooldownComponent.startCooldown(entity, 500.0F);
    }

    private BloodRiverProjectile createProjectile(LivingEntity entity) {
        BloodRiverProjectile proj = new BloodRiverProjectile(entity.level, entity);
        return proj;
    }

    private static boolean canUnlock(LivingEntity entity) {
        return DevilFruitCapability.get(entity).hasAwakenedFruit();
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Blood River", AbilityCategory.DEVIL_FRUITS, BloodRiver::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(400.0F)})
                .addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips())
                .setIcon(ICON)
                .setSourceElement(SourceElement.SHOCKWAVE)
                .setUnlockCheck(BloodRiver::canUnlock)
                .build();
    }
}
