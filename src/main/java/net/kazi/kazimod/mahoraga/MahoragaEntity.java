package net.kazi.kazimod.mahoraga;

import java.util.*;
import javax.annotation.Nullable;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.player.*;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.nbt.*;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.network.NetworkHooks;
import xyz.pixelatedw.mineminenomi.abilities.rokushiki.GeppoAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;
import xyz.pixelatedw.mineminenomi.api.helpers.*;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.entities.mobs.OPEntity;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;

public final class MahoragaEntity extends OPEntity {
    public static final float MODEL_SCALE=.54F;
    public static final ResourceLocation TEXTURE=new ResourceLocation("kazimod","textures/entities/mahoraga.png");
    private static final DataParameter<Optional<UUID>> OWNER=EntityDataManager.defineId(MahoragaEntity.class,DataSerializers.OPTIONAL_UUID);
    private static final DataParameter<Integer> ACTION=EntityDataManager.defineId(MahoragaEntity.class,DataSerializers.INT);
    private static final DataParameter<Integer> START=EntityDataManager.defineId(MahoragaEntity.class,DataSerializers.INT);
    private static final DataParameter<Integer> WHEEL=EntityDataManager.defineId(MahoragaEntity.class,DataSerializers.INT);
    private static final DataParameter<Integer> TURN=EntityDataManager.defineId(MahoragaEntity.class,DataSerializers.INT);
    private static final DataParameter<Integer> IMPACT=EntityDataManager.defineId(MahoragaEntity.class,DataSerializers.INT);
    private static final DataParameter<Integer> ROCK=EntityDataManager.defineId(MahoragaEntity.class,DataSerializers.INT);
    private static final DataParameter<Float> DISMISS_FROM=EntityDataManager.defineId(MahoragaEntity.class,DataSerializers.FLOAT);
    private static final DataParameter<BlockPos> IMPACT_POS=EntityDataManager.defineId(MahoragaEntity.class,DataSerializers.BLOCK_POS);
    private final AdaptationMemory adaptation=new AdaptationMemory();
    private final CombatMemory tactics=new CombatMemory();
    private int decisionCooldown, dodgeCooldown, jumpCooldown, rangedCooldown, blockCooldown;
    private int noPathTicks, dropTicks;
    private boolean alternateSlash;
    private LivingEntity droppingTarget;
    private Vector3d dodgeVelocity=Vector3d.ZERO;
    private long lastThreat;
    private float damageMultiplier=1;

    public MahoragaEntity(EntityType<? extends MahoragaEntity> type,World world) {
        super(type,world,new ResourceLocation[]{TEXTURE});
        maxUpStep=1.5F;
        setPersistenceRequired();
    }
    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return OPEntity.createAttributes().add(Attributes.MAX_HEALTH,700)
                .add(Attributes.ATTACK_DAMAGE,32).add(Attributes.MOVEMENT_SPEED,.34)
                .add(Attributes.FOLLOW_RANGE,50).add(Attributes.KNOCKBACK_RESISTANCE,.9)
                .add(Attributes.ARMOR,0).add(Attributes.ARMOR_TOUGHNESS,0);
    }
    @Override protected void registerGoals() { goalSelector.addGoal(0,new SwimGoal(this)); }
    @Override protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(OWNER,Optional.empty());entityData.define(ACTION,0);entityData.define(START,0);
        entityData.define(WHEEL,0);entityData.define(TURN,0);entityData.define(IMPACT,-1000);entityData.define(ROCK,0);
        entityData.define(IMPACT_POS,BlockPos.ZERO);
        entityData.define(DISMISS_FROM,80F);
    }
    public void setOwner(LivingEntity owner) {
        entityData.set(OWNER,Optional.of(owner.getUUID()));
        EntityStatsCapability.get(this).setDoriki(Math.max(4000,Math.min(12000,EntityStatsCapability.get(owner).getDoriki())));
        EntityStatsCapability.get(this).setHeart(false);
        MobsHelper.unlockAndEquipAbility(this,GeppoAbility.INSTANCE);
        MahoragaHaki.initialize(this);
        setHealth(getMaxHealth());
    }
    @Nullable public UUID ownerId() { return entityData.get(OWNER).orElse(null); }
    @Nullable public LivingEntity owner() {
        UUID id=ownerId(); if(id==null)return null;
        Entity e=level instanceof ServerWorld?((ServerWorld)level).getEntity(id):level.getPlayerByUUID(id);
        return e instanceof LivingEntity?(LivingEntity)e:null;
    }
    public MahoragaAction action() { return MahoragaAction.from(entityData.get(ACTION)); }
    public float actionAge(float partial) { return Math.max(0,((int)level.getGameTime()-entityData.get(START))+partial); }
    public int wheelTurns() { return entityData.get(WHEEL); }
    public int wheelProgress() { return entityData.get(TURN); }
    public float impactAge(float partial) { return ((int)level.getGameTime()-entityData.get(IMPACT))+partial; }
    public BlockState heldBlock() { return Block.stateById(entityData.get(ROCK)); }
    public Vector3d impactPosition() { BlockPos p=entityData.get(IMPACT_POS);return new Vector3d(p.getX()+.5,p.getY()+.05,p.getZ()+.5); }
    public float finalDamageMultiplier() { return damageMultiplier; }
    public void beginSummon() { start(MahoragaAction.SUMMON);playSound(SoundEvents.WITHER_SPAWN,1.4F,.65F); }
    public boolean isDismissing() {return action()==MahoragaAction.DISMISS;}
    public float dismissalStartAge() {return entityData.get(DISMISS_FROM);}
    public void beginDismiss() {
        if(level.isClientSide || isDismissing() || !isAlive())return;
        entityData.set(DISMISS_FROM,action()==MahoragaAction.SUMMON?MathHelper.clamp(actionAge(0),0,80):80F);
        start(MahoragaAction.DISMISS);
        setTarget(null);droppingTarget=null;
        entityData.set(ROCK,0);entityData.set(TURN,0);
        setNoAi(true);setNoGravity(true);setDeltaMovement(Vector3d.ZERO);
    }
    @Override public boolean isPushable() {return !isDismissing() && super.isPushable();}
    private void start(MahoragaAction action) {
        entityData.set(ACTION,action.ordinal());entityData.set(START,(int)level.getGameTime());
        navigation.stop();
        if(action!=MahoragaAction.DODGE && action!=MahoragaAction.AIR_TAKEDOWN)
            setDeltaMovement(getDeltaMovement().multiply(.25,1,.25));
    }
    public boolean validEnemy(LivingEntity target) {
        LivingEntity owner=owner();
        return !isDismissing() && owner!=null && target!=null && target!=this && target!=owner && target.isAlive()
                && target.level==level && !MahoragaFeatures.allied(owner,target)
                && !AbilityHelper.isInCreativeOrSpectator(target) && target.distanceToSqr(owner)<=2500;
    }
    public void defend(LivingEntity attacker) {
        if(!validEnemy(attacker))return;
        if(getTarget()==null || !validEnemy(getTarget()) || distanceToSqr(attacker)<distanceToSqr(getTarget())*1.4) setTarget(attacker);
        lastThreat=level.getGameTime();
    }
    @Override public boolean isAlliedTo(Entity other) { return other==this || MahoragaFeatures.allied(owner(),other); }
    @Override public boolean hurt(DamageSource source,float amount) {
        if(level.isClientSide)return super.hurt(source,amount);
        if((action()==MahoragaAction.SUMMON || isDismissing()) && source!=DamageSource.OUT_OF_WORLD)return false;
        if(MahoragaFeatures.allied(owner(),source.getEntity()))return false;
        if(amount<=0 || !Float.isFinite(amount))return false;
        MahoragaDamage signature=MahoragaDamage.identify(source);
        adaptation.observe(signature.attack,signature.category,level.getGameTime());entityData.set(TURN,0);
        if(source.getEntity() instanceof LivingEntity)defend((LivingEntity)source.getEntity());
        float reduction=adaptation.multiplier(signature.attack,signature.category);
        if(reduction<=0)return false;
        if(!signature.attack.isEmpty() && source.getEntity() instanceof LivingEntity
                && validEnemy((LivingEntity)source.getEntity()) && canRaiseGuard() && random.nextFloat()<.8F) {
            Vector3d incoming=source.getEntity().position().subtract(position()).multiply(1,0,1).normalize();
            if(incoming.dot(getLookAngle().multiply(1,0,1).normalize())>.1)raiseGuard();
        }
        if(action()==MahoragaAction.BLOCK && !signature.attack.isEmpty() && source.getEntity()!=null) {
            Vector3d incoming=source.getEntity().position().subtract(position()).multiply(1,0,1).normalize();
            if(incoming.dot(getLookAngle().multiply(1,0,1).normalize())>.1){reduction*=.5F;playSound(SoundEvents.SHIELD_BLOCK,1.1F,.7F);}
        }
        // Apply after MMNM bonuses and armor, without leaking a multiplier into nested damage calls.
        float previous=damageMultiplier;damageMultiplier=reduction;
        try{return super.hurt(source,amount);}finally{damageMultiplier=previous;}
    }
    @Override public void aiStep() {
        super.aiStep();
        if(level.isClientSide)return;
        LivingEntity owner=owner();
        if(isDismissing()) {
            navigation.stop();setDeltaMovement(Vector3d.ZERO);
            if(owner==null || !owner.isAlive() || actionAge(0)>=MahoragaAction.DISMISS.ticks) {
                if(owner!=null)preserveFor(owner);
                if(owner!=null && owner.getPersistentData().hasUUID(MahoragaFeatures.SUMMON_TAG)
                        && getUUID().equals(owner.getPersistentData().getUUID(MahoragaFeatures.SUMMON_TAG)))
                    owner.getPersistentData().remove(MahoragaFeatures.SUMMON_TAG);
                remove();
            }
            return;
        }
        if(owner==null || !owner.isAlive() || !MahoragaAbility.awakened(owner)) { remove();return; }
        if(owner instanceof PlayerEntity && !AbilityDataCapability.get(owner).hasEquippedAbility(MahoragaAbility.INSTANCE)) { remove();return; }
        if(!isAlive())return;
        if(distanceToSqr(owner)>2500) { returnNearOwner(owner);return; }
        int previousProgress=adaptation.progress();
        adaptation.tick(level.getGameTime());
        if(previousProgress==0 && adaptation.progress()>0)playSound(MahoragaFeatures.WHEEL_SOUND.get(),.13F,1);
        entityData.set(WHEEL,adaptation.turns());entityData.set(TURN,adaptation.progress());
        if(decisionCooldown>0)decisionCooldown--;if(dodgeCooldown>0)dodgeCooldown--;if(jumpCooldown>0)jumpCooldown--;
        if(rangedCooldown>0)rangedCooldown--;if(blockCooldown>0)blockCooldown--;
        tickDrop();
        if(action()==MahoragaAction.SUMMON) {
            navigation.stop();setDeltaMovement(0,0,0);
            if(actionAge(0)>=MahoragaAction.SUMMON.ticks)start(MahoragaAction.IDLE);
            return;
        }
        LivingEntity target=getTarget();
        if(!validEnemy(target) || level.getGameTime()-lastThreat>600) { setTarget(null);target=null; }
        if(tickCount%10==0) {
            LivingEntity attacker=owner.getLastHurtByMob();
            if(owner.tickCount-owner.getLastHurtByMobTimestamp()<200 && validEnemy(attacker))defend(attacker);
            if(getTarget()==null && owner.tickCount-owner.getLastHurtMobTimestamp()<120 && validEnemy(owner.getLastHurtMob()))defend(owner.getLastHurtMob());
            target=getTarget();
            if(target!=null)tactics.opponent(target.getUUID()).sample(distanceTo(target),target.swinging,target.isUsingItem() && distanceTo(target)>6);
        }
        if(action()!=MahoragaAction.IDLE) { tickAction(target);return; }
        if(tickCount%4==0 && dodgeCooldown==0 && dodgeProjectile())return;
        if(distanceToSqr(owner)>1600 || target==null) {
            if(distanceToSqr(owner)>100){if(tickCount%8==0 || navigation.isDone())navigation.moveTo(owner,1.35);}
            else { navigation.stop();getLookControl().setLookAt(owner,20,20); }
            recoverPath(owner);return;
        }
        face(target);
        CombatMemory.Profile profile=tactics.opponent(target.getUUID());
        double distance=distanceTo(target),height=target.getY()-getY();
        if(decisionCooldown==0) {
            if(height>3 && distance<42) {
                if(jumpCooldown==0 && (distance<24 || random.nextFloat()<.55F)) {start(MahoragaAction.AIR_JUMP);return;}
                if(rangedCooldown==0) {start(MahoragaAction.THROW_BLOCK);return;}
            }
            if(!isOnGround() && height<4 && height>-6 && distance<6) {start(MahoragaAction.AIR_TAKEDOWN);return;}
            if(distance<7 && canSee(target)) {
                if(canRaiseGuard() && target.swinging && (profile.aggressive() || random.nextFloat()<.85F)) {raiseGuard();return;}
                if(profile.aggressive() && random.nextFloat()<.4F)start(MahoragaAction.PUNCH_BARRAGE);
                else if(random.nextFloat()<.28F)start(MahoragaAction.OVERHEAD_SLAM);
                else {start(alternateSlash?MahoragaAction.SLASH_LEFT:MahoragaAction.SLASH_RIGHT);alternateSlash=!alternateSlash;}
                return;
            }
            if(rangedCooldown==0 && distance>10 && distance<42 && canSee(target)
                    && (profile.kiting() || random.nextFloat()<.3F)) {start(MahoragaAction.AIR_BLAST);return;}
        }
        Vector3d predicted=target.position().add(target.getDeltaMovement().multiply(profile.kiting()?8:3,0,profile.kiting()?8:3));
        if(tickCount%8==0 || navigation.isDone()){
            if(predicted.distanceToSqr(owner.position())<=2304)navigation.moveTo(predicted.x,predicted.y,predicted.z,profile.kiting()?1.7:1.4);
            else navigation.moveTo(target,1.4);
        }
        recoverPath(owner);
    }
    private void face(LivingEntity target) {
        if(target==null)return;
        Vector3d d=target.getBoundingBox().getCenter().subtract(getEyePosition(1));
        float desired=(float)(MathHelper.atan2(d.z,d.x)*180/Math.PI)-90;
        yRot+=MathHelper.clamp(MathHelper.wrapDegrees(desired-yRot),-18,18);
        yBodyRot=yRot;yHeadRot=yRot;
        xRot=MathHelper.clamp((float)(-MathHelper.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))*180/Math.PI),-65,65);
    }
    private void tickAction(LivingEntity target) {
        MahoragaAction action=action();int age=(int)actionAge(0);
        if(action!=MahoragaAction.DODGE && target!=null)face(target);
        if(action==MahoragaAction.SLASH_RIGHT || action==MahoragaAction.SLASH_LEFT) {
            if(age>=7 && age<=10 && target!=null && distanceToSqr(target)>12)lunge(.32);
            if(age==11)melee(7,32,SourceType.SLASH,1.1);
        } else if(action==MahoragaAction.OVERHEAD_SLAM && age==24) {
            impact(8,48);playSound(SoundEvents.GENERIC_EXPLODE,2,.65F);
        } else if(action==MahoragaAction.PUNCH_BARRAGE && age>=10 && age<=30 && (age-10)%4==0) {
            if(target!=null && distanceToSqr(target)>9)lunge(.18);
            melee(6,10,SourceType.FIST,.15);
        } else if(action==MahoragaAction.DODGE && age<9) {
            if(safeMotion(dodgeVelocity))setDeltaMovement(dodgeVelocity.x,getDeltaMovement().y,dodgeVelocity.z);
        } else if(action==MahoragaAction.AIR_JUMP && age==10 && target!=null) {
            IAbility ability=AbilityDataCapability.get(this).getEquippedAbility(GeppoAbility.INSTANCE);
            if(ability!=null && ability.canUse(this).isSuccess()) {
                ability.use(this);
                Vector3d dir=target.position().subtract(position()).multiply(1,0,1).normalize();
                setDeltaMovement(dir.x*1.1,Math.max(1.25,getDeltaMovement().y),dir.z*1.1);hasImpulse=true;
            }
            jumpCooldown=24;
        } else if(action==MahoragaAction.AIR_TAKEDOWN && age==10 && validEnemy(target) && distanceToSqr(target)<36 && canSee(target)) {
            strike(target,18,SourceType.BLUNT,0);
            droppingTarget=target;dropTicks=0;
            AbilityHelper.setDeltaMovement(target,0,-3.4,0);setDeltaMovement(getDeltaMovement().multiply(.4,0,.4).add(0,-1.7,0));
        } else if(action==MahoragaAction.THROW_BLOCK) {
            if(age==10 && !pickBlock()) {start(MahoragaAction.AIR_BLAST);return;}
            if(age==25 && target!=null) {shoot(target,0);entityData.set(ROCK,0);rangedCooldown=65;}
        } else if(action==MahoragaAction.AIR_BLAST && age==30 && target!=null) {shoot(target,1);rangedCooldown=60;}
        if(age>=action.ticks) {start(MahoragaAction.IDLE);entityData.set(ROCK,0);decisionCooldown=5;}
    }
    private boolean canRaiseGuard() {
        if(blockCooldown>0 || !isOnGround())return false;
        return action().canRecoverIntoGuard(actionAge(0));
    }
    private void raiseGuard() {start(MahoragaAction.BLOCK);blockCooldown=34;}
    private void lunge(double speed) {
        Vector3d step=getLookAngle().multiply(1,0,1).normalize().scale(speed);
        if(safeMotion(step)){setDeltaMovement(step.x,getDeltaMovement().y,step.z);hasImpulse=true;}
    }
    private void melee(double reach,float damage,SourceType type,double knockback) {
        playSound(type==SourceType.SLASH?SoundEvents.PLAYER_ATTACK_SWEEP:SoundEvents.PLAYER_ATTACK_STRONG,1.6F,.7F);
        Vector3d forward=getLookAngle().multiply(1,0,1).normalize();
        for(LivingEntity victim:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(reach,2,reach))) {
            if(!validEnemy(victim) || distanceToSqr(victim)>reach*reach || !canSee(victim))continue;
            Vector3d direction=victim.position().subtract(position()).multiply(1,0,1).normalize();
            if(forward.dot(direction)<.05)continue;
            strike(victim,damage,type,knockback);
        }
    }
    private boolean strike(LivingEntity target,float damage,SourceType type,double knockback) {
        if(!validEnemy(target))return false;
        ModDamageSource source=new AbilityDamageSource("mahoraga_"+action().clip,this,MahoragaAbility.INSTANCE);
        source.setSourceTypes(new ArrayList<>(Arrays.asList(type,SourceType.PHYSICAL)));
        if(type==SourceType.FIST && action()==MahoragaAction.PUNCH_BARRAGE)target.invulnerableTime=0;
        boolean hit=target.hurt(source,MahoragaHaki.infuse(this,source,damage));
        if(hit && knockback>0) {
            Vector3d direction=target.position().subtract(position()).multiply(1,0,1).normalize();
            AbilityHelper.setDeltaMovement(target,direction.x*knockback,.4,direction.z*knockback);
        }
        return hit;
    }
    private void impact(double radius,float damage) {
        entityData.set(IMPACT,(int)level.getGameTime());
        entityData.set(IMPACT_POS,blockPosition());
        for(LivingEntity victim:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(radius,3,radius)))
            if(distanceToSqr(victim)<=radius*radius && canSee(victim))strike(victim,damage,SourceType.BLUNT,1.8);
    }
    private void tickDrop() {
        if(droppingTarget==null)return;
        LivingEntity target=droppingTarget;
        if(!validEnemy(target) || ++dropTicks>40 || distanceToSqr(target)>625) {droppingTarget=null;return;}
        if(dropTicks>2 && (target.isOnGround() || target.isInWater())) {
            strike(target,36,SourceType.BLUNT,1.4);entityData.set(IMPACT,(int)level.getGameTime());
            entityData.set(IMPACT_POS,target.blockPosition());
            playSound(SoundEvents.GENERIC_EXPLODE,1.5F,.7F);droppingTarget=null;return;
        }
        target.fallDistance=0;
        target.addEffect(new EffectInstance(ModEffects.MOVEMENT_BLOCKED.get(),3,0,false,false));
        AbilityHelper.setDeltaMovement(target,0,-3.4,0);
    }
    private void shoot(LivingEntity target,int kind) {
        if(!validEnemy(target) || !canSee(target))return;
        MahoragaProjectile projectile=new MahoragaProjectile(MahoragaFeatures.PROJECTILE.get(),level);
        projectile.setGuardian(this);projectile.configure(kind,heldBlock());
        Vector3d mouth=position().add(0,kind==0?3.4:3.8,0).add(getLookAngle().scale(1.1));
        projectile.setPos(mouth.x,mouth.y,mouth.z);
        Vector3d aim=target.getBoundingBox().getCenter().add(target.getDeltaMovement().scale(Math.min(8,distanceTo(target)/1.8))).subtract(mouth);
        if(kind==0)aim=aim.add(0,Math.sqrt(aim.x*aim.x+aim.z*aim.z)*.11,0);
        projectile.shoot(aim.x,aim.y,aim.z,kind==0?1.8F:2.5F,0);
        level.addFreshEntity(projectile);
        playSound(kind==0?SoundEvents.IRON_GOLEM_ATTACK:SoundEvents.ENDER_DRAGON_SHOOT,2,kind==0?.7F:1.1F);
    }
    private boolean pickBlock() {
        BlockPos base=blockPosition().below();
        for(int i=0;i<12;i++) {
            BlockPos pos=base.offset(random.nextInt(5)-2,random.nextInt(2)-1,random.nextInt(5)-2);
            if(!level.hasChunkAt(pos))continue;
            BlockState block=level.getBlockState(pos);
            if(block.isAir() || !block.getFluidState().isEmpty() || block.getDestroySpeed(level,pos)<0 || block.hasTileEntity()
                    || !block.isCollisionShapeFullBlock(level,pos))continue;
            if(ForgeEventFactory.getMobGriefingEvent(level,this)) {
                LivingEntity owner=owner();
                if(owner instanceof ServerPlayerEntity && MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level,pos,block,(PlayerEntity)owner)))continue;
                if(!level.destroyBlock(pos,false,this))continue;
            }
            entityData.set(ROCK,Block.getId(block));playSound(SoundEvents.STONE_BREAK,1.4F,.7F);return true;
        }
        return false;
    }
    private boolean dodgeProjectile() {
        Vector3d center=getBoundingBox().getCenter();int examined=0;
        for(ProjectileEntity projectile:level.getEntitiesOfClass(ProjectileEntity.class,getBoundingBox().inflate(14))) {
            if(++examined>48)break;
            if(projectile.getOwner()==this || MahoragaFeatures.allied(owner(),projectile.getOwner()))continue;
            Vector3d d=projectile.position().subtract(center),v=projectile.getDeltaMovement();
            if(!CombatMemory.incoming(d.x,d.y,d.z,v.x,v.y,v.z,2.8))continue;
            Vector3d side=new Vector3d(-v.z,0,v.x).normalize().scale(1.1);
            if(!safeMotion(side.scale(4)))side=side.scale(-1);
            if(!safeMotion(side.scale(4)))continue;
            dodgeVelocity=side;start(MahoragaAction.DODGE);dodgeCooldown=32;
            playSound(SoundEvents.PLAYER_ATTACK_SWEEP,1,.55F);
            if(projectile.getOwner() instanceof LivingEntity) {
                LivingEntity attacker=(LivingEntity)projectile.getOwner();defend(attacker);
                tactics.opponent(attacker.getUUID()).sample(distanceTo(attacker),false,true);
            }
            return true;
        }
        return false;
    }
    private boolean safeMotion(Vector3d delta) {
        LivingEntity owner=owner();if(owner==null || position().add(delta).distanceToSqr(owner.position())>2401)return false;
        if(!level.hasChunkAt(new BlockPos(position().add(delta))) || !level.noCollision(this,getBoundingBox().move(delta)))return false;
        Vector3d start=getBoundingBox().getCenter();
        if(level.clip(new RayTraceContext(start,start.add(delta),RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,this)).getType()!=RayTraceResult.Type.MISS)return false;
        return !isOnGround() || !level.getBlockState(new BlockPos(position().add(delta)).below()).getCollisionShape(level,new BlockPos(position().add(delta)).below()).isEmpty();
    }
    private void recoverPath(LivingEntity owner) {
        if(tickCount%20!=0)return;
        if(navigation.isDone() && distanceToSqr(owner)>225)noPathTicks+=20;else noPathTicks=0;
        if(noPathTicks>100)returnNearOwner(owner);
    }
    private void returnNearOwner(LivingEntity owner) {
        setTarget(null);navigation.stop();droppingTarget=null;start(MahoragaAction.IDLE);
        for(int i=0;i<24;i++) {
            double a=i*Math.PI/4,r=4+i/8;
            BlockPos top=new BlockPos(owner.position().add(Math.cos(a)*r,6,Math.sin(a)*r));
            for(int y=0;y<18;y++) {
                BlockPos ground=top.below(y);
                if(!level.hasChunkAt(ground) || !level.getBlockState(ground).isFaceSturdy(level,ground,Direction.UP))continue;
                Vector3d to=new Vector3d(ground.getX()+.5,ground.getY()+1.01,ground.getZ()+.5);
                if(!level.noCollision(this,getBoundingBox().move(to.subtract(position()))))continue;
                teleportTo(to.x,to.y,to.z);setDeltaMovement(Vector3d.ZERO);noPathTicks=0;return;
            }
        }
        // Never force chunks or leave an orphaned guardian beyond the leash.
        remove();
    }
    @Override public boolean causeFallDamage(float distance,float multiplier) { return false; }
    @Override public AxisAlignedBB getBoundingBoxForCulling() { return getBoundingBox().inflate(5); }
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit) {}
    @Override public void die(DamageSource source) {
        LivingEntity owner=owner();
        if(owner!=null)owner.getPersistentData().remove(MahoragaFeatures.RESERVE_TAG);
        super.die(source);
    }

    public void preserveFor(LivingEntity owner) {
        if(level.isClientSide)return;
        CompoundNBT saved=new CompoundNBT();
        saved.putFloat("Health",Math.max(0,getHealth()));
        saved.putFloat("MaxHealth",getMaxHealth());
        saved.putLong("LastHeal",level.getGameTime());
        writeAdaptation(saved);
        owner.getPersistentData().put(MahoragaFeatures.RESERVE_TAG,saved);
    }

    public void restoreFor(LivingEntity owner) {
        if(!owner.getPersistentData().contains(MahoragaFeatures.RESERVE_TAG,10))return;
        CompoundNBT saved=owner.getPersistentData().getCompound(MahoragaFeatures.RESERVE_TAG);
        if(saved.contains("Health"))setHealth(MathHelper.clamp(saved.getFloat("Health"),1,getMaxHealth()));
        readAdaptation(saved);
    }

    private void writeAdaptation(CompoundNBT nbt) {
        adaptation.expire(level.getGameTime());
        CompoundNBT attacks=new CompoundNBT(),categories=new CompoundNBT();
        CompoundNBT attackExpires=new CompoundNBT(),categoryExpires=new CompoundNBT();
        adaptation.attacks.forEach(attacks::putInt);adaptation.categories.forEach(categories::putInt);
        adaptation.attackExpires.forEach(attackExpires::putLong);
        adaptation.categoryExpires.forEach(categoryExpires::putLong);
        nbt.put("AdaptedAttacks",attacks);nbt.put("AdaptedCategories",categories);
        nbt.put("AttackExpires",attackExpires);nbt.put("CategoryExpires",categoryExpires);
        nbt.putInt("WheelTurns",adaptation.turns());
    }

    private void readAdaptation(CompoundNBT nbt) {
        adaptation.attacks.clear();adaptation.categories.clear();
        adaptation.attackExpires.clear();adaptation.categoryExpires.clear();
        long now=level.getGameTime(),fallback=now+AdaptationMemory.ADAPTATION_TICKS;
        CompoundNBT attacks=nbt.getCompound("AdaptedAttacks"),categories=nbt.getCompound("AdaptedCategories");
        CompoundNBT attackExpires=nbt.getCompound("AttackExpires"),categoryExpires=nbt.getCompound("CategoryExpires");
        attacks.getAllKeys().stream().limit(128).forEach(k->{
            long expiry=attackExpires.contains(k)?attackExpires.getLong(k):fallback;
            if(expiry>now){adaptation.attacks.put(k,MathHelper.clamp(attacks.getInt(k),0,4));adaptation.attackExpires.put(k,Math.min(expiry,fallback));}
        });
        categories.getAllKeys().stream().filter(k->k.startsWith("element:") || k.startsWith("type:")).limit(32).forEach(k->{
            long expiry=categoryExpires.contains(k)?categoryExpires.getLong(k):fallback;
            if(expiry>now){adaptation.categories.put(k,MathHelper.clamp(categories.getInt(k),0,7));adaptation.categoryExpires.put(k,Math.min(expiry,fallback));}
        });
        adaptation.restoreTurns(nbt.getInt("WheelTurns"));entityData.set(WHEEL,adaptation.turns());
    }

    @Override public void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        if(ownerId()!=null)nbt.putUUID("Summoner",ownerId());
        writeAdaptation(nbt);
    }
    @Override public void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        if(nbt.hasUUID("Summoner"))entityData.set(OWNER,Optional.of(nbt.getUUID("Summoner")));
        readAdaptation(nbt);
        if(!level.isClientSide)MahoragaHaki.initialize(this);
    }
    @Override public IPacket<?> getAddEntityPacket() { return NetworkHooks.getEntitySpawningPacket(this); }
}
