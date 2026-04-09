package net.kazi.kazimod.abilities.SaberRework;

import net.MrMagicalCart.cartaddon.init.CartValues;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SAnimateHandPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
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
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.IEntityStats;
import xyz.pixelatedw.mineminenomi.entities.projectiles.swordsman.SanbyakurokujuPoundHoProjectile;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;

public class HeavenSlashAbility extends Ability {

    private static final ResourceLocation ICON =
            new ResourceLocation("kazimod", "textures/abilities/heaven_slash_red.png");

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "kazimod",
            "heaven_slash",
            new Pair[]{ImmutablePair.of("Launches a larger crimson pound slash straight ahead.", null)}
    );

    private static final float COOLDOWN = 300.0F;
    private static final float DAMAGE = 30.0F;
    private static final float SPEED = 2.55F;
    private static final int ANIMATION_TICKS = 7;

    public static final AbilityCore<HeavenSlashAbility> INSTANCE =
            new AbilityCore.Builder<>("Heaven Slash", AbilityCategory.STYLE, HeavenSlashAbility::new)
                    .addDescriptionLine(DESCRIPTION)
                    .addAdvancedDescriptionLine(
                            AbilityDescriptionLine.NEW_LINE,
                            CooldownComponent.getTooltip(COOLDOWN)
                    )
                    .addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips())
                    .setIcon(ICON)
                    .setSourceHakiNature(SourceHakiNature.IMBUING)
                    .setSourceType(new SourceType[]{SourceType.SLASH})
                    .setUnlockCheck(HeavenSlashAbility::canUnlock)
                    .build();

    private final AnimationComponent animationComponent = new AnimationComponent(this);
    private final ProjectileComponent projectileComponent = new ProjectileComponent(this, this::createProjectile);

    public HeavenSlashAbility(AbilityCore<HeavenSlashAbility> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.animationComponent, this.projectileComponent});
        this.addCanUseCheck(AbilityHelper::canUseSwordsmanAbilities);
        this.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        ItemStack stack = entity.getMainHandItem();
        stack.hurtAndBreak(1, entity, user -> user.broadcastBreakEvent(EquipmentSlotType.MAINHAND));

        this.animationComponent.start(entity, ModAnimations.CANNON_SLASH, ANIMATION_TICKS);
        this.projectileComponent.shoot(entity, SPEED, 0.0F);

        if (entity.level instanceof ServerWorld) {
            ServerWorld world = (ServerWorld) entity.level;
            world.getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));
        }

        entity.level.playSound((PlayerEntity) null, entity.blockPosition(), net.minecraft.util.SoundEvents.BLAZE_SHOOT, SoundCategory.PLAYERS, 1.6F, 0.8F);
        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    private SanbyakurokujuPoundHoProjectile createProjectile(LivingEntity entity) {
        SanbyakurokujuPoundHoProjectile projectile = new SanbyakurokujuPoundHoProjectile(entity.level, entity, this);
        projectile.setDamage(DAMAGE);
        projectile.setEntityCollisionSize(4.2D, 1.7D, 4.2D);
        projectile.setBlockCollisionSize(3.5D, 1.4D, 3.5D);
        return projectile;
    }

    private static boolean canUnlock(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        }
        IEntityStats stats = EntityStatsCapability.get(entity);
        return stats != null && CartValues.SABER.equals(stats.getFightingStyle());
    }
}
