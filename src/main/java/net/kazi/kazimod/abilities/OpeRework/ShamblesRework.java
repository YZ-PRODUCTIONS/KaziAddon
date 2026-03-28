//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.abilities.OpeRework;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;

import net.MrMagicalCart.cartaddon.abilities.opeextra.ReworkedOpeHelper;
import net.MrMagicalCart.cartaddon.abilities.opeextra.ReworkedRoomAbility;
import net.MrMagicalCart.cartaddon.init.CartEffects;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.SPlayerPositionLookPacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
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
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.protection.ProtectedArea;
import xyz.pixelatedw.mineminenomi.api.protection.DefaultProtectionRules;
import xyz.pixelatedw.mineminenomi.api.protection.block.RestrictedBlockProtectionRule;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.world.ProtectedAreasData;
import xyz.pixelatedw.mineminenomi.entities.SphereEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;
import xyz.pixelatedw.mineminenomi.init.ModEntityPredicates;
import xyz.pixelatedw.mineminenomi.init.ModSounds;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class ShamblesRework extends Ability {
    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText("mineminenomi", "shambles", new Pair[]{ImmutablePair.of("The user swaps place with the closest entity or block within the ROOM. Alt mode allows it to switch multiple entities within the ROOM.", (Object)null)});
    private static final ResourceLocation SHAMBLES_SINGLE_ICON = new ResourceLocation("mineminenomi", "textures/abilities/shambles.png");
    private static final ResourceLocation SHAMBLES_GROUP_ICON = new ResourceLocation("mineminenomi", "textures/abilities/shambles_group.png");
    private static final float COOLDOWN = 80.0F;
    private static final float GROUP_COOLDOWN = 120.0F;
    public static final AbilityCore<ShamblesRework> INSTANCE;
    private final AltModeComponent<Mode> altModeComponent;
    private static final Predicate<Entity> SHAMBLES_LIST;

    public ShamblesRework(AbilityCore<ShamblesRework> core) {
        super(core);
        this.altModeComponent = (new AltModeComponent(this, Mode.class, ShamblesRework.Mode.SINGLE)).addChangeModeEvent(this::onAltModeChange);
        super.isNew = true;
        super.addComponents(new AbilityComponent[]{this.altModeComponent});
        super.addCanUseCheck(AbilityHelper::canUseMomentumAbilities);
        super.addCanUseCheck(ReworkedOpeHelper::hasRoomActive);
        super.addUseEvent(this::onUseEvent);
    }

    private void onUseEvent(LivingEntity entity, IAbility ability) {
        ReworkedRoomAbility reworkedRoomAbility = (ReworkedRoomAbility)AbilityDataCapability.get(entity).getEquippedAbility(ReworkedRoomAbility.INSTANCE);
        boolean hadTarget = false;
        if (this.altModeComponent.getCurrentMode() == ShamblesRework.Mode.SINGLE) {
            RayTraceResult mop = WyHelper.rayTraceBlocksAndEntities(entity, (double)reworkedRoomAbility.getROOMSize());
            if (mop instanceof EntityRayTraceResult) {
                EntityRayTraceResult entityRayTraceResult = (EntityRayTraceResult)mop;
                Entity target = entityRayTraceResult.getEntity();
                if (!reworkedRoomAbility.isEntityInRoom(target)) {
                    return;
                }

                if (!SHAMBLES_LIST.test(target)) {
                    return;
                }

                float[] beforeCoords = new float[]{(float)entity.getX(), (float)entity.getY(), (float)entity.getZ(), entity.yRot, entity.xRot};
                entity.moveTo(target.getX(), target.getY(), target.getZ(), target.yRot, target.xRot);
                entity.moveTo(target.getX(), target.getY(), target.getZ());
                target.moveTo((double)beforeCoords[0], (double)beforeCoords[1], (double)beforeCoords[2], beforeCoords[3], beforeCoords[4]);
                entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.TELEPORT_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.0F);
                entity.level.playSound((PlayerEntity)null, target.blockPosition(), (SoundEvent)ModSounds.TELEPORT_SFX.get(), SoundCategory.PLAYERS, 2.0F, 1.0F);
                if (!(target instanceof PlayerEntity) && target instanceof LivingEntity) {
                    ((LivingEntity)target).addEffect(new EffectInstance((Effect)CartEffects.DISABLED_ABILITIES.get(), 20, 0));
                }

                hadTarget = true;
            } else if (mop instanceof BlockRayTraceResult) {
                BlockRayTraceResult result = (BlockRayTraceResult)mop;
                BlockPos pos = result.getBlockPos();
                BlockState state = entity.level.getBlockState(pos);
                BlockPos entityPos = entity.blockPosition();
                BlockState entityPosState = entity.level.getBlockState(entityPos);
                boolean isInsideRoom = reworkedRoomAbility.isPositionInRoom(pos);
                boolean isDestinationBanned = RestrictedBlockProtectionRule.INSTANCE.isBanned(state);
                boolean isOriginBanned = RestrictedBlockProtectionRule.INSTANCE.isBanned(entityPosState);
                if (isInsideRoom && !isDestinationBanned && !isOriginBanned) {
                    BlockPos beforePos = entity.blockPosition();
                    ProtectedAreasData protectedAreaData = ProtectedAreasData.get(entity.level);
                    boolean a1 = protectedAreaData.isInsideRestrictedArea(beforePos.getX(), beforePos.getY(), beforePos.getZ());
                    boolean a2 = protectedAreaData.isInsideRestrictedArea(pos.getX(), pos.getY(), pos.getZ());
                    if (a1 != a2) {
                        return;
                    }

                    boolean destructionDisabled = !CommonConfig.INSTANCE.isAbilityGriefingEnabled();
                    if (!destructionDisabled) {
                        ProtectedArea area = protectedAreaData.getProtectedArea(pos.getX(), pos.getY(), pos.getZ());
                        destructionDisabled = area != null && !area.canDestroyBlocks();
                    }

                    if (destructionDisabled) {
                        this.teleportEntity(entity, (double)pos.getX() + 0.5D, (double)(pos.getY() + 1), (double)pos.getZ() + 0.5D);
                        entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.TELEPORT_SFX.get(), SoundCategory.PLAYERS, 0.5F, 1.0F);
                        hadTarget = true;
                    } else {
                        this.teleportEntity(entity, (double)pos.getX(), (double)(pos.getY() + 1), (double)pos.getZ());
                        boolean b1 = AbilityHelper.placeBlockIfAllowed(entity, beforePos, state, 3, DefaultProtectionRules.AIR_CORE_FOLIAGE_ORE);
                        boolean b2 = AbilityHelper.placeBlockIfAllowed(entity, pos, Blocks.AIR.defaultBlockState(), 3, DefaultProtectionRules.AIR_CORE_FOLIAGE_ORE);
                        if (b1 && b2) {
                            entity.level.playSound((PlayerEntity)null, entity.blockPosition(), (SoundEvent)ModSounds.TELEPORT_SFX.get(), SoundCategory.PLAYERS, 0.5F, 1.0F);
                            hadTarget = true;
                        }
                    }
                }
            }
        } else if (this.altModeComponent.getCurrentMode() == ShamblesRework.Mode.GROUP) {
            BlockPos centerPos = reworkedRoomAbility.getCenterBlock();
            Vector3d centerVec = new Vector3d((double)centerPos.getX(), (double)centerPos.getY(), (double)centerPos.getZ());
            Predicate<Entity> groupCheck = ModEntityPredicates.getEnemyFactions(entity).and(SHAMBLES_LIST);
            List<Entity> targets = WyHelper.getNearbyEntities(centerVec, entity.level, (double)reworkedRoomAbility.getROOMSize(), groupCheck, new Class[]{Entity.class});
            Collections.shuffle(targets);

            for(int i = 0; i < targets.size() && i < targets.size() && i + 1 < targets.size(); i += 2) {
                Entity target1 = (Entity)targets.get(i);
                Entity target2 = (Entity)targets.get(i + 1);
                if (reworkedRoomAbility.isPositionInRoom(target1.blockPosition()) && reworkedRoomAbility.isPositionInRoom(target2.blockPosition())) {
                    float[] beforeCoords = new float[]{(float)target2.getX(), (float)target2.getY(), (float)target2.getZ(), target2.yRot, target2.xRot};
                    target2.moveTo(target1.getX(), target1.getY(), target1.getZ(), target1.yRot, target1.xRot);
                    target2.moveTo(target1.getX(), target1.getY(), target1.getZ());
                    target1.moveTo((double)beforeCoords[0], (double)beforeCoords[1], (double)beforeCoords[2], beforeCoords[3], beforeCoords[4]);
                    entity.level.playSound((PlayerEntity)null, target2.blockPosition(), (SoundEvent)ModSounds.TELEPORT_SFX.get(), SoundCategory.PLAYERS, 0.5F, 1.0F);
                    entity.level.playSound((PlayerEntity)null, target1.blockPosition(), (SoundEvent)ModSounds.TELEPORT_SFX.get(), SoundCategory.PLAYERS, 0.5F, 1.0F);
                }
            }

            if (targets.size() >= 2) {
                hadTarget = true;
            }
        }

        float percentHp = entity.getHealth() * 100.0F / entity.getMaxHealth();
        if (hadTarget) {
            if (this.altModeComponent.getCurrentMode() == ShamblesRework.Mode.GROUP) {
                super.cooldownComponent.startCooldown(entity, GROUP_COOLDOWN);
            } else {
                super.cooldownComponent.startCooldown(entity, 80.0F + 2.0F * (100.0F - percentHp));
            }
        }

    }

    private void teleportEntity(LivingEntity entity, double x, double y, double z) {
        entity.moveTo(x, y, z, entity.yRot, entity.xRot);
        entity.moveTo(x, y, z);
        if (entity instanceof ServerPlayerEntity) {
            ((ServerPlayerEntity) entity).connection.teleport(
                    x,
                    y,
                    z,
                    entity.yRot,
                    entity.xRot,
                    EnumSet.noneOf(SPlayerPositionLookPacket.Flags.class)
            );
        }
    }

    private void onAltModeChange(LivingEntity entity, IAbility ability, Enum<?> mode) {
        if (mode == ShamblesRework.Mode.SINGLE) {
            super.setDisplayIcon(SHAMBLES_SINGLE_ICON);
        } else if (mode == ShamblesRework.Mode.GROUP) {
            super.setDisplayIcon(SHAMBLES_GROUP_ICON);
        }

    }

    static {
        INSTANCE = (new AbilityCore.Builder("Shambles", AbilityCategory.DEVIL_FRUITS, ShamblesRework::new)).addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{AbilityDescriptionLine.NEW_LINE, CooldownComponent.getTooltip(80.0F, 280.0F)}).build();
        SHAMBLES_LIST = (target) -> {
            if (target instanceof LightningEntity) {
                return false;
            } else if (target instanceof SphereEntity) {
                return false;
            } else {
                return !(target instanceof AbilityProjectileEntity) || ((AbilityProjectileEntity)target).isPhysical();
            }
        };
    }

    public static enum Mode {
        SINGLE,
        GROUP;

        private Mode() {
        }
    }
}
