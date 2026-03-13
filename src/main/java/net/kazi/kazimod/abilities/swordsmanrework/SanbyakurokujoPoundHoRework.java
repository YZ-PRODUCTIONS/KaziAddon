package net.kazi.kazimod.abilities.swordsmanrework;

import net.MrMagicalCart.cartaddon.entities.projectiles.swordsmenextra.ReworkedSanbyakurokujuPoundHoProjectile;
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
import xyz.pixelatedw.mineminenomi.api.abilities.components.ProjectileComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.data.entity.quests.IQuestData;
import xyz.pixelatedw.mineminenomi.data.entity.quests.QuestDataCapability;
import xyz.pixelatedw.mineminenomi.entities.projectiles.swordsman.SanbyakurokujuPoundHoProjectile;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModQuests;

public class SanbyakurokujoPoundHoRework extends Ability {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "sanbyakurokuju_pound_ho",
            new Pair[]{ImmutablePair.of(
                    "The user launches a powerful ranged slash, causing great destruction",
                    (Object) null
            )}
    );

    private static final ITextComponent[] FOXFIRE_DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod", "sanbyakurokuju_pound_ho_foxfire",
            new Pair[]{ImmutablePair.of(
                    "§6Foxfire Mode§r (requires §6Foxfire Style§r): Fires a superheated foxfire-imbued pound cannon at twice the speed.",
                    (Object) null
            )}
    );

    private static final float COOLDOWN         = 320.0F;
    private static final float FOXFIRE_COOLDOWN  = 320.0F;
    private static final float DAMAGE           = 30.0F;
    private static final int   ANIMATION_TICKS  = 7;

    private static final float NORMAL_SPEED     = 3.0F;
    private static final float FOXFIRE_SPEED    = 6.0F;

    public static final AbilityCore<SanbyakurokujoPoundHoRework> INSTANCE;

    // =========================================================
    // COMPONENTS
    // =========================================================

    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);

    // No AltModeComponent — foxfire state is controlled exclusively by FoxfireStyleAbility
    private boolean isFoxfireMode = false;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public SanbyakurokujoPoundHoRework(AbilityCore<SanbyakurokujoPoundHoRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{
                this.animationComponent,
                this.projectileComponent
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
        this.animationComponent.start(entity, ModAnimations.CANNON_SLASH, ANIMATION_TICKS);

        if (this.isFoxfireMode) {
            ReworkedSanbyakurokujuPoundHoProjectile proj = new ReworkedSanbyakurokujuPoundHoProjectile(entity.level, entity, this);
            entity.level.addFreshEntity(proj);
            proj.shootFromRotation(entity, entity.xRot, entity.yRot, 0.0F, FOXFIRE_SPEED, 1.0F);
            // Notify FoxfireStyleAbility that a foxfire ability was used
            notifyFoxfireStyle(entity);
        } else {
            this.projectileComponent.shoot(entity);
        }

        if (!entity.level.isClientSide) {
            ((ServerWorld) entity.level).getChunkSource()
                    .broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
        }

        this.cooldownComponent.startCooldown(entity, this.isFoxfireMode ? FOXFIRE_COOLDOWN : COOLDOWN);
    }

    // =========================================================
    // PROJECTILE FACTORY (normal mode only)
    // =========================================================

    private SanbyakurokujuPoundHoProjectile createProjectile(LivingEntity entity) {
        SanbyakurokujuPoundHoProjectile proj = new SanbyakurokujuPoundHoProjectile(entity.level, entity, this);
        proj.setDamage(DAMAGE);
        return proj;
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
        return props.isSwordsman() && questProps.hasFinishedQuest(ModQuests.SWORDSMAN_TRIAL_03);
    }

    // =========================================================
    // STATIC INIT
    // =========================================================

    static {
        INSTANCE = (new AbilityCore.Builder<>("Sanbyakurokuju Pound Ho", AbilityCategory.STYLE, SanbyakurokujoPoundHoRework::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN)
                })
                .addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips())
                .addDescriptionLine(FOXFIRE_DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(FOXFIRE_COOLDOWN)
                })
                .setSourceHakiNature(SourceHakiNature.IMBUING)
                .setSourceType(new SourceType[]{SourceType.SLASH})
                .setSourceElement(SourceElement.FIRE)
                .setUnlockCheck(SanbyakurokujoPoundHoRework::canUnlock)
                .build();
    }
}