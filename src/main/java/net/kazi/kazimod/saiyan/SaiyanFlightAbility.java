package net.kazi.kazimod.saiyan;

import net.MrMagicalCart.cartaddon.abilities.generic.FlyAltAbility;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.packets.server.SFlightValuePacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

/** Fuwa's passive controls and animation for Saiyan flight. */
public final class SaiyanFlightAbility extends FlyAltAbility {
    private static final String BASE_SPEED="KiFlightBaseSpeed";
    public static final AbilityCore<SaiyanFlightAbility> INSTANCE=new AbilityCore.Builder<>(
            "Ki Flight",AbilityCategory.RACIAL,AbilityType.PASSIVE,SaiyanFlightAbility::new)
            .setUnlockCheck(SaiyanState::hasFruit)
            .setIcon(SaiyanAbility.icon("ki_flight"))
            .addDescriptionLine(new StringTextComponent("Passive ki flight. Double-tap jump to fly, then use jump and sneak to change altitude. Transformations increase flight speed, and combat does not slow it down."))
            .build();

    public SaiyanFlightAbility(AbilityCore<SaiyanFlightAbility> core){
        super(core);
        setDisplayName(new StringTextComponent("Ki Flight"));
        setDisplayIcon(SaiyanAbility.icon("ki_flight"));
        addCanUseCheck((user,ability)->user.isAlive()&&SaiyanState.hasFruit(user)
                &&!SaiyanFeatures.isActionLocked(user)&&!AbilityHelper.isWeakenedByKairosekiOrWater(user)&&!user.hasEffect(ModEffects.ABILITY_OFF.get())
                ?AbilityUseResult.success():AbilityUseResult.fail(null));
        addRemoveEvent((user,ability)->{
            if(user instanceof PlayerEntity&&!user.level.isClientSide){
                restoreFlightSpeed((PlayerEntity)user);
                updateAura((PlayerEntity)user,false);
                ((PlayerEntity)user).onUpdateAbilities();
                WyNetwork.sendTo(new SFlightValuePacket(false),(PlayerEntity)user);
            }
        });
    }

    @Override public void tick(LivingEntity user){
        super.tick(user);
        if(!(user instanceof PlayerEntity)||user.level.isClientSide)return;
        PlayerEntity player=(PlayerEntity)user;
        boolean flying=!player.isCreative()&&!player.isSpectator()&&player.abilities.flying&&!player.isOnGround()
                &&!isPaused()&&canUse(player).isSuccess();
        boolean creativeFlight=player.isCreative()&&!player.isSpectator()&&player.abilities.flying&&!player.isOnGround()&&!SaiyanFeatures.isActionLocked(player)
                &&player.isAlive()&&SaiyanState.hasFruit(player)&&!AbilityHelper.isWeakenedByKairosekiOrWater(player);
        updateAura(player,flying||creativeFlight);
        if(flying){
            CompoundNBT data=SaiyanState.get(player);
            if(!data.contains(BASE_SPEED))data.putFloat(BASE_SPEED,player.abilities.getFlyingSpeed());
            setFlightSpeed(player,data.getFloat(BASE_SPEED)*SaiyanBalance.flightSpeedMultiplier(SaiyanState.form(player)));
            player.fallDistance=0;
        }else restoreFlightSpeed(player);
    }

    @Override public int getHeightDifference(PlayerEntity player){return 35;}
    @Override public float getSpeedMultiplier(PlayerEntity player){return .91F;}

    private static void setFlightSpeed(PlayerEntity player,float speed){
        if(Math.abs(player.abilities.getFlyingSpeed()-speed)<.00001F)return;
        player.abilities.setFlyingSpeed(speed);player.onUpdateAbilities();
    }
    private static void restoreFlightSpeed(PlayerEntity player){
        CompoundNBT data=SaiyanState.get(player);
        if(!data.contains(BASE_SPEED))return;
        setFlightSpeed(player,data.getFloat(BASE_SPEED));data.remove(BASE_SPEED);
    }

    private static void updateAura(PlayerEntity player,boolean flying){
        CompoundNBT data=SaiyanState.get(player);boolean changed=data.getBoolean("Flight")!=flying;
        data.putBoolean("Flight",flying);
        Entity current=player.level.getEntity(data.getInt("FlightAuraEntity"));
        SaiyanEffect aura=current instanceof SaiyanEffect&&current.isAlive()&&((SaiyanEffect)current).owner()==player
                &&((SaiyanEffect)current).kind()==SaiyanEffect.FLIGHT_AURA?(SaiyanEffect)current:null;
        // Persistent auras now render from the player's interpolated position on the client.
        if(aura!=null)aura.remove();
        data.remove("FlightAuraEntity");
        if(changed)SaiyanFeatures.sync(player);
    }

    static void stopFlight(PlayerEntity player){
        restoreFlightSpeed(player);
        updateAura(player,false);
        if(!player.isCreative()&&!player.isSpectator()&&(player.abilities.mayfly||player.abilities.flying)){
            player.abilities.flying=false;player.abilities.mayfly=false;
            player.onUpdateAbilities();
            if(!player.level.isClientSide)WyNetwork.sendTo(new SFlightValuePacket(false),player);
        }
    }
}
