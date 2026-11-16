package net.kazi.kazimod.ghoul;

import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;

public final class GhoulAbility extends Ability {
    public static final String[] NAMES={"Kagune","Crystal Volley","Wing Burst","Piercing Feather","Scarlet Rain",
        "Crimson Guard","Cleaver","Bulwark Rush","Guillotine","Ravenous Flurry","Predator's Grasp","Crimson Vault","Rending Spiral",
        "Tail Spear","Sweeping Tail","Counter Coil","Predator Rush"};
    public static final int[] COST={0,12,10,18,25,0,15,20,28,16,18,12,25,8,12,14,20};
    public static final int[] COOL={10,70,100,160,300,100,100,180,260,100,160,140,240,60,100,160,200};
    public static final int[] LIFE={0,26,14,36,44,100,30,34,42,34,36,50,42,20,26,28,48};
    public static final AbilityCore<GhoulAbility>[] CORES=create();
    @SuppressWarnings("unchecked") private static AbilityCore<GhoulAbility>[] create() {
        AbilityCore<GhoulAbility>[] a=new AbilityCore[17];
        for(int i=0;i<a.length;i++) { final int id=i;
            a[i]=new AbilityCore.Builder<GhoulAbility>(NAMES[i],AbilityCategory.RACIAL,c->new GhoulAbility(c,id))
                .setUnlockCheck(p->GhoulFeatures.AVAILABLE&&GhoulState.ghoul(p)&&(id==0||GhoulState.type(p)==(id-1)/4))
                .setSourceHakiNature(i>0&&i<5?SourceHakiNature.IMBUING:SourceHakiNature.HARDENING)
                .setSourceType(i>0&&i<5?SourceType.PROJECTILE:SourceType.FIST).setSourceElement(SourceElement.NONE)
                .setIcon(new ResourceLocation("kazimod","textures/abilities/ghoul/"+i+".png"))
                .addDescriptionLine(new StringTextComponent(description(i))).build();
        } return a;
    }
    private static String description(int i) {
        String[] d={"Deploy or retract your chosen kagune.","Fire six crystal shards toward your aim.","Dash toward your aim, scattering crystals behind you.","Charge a piercing crystal shot.","Leap and rain crystal shards toward your aim.","Hold a frontal shield. 75% damage reduction, 95% on a perfect guard. Drains RC; use again to stop.","Sweep a heavy blade in front of you.","Rush behind your shield and knock enemies away.","Slam your blade down, sending a ground shockwave forward.","Rapidly stab forward with four tendrils.","Latch onto a visible enemy and pull them into striking range.","Vault forward with your tendrils and strike on landing.","Lash out in all directions, finishing with an outward sweep.","Thrust your tail straight ahead.","Sweep low and briefly trip nearby enemies.","Catch a frontal melee hit and retaliate with your tail.","Lunge forward, strike twice, then deliver an overhead finisher."};
        return d[i]+(i==0?"":" RC: "+COST[i]+" | Cooldown: "+COOL[i]/20F+"s");
    }
    public final int move;
    private final ContinuousComponent continuous=new ContinuousComponent(this,true);
    private final DealDamageComponent damage=new DealDamageComponent(this);
    private int ticks; private LivingEntity captive;
    private final Set<UUID> struck=new HashSet<>();
    private final List<LivingEntity> carried=new ArrayList<>();
    private Vector3d rushDirection, waveOrigin, waveDirection;
    private boolean waveBlocked;
    public GhoulAbility(AbilityCore<GhoulAbility> core,int move) {
        super(core); this.move=move; isNew=true; addComponents(continuous,damage);
        addCanUseCheck((p,a)-> {
            if(!GhoulFeatures.AVAILABLE) return fail("Ghoul is currently unavailable.");
            if(!GhoulState.ghoul(p)||(move>0&&GhoulState.type(p)!=(move-1)/4)) return fail("This technique belongs to a different kagune.");
            if(continuous.isContinuous()) return AbilityUseResult.success();
            if(GhoulState.action(p)!=0) return fail("Finish your current kagune technique first.");
            if(move>0&&!GhoulState.active(p)) return fail("Deploy your kagune first.");
            if(GhoulState.rc(p)<COST[move]) return fail("Not enough RC.");
            return AbilityUseResult.success();
        });
        addUseEvent((p,a)-> {
            if(p.level.isClientSide) return;
            if(continuous.isContinuous()){continuous.stopContinuity(p);return;}
            if(move==0){GhoulState.get(p).putBoolean("Deployed",!GhoulState.active(p)); GhoulState.get(p).putLong("DeployTime",p.level.getGameTime());cooldownComponent.startCooldown(p,10); sync(p);return;}
            if(!GhoulState.spend(p,COST[move])) return;
            ticks=0; struck.clear(); carried.clear(); captive=move==10?GhoulFeatures.aim(p,12):null;
            rushDirection=p.getLookAngle().multiply(1,0,1).normalize();waveOrigin=null;waveBlocked=false;
            if(move==15)GhoulState.get(p).putBoolean("CounterHit",false);
            if(captive!=null) GhoulState.get(p).putInt("Target",captive.getId());
            GhoulState.pose(p,move,LIFE[move]+2); continuous.startContinuity(p);
            if(move==2) launch(p,p.getLookAngle().scale(1.25));
            if(move==4) launch(p,new Vector3d(0,.85,0));
            if(move==11) launch(p,p.getLookAngle().scale(1.05).add(0,.65,0));
            p.level.playSound(null,p.blockPosition(),move<5?SoundEvents.TRIDENT_RIPTIDE_1:SoundEvents.ARMOR_EQUIP_NETHERITE,SoundCategory.PLAYERS,.35F,move<5?1.5F:.75F); sync(p);
        });
        continuous.addTickEvent((p,a)->tickMove(p));
        continuous.addEndEvent((p,a)-> {if(!p.level.isClientSide){GhoulState.pose(p,0,0);GhoulState.get(p).remove("Target");if(captive!=null)captive.getPersistentData().remove("KaziGhoulHeldUntil");for(LivingEntity t:carried)t.getPersistentData().remove("KaziGhoulHeldUntil");cooldownComponent.startCooldown(p,COOL[move]);sync(p);}captive=null;struck.clear();carried.clear();});
        addRemoveEvent((p,a)->{if(continuous.isContinuous())continuous.stopContinuity(p);});
    }
    private AbilityUseResult fail(String s){return AbilityUseResult.fail(new StringTextComponent(s));}
    private void sync(LivingEntity p){if(p instanceof PlayerEntity)GhoulFeatures.sync((PlayerEntity)p);}
    private void tickMove(LivingEntity p) {
        if(p.level.isClientSide)return;
        if(!p.isAlive()||!GhoulState.ghoul(p)||!GhoulState.active(p)||GhoulState.type(p)!=(move-1)/4){continuous.stopContinuity(p);return;}
        ticks++;
        switch(move){
            case 1: if(ticks>=6&&ticks<=21&&ticks%3==0)shoot(p,8,false,p.getLookAngle());break;
            case 2: if(ticks<=8)launch(p,p.getLookAngle().scale(1.25));if(ticks%3==0&&ticks<10)shoot(p,6,false,p.getLookAngle().scale(-1).yRot((ticks-5)*.08F));break;
            case 3: if(ticks==22)shoot(p,27,true,p.getLookAngle());break;
            case 4: if(ticks>=10&&ticks<=34&&ticks%2==0){
                Vector3d aim=p.getLookAngle();if(aim.y>-.15)aim=new Vector3d(aim.x,-.45,aim.z).normalize();
                double spread=(ticks%7-3)*.045;shoot(p,6,false,aim.yRot((float)spread));
            }break;
            case 5: if(!GhoulState.spend(p,.35F)){continuous.stopContinuity(p);return;}break;
            case 6: if(ticks==16){hit(p,3.8,24,.1,false);GhoulFeatures.effect(p,4,p.position().add(0,.8,0),3.2F);}break;
            case 7: bulwark(p);break;
            case 8: if(ticks>=22&&ticks<=36&&ticks%2==0)shockwave(p,(ticks-22)/2+1);break;
            case 9: if(ticks==6||ticks==12||ticks==18||ticks==24){struck.clear();hit(p,4.5,7,.85,false);}break;
            case 10:
                if(ticks<7)break;
                if(captive!=null&&captive.isAlive()&&p.distanceTo(captive)<14&&p.canSee(captive)&&GhoulFeatures.canHit(p,captive)&&GhoulState.get(p).getLong("LastHit")<=GhoulState.get(p).getLong("Start")){
                    captive.getPersistentData().putLong("KaziGhoulHeldUntil",p.level.getGameTime()+3);
                    Vector3d destination=p.position().add(p.getLookAngle().multiply(1,0,1).normalize().scale(1.6));Vector3d pull=destination.subtract(captive.position());
                    launch(captive,pull.normalize().scale(Math.min(.7,pull.length())));
                    if(p.distanceTo(captive)<2.6&&ticks>=12){strike(p,captive,19,false);captive.getPersistentData().remove("KaziGhoulHeldUntil");captive=null;GhoulState.get(p).remove("Target");sync(p);}
                }else if(captive!=null){captive.getPersistentData().remove("KaziGhoulHeldUntil");captive=null;GhoulState.get(p).remove("Target");sync(p);}break;
            case 11: if(ticks>5&&p.isOnGround()){hit(p,4,20,-1,false);GhoulFeatures.effect(p,2,p.position(),2);continuous.stopContinuity(p);return;}break;
            case 12: if(ticks==9||ticks==18||ticks==28){struck.clear();hit(p,4.5,ticks==28?16:8,-1,false);GhoulFeatures.effect(p,4,p.position().add(0,.8,0),4);}break;
            case 13: if(ticks==7)hit(p,5.5,17,.97,false);break;
            case 14: if(ticks==12){hit(p,4.5,14,-.2,true);GhoulFeatures.effect(p,4,p.position().add(0,.15,0),4);}break;
            case 15: if(GhoulState.get(p).getBoolean("CounterHit")){GhoulState.get(p).putBoolean("CounterHit",false);Entity attacker=p.level.getEntity(GhoulState.get(p).getInt("CounterTarget"));if(attacker instanceof LivingEntity&&p.distanceTo(attacker)<=5&&p.canSee(attacker)&&GhoulFeatures.canHit(p,(LivingEntity)attacker)){strike(p,(LivingEntity)attacker,24,false);launch(attacker,attacker.position().subtract(p.position()).normalize().scale(1.3).add(0,.25,0));}GhoulFeatures.effect(p,4,p.position().add(0,.8,0),3);continuous.stopContinuity(p);return;}break;
            case 16:
                if(ticks<10){
                    if(captive==null)captive=GhoulFeatures.aim(p,3);
                    Vector3d approach=captive==null?rushDirection.scale(.85):captive.position().subtract(p.position()).multiply(1,0,1);
                    if(captive!=null)approach=approach.normalize().scale(Math.max(0,Math.min(.65,p.distanceTo(captive)-1.6)));
                    launch(p,approach.add(0,p.getDeltaMovement().y,0));
                }
                if(ticks==12||ticks==22||ticks==34){if(captive==null)captive=GhoulFeatures.aim(p,3.5);if(captive!=null&&p.distanceTo(captive)<4&&p.canSee(captive)&&GhoulFeatures.canHit(p,captive)){strike(p,captive,ticks==34?21:8,true);if(ticks==34){launch(captive,rushDirection.scale(1.1).add(0,.3,0));GhoulFeatures.effect(p,2,captive.position(),1.4F);}}}break;
        }
        if(ticks>=LIFE[move])continuous.stopContinuity(p);
    }
    private void hit(LivingEntity p,double range,float amount,double cone,boolean trip){
        for(LivingEntity t:p.level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(range),e->GhoulFeatures.canHit(p,e))){
            Vector3d d=t.getBoundingBox().getCenter().subtract(p.getBoundingBox().getCenter());
            if(d.lengthSqr()>range*range||d.normalize().dot(p.getLookAngle())<cone||!p.canSee(t)||!struck.add(t.getUUID()))continue;
            if(strike(p,t,amount,move==9||move==12)) {t.knockback(move==12?.85F:.2F,p.getX()-t.getX(),p.getZ()-t.getZ());if(trip){t.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN,20,3));t.getPersistentData().putLong("KaziGhoulHeldUntil",p.level.getGameTime()+10);}}
        }
    }
    private void shoot(LivingEntity p,float amount,boolean pierce,Vector3d direction){
        GhoulProjectile shot=new GhoulProjectile(p,getCore(),amount,pierce);
        Vector3d right=p.getLookAngle().cross(new Vector3d(0,1,0)).normalize();Vector3d at=p.getEyePosition(1).add(right.scale(ticks%2==0?.32:-.32)).add(0,-.1,0);
        shot.setPos(at.x,at.y,at.z);shot.setDeltaMovement(direction.normalize().scale(pierce?2.6:1.8));p.level.addFreshEntity(shot);
        p.level.playSound(null,p.blockPosition(),SoundEvents.TRIDENT_THROW,SoundCategory.PLAYERS,.25F,pierce?.65F:1.7F);
    }
    private boolean strike(LivingEntity p,LivingEntity t,float amount,boolean repeated){
        if(repeated)t.invulnerableTime=0;boolean result=damage.hurtTarget(p,t,amount);if(result){GhoulFeatures.effect(p,0,t.getBoundingBox().getCenter(),.8F);p.level.playSound(null,t.blockPosition(),SoundEvents.PLAYER_ATTACK_STRONG,SoundCategory.PLAYERS,.4F,.85F);}return result;
    }
    private void bulwark(LivingEntity p){
        if(ticks>=5&&ticks<=24){
            launch(p,rushDirection.scale(.7).add(0,p.getDeltaMovement().y,0));
            for(LivingEntity t:p.level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(1.7),e->GhoulFeatures.canHit(p,e)))if(p.canSee(t)&&t.position().subtract(p.position()).normalize().dot(rushDirection)>.2&&!carried.contains(t)&&carried.size()<3)carried.add(t);
            for(LivingEntity t:carried)if(t.isAlive()&&p.distanceTo(t)<5&&p.canSee(t)){t.getPersistentData().putLong("KaziGhoulHeldUntil",p.level.getGameTime()+3);Vector3d pull=p.position().add(rushDirection.scale(1.4)).subtract(t.position());launch(t,pull.scale(.5).add(rushDirection.scale(.5)));}
        }
        if(ticks==25)for(LivingEntity t:carried)if(t.isAlive()&&p.distanceTo(t)<5&&p.canSee(t)&&GhoulFeatures.canHit(p,t)){strike(p,t,20,false);launch(t,rushDirection.scale(1.7).add(0,.4,0));t.getPersistentData().remove("KaziGhoulHeldUntil");}
    }
    private void shockwave(LivingEntity p,int step){
        if(waveBlocked)return;if(waveOrigin==null){waveOrigin=p.position();waveDirection=p.getLookAngle().multiply(1,0,1).normalize();}
        Vector3d center=waveOrigin.add(waveDirection.scale(step));BlockRayTraceResult ground=p.level.clip(new RayTraceContext(center.add(0,2,0),center.add(0,-3,0),RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,p));
        if(ground.getType()!=RayTraceResult.Type.BLOCK||Math.abs(ground.getLocation().y-waveOrigin.y)>1.6){waveBlocked=true;return;}
        center=ground.getLocation().add(0,.05,0);if(!p.level.noCollision(new AxisAlignedBB(center.x-.2,center.y+.1,center.z-.2,center.x+.2,center.y+1.2,center.z+.2))){waveBlocked=true;return;}
        GhoulFeatures.effect(p,1,center,1.1F);
        for(LivingEntity t:p.level.getEntitiesOfClass(LivingEntity.class,new AxisAlignedBB(center,center).inflate(1.5,2,1.5),e->GhoulFeatures.canHit(p,e)))if(p.canSee(t)&&struck.add(t.getUUID())){strike(p,t,32,false);launch(t,waveDirection.scale(.8).add(0,.5,0));}
    }
    public static void launch(Entity p,Vector3d v){p.setDeltaMovement(v);p.hurtMarked=true;}
}
