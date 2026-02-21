//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.GomuRework;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine.IDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.gomu.GomuGomuNoPistolProjectile;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

public class RedRocAbility extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "gomu_gomu_no_pistol", new Pair[]{ImmutablePair.of("The user stretches their arm to punch the opponent.", (Object)null)});
    private static final TranslationTextComponent GOMU_GOMU_NO_PISTOL_NAME = new TranslationTextComponent(WyRegistry.registerName("ability.mineminenomi.gomu_gomu_no_pistol", "Gomu Gomu no Pistol"));
    private static final ResourceLocation GOMU_GOMU_NO_PISTOL_ICON = new ResourceLocation("mineminenomi", "textures/abilities/gomu_gomu_no_pistol.png");
    private static final int COOLDOWN = 30;
    private static final AbilityDescriptionLine.IDescriptionLine NO_GEAR_NAME_DESC;
    public static final AbilityCore<RedRocAbility> INSTANCE;
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);

    public RedRocAbility(AbilityCore<RedRocAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.projectileComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        AbilityProjectileEntity projectile = this.createProjectile(entity);
        this.projectileComponent.shoot(projectile, entity, 2.0F, 0.0F);
        entity.swing(Hand.MAIN_HAND, true);
        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.GOMU_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.0F);
        this.cooldownComponent.startCooldown(entity, 30.0F);
    }

    private AbilityProjectileEntity createProjectile(LivingEntity entity) {
        return new GomuGomuNoPistolProjectile(entity.level, entity);
    }

    static {
        NO_GEAR_NAME_DESC = IDescriptionLine.of(AbilityHelper.mentionText(GOMU_GOMU_NO_PISTOL_NAME));
        INSTANCE = (new AbilityCore.Builder("Gomu Gomu no Pistol", AbilityCategory.DEVIL_FRUITS, RedRocAbility::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        NO_GEAR_NAME_DESC,
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(30.0F)
                })
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .setSourceType(new SourceType[]{SourceType.FIST})
                .build();
    }
}