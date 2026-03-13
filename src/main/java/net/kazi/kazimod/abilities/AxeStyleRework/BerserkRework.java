//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.AxeStyleRework;

import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
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
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;

public class BerserkRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "berserk", new Pair[]{ImmutablePair.of(" Enters a short berserk state — every hit restores a bit of health and deals 5% more bonus damage.", (Object)null)});
    public static final AbilityCore<BerserkRework> INSTANCE;
    private final ContinuousComponent continuousComponent = (new ContinuousComponent(this, true)).addEndEvent(100, this::endContinuityEvent);

    public BerserkRework(AbilityCore<? extends IAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent});
        this.addUseEvent(this::onUseEvent);
        this.addCanUseCheck(AbilityLimits::requiresAxe);
        this.addCanUseCheck(AbilityLimits::requirestwoAxe);
    }

    private void onUseEvent(LivingEntity entity, IAbility iAbility) {
        this.continuousComponent.triggerContinuity(entity, 200.0F);
    }

    private void endContinuityEvent(LivingEntity entity, IAbility iAbility) {
        this.cooldownComponent.startCooldown(entity, 300.0F);
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.getFightingStyle().equals(CartValues.DOUBLE_AXE) && questProps.hasFinishedQuest(CartQuests.AXE_TRIAL_03);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Berserk", AbilityCategory.STYLE, BerserkRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, ContinuousComponent.getTooltip(200.0F), CooldownComponent.getTooltip(300.0F)}).setUnlockCheck(BerserkRework::canUnlock).build();
    }
}
