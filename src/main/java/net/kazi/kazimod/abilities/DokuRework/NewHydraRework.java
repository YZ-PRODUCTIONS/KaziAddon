package net.kazi.kazimod.abilities.DokuRework;

import net.MrMagicalCart.cartaddon.init.CartMorphs;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.morph.MorphInfo;
import xyz.pixelatedw.mineminenomi.entities.projectiles.doku.HydraProjectile;
import xyz.pixelatedw.mineminenomi.wypi.WyRegistry;

public class NewHydraRework
extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText((String)"kazimod", (String)"hydra", (Pair[])new Pair[]{ImmutablePair.of((Object)"Launches a dragon made out of liquid poison at the opponent", null)});
    private static final ITextComponent NORMAL_NAME = new TranslationTextComponent(WyRegistry.registerName((String)"ability.mineminenomi.hydra", (String)"Hydra"));
    private static final ITextComponent VENOM_NAME = new TranslationTextComponent(WyRegistry.registerName((String)"ability.mineminenomi.hydra_venom", (String)"Venom Hydra"));
    private static final ResourceLocation NORMAL_ICON = new ResourceLocation("mineminenomi", "textures/abilities/hydra.png");
    private static final ResourceLocation VENOM_ICON = new ResourceLocation("mineminenomi", "textures/abilities/hydra_venom.png");
    private static final int COOLDOWN = 200;
    public static final AbilityCore<NewHydraRework> INSTANCE = new AbilityCore.Builder<NewHydraRework>("Hydra", AbilityCategory.DEVIL_FRUITS, NewHydraRework::new).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip((float)200.0f)}).addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips()).setSourceHakiNature(SourceHakiNature.SPECIAL).setSourceElement(SourceElement.POISON).build();
    private final ProjectileComponent projectileComponent = new ProjectileComponent((IAbility)this, this::createProjectile);
    private final AltModeComponent<Mode> altModeComponent = new AltModeComponent<Mode>((IAbility)this, Mode.class, Mode.NORMAL, true).addChangeModeEvent(this::onAltModeChange);

    public NewHydraRework(AbilityCore<NewHydraRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.projectileComponent, this.altModeComponent});
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        boolean isDemonForm = ((MorphInfo)CartMorphs.VENOM_DEMON2.get()).isActive(entity);
        this.projectileComponent.shoot(entity, isDemonForm ? 4.0f : 2.0f, 0.0f);
        this.cooldownComponent.startCooldown(entity, 200.0f);
    }

    private HydraProjectile createProjectile(LivingEntity entity) {
        boolean isDemonForm = ((MorphInfo)CartMorphs.VENOM_DEMON2.get()).isActive(entity);
        HydraProjectile proj = new HydraProjectile(entity.level, entity, isDemonForm);
        return proj;
    }

    public void setNormalMode(LivingEntity entity) {
        this.altModeComponent.setMode(entity, Mode.NORMAL);
    }

    public void setVenomMode(LivingEntity entity) {
        this.altModeComponent.setMode(entity, Mode.VENOM);
    }

    private void onAltModeChange(LivingEntity entity, IAbility ability, Mode mode) {
        if (mode == Mode.VENOM) {
            super.setDisplayName(VENOM_NAME);
            super.setDisplayIcon(VENOM_ICON);
        } else if (mode == Mode.NORMAL) {
            super.setDisplayName(NORMAL_NAME);
            super.setDisplayIcon(NORMAL_ICON);
        }
    }

    private static enum Mode {
        NORMAL,
        VENOM;

    }
}

