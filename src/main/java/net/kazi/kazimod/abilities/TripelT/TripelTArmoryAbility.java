package net.kazi.kazimod.abilities.TripelT;

import net.kazi.kazimod.init.KaziItems2;
import net.kazi.kazimod.init.KaziMorphs;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUseResult;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AltModeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ItemSpawnComponent;

public class TripelTArmoryAbility extends Ability {

    private static final ITextComponent[] DESCRIPTION = {
            new StringTextComponent("Summon the Triple T Bat. Automatically switches to Triple T Staff in God form.")
    };

    public static final AbilityCore<TripelTArmoryAbility> INSTANCE;

    private static final ResourceLocation BAT_ICON = new ResourceLocation("kazimod", "textures/abilities/triple_t_bat.png");
    private static final ResourceLocation STAFF_ICON = new ResourceLocation("kazimod", "textures/abilities/triple_t_staff.png");

    private Mode currentMode = Mode.BAT;
    private final AltModeComponent<Mode> altModeComponent =
            new AltModeComponent<>(this, Mode.class, Mode.BAT).addChangeModeEvent(this::onModeChange);
    private final ContinuousComponent continuousComponent =
            new ContinuousComponent(this, true).addStartEvent(this::onStart).addTickEvent(this::onTick).addEndEvent(this::onEnd);
    private final ItemSpawnComponent itemSpawnComponent = new ItemSpawnComponent(this);

    public TripelTArmoryAbility(AbilityCore<TripelTArmoryAbility> core) {
        super(core);
        this.isNew = true;
        this.setDisplayName(new StringTextComponent("Triple T Bat"));
        this.setDisplayIcon(BAT_ICON);
        this.addComponents(new AbilityComponent[]{this.altModeComponent, this.continuousComponent, this.itemSpawnComponent});
        this.addCanUseCheck(this::canUse);
        this.addUseEvent(this::onUse);
    }

    private AbilityUseResult canUse(LivingEntity entity, IAbility ability) {
        return AbilityUseResult.fail(new StringTextComponent("Your transformation now automatically grants its weapon."));
    }

    public void dismiss(LivingEntity entity) {
        if (this.continuousComponent.isContinuous()) this.continuousComponent.stopContinuity(entity);
    }

    private void onUse(LivingEntity entity, IAbility ability) {
        TripelTHelper.playTungSound(entity, 1.9F, resolveMode(entity) == Mode.STAFF ? 1.12F : 0.94F);
        if (this.continuousComponent.isContinuous()) {
            this.continuousComponent.stopContinuity(entity);
        } else {
            this.continuousComponent.triggerContinuity(entity);
        }
    }

    private void onStart(LivingEntity entity, IAbility ability) {
        Mode mode = resolveMode(entity);
        this.currentMode = mode;
        this.altModeComponent.setMode(entity, mode);
        if (mode == Mode.STAFF) {
            this.setDisplayName(new StringTextComponent("Triple T Staff"));
            this.setDisplayIcon(STAFF_ICON);
        } else {
            this.setDisplayName(new StringTextComponent("Triple T Bat"));
            this.setDisplayIcon(BAT_ICON);
        }
        ItemStack stack = new ItemStack((IItemProvider) (mode == Mode.BAT ? KaziItems2.TRIPLE_T_BAT.get() : KaziItems2.TRIPLE_T_STAFF.get()));
        this.itemSpawnComponent.spawnItem(entity, stack);
    }

    private void onTick(LivingEntity entity, IAbility ability) {
        Mode targetMode = resolveMode(entity);

        if (targetMode != this.currentMode) {
            this.continuousComponent.stopContinuity(entity);
            this.currentMode = targetMode;
            return;
        }

        boolean hasTransform = KaziMorphs.TRIPEL_T.get().isActive(entity) || KaziMorphs.TRIPEL_T_GOD.get().isActive(entity);
        if (!hasTransform) {
            this.continuousComponent.stopContinuity(entity);
        }
    }

    private void onEnd(LivingEntity entity, IAbility ability) {
        this.itemSpawnComponent.despawnItems(entity);
        this.currentMode = Mode.BAT;
    }

    private void onModeChange(LivingEntity entity, IAbility ability, Enum<?> mode) {
        if (mode == Mode.STAFF) {
            this.setDisplayName(new StringTextComponent("Triple T Staff"));
            this.setDisplayIcon(STAFF_ICON);
        } else {
            this.setDisplayName(new StringTextComponent("Triple T Bat"));
            this.setDisplayIcon(BAT_ICON);
        }
    }

    private Mode resolveMode(LivingEntity entity) {
        if (KaziMorphs.TRIPEL_T_GOD.get().isActive(entity)) {
            return Mode.STAFF;
        }
        return Mode.BAT;
    }

    static {
        INSTANCE = new AbilityCore.Builder<>("Triple T Armory", AbilityCategory.DEVIL_FRUITS, TripelTArmoryAbility::new)
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(AbilityDescriptionLine.NEW_LINE, ContinuousComponent.getTooltip())
                .build();
    }

    public enum Mode {
        BAT,
        STAFF
    }
}
