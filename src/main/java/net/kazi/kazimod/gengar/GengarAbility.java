package net.kazi.kazimod.gengar;

import java.util.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

public final class GengarAbility extends Ability {
    public enum Move {
        BALL("Shadow Ball","shadow_ball",2,160,"Charge a spectral orb for up to two seconds. Use again to fire early. The orb collapses into a 2-4 block blast."),
        PASSAGE("Phantom Passage","phantom_passage",4,280,"Sink into your shadow and surge over the ground for two seconds. Avoid ordinary attacks while submerged; emerge with a claw sweep."),
        HAUNT("Nightmare Haunting","nightmare_haunting",5,400,"Send grinning ghosts after nearby visible enemies for six seconds. Hits build up to three haunting stacks, slowing targets and weakening their attacks."),
        EATER("Dream Eater","dream_eater",6,320,"Bite a haunted enemy within 18 blocks. Consume your haunting stacks to increase damage and recover up to 15% of your maximum health."),
        GRASP("Spectral Grasp","spectral_grasp",7,360,"Ghostly hands hold an aimed enemy briefly before throwing them. Alternate mode: Shadow Tag marks a six-block boundary and pulls its victim back once."),
        PERISH("Perish Curse","perish_curse",9,1800,"Open a 12-block curse circle for six seconds. Enemies caught inside for the final four seconds suffer a powerful spectral detonation. Leaving the circle breaks their curse.");
        public final String title,id,description;public final int action,cooldown;
        Move(String title,String id,int action,int cooldown,String description){this.title=title;this.id=id;this.action=action;this.cooldown=cooldown;this.description=description;}
    }
    public enum Mode { GRASP, SHADOW_TAG }
    public static final AbilityCore<GengarAbility>[] CORES=create();
    @SuppressWarnings("unchecked") private static AbilityCore<GengarAbility>[] create(){
        AbilityCore<GengarAbility>[] result=new AbilityCore[Move.values().length];
        for(Move m:Move.values())result[m.ordinal()]=new AbilityCore.Builder<GengarAbility>(m.title,AbilityCategory.DEVIL_FRUITS,c->new GengarAbility(c,m))
                .setUnlockCheck(GengarFeatures::awakened).setIcon(icon(m.id)).setSourceHakiNature(SourceHakiNature.IMBUING)
                .setSourceType(SourceType.PROJECTILE).setSourceElement(SourceElement.NONE)
                .addDescriptionLine(new StringTextComponent(m.description)).addAdvancedDescriptionLine(CooldownComponent.getTooltip(m.cooldown)).build();
        return result;
    }
    public static ResourceLocation icon(String id){return new ResourceLocation("kazimod","textures/abilities/gengar/"+id+".png");}
    public static AbilityCore<GengarAbility> core(Move move){return CORES[move.ordinal()];}
    public static boolean isGengar(AbilityCore<?> core){return CORES!=null&&Arrays.asList(CORES).contains(core);}
    public static Move moveForAction(int action){for(Move m:Move.values())if(m.action==action||m==Move.GRASP&&action==8)return m;return null;}
    public final Move move;
    private final ContinuousComponent continuous=new ContinuousComponent(this,true);
    private final DealDamageComponent damage=new DealDamageComponent(this);
    private final GrabEntityComponent grab=new GrabEntityComponent(this,false,false,false,1);
    private final AltModeComponent<Mode> modes=new AltModeComponent<>(this,Mode.class,Mode.GRASP);
    private final Map<UUID,Integer> curses=new HashMap<>();
    private LivingEntity target;private Vector3d center;private GengarEffect field;
    private int ticks;private boolean completed,tag,pulled;
    public GengarAbility(AbilityCore<GengarAbility> core,Move move){
        super(core);this.move=move;isNew=true;addComponents(continuous,damage);
        if(move==Move.GRASP){addComponents(modes,grab);addTickEvent((user,ability)->{
            boolean tagMode=modes.getCurrentMode()==Mode.SHADOW_TAG;
            setDisplayName(new StringTextComponent(tagMode?"Shadow Tag":"Spectral Grasp"));setDisplayIcon(icon(tagMode?"shadow_tag":move.id));
        });}
        addCanUseCheck((user,ability)->{
            if(continuous.isContinuous())return AbilityUseResult.success();
            if(!(user instanceof PlayerEntity)||!GengarFeatures.awakened(user)||!GengarFeatures.active(user))return fail("Enter Mega Phantom Form first.");
            if(AbilityHelper.isWeakenedByKairosekiOrWater(user))return fail("Your Devil Fruit is weakened.");
            if(GengarFeatures.action(user)>0)return fail("Finish your current technique first.");
            if(move==Move.PASSAGE&&!user.isOnGround())return fail("Phantom Passage needs solid ground.");
            if(move==Move.EATER||move==Move.GRASP){LivingEntity aimed=GengarFeatures.aim(user,move==Move.EATER?18:20);
                if(aimed==null)return fail("Aim at a visible enemy.");
                if(move==Move.EATER&&!GengarFeatures.hauntedBy(user,aimed))return fail("Haunt that enemy before using Dream Eater.");
            }
            return AbilityUseResult.success();
        });
        addUseEvent((user,ability)->useMove(user));continuous.addTickEvent((user,ability)->tickMove(user));continuous.addEndEvent((user,ability)->end(user));
        addRemoveEvent((user,ability)->{if(continuous.isContinuous())continuous.stopContinuity(user);});
    }
    private AbilityUseResult fail(String message){return AbilityUseResult.fail(new StringTextComponent(message));}
    private void useMove(LivingEntity user){
        if(user.level.isClientSide)return;
        if(continuous.isContinuous()){completed=move==Move.BALL;continuous.stopContinuity(user);return;}
        if(!GengarFeatures.awakened(user)||!GengarFeatures.active(user))return;
        target=null;field=null;ticks=0;completed=false;pulled=false;curses.clear();center=user.position();tag=move==Move.GRASP&&modes.getCurrentMode()==Mode.SHADOW_TAG;
        if(move==Move.EATER||move==Move.GRASP){
            target=GengarFeatures.aim(user,move==Move.EATER?18:20);if(target==null)return;
            if(move==Move.EATER&&!GengarFeatures.hauntedBy(user,target))return;
            if(move==Move.GRASP&&!tag&&!grab.grabManually(user,target))return;
        }
        continuous.startContinuity(user);GengarFeatures.pose(user,tag?8:move.action,duration()+4);
        if(move==Move.PERISH)field=GengarEffect.spawn(user,GengarEffect.CURSE,center,120,12);
        if(move==Move.GRASP){center=target.position();field=GengarEffect.spawn(user,tag?GengarEffect.TAG:GengarEffect.HANDS,center,duration(),tag?6:2);if(!tag)field.attach(target);}
        if(move==Move.EATER)field=GengarEffect.spawn(user,GengarEffect.BITE,target.position(),24,target.getBbHeight()).attach(target);
        GengarFeatures.sound(user,move==Move.PERISH?SoundEvents.WITHER_AMBIENT:SoundEvents.EVOKER_PREPARE_ATTACK,.6F,move==Move.HAUNT?1.45F:.7F);
    }
    private int duration(){switch(move){case BALL:return 40;case PASSAGE:return 40;case HAUNT:return 120;case EATER:return 24;case GRASP:return tag?100:30;default:return 120;}}
    private void tickMove(LivingEntity user){
        if(user.level.isClientSide)return;
        if(!user.isAlive()||!GengarFeatures.active(user)||!GengarFeatures.awakened(user)||user.isSpectator()||AbilityHelper.isWeakenedByKairosekiOrWater(user)){
            completed=false;continuous.stopContinuity(user);return;
        }
        ticks++;
        if(target!=null&&(!target.isAlive()||target.level!=user.level||!GengarFeatures.canHit(user,target)||user.distanceToSqr(target)>32*32||!user.canSee(target))){continuous.stopContinuity(user);return;}
        switch(move){
            case BALL: break;
            case PASSAGE:
                if(user.horizontalCollision||!user.isOnGround()&&ticks>5){completed=true;continuous.stopContinuity(user);return;}
                Vector3d forward=user.getLookAngle().multiply(1,0,1).normalize();
                AbilityHelper.setDeltaMovement(user,forward.scale(.9).add(0,user.getDeltaMovement().y,0));break;
            case HAUNT:
                if(ticks%20==1){List<LivingEntity> enemies=enemies(user,user.position(),20);enemies.sort(Comparator.comparingDouble(user::distanceToSqr));
                    for(int i=0;i<Math.min(3,enemies.size());i++)shoot(user,1,6,enemies.get(i),i);
                }break;
            case EATER:
                if(ticks==12){int stacks=GengarFeatures.consumeHaunt(user,target);if(stacks>0&&damage.hurtTarget(user,target,28+8*stacks)){
                    user.heal(user.getMaxHealth()*.05F*stacks);GengarEffect.spawn(user,GengarEffect.HEAL,user.position(),24,1.4F).attach(user);
                    GengarFeatures.sound(user,SoundEvents.PHANTOM_BITE,.8F,.65F);
                }}break;
            case GRASP:
                if(tag){if(!pulled&&target.position().distanceToSqr(center)>36){
                    pulled=true;Vector3d pull=center.subtract(target.position()).normalize().scale(1.6).add(0,.2,0);
                    AbilityHelper.setDeltaMovement(target,pull);GengarEffect.spawn(user,GengarEffect.HANDS,target.position(),16,2).attach(target);
                    completed=true;continuous.stopContinuity(user);return;
                }}else{
                    if(!grab.canContinueGrab(user)){continuous.stopContinuity(user);return;}
                    target.getPersistentData().putLong("KaziGengarHeldUntil",user.level.getGameTime()+3);
                    target.addEffect(new EffectInstance(ModEffects.GRABBED.get(),3,1,false,false));target.stopUsingItem();AbilityHelper.setDeltaMovement(target,Vector3d.ZERO);
                }break;
            case PERISH:
                if(user.position().distanceToSqr(center)>24*24){continuous.stopContinuity(user);return;}
                Set<UUID> inside=new HashSet<>();for(LivingEntity enemy:enemies(user,center,12)){inside.add(enemy.getUUID());curses.merge(enemy.getUUID(),1,Integer::sum);}
                curses.keySet().retainAll(inside);if(ticks%20==0)GengarFeatures.sound(user,SoundEvents.NOTE_BLOCK_BELL,.5F,.5F+ticks/240F);break;
        }
        if(ticks>=duration()){completed=true;continuous.stopContinuity(user);}
    }
    private void end(LivingEntity user){
        if(user.level.isClientSide)return;
        boolean valid=completed&&user.isAlive()&&GengarFeatures.active(user)&&GengarFeatures.awakened(user)&&!AbilityHelper.isWeakenedByKairosekiOrWater(user);
        LivingEntity victim=target;if(victim!=null)victim.getPersistentData().remove("KaziGengarHeldUntil");if(grab.hasGrabbedEntity())grab.release(user);
        if(field!=null)field.remove();
        if(valid){
            if(move==Move.BALL){shoot(user,0,20+25*Math.min(1,ticks/40F),null,0);GengarFeatures.pose(user,3,12);}
            if(move==Move.PASSAGE){
                for(LivingEntity enemy:enemies(user,user.position(),4))if(enemy.position().subtract(user.position()).normalize().dot(user.getLookAngle())>-.1)damage.hurtTarget(user,enemy,24);
                GengarEffect.spawn(user,GengarEffect.SWEEP,user.position(),18,3);GengarFeatures.pose(user,10,14);
            }
            if(move==Move.GRASP&&!tag&&victim!=null&&GengarFeatures.canHit(user,victim)){
                damage.hurtTarget(user,victim,24);GengarFeatures.haunt(user,victim);AbilityHelper.setDeltaMovement(victim,user.getLookAngle().scale(2).add(0,.45,0));
            }
            if(move==Move.PERISH){
                for(LivingEntity enemy:enemies(user,center,12))if(curses.getOrDefault(enemy.getUUID(),0)>=80){
                    damage.hurtTarget(user,enemy,90);AbilityHelper.setDeltaMovement(enemy,enemy.position().subtract(center).normalize().scale(1.2).add(0,.5,0));
                }
                GengarEffect.spawn(user,GengarEffect.BLAST,center,32,12);GengarFeatures.sound(user,SoundEvents.GENERIC_EXPLODE,1,.65F);
            }
        }
        if(GengarFeatures.action(user)==move.action||move==Move.GRASP&&GengarFeatures.action(user)==8)GengarFeatures.pose(user,0,0);
        if(move==Move.PASSAGE)AbilityHelper.setDeltaMovement(user,user.getDeltaMovement().multiply(.2,1,.2));
        target=null;field=null;center=null;curses.clear();completed=false;cooldownComponent.startCooldown(user,move.cooldown);GengarFeatures.sync(user);
    }
    private void shoot(LivingEntity user,int kind,float power,LivingEntity target,int spread){
        Vector3d at=user.getEyePosition(1).add(user.getLookAngle().scale(.7));GengarProjectile projectile=new GengarProjectile(user,getCore(),kind,power);
        if(target!=null)projectile.target(target);projectile.setPos(at.x,at.y,at.z);
        Vector3d direction=user.getLookAngle().yRot((spread-1)*.25F);if(kind==0)direction=user.getLookAngle();
        projectile.shoot(direction.x,direction.y,direction.z,kind==0?1.65F:.65F,0);user.level.addFreshEntity(projectile);
        GengarFeatures.sound(user,kind==0?SoundEvents.WITHER_SHOOT:SoundEvents.VEX_AMBIENT,.45F,kind==0?.7F:1.35F);
    }
    private List<LivingEntity> enemies(LivingEntity user,Vector3d center,double radius){
        List<LivingEntity> out=new ArrayList<>();
        for(LivingEntity e:user.level.getEntitiesOfClass(LivingEntity.class,new AxisAlignedBB(center,center).inflate(radius)))
            if(e.position().distanceToSqr(center)<=radius*radius&&GengarFeatures.canHit(user,e)&&user.canSee(e))out.add(e);
        return out;
    }
}
