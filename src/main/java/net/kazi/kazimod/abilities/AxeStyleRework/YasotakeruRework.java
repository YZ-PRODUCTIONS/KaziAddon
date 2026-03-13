//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.AxeStyleRework;

import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.entities.projectiles.axestyle.YasotakeruProjectile;
import net.MrMagicalCart.cartaddon.init.CartAnimations;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.ITextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.RepeaterAbility2;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ChargeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RequireMorphComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;

public class YasotakeruRework extends RepeaterAbility2 {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "yasotakeru", new Pair[]{ImmutablePair.of("The user fires multiple cutting air blades.", (Object)null)});
    private static final float CHARGE_TIME = 60.0F;
    private static final float COOLDOWN = 400.0F;
    public static final AbilityCore<YasotakeruRework> INSTANCE;
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final ChargeComponent chargeComponent = (new ChargeComponent(this)).addStartEvent(this::startChargeEvent).addEndEvent(this::endChargeEvent);
    private float cooldown;
    private int triggers;
    private int interval;

    public YasotakeruRework(AbilityCore<YasotakeruRework> core) {
        super(core);
        this.isNew = true;
        this.cooldown = 400.0F;
        this.triggers = 6;
        this.interval = 10;
        this.addComponents(new AbilityComponent[]{this.projectileComponent, this.chargeComponent, this.animationComponent});
        this.addCanUseCheck(AbilityLimits::requirestwoAxe);
        this.addCanUseCheck(AbilityLimits::requiresAxe);
        this.addCanUseCheck(AbilityLimits::fruitless);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        this.chargeComponent.startCharging(entity, 0.0F);
    }

    private void startChargeEvent(LivingEntity entity, IAbility ability) {
        this.animationComponent.start(entity, CartAnimations.YASOTAKERU, 60);
    }

    private void endChargeEvent(LivingEntity entity, IAbility ability) {
        this.setCustomShootLogic((living) -> {
            for(int i = 0; i < 5; ++i) {
                this.projectileComponent.shootWithSpread(living, 1.7F, 1.0F, 2);
            }

        });
    }

    public int getMaxTriggers() {
        return this.triggers;
    }

    public int getTriggerInterval() {
        return this.interval;
    }

    public float getRepeaterCooldown() {
        return this.cooldown;
    }

    public AbilityProjectileEntity getProjectileFactory(LivingEntity entity) {
        AbilityProjectileEntity projectile = new YasotakeruProjectile(entity.level, entity);
        return projectile;
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.getFightingStyle().equals(CartValues.DOUBLE_AXE) && questProps.hasFinishedQuest(CartQuests.AXE_TRIAL_07);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Yasotakeru", AbilityCategory.STYLE, YasotakeruRework::new)).addDescriptionLine(DESCRIPTION).addDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, RequireMorphComponent.getTooltip()}).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(400.0F)}).addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips()).setSourceType(new SourceType[]{SourceType.SLASH}).setSourceHakiNature(SourceHakiNature.SPECIAL).setUnlockCheck(YasotakeruRework::canUnlock).build();
    }
}
