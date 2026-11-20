package net.kazi.kazimod.petal;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;
public final class PetalAbility extends Ability {
 public static final int BANKAI_RELEASE_LIFE=82;
 public static final double PRISON_RANGE=50;
 public static final float HAKUTEIKEN_DAMAGE=68, BURIAL_RADIUS=10.5F, BANKAI_BURIAL_RADIUS=15;
 public static final String[] NAMES={"Scatter","Petal Stream","Blossom Guard","Petal Step","Blooming Prison","Blossom Burial","Senbonzakura Kageyoshi","Senkei","Shukei: Hakuteiken"};
 public static final int[] COOL={10,80,100,8,320,360,1200,600,1200},LIFE={12000,80,80,6,20,28,640,300,HakuteikenTiming.DURATION};
 private static final String[] DESC={"Dissolve your sword into blade petals. Use again to retract.","Guide a blade stream with your aim for up to 4 seconds. Walls stop it. Hits every half second. Use again to cancel.","Hold a frontal petal shield for up to 4 seconds. Reduces frontal damage by 75%. Deletes projectiles up to 30 damage (45 in Bankai). Rear attacks bypass it.","Dash in your aimed direction. Two charges, replenishing one every 6 seconds. No invulnerability; walls stop movement.","Surround an aimed enemy up to 50 blocks away with petals. A hit traps the target in a breakable cage, stunning them in place for up to 4 seconds, even in midair. A miss halves the cooldown. Alternate mode in Bankai: Gokei, a larger contracting storm.","Mark an aimed ground location, then drive petals downward after a 1.4-second warning. Deals area damage once in a 10.5-block radius (15 in Bankai). Requires solid ground.","Release Bankai: drop your blade, raise immense blades, then scatter them. Two-second vulnerable entrance; lasts up to 30 seconds. Use again to end.","Toggle a 25-block-radius sword arena for up to 15 seconds. Use again to dismiss it; cooldown starts when it ends. Disables guard, adds 30% to melee strikes and drops swords on every enemy in the arena every 2 seconds. Falling swords bypass dodges and Future Sight. The sword wall, ceiling and floor block movement until the arena ends.","Condense your petals into the White Emperor Sword and wings. Gather the petals for five seconds as immense white wings, a halo and a concentrated blade form, then rush along your aim for 1.2 seconds with one powerful slash per enemy that bypasses Future Sight. Steer with your aim; walls stop the rush. Ends Bankai."};
 public enum Mode { PRISON,GOKEI }
 public static final AbilityCore<PetalAbility>[] CORES=create();
 @SuppressWarnings("unchecked") private static AbilityCore<PetalAbility>[] create(){AbilityCore<PetalAbility>[] a=new AbilityCore[9];for(int i=0;i<9;i++){final int id=i;String key="petal_"+xyz.pixelatedw.mineminenomi.wypi.WyHelper.getResourceName(NAMES[i]);a[i]=new AbilityCore.Builder<PetalAbility>(NAMES[i],key,AbilityCategory.DEVIL_FRUITS,AbilityType.ACTION,c->new PetalAbility(c,id)).setUnlockCheck(p->id!=0&&id!=3&&PetalState.fruit(p)&&(id<6||PetalState.awakened(p))).setSourceHakiNature(SourceHakiNature.IMBUING).setSourceType(SourceType.PROJECTILE,SourceType.SLASH).setSourceElement(SourceElement.NONE).setIcon(icon(i)).addDescriptionLine(new StringTextComponent(DESC[i])).addDescriptionLine(new StringTextComponent("Cooldown: "+COOL[i]/20F+"s.")).build();}return a;}
 public static AbilityCore<?>[] available(){return java.util.stream.IntStream.range(0,CORES.length).filter(i->i!=0&&i!=3).mapToObj(i->CORES[i]).toArray(AbilityCore<?>[]::new);}
 public static ResourceLocation icon(int i){return new ResourceLocation("kazimod","textures/abilities/petal/"+i+".png");}
 public final int move;private int ticks;private boolean gokei;private PetalField field;private Vector3d destination;
 private final ContinuousComponent continuous=new ContinuousComponent(this,true);
 private final DealDamageComponent damage=new DealDamageComponent(this);
 private final AltModeComponent<Mode> mode=new AltModeComponent<>(this,Mode.class,Mode.PRISON);
 public PetalAbility(AbilityCore<PetalAbility> core,int move){super(core);this.move=move;isNew=true;addComponents(continuous,damage);
  if(move==4){addComponents(mode);mode.addChangeModeEvent((p,a,m)->{setDisplayName(new StringTextComponent(m==Mode.GOKEI?"Gokei":NAMES[4]));setDisplayIcon(icon(m==Mode.GOKEI?9:4));});}
  addCanUseCheck((p,a)->{
   if(!core.canUnlock(p)||!PetalFeatures.usable(p))return fail("This fruit technique is unavailable.");
   if(continuous.isContinuous())return AbilityUseResult.success();
   if(PetalState.action(p)>=0)return fail("Finish your current technique first.");
   if((move==7||move==8||move==4&&mode.isMode(Mode.GOKEI))&&!PetalState.bankai(p))return fail("Release Senbonzakura Kageyoshi first.");
   if(move==2&&PetalState.get(p).getLong("Senkei")>p.level.getGameTime())return fail("Senkei sacrifices your defensive swarm.");
   if(move==3&&charges(p)==0)return fail("Petal Step is recharging.");
   if(move==4&&mode.isMode(Mode.GOKEI)&&PetalFeatures.target(p,PRISON_RANGE)==null)return fail("Aim at a visible enemy within 50 blocks.");
   if(move==5&&PetalFeatures.ground(p,24)==null)return fail("Aim at solid ground within range.");
   return AbilityUseResult.success();
  });
  addUseEvent((p,a)->{if(p.level.isClientSide)return;if(continuous.isContinuous()){continuous.stopContinuity(p);return;}
   gokei=move==4&&mode.isMode(Mode.GOKEI);
   ticks=0;field=null;
   if(move==4){
    LivingEntity aimed=PetalFeatures.target(p,PRISON_RANGE);
    if(gokei){if(aimed==null)return;destination=aimed.position();}
    else{
     LivingEntity target=aimed==null?null:PetalFeatures.bodyOwner(aimed);
     if(target==null||p.distanceToSqr(aimed)>PRISON_RANGE*PRISON_RANGE||PetalFeatures.prisonStunned(target)||!landPrisonHit(p,target)){
      cooldownComponent.startCooldown(p,COOL[4]/2);sound(p,SoundEvents.TRIDENT_THROW,1.4F);return;
     }
     if(target.isAlive())field=PetalField.prison(p,target);
    }
   }else if(move==5){destination=PetalFeatures.ground(p,24);if(destination==null)return;}
   if(move==0){PetalState.get(p).putBoolean("Scatter",true);field=PetalField.spawn(p,0,p.position(),22,1);}
   else {PetalState.pose(p,move,move==7?16:LIFE[move]);if(move==6)PetalState.get(p).putBoolean("Bankai",false);}
   if(move==3){int charges=charges(p);PetalState.get(p).putInt("Steps",charges-1);if(charges==2)PetalState.get(p).putLong("StepAt",p.level.getGameTime()+120);}
   if(move==1||move==2)field=PetalField.spawn(p,move,p.position(),LIFE[move],PetalState.bankai(p)?1.25F:1);
   if(move==3)field=PetalField.spawn(p,3,p.position(),14,1);
   if(move==4&&gokei)field=PetalField.spawn(p,9,destination,100,5);
   if(move==5)field=PetalField.spawn(p,5,destination,48,PetalState.bankai(p)?BANKAI_BURIAL_RADIUS:BURIAL_RADIUS);
   if(move==6)field=PetalField.spawn(p,6,p.position(),BANKAI_RELEASE_LIFE,1);
   if(move==7){field=PetalField.spawn(p,7,p.position(),300,25);PetalState.get(p).putLong("Senkei",p.level.getGameTime()+300);}
   if(move==8){PetalState.get(p).putLong("Senkei",0);for(PetalField old:p.level.getEntitiesOfClass(PetalField.class,p.getBoundingBox().inflate(96),e->e.owner()==p&&(e.kind()==7||e.kind()==6)))old.remove();field=PetalField.spawn(p,8,p.position(),HakuteikenTiming.DURATION,1);field.setPower(HAKUTEIKEN_DAMAGE);}
   if(move==7)continuous.triggerContinuity(p,LIFE[7]);else continuous.startContinuity(p);sound(p,(move==6||move==8)?SoundEvents.BEACON_ACTIVATE:SoundEvents.TRIDENT_THROW,(move==6||move==8)?.65F:1.4F);sync(p);
   if(move==6)net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsCutscenes.startKageyoshi(p,field);
  });
  continuous.addTickEvent((p,a)->{if(p.level.isClientSide)return;ticks++;
   if(!PetalFeatures.usable(p)||!PetalState.scattered(p)){continuous.stopContinuity(p);return;}
   if(move==0)return;
   if(move==6){if(ticks==40){PetalState.get(p).putBoolean("Bankai",true);PetalState.pose(p,-1,0);sound(p,SoundEvents.BEACON_POWER_SELECT,1);sync(p);}if(ticks>40&&!PetalState.bankai(p)){continuous.stopContinuity(p);return;}}
   else if((move==1||move==2)&&PetalState.action(p)!=move){continuous.stopContinuity(p);return;}
   if(move==7&&(!PetalState.bankai(p)||field==null||field.removed)){continuous.stopContinuity(p);return;}
   if(move==3){Vector3d v=p.getLookAngle().scale(.9);if(p.level.noCollision(p.getBoundingBox().move(v))) {p.setDeltaMovement(v);p.hurtMarked=true;}else{p.setDeltaMovement(Vector3d.ZERO);continuous.stopContinuity(p);return;}}
   if(move==8&&!PetalState.bankai(p)){continuous.stopContinuity(p);return;}
   if(move==8&&ticks==46)sound(p,SoundEvents.BEACON_POWER_SELECT,.8F);
   if(move==8&&ticks==HakuteikenTiming.CHARGE)sound(p,SoundEvents.TRIDENT_THROW,.7F);
   if(move==8&&HakuteikenTiming.rushing(ticks)){
    if(p.horizontalCollision){continuous.stopContinuity(p);return;}
    xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper.setDeltaMovement(p,p.getLookAngle().scale(HakuteikenTiming.RUSH_SPEED));p.fallDistance=0;
   }
   if(ticks>=LIFE[move])continuous.stopContinuity(p);
  });
  continuous.addEndEvent((p,a)->{if(p.level.isClientSide)return;
   if(move==6)net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsCutscenes.cancel(p,net.kazi.kazimod.abilities.cinematics.AwakeningCutsceneType.SENBONZAKURA_KAGEYOSHI);
   if(move==0){PetalState.reset(p);}else if(move==6){PetalState.get(p).putBoolean("Bankai",false);PetalState.get(p).putLong("Senkei",0);}
   if(PetalState.action(p)==move)PetalState.pose(p,-1,0);
   if(field!=null&&(move==1||move==2||move==8||move==6))field.remove();
   if(move==7){if(field!=null)field.remove();PetalState.get(p).putLong("Senkei",0);}
   if(field!=null&&ticks<LIFE[move]&&(move==4||move==5)){field.remove();if(move==7)PetalState.get(p).putLong("Senkei",0);}
   if(move==8){PetalState.reset(p);}
   cooldownComponent.startCooldown(p,COOL[move]);sync(p);
  });
  addRemoveEvent((p,a)->{if(continuous.isContinuous())continuous.stopContinuity(p);});
 }
 public static int charges(LivingEntity p){long now=p.level.getGameTime();if(!PetalState.get(p).contains("Steps")){PetalState.get(p).putInt("Steps",2);return 2;}int n=PetalState.get(p).getInt("Steps");long at=PetalState.get(p).getLong("StepAt");while(n<2&&now>=at){n++;at+=120;}PetalState.get(p).putInt("Steps",n);PetalState.get(p).putLong("StepAt",at);return n;}
 private boolean landPrisonHit(LivingEntity p,LivingEntity target){
  float health=target.getHealth()+target.getAbsorptionAmount();
  return hurt(p,target,1)&&target.getHealth()+target.getAbsorptionAmount()<health;
 }
 public boolean hurt(LivingEntity p,LivingEntity target,float amount){
  LivingEntity body=PetalFeatures.bodyOwner(target);if(body==null||!PetalFeatures.canHit(p,body))return false;
  DamageSource source=damage.getDamageSource(p);
  if(move==7&&source instanceof xyz.pixelatedw.mineminenomi.init.ModDamageSource)((xyz.pixelatedw.mineminenomi.init.ModDamageSource)source).setUnavoidable();
  return damage.hurtTarget(p,body,amount,source);
 }
 public static boolean bypassesDodge(DamageSource source){
  return source instanceof AbilityDamageSource&&((AbilityDamageSource)source).getAbilitySource()==CORES[7];
 }
 public static boolean bypassesFutureSight(DamageSource source){
  return bypassesDodge(source)||source instanceof AbilityDamageSource&&((AbilityDamageSource)source).getAbilitySource()==CORES[8];
 }
 private static AbilityUseResult fail(String s){return AbilityUseResult.fail(new StringTextComponent(s));}
 private static void sync(LivingEntity p){if(p instanceof PlayerEntity)PetalFeatures.sync((PlayerEntity)p);}
 private static void sound(LivingEntity p,SoundEvent event,float pitch){p.level.playSound(null,p.blockPosition(),event,SoundCategory.PLAYERS,.55F,pitch);}
}
