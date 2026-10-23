package net.kazi.kazimod.doku;

import net.kazi.kazimod.abilities.DokuRework.NewVenomRoadRework;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.vector.Vector3d;
import xyz.pixelatedw.mineminenomi.abilities.PropelledFlightAbility;
import xyz.pixelatedw.mineminenomi.api.IPlayerAbilities;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

/** Uses Seiryu's acceleration, steering and friction integration. */
public final class VenomRoadFlightAbility extends PropelledFlightAbility {
    public static final AbilityCore<VenomRoadFlightAbility> INSTANCE=new AbilityCore.Builder<>("Venom Road Flight",AbilityCategory.DEVIL_FRUITS,AbilityType.PASSIVE,VenomRoadFlightAbility::new).build();
    private boolean ownsFlight,previousMayfly,previousFlying,previousCustom;
    public VenomRoadFlightAbility(AbilityCore<VenomRoadFlightAbility> core){super(core);addCanUseCheck((u,a)->allowed(u)?AbilityUseResult.success():AbilityUseResult.fail(null));}
    public static boolean allowed(LivingEntity user){return user.isAlive()&&NewVenomRoadRework.active(user)&&AbilityHelper.canUseMomentumAbilities(user)&&!AbilityHelper.isWeakenedByKairosekiOrWater(user)&&!user.hasEffect(ModEffects.ABILITY_OFF.get());}
    @Override public void tick(LivingEntity user){
        if(!(user instanceof PlayerEntity))return;
        PlayerEntity player=(PlayerEntity)user;
        if(!allowed(user)||isPaused()){release(player);return;}
        if(!ownsFlight){previousMayfly=player.abilities.mayfly;previousFlying=player.abilities.flying;previousCustom=((IPlayerAbilities)player.abilities).hasCustomFlight();ownsFlight=true;}
        stamina=100;isRecovering=false;hasLanded=false;
        boolean changed=!player.abilities.flying||!player.abilities.mayfly;
        player.abilities.flying=true;player.abilities.mayfly=true;
        if(!AbilityHelper.isInCreativeOrSpectator(player))((IPlayerAbilities)player.abilities).setCustomFlight(true);
        if(changed&&!player.level.isClientSide)player.onUpdateAbilities();
        boolean grounded=player.isOnGround();
        super.tick(user);
        // Grounded starts need lift before the vanilla collision flags clear.
        if(grounded||player.isOnGround()){Vector3d motion=player.getDeltaMovement();AbilityHelper.setDeltaMovement(player,motion.x,Math.max(.35,motion.y),motion.z);}
        player.fallDistance=0;
    }
    public void release(PlayerEntity player){
        if(!ownsFlight)return;
        if(!AbilityHelper.isInCreativeOrSpectator(player)){player.abilities.mayfly=previousMayfly;player.abilities.flying=previousFlying;}
        ((IPlayerAbilities)player.abilities).setCustomFlight(previousCustom);ownsFlight=false;speed=0;
        ((net.kazi.kazimod.mixin.VenomRoadFlightAccessor)(Object)this).kazi$resetRoadMovement(Vector3d.ZERO);
        if(!player.level.isClientSide)player.onUpdateAbilities();
    }
    @Override public void onRemove(LivingEntity user){if(user instanceof PlayerEntity)release((PlayerEntity)user);}
    @Override public float getMaxSpeed(LivingEntity user){return user.isSprinting()?1.05F:.7F;}
    @Override protected float getAcceleration(LivingEntity user){return user.isSprinting()?.0085F:.0035F;}
    @Override protected int getHeightDifference(LivingEntity user){return 75;}
    @Override public boolean canRenderGauge(PlayerEntity user){return false;}
    @Override public CompoundNBT save(CompoundNBT nbt){super.save(nbt);nbt.putBoolean("RoadOwnsFlight",ownsFlight);nbt.putBoolean("RoadMayfly",previousMayfly);nbt.putBoolean("RoadFlying",previousFlying);nbt.putBoolean("RoadCustom",previousCustom);return nbt;}
    @Override public void load(CompoundNBT nbt){super.load(nbt);ownsFlight=nbt.getBoolean("RoadOwnsFlight");previousMayfly=nbt.getBoolean("RoadMayfly");previousFlying=nbt.getBoolean("RoadFlying");previousCustom=nbt.getBoolean("RoadCustom");}
}
