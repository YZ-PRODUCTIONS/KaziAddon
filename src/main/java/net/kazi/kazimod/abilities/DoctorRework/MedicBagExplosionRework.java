//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.DoctorRework;

import java.util.Optional;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.MathHelper;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.HealComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.RangeComponent.RangeType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.helpers.ItemsHelper;
import xyz.pixelatedw.mineminenomi.api.util.TargetsPredicate;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModArmors;
import xyz.pixelatedw.mineminenomi.init.ModParticleEffects;
import xyz.pixelatedw.mineminenomi.init.ModQuests;
import xyz.pixelatedw.mineminenomi.particles.effects.ParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class MedicBagExplosionRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "medic_bag_explosion", new Pair[]{ImmutablePair.of("By sacrificing the medic bag's durability the user can heal themselves with regeneration while applying debuffs to nearby enemies.", (Object)null)});
    public static final TargetsPredicate ENEMY_AREA_CHECK = (new TargetsPredicate()).testEnemyFaction();
    public static final TargetsPredicate FRIENDLY_AREA_CHECK = (new TargetsPredicate()).testFriendlyFaction();
    private static final int COOLDOWN = 800;
    private static final int RANGE = 10;
    private static final int MIN_HEAL = 2;
    private static final int MAX_HEAL = 25;
    public static final AbilityCore<MedicBagExplosionRework> INSTANCE;
    private final RangeComponent rangeComponent = new RangeComponent(this);
    private final HealComponent healComponent = new HealComponent(this);

    public MedicBagExplosionRework(AbilityCore<MedicBagExplosionRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.rangeComponent, this.healComponent});
        this.addCanUseCheck(AbilityHelper::requiresMedicBag);
        this.addUseEvent(this::useEvent);
    }

    private void useEvent(LivingEntity entity, IAbility ability) {
        boolean isHandEmpty = entity.getMainHandItem().isEmpty();
        Optional<ItemStack> medicBag = !isHandEmpty ? Optional.ofNullable(entity.getMainHandItem()) : ItemsHelper.findItemInSlot(entity, EquipmentSlotType.CHEST, (Item)ModArmors.MEDIC_BAG.get());
        if (medicBag.isPresent()) {
            float heal = (float)MathHelper.clamp(WyHelper.percentage((double)20.0F, (double)entity.getMaxHealth()), (double)5.0F, (double)25.0F);
            this.healComponent.healTarget(entity, entity, heal);

            for(LivingEntity target : this.rangeComponent.getTargetsInArea(entity, 10.0F, ENEMY_AREA_CHECK)) {
                int effect = (int)WyHelper.randomWithRange(0, 6);
                switch (effect) {
                    case 0:
                        target.addEffect(new EffectInstance(Effects.BLINDNESS, 200, 1));
                        break;
                    case 1:
                        target.addEffect(new EffectInstance(Effects.CONFUSION, 200, 1));
                        break;
                    case 2:
                        target.addEffect(new EffectInstance(Effects.DIG_SLOWDOWN, 200, 1));
                        break;
                    case 3:
                        target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 200, 1));
                        break;
                    case 4:
                        target.addEffect(new EffectInstance(Effects.POISON, 200, 1));
                        break;
                    case 5:
                        target.addEffect(new EffectInstance(Effects.WITHER, 200, 1));
                        break;
                    case 6:
                        target.addEffect(new EffectInstance(Effects.WEAKNESS, 200, 1));
                }
            }

            for(LivingEntity target : this.rangeComponent.getTargetsInArea(entity, 10.0F, FRIENDLY_AREA_CHECK)) {
                target.addEffect(new EffectInstance(Effects.REGENERATION, 100, 2));
            }

            WyHelper.spawnParticleEffect((ParticleEffect)ModParticleEffects.MEDIC_BAG_EXPLOSION.get(), entity, entity.position().x, entity.position().y, entity.position().z);
            this.cooldownComponent.startCooldown(entity, 800.0F);
            ((ItemStack)medicBag.get()).hurtAndBreak(250, entity, (user) -> user.broadcastBreakEvent(EquipmentSlotType.MAINHAND));
        }
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        } else {
            PlayerEntity player = (PlayerEntity)entity;
            IEntityStats props = EntityStatsCapability.get(player);
            IQuestData questProps = QuestDataCapability.get(player);
            return props.isDoctor() && questProps.hasFinishedQuest(ModQuests.DOCTOR_TRIAL_03);
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Medic Bag Explosion", AbilityCategory.STYLE, MedicBagExplosionRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(800.0F), RangeComponent.getTooltip(10.0F, RangeType.AOE), HealComponent.getTooltip(5.0F, 50.0F)}).setUnlockCheck(MedicBagExplosionRework::canUnlock).build();
    }
}
