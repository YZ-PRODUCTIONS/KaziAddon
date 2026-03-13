package net.kazi.kazimod.abilities.swordsmanrework;

import net.MrMagicalCart.cartaddon.entities.projectiles.swordsmenextra.ReworkedYakkodoriProjectile;
import net.kazi.kazimod.entities.projectiles.YakkodoriReworkProjectile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
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
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModQuests;

public class YakkodoriRework extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "mineminenomi", "yakkodori",
            new Pair[]{ImmutablePair.of(
                    "Launches a crescent moon-shaped slash, which destroys everything in its path",
                    (Object) null
            )}
    );

    private static final ITextComponent[] FOXFIRE_DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "yakkodori_foxfire",
            new Pair[]{ImmutablePair.of(
                    "§6Foxfire Mode§r (requires §6Foxfire Style§r): Fires a supercharged foxfire-imbued crescent slash.",
                    (Object) null
            )}
    );

    private static final float COOLDOWN        = 200.0F;
    private static final float FOXFIRE_COOLDOWN = 200.0F;
    private static final float DAMAGE          = 15.0F;
    private static final int   ANIMATION_TICKS = 7;

    public static final AbilityCore<YakkodoriRework> INSTANCE;

    // =========================================================
    // COMPONENTS
    // =========================================================

    private final AnimationComponent animationComponent = new AnimationComponent(this);

    // No AltModeComponent — foxfire state is controlled exclusively by FoxfireStyleAbility
    private boolean isFoxfireMode = false;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public YakkodoriRework(AbilityCore<YakkodoriRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.animationComponent
        });
        this.addCanUseCheck(AbilityHelper::canUseSwordsmanAbilities);
        this.addUseEvent(this::onUseEvent);
    }

    // =========================================================
    // FOXFIRE MODE — only FoxfireStyleAbility may call these
    // =========================================================

    public void switchFoxfireMode(LivingEntity entity) {
        this.isFoxfireMode = true;
    }

    public void switchNormalMode(LivingEntity entity) {
        this.isFoxfireMode = false;
    }

    // =========================================================
    // USE EVENT
    // =========================================================

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        ItemStack stack = entity.getMainHandItem();
        stack.hurtAndBreak(1, entity, (user) -> user.broadcastBreakEvent(EquipmentSlotType.MAINHAND));
        this.animationComponent.start(entity, ModAnimations.UPPER_SLASH, ANIMATION_TICKS);

        if (this.isFoxfireMode) {
            ReworkedYakkodoriProjectile proj = new ReworkedYakkodoriProjectile(entity.level, entity);
            entity.level.addFreshEntity(proj);
            proj.shootFromRotation(entity, entity.xRot, entity.yRot, 0.0F, 3.6F, 1.0F);
            // Notify FoxfireStyleAbility that a foxfire ability was used
            notifyFoxfireStyle(entity);
        } else {
            YakkodoriReworkProjectile proj = new YakkodoriReworkProjectile(entity.level, entity);
            entity.level.addFreshEntity(proj);
            proj.shootFromRotation(entity, entity.xRot, entity.yRot, 0.0F, 2.0F, 1.0F);
        }

        if (!entity.level.isClientSide) {
            ((ServerWorld) entity.level).getChunkSource()
                    .broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
        }

        this.cooldownComponent.startCooldown(entity, this.isFoxfireMode ? FOXFIRE_COOLDOWN : COOLDOWN);
        this.animationComponent.stop(entity);
    }

    // =========================================================
    // NOTIFY FOXFIRE STYLE
    // =========================================================

    private static void notifyFoxfireStyle(LivingEntity entity) {
        FoxfireStyleAbility foxfire = FoxfireStyleAbility.getEquippedAbility(
                entity, FoxfireStyleAbility.INSTANCE, FoxfireStyleAbility.class);
        if (foxfire != null) {
            foxfire.setAbilityUsed(true);
        }
    }

    // =========================================================
    // UNLOCK CHECK
    // =========================================================

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) return false;
        PlayerEntity player = (PlayerEntity) entity;
        IEntityStats props = EntityStatsCapability.get(player);
        IQuestData questProps = QuestDataCapability.get(player);
        return props.isSwordsman() && questProps.hasFinishedQuest(ModQuests.SWORDSMAN_TRIAL_02);
    }

    // =========================================================
    // STATIC INIT
    // =========================================================

    static {
        INSTANCE = (new AbilityCore.Builder<>("Yakkodori", AbilityCategory.STYLE, YakkodoriRework::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN),
                        DealDamageComponent.getTooltip(DAMAGE)
                })
                .addDescriptionLine(FOXFIRE_DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(FOXFIRE_COOLDOWN),
                        DealDamageComponent.getTooltip(DAMAGE)
                })
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .setSourceType(new SourceType[]{SourceType.SLASH})
                .setSourceElement(SourceElement.FIRE)
                .setUnlockCheck(YakkodoriRework::canUnlock)
                .build();
    }
}