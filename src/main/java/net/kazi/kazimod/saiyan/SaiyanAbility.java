package net.kazi.kazimod.saiyan;

import java.awt.Color;
import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.entities.projectiles.goro.LightningEntity;
import xyz.pixelatedw.mineminenomi.data.world.ProtectedAreasData;
import xyz.pixelatedw.mineminenomi.api.protection.ProtectedArea;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

public final class SaiyanAbility extends Ability {
    public enum Move {
        CHARGE("Ki Charge","ki_charge",0,0,20,"Gather ki in a rising aura while stunned and unable to use other abilities. Base form only; finish other active abilities first. Taking a hit interrupts charging and triggers a 30-second cooldown. Use again to stop. Restores 6% of maximum ki per second."),
        // Keep the old core registered so existing player saves can be migrated.
        BLAST("Ki Blast","ki_blast",0,8,12,"Fire a fast golden ki blast. Alternate mode charges a larger blast for 30 ki."),
        METEOR("Meteor Combination","meteor_combination",0,20,160,"Lunge forward, grab the enemy, and slam them into the ground. While Ultra Instinct is active, this automatically becomes Celestial Meteor: carry them into space, pause above Earth for one second, then dive back down. Use again to release early."),
        KAMEHAMEHA("Kamehameha","kamehameha",0,18,240,"Cup a blue ki sphere, then unleash a steerable continuous beam that hits once per second. Charges for 2 seconds; drains 12 ki per second while firing. Use again to cancel."),
        TRANSMISSION("Instant Transmission","instant_transmission",0,18,160,"Blink behind the target in your sights, or to a safe point along your aim. Range increases with your active form: 24 / 32 / 40 / 48 / 64 / 80 / 96 blocks in Base / SS1 / SS2 / SS3 / God / Blue / Ultra Instinct."),
        TRANSFORM("Saiyan Transformation","super_saiyan",1,30,40,"Transform into your selected awakening. Alternate mode cycles unlocked forms. Use again to return to base; stronger forms drain ki faster. Ultra Instinct has a six-second entrance that stuns you and prevents all incoming damage. Once transformed, it automatically dodges avoidable attacks below 30 damage and teleports behind the attacker when space allows."),
        DRAGON("Dragon Fist","dragon_fist",3,65,500,"Rush forward behind a translucent energy dragon. While Ultra Instinct is active, this automatically becomes Dragon Ascension: a five-times-larger dragon bites an enemy, carries them 200 blocks upward, and slams them back down. Requires Super Saiyan 3 awakening."),
        SPIRIT("Spirit Bomb","spirit_bomb",4,160,SaiyanBalance.SPIRIT_COOLDOWN,"Raise both hands to gather a giant blue-white sphere for 8 seconds, then throw it toward your aim. Requires Super Saiyan God awakening. Use again to cancel before release.");
        final String title,icon,description;final int tier,cost,cooldown;
        Move(String title,String icon,int tier,int cost,int cooldown,String description){this.title=title;this.icon=icon;this.tier=tier;this.cost=cost;this.cooldown=cooldown;this.description=description;}
    }
    public enum Mode { NORMAL, ALTERNATE }
    public enum Form { SUPER_SAIYAN, SUPER_SAIYAN_2, SUPER_SAIYAN_3, SUPER_SAIYAN_GOD, SUPER_SAIYAN_BLUE, ULTRA_INSTINCT }
    public static final AbilityCore<SaiyanAbility>[] CORES=createCores();
    @SuppressWarnings("unchecked") private static AbilityCore<SaiyanAbility>[] createCores(){
        AbilityCore<SaiyanAbility>[] result=new AbilityCore[Move.values().length];
        for(Move move:Move.values())result[move.ordinal()]=new AbilityCore.Builder<SaiyanAbility>(move.title,AbilityCategory.RACIAL,core->new SaiyanAbility(core,move))
                .setUnlockCheck(user->move!=Move.BLAST&&SaiyanState.hasFruit(user)&&SaiyanState.tier(user)>=move.tier)
                .setSourceHakiNature(SourceHakiNature.IMBUING).setSourceType(move==Move.METEOR||move==Move.DRAGON?SourceType.FIST:SourceType.PROJECTILE)
                .setSourceElement(SourceElement.LIGHT).setIcon(icon(move.icon))
                .addDescriptionLine(new StringTextComponent(move.description))
                .addDescriptionLine(new StringTextComponent("Ki cost: "+move.cost+". Cooldown: "+(move.cooldown/20F)+" seconds"+(move==Move.DRAGON?" (15 seconds in Ultra Instinct)":"")+"."))
                .build();
        return result;
    }
    public static AbilityCore<SaiyanAbility> core(Move move){return CORES[move.ordinal()];}
    public static boolean isSaiyan(AbilityCore<?> core){return core==SaiyanFlightAbility.INSTANCE||Arrays.asList(CORES).contains(core);}
    public static ResourceLocation icon(String name){return new ResourceLocation("kazimod","textures/abilities/saiyan/"+name+".png");}
    private final Move move;
    private final ContinuousComponent continuous=new ContinuousComponent(this,true);
    private final DealDamageComponent damage=new DealDamageComponent(this);
    private final GrabEntityComponent grab=new GrabEntityComponent(this,false,false,false,1);
    private final AltModeComponent<Mode> mode=new AltModeComponent<Mode>(this,Mode.class,Mode.NORMAL){
        @Override public void load(CompoundNBT data){
            CompoundNBT saved=data.copy();String current=saved.getString("currentMode");
            if(!Mode.NORMAL.name().equals(current)&&!Mode.ALTERNATE.name().equals(current))saved.putString("currentMode",Mode.NORMAL.name());
            super.load(saved);
        }
    };
    private final AltModeComponent<Form> formMode=new AltModeComponent<Form>(this,Form.class,Form.SUPER_SAIYAN){
        @Override public void setMode(LivingEntity user,Enum form){
            super.setMode(user,form.ordinal()<SaiyanState.tier(user)?form:Form.SUPER_SAIYAN);
        }
    };
    private int ticks;
    private boolean chargeInterrupted;
    private SaiyanMeteor meteor;
    private SaiyanFinisher finisher;
    private LightningEntity beam;
    private SaiyanEffect effect;
    private final Set<UUID> hit=new HashSet<>();

    public SaiyanAbility(AbilityCore<SaiyanAbility> core,Move move){
        super(core);this.move=move;isNew=true;addComponents(continuous,damage);
        if(move==Move.CHARGE)addComponents(new DamageTakenComponent(this).addOnHurtEvent((user,ability,source,amount)->{
            if(!user.level.isClientSide&&amount>0&&continuous.isContinuous()){
                chargeInterrupted=true;
                continuous.stopContinuity(user);
            }
            return amount;
        }));
        if(move==Move.METEOR||move==Move.DRAGON)addComponents(grab);
        if(move==Move.BLAST){
            addComponents(mode);
            mode.addChangeModeEvent((user,ability,next)->setDisplayName(new StringTextComponent(next==Mode.NORMAL?move.title:"Charged Ki Blast")));
        }
        if(move==Move.METEOR||move==Move.DRAGON){
            addTickEvent((user,ability)->{
                String title=SaiyanState.form(user)==SaiyanBalance.ULTRA_INSTINCT
                        ?move==Move.DRAGON?"Ultra Instinct: Dragon Ascension":"Ultra Instinct: Celestial Meteor":move.title;
                if(!getDisplayName().getString().equals(title))setDisplayName(new StringTextComponent(title));
            });
        }
        if(move==Move.TRANSFORM){
            addComponents(formMode,new DamageTakenComponent(this).addOnAttackEvent((user,ability,source,amount)->
                    SaiyanFeatures.isInstinctTransforming(user)?0:continuous.isContinuous()?SaiyanInstinct.onAttack(user,source,amount):amount));
            formMode.addChangeModeEvent((user,ability,form)->{
                int selected=form.ordinal()+1;
                setDisplayName(new StringTextComponent(SaiyanBalance.FORMS[selected]));
                setDisplayIcon(icon(new String[]{"","super_saiyan","super_saiyan_2","super_saiyan_3","super_saiyan_god","super_saiyan_blue","ultra_instinct"}[selected]));
            });
        }
        addCanUseCheck((user,ability)->{
            if(move==Move.BLAST)return AbilityUseResult.fail(new StringTextComponent("Ki Blast has been removed."));
            if(!(user instanceof PlayerEntity)||!core.canUnlock(user))return AbilityUseResult.fail(new StringTextComponent("This Saiyan technique is not unlocked."));
            if(continuous.isContinuous() || move==Move.TRANSFORM&&SaiyanState.form(user)>0)return AbilityUseResult.success();
            if(SaiyanFeatures.isKiCharging(user)&&move!=Move.CHARGE)return AbilityUseResult.fail(new StringTextComponent("Stop Ki Charge before using another ability."));
            if(move==Move.CHARGE){
                if(SaiyanState.form(user)>0)return AbilityUseResult.fail(new StringTextComponent("Return to base form before using Ki Charge."));
                for(IAbility other:AbilityDataCapability.get(user).getEquippedAbilities()){
                    if(other==null||other==ability)continue;
                    if(other.getComponent(ModAbilityKeys.CONTINUOUS).map(ContinuousComponent::isContinuous).orElse(false)
                            ||other.getComponent(ModAbilityKeys.CHARGE).map(ChargeComponent::isCharging).orElse(false))
                        return AbilityUseResult.fail(new StringTextComponent("Finish your active ability before using Ki Charge."));
                }
            }
            if(AbilityHelper.isWeakenedByKairosekiOrWater(user))return AbilityUseResult.fail(new StringTextComponent("Your Devil Fruit is weakened."));
            if(SaiyanState.action(user)>=2)return AbilityUseResult.fail(new StringTextComponent("Finish your current technique first."));
            float cost=move==Move.BLAST&&mode.isMode(Mode.ALTERNATE)?30:move.cost;
            if(SaiyanState.ki(user)<cost)return AbilityUseResult.fail(new StringTextComponent("Not enough ki. Use Ki Charge."));
            return AbilityUseResult.success();
        });
        addUseEvent((user,ability)->useTechnique(user));
        continuous.addTickEvent((user,ability)->tickTechnique(user));
        continuous.addEndEvent((user,ability)->endTechnique(user));
        addRemoveEvent((user,ability)->{
            if(continuous.isContinuous())continuous.stopContinuity(user);
            if(!user.level.isClientSide&&move==Move.TRANSFORM)SaiyanFeatures.setForm(user,0);
        });
    }
    private void useTechnique(LivingEntity user){
        if(user.level.isClientSide)return;
        PlayerEntity player=(PlayerEntity)user;
        if(continuous.isContinuous()){continuous.stopContinuity(user);return;}
        if((SaiyanFeatures.isKiCharging(user)&&move!=Move.CHARGE)||(move==Move.CHARGE&&SaiyanState.form(user)>0))return;
        if(move==Move.TRANSFORM){
            if(SaiyanState.form(user)>0){SaiyanFeatures.setForm(user,0);cooldownComponent.startCooldown(user,40);return;}
            if(formMode.getCurrentMode().ordinal()+1>SaiyanState.tier(user))return;
            if(!SaiyanState.spend(user,move.cost))return;
            int selected=formMode.getCurrentMode().ordinal()+1;
            if(selected==SaiyanBalance.ULTRA_INSTINCT){
                SaiyanFlightAbility.stopFlight(player);
                if(player.abilities.flying){player.abilities.flying=false;player.onUpdateAbilities();}
                player.stopRiding();
                for(IAbility other:new ArrayList<IAbility>(AbilityDataCapability.get(user).getEquippedAbilities()))
                    if(other!=null&&other!=this)AbilityHelper.emergencyStopAbility(user,other);
            }
            SaiyanState.get(user).putBoolean("Charging",false);
            SaiyanState.pose(user,10,SaiyanBalance.transformationTicks(selected));
            SaiyanFeatures.setForm(user,selected);
            continuous.startContinuity(user);
            if(selected==SaiyanBalance.ULTRA_INSTINCT)SaiyanFeatures.updateTransformationLock(user);
            if(SaiyanState.form(user)!=SaiyanBalance.ULTRA_INSTINCT)SaiyanFeatures.play(user,SaiyanFeatures.TRANSFORM.get(),.7F,1);
            SaiyanFeatures.sync(player);
            return;
        }
        if(move==Move.TRANSMISSION){teleport(player);return;}
        float cost=move==Move.BLAST&&mode.isMode(Mode.ALTERNATE)?30:move.cost;
        if(!SaiyanState.spend(user,cost))return;
        if(move==Move.BLAST&&mode.isMode(Mode.NORMAL)){
            shoot(user,0,6,.32F,2.2F);SaiyanState.pose(user,5,8);cooldownComponent.startCooldown(user,12);return;
        }
        ticks=0;hit.clear();chargeInterrupted=false;continuous.startContinuity(user);
        if((move==Move.METEOR||move==Move.DRAGON)&&SaiyanState.form(user)==SaiyanBalance.ULTRA_INSTINCT){
            finisher=new SaiyanFinisher(user,grab,damage,move==Move.DRAGON);
            SaiyanFeatures.sync(player);return;
        }
        if(move==Move.METEOR)meteor=new SaiyanMeteor(user,grab,damage);
        if(move==Move.CHARGE){
            SaiyanFlightAbility.stopFlight(player);
            if(player.abilities.flying){player.abilities.flying=false;player.onUpdateAbilities();}
            player.stopUsingItem();SaiyanState.get(user).putBoolean("Charging",true);
            stunWhileCharging(user);SaiyanState.pose(user,1,40);
        }
        else {SaiyanState.get(user).putBoolean("Charging",false);SaiyanState.pose(user,action(),200);}
        if(move==Move.KAMEHAMEHA||move==Move.SPIRIT||move==Move.BLAST){
            effect=SaiyanEffect.spawn(user,move==Move.SPIRIT?2:1,user.position(),move==Move.SPIRIT?SaiyanBalance.SPIRIT_WINDUP+2:42,move==Move.SPIRIT?5:1);
        }
        if(move==Move.DRAGON)effect=SaiyanEffect.spawn(user,3,user.position(),26,2);
        SaiyanFeatures.sync(player);
    }
    private int action(){switch(move){case KAMEHAMEHA:return 2;case SPIRIT:return 4;case BLAST:return 9;case METEOR:return 6;case DRAGON:return 7;default:return 1;}}
    private void tickTechnique(LivingEntity user){
        if(user.level.isClientSide)return;
        if(!user.isAlive()||!SaiyanState.hasFruit(user)||AbilityHelper.isWeakenedByKairosekiOrWater(user)){continuous.stopContinuity(user);return;}
        if(finisher!=null){if(!finisher.tick())continuous.stopContinuity(user);return;}
        ticks++;
        switch(move){
            case TRANSFORM:
                if(SaiyanState.form(user)==0)continuous.stopContinuity(user);
                break;
            case CHARGE:
                if(!SaiyanState.get(user).getBoolean("Charging")||SaiyanState.form(user)>0){continuous.stopContinuity(user);break;}
                stunWhileCharging(user);
                SaiyanState.addKi(user,SaiyanState.maxKi(user)*.003F);SaiyanState.pose(user,1,8);
                if(SaiyanState.ki(user)>=SaiyanState.maxKi(user))continuous.stopContinuity(user);
                break;
            case BLAST:
                if(ticks>=30){shoot(user,1,22,1F,1.5F);continuous.stopContinuity(user);}break;
            case KAMEHAMEHA:
                if(ticks==40){
                    SaiyanState.pose(user,3,110);Vector3d origin=hand(user);
                    beam=new LightningEntity(user,origin.x,origin.y,origin.z,user.yRot,user.xRot,80,20,getCore());
                    beam.setSize(.65F+.08F*SaiyanState.form(user));beam.setBoxSizeDivision(1);
                    beam.setColor(new Color(SaiyanState.form(user)==0?0x46afff:SaiyanBalance.COLORS[SaiyanState.form(user)]));beam.setDamage(7*SaiyanState.power(user));
                    beam.setSegments(1);beam.setBranches(1);beam.setLightningMimic(false);beam.setLightningMovement(false);beam.setEnergyEffect(false);
                    beam.setTargetTimeToReset(Integer.MAX_VALUE);beam.setMaxLife(110);beam.setExplosion(0);beam.setAffectedByImbuing();
                    user.level.addFreshEntity(beam);
                }
                if(ticks>=40){
                    if(beam==null||!beam.isAlive()||!SaiyanState.spend(user,.6F)){continuous.stopContinuity(user);break;}
                    // Native hit resets only run at full beam length; steering and walls must not stop damage pulses.
                    if((ticks-40)%20==0){
                        beam.targets.clear();beam.explosionTargets.clear();beam.clearTargets();
                    }
                    Vector3d pos=hand(user);beam.setPos(pos.x,pos.y,pos.z);beam.yRot=user.yRot;beam.xRot=user.xRot;
                    beam.setDeltaMovement(Vector3d.ZERO);
                    float length=(float)pos.distanceTo(user.level.clip(new RayTraceContext(pos,pos.add(user.getLookAngle().scale(80)),RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,user)).getLocation());
                    beam.setLength(length);
                }
                if(ticks>=140)continuous.stopContinuity(user);break;
            case METEOR:
                if(meteor==null||!meteor.tick())continuous.stopContinuity(user);break;
            case DRAGON:
                AbilityHelper.setDeltaMovement(user,user.getLookAngle().scale(1.65));user.fallDistance=0;
                for(LivingEntity enemy:user.level.getEntitiesOfClass(LivingEntity.class,user.getBoundingBox().inflate(2)))
                    if(canHit(user,enemy)&&hit.add(enemy.getUUID())){damage.hurtTarget(user,enemy,40*(1+(SaiyanState.power(user)-1)*.5F));AbilityHelper.setDeltaMovement(enemy,user.getLookAngle().scale(2).add(0,.5,0));}
                if(ticks>=24||user.horizontalCollision)continuous.stopContinuity(user);break;
            case SPIRIT:
                if(ticks>=SaiyanBalance.SPIRIT_WINDUP){shoot(user,2,72,5,SaiyanBalance.SPIRIT_SPEED);continuous.stopContinuity(user);}break;
            default:break;
        }
    }
    private void endTechnique(LivingEntity user){
        if(user.level.isClientSide)return;
        boolean instinctDragon=move==Move.DRAGON&&finisher!=null;
        if(beam!=null){beam.remove();beam=null;}
        if(effect!=null){effect.remove();effect=null;}
        if(finisher!=null){finisher.release();finisher=null;}
        if(meteor!=null){meteor.release();meteor=null;}
        if(move==Move.TRANSFORM)SaiyanFeatures.setForm(user,0);
        else if(move==Move.CHARGE){SaiyanState.get(user).putBoolean("Charging",false);if(SaiyanState.action(user)==1)SaiyanState.pose(user,0,0);}
        else if(SaiyanState.action(user)!=13)SaiyanState.pose(user,0,0);
        cooldownComponent.startCooldown(user,move==Move.CHARGE&&chargeInterrupted?30*20:instinctDragon?15*20:move.cooldown);
        chargeInterrupted=false;
        if(user instanceof PlayerEntity)SaiyanFeatures.sync((PlayerEntity)user);
    }
    private static void stunWhileCharging(LivingEntity user){
        user.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(),3,0,false,false,false));
        user.setDeltaMovement(Vector3d.ZERO);user.setSprinting(false);user.fallDistance=0;
    }
    private void shoot(LivingEntity user,int kind,float base,float size,float speed){
        SaiyanProjectile shot=new SaiyanProjectile(user,getCore(),kind,base*SaiyanState.power(user),size);
        Vector3d origin=kind==2?user.position().add(0,8,0):hand(user);
        shot.setPos(origin.x,origin.y,origin.z);
        Vector3d aim=user.getEyePosition(1).add(user.getLookAngle().scale(80)).subtract(origin).normalize();
        shot.shoot(aim.x,aim.y,aim.z,speed,0);user.level.addFreshEntity(shot);
        SaiyanFeatures.play(user,SaiyanFeatures.BLAST.get(),kind==2?.9F:.5F,kind==2?.7F:1.1F);
    }
    public static Vector3d hand(LivingEntity user){return user.getEyePosition(1).add(0,-.35,0).add(user.getLookAngle().scale(.9));}
    public static Vector3d kamehamehaOrigin(LivingEntity user){return user.getEyePosition(1).add(0,-.25,0).add(user.getLookAngle().scale(2.5));}
    public static boolean canHit(LivingEntity user,LivingEntity target){
        if(target==user||!target.isAlive()||target.isSpectator()||user.isAlliedTo(target))return false;
        if(user instanceof PlayerEntity&&target instanceof PlayerEntity&&!((PlayerEntity)user).canHarmPlayer((PlayerEntity)target))return false;
        BlockPos pos=target.blockPosition();
        ProtectedArea area=ProtectedAreasData.get(user.level).getProtectedArea(pos.getX(),pos.getY(),pos.getZ());
        return area==null||area.canHurtEntities();
    }
    private static LivingEntity findTarget(LivingEntity user,double range){
        Vector3d from=user.getEyePosition(1),to=from.add(user.getLookAngle().scale(range));
        LivingEntity closest=null;double distance=range*range;
        for(LivingEntity target:user.level.getEntitiesOfClass(LivingEntity.class,new AxisAlignedBB(from,to).inflate(1))) {
            if(!canHit(user,target)||!user.canSee(target))continue;
            Optional<Vector3d> hit=target.getBoundingBox().inflate(.7).clip(from,to);
            if(hit.isPresent()&&hit.get().distanceToSqr(from)<distance){closest=target;distance=hit.get().distanceToSqr(from);}
        }return closest;
    }
    private void teleport(PlayerEntity user){
        int range=SaiyanBalance.transmissionRange(SaiyanState.form(user));
        LivingEntity target=findTarget(user,range);
        Vector3d destination;
        if(target!=null){Vector3d back=target.getLookAngle().multiply(1,0,1).normalize().scale(-2);destination=target.position().add(back);}
        else {
            Vector3d start=user.getEyePosition(1),end=start.add(user.getLookAngle().scale(range));
            BlockRayTraceResult ray=user.level.clip(new RayTraceContext(start,end,RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,user));
            destination=ray.getLocation().subtract(user.getLookAngle().scale(1.2)).add(0,-user.getEyeHeight(),0);
        }
        Vector3d safe=null;
        for(int i=0;i<4;i++){
            Vector3d candidate=destination.add(0,i,0);BlockPos pos=new BlockPos(candidate);
            if(user.level.hasChunkAt(pos)&&user.level.getWorldBorder().isWithinBounds(pos)&&candidate.y>0&&candidate.y<user.level.getMaxBuildHeight()-2
                    &&user.level.noCollision(user,user.getBoundingBox().move(candidate.subtract(user.position())))&&!user.level.containsAnyLiquid(user.getBoundingBox().move(candidate.subtract(user.position())))){safe=candidate;break;}
        }
        if(safe==null){user.displayClientMessage(new StringTextComponent("No safe teleport destination."),true);return;}
        if(!SaiyanState.spend(user,18))return;
        SaiyanEffect.spawn(user,5,user.position(),12,1);SaiyanFeatures.play(user,SaiyanFeatures.TELEPORT.get(),.6F,1);
        user.teleportTo(safe.x,safe.y,safe.z);user.fallDistance=0;
        SaiyanEffect.spawn(user,5,user.position(),12,1);SaiyanState.pose(user,8,10);
        cooldownComponent.startCooldown(user,160);
    }
}
