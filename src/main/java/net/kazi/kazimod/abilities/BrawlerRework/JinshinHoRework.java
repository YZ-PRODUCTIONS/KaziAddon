//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.BrawlerRework;

import java.util.List;
import net.MrMagicalCart.cartaddon.init.CartQuests;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.DevilFruitHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.particles.effects.GroundParticlesEffect;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;

public class JinshinHoRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "jishin_ho", new Pair[]{ImmutablePair.of("Punches the ground to cause a quake that damages everyone around", (Object)null)});
    public static final ParticleEffect PARTICLES = new GroundParticlesEffect(7, 100);
    private static final float COOLDOWN = 250.0F;
    private static final float DAMAGE = 35.0F;
    private static final float RANGE = 7.0F;
    public static final AbilityCore<JinshinHoRework> INSTANCE;
    private final ContinuousComponent continuousComponent = new ContinuousComponent(this);
    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);
    private final RangeComponent rangeComponent = new RangeComponent(this);

    public JinshinHoRework(AbilityCore<JinshinHoRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.continuousComponent, this.dealDamageComponent, this.rangeComponent});
        this.addCanUseCheck(AbilityHelper::canUseBrawlerAbilities);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity player, IAbility ability) {
        PARTICLES.spawn(player.level, player.getX(), player.getY(), player.getZ(), (double)0.0F, (double)0.0F, (double)0.0F);
        List<LivingEntity> targets = this.rangeComponent.getTargetsInArea(player, 7.0F);
        targets.remove(player);
        targets.removeIf((entity) -> !entity.isOnGround() && DevilFruitHelper.getDifferenceToFloor(player) > (double)3.5F);

        for(LivingEntity target : targets) {
            boolean flag = this.dealDamageComponent.hurtTarget(player, target, 35.0F);
            if (flag) {
                AbilityHelper.setDeltaMovement(target, (double)0.0F, (double)0.75F, (double)0.0F);
            }
        }

        this.cooldownComponent.startCooldown(player, 250.0F);
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.isBrawler() && questProps.hasFinishedQuest(CartQuests.BRAWLER_TRIAL_05);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Jishin Ho", AbilityCategory.STYLE, JinshinHoRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(250.0F), DealDamageComponent.getTooltip(35.0F), RangeComponent.getTooltip(7.0F, RangeType.AOE)}).setSourceType(new SourceType[]{SourceType.FIST, SourceType.INDIRECT}).setSourceElement(SourceElement.SHOCKWAVE).setSourceHakiNature(SourceHakiNature.HARDENING).setUnlockCheck(JinshinHoRework::canUnlock).build();
    }
}
