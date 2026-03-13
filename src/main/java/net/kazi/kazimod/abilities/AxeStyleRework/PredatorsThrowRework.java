//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.AxeStyleRework;

import net.MrMagicalCart.cartaddon.api.helpers.AbilityLimits;
import net.MrMagicalCart.cartaddon.entities.projectiles.axestyle.AxeThrowProjectile;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AnimationComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;

public class PredatorsThrowRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("cartaddon", "predators_throw", new Pair[]{ImmutablePair.of("", (Object)null)});
    public static final AbilityCore<PredatorsThrowRework> INSTANCE;
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);
    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);

    public PredatorsThrowRework(AbilityCore<PredatorsThrowRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.projectileComponent, this.animationComponent, this.dealDamageComponent});
        this.addCanUseCheck(AbilityLimits::requirestwoAxe);
        this.addCanUseCheck(AbilityLimits::requiresAxe);
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        this.projectileComponent.shoot(entity, 2.2F, 1.2F);
        this.animationComponent.start(entity, ModAnimations.RAISE_RIGHT_ARM, 4);
        this.animationComponent.start(entity, ModAnimations.LOWERING_RIGHT_ARM, 4);
        if (!entity.level.isClientSide) {
            ((ServerWorld)entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
        }

        this.cooldownComponent.startCooldown(entity, 200.0F);
    }

    private AxeThrowProjectile createProjectile(LivingEntity entity) {
        AxeThrowProjectile proj = new AxeThrowProjectile(entity.level, entity);
        return proj;
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.getFightingStyle().equals(CartValues.DOUBLE_AXE) && questProps.hasFinishedQuest(CartQuests.AXE_TRIAL_01);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Predators Throw", AbilityCategory.STYLE, PredatorsThrowRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(200.0F)}).addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips()).setSourceHakiNature(SourceHakiNature.IMBUING).setUnlockCheck(PredatorsThrowRework::canUnlock).build();
    }
}
