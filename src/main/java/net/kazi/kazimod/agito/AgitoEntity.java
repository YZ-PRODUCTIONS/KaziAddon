package net.kazi.kazimod.agito;

import java.util.*;
import javax.annotation.Nullable;
import net.kazi.kazimod.mahoraga.MahoragaAbility;
import net.kazi.kazimod.mahoraga.MahoragaFeatures;
import net.kazi.kazimod.mahoraga.MahoragaHaki;
import net.kazi.kazimod.shikigami.TenShadowsBeast;
import net.kazi.kazimod.shikigami.TenShadowsTaming;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.player.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.entities.mobs.OPEntity;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;

public final class AgitoEntity extends OPEntity implements TenShadowsBeast {
    public static final float MODEL_SCALE=.5F,SELF_HEAL=.01F,OWNER_HEAL=.02F;
    public static final ResourceLocation TEXTURE=new ResourceLocation("kazimod","textures/entities/agito.png");
    private static final DataParameter<Optional<UUID>> OWNER=EntityDataManager.defineId(AgitoEntity.class,DataSerializers.OPTIONAL_UUID);
    private static final DataParameter<Boolean> TAMING=EntityDataManager.defineId(AgitoEntity.class,DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> ACTION=EntityDataManager.defineId(AgitoEntity.class,DataSerializers.INT);
    private static final DataParameter<Integer> START=EntityDataManager.defineId(AgitoEntity.class,DataSerializers.INT);
    private static final DataParameter<Float> DISMISS_FROM=EntityDataManager.defineId(AgitoEntity.class,DataSerializers.FLOAT);
    private int attackCooldown,jumpCooldown,healingRequestTicks,lastHealingToggleTick=-1000;
    private boolean healingRequested,alternatePunch;
    private long lastThreat;
    private Vector3d heldPosition;
    private PlayerEntity heldPlayer;

    public AgitoEntity(EntityType<? extends AgitoEntity> type,World world){
        super(type,world,new ResourceLocation[]{TEXTURE});maxUpStep=1.2F;setPersistenceRequired();
    }
    public static AttributeModifierMap.MutableAttribute createAttributes(){
        return OPEntity.createAttributes().add(Attributes.MAX_HEALTH,500).add(Attributes.ATTACK_DAMAGE,26)
                .add(Attributes.MOVEMENT_SPEED,.34).add(Attributes.FOLLOW_RANGE,50)
                .add(Attributes.KNOCKBACK_RESISTANCE,.8).add(Attributes.ARMOR,0).add(Attributes.ARMOR_TOUGHNESS,0);
    }
    @Override protected void registerGoals(){goalSelector.addGoal(0,new SwimGoal(this));}
    @Override protected void defineSynchedData(){
        super.defineSynchedData();entityData.define(OWNER,Optional.empty());entityData.define(TAMING,false);entityData.define(ACTION,0);
        entityData.define(START,0);entityData.define(DISMISS_FROM,80F);
    }
    public void setOwner(LivingEntity owner){
        entityData.set(OWNER,Optional.of(owner.getUUID()));
        EntityStatsCapability.get(this).setDoriki(Math.max(3000,Math.min(10000,EntityStatsCapability.get(owner).getDoriki())));
        EntityStatsCapability.get(this).setHeart(false);
        MahoragaHaki.initialize(this);
        setHealth(getMaxHealth());
    }
    @Nullable public UUID ownerId(){return entityData.get(OWNER).orElse(null);}
    @Override public String shadowId(){return "agito";}
    @Override public boolean isTaming(){return entityData.get(TAMING);}
    @Override public void setTaming(boolean taming){entityData.set(TAMING,taming);}
    @Nullable public LivingEntity owner(){
        UUID id=ownerId();if(id==null)return null;
        Entity e=level instanceof ServerWorld?((ServerWorld)level).getEntity(id):level.getPlayerByUUID(id);
        return e instanceof LivingEntity?(LivingEntity)e:null;
    }
    public AgitoAction action(){return AgitoAction.from(entityData.get(ACTION));}
    public float actionAge(float partial){return Math.max(0,(int)level.getGameTime()-entityData.get(START)+partial);}
    public boolean isHealing(){return action()==AgitoAction.HEAL;}
    public boolean isDismissing(){return action()==AgitoAction.DISMISS;}
    public boolean portal(){return action()==AgitoAction.SUMMON || isDismissing();}
    public float dismissalStartAge(){return entityData.get(DISMISS_FROM);}
    private void start(AgitoAction next){
        entityData.set(ACTION,next.ordinal());entityData.set(START,(int)level.getGameTime());navigation.stop();
    }
    public void beginSummon(){start(AgitoAction.SUMMON);playSound(SoundEvents.EVOKER_PREPARE_SUMMON,.8F,.7F);}
    public void beginDismiss(){
        if(level.isClientSide || isDismissing() || !isAlive())return;
        entityData.set(DISMISS_FROM,action()==AgitoAction.SUMMON?MathHelper.clamp(actionAge(0),0,80):80F);
        stopHealing();setTarget(null);start(AgitoAction.DISMISS);setNoGravity(true);setDeltaMovement(Vector3d.ZERO);
    }
    public boolean validEnemy(LivingEntity target){
        LivingEntity owner=owner();
        return !portal() && owner!=null && target!=null && target!=this && target.isAlive() && target.level==level
                && (isTaming()?target==owner:!MahoragaFeatures.allied(owner,target)) && !AbilityHelper.isInCreativeOrSpectator(target)
                && target.distanceToSqr(owner)<=2500;
    }
    public void defend(LivingEntity enemy){
        if(!validEnemy(enemy) || isHealing() || healingRequested)return;
        if(!validEnemy(getTarget()) || distanceToSqr(enemy)<distanceToSqr(getTarget())*1.4)setTarget(enemy);
        lastThreat=level.getGameTime();
    }
    @Override public boolean isAlliedTo(Entity other){return other==this || !isTaming()&&MahoragaFeatures.allied(owner(),other);}
    @Override public boolean isPushable(){return !isHealing() && !portal() && super.isPushable();}

    public void toggleHealing(PlayerEntity player){
        if(level.isClientSide || isTaming() || !player.getUUID().equals(ownerId()) || !isAlive() || portal())return;
        if(tickCount-lastHealingToggleTick<8)return;
        lastHealingToggleTick=tickCount;
        if(isHealing() || healingRequested){stopHealing();return;}
        if(distanceToSqr(player)>36 || !player.isAlive() || player.isPassenger() || player.isFallFlying() || !canSee(player))return;
        healingRequested=true;healingRequestTicks=0;setTarget(null);start(AgitoAction.IDLE);
        player.displayClientMessage(new TranslationTextComponent("message.kazimod.agito.approaching"),true);
    }
    public void stopHealing(){
        healingRequested=false;healingRequestTicks=0;heldPosition=null;
        if(heldPlayer!=null){heldPlayer.removeEffect(AgitoFeatures.EMBRACE.get());heldPlayer=null;}
        if(!level.isClientSide && isHealing())start(AgitoAction.IDLE);
    }
    private void startHealing(PlayerEntity player){
        Vector3d toward=player.position().subtract(position()).multiply(1,0,1).normalize();
        if(toward.lengthSqr()<.1)return;
        Vector3d at=player.position().subtract(toward.scale(1.35));
        AxisAlignedBB box=getBoundingBox().move(at.subtract(position()));
        if(level.noCollision(this,box))setPos(at.x,at.y,at.z);
        face(player);navigation.stop();setTarget(null);setDeltaMovement(Vector3d.ZERO);
        yRot=yBodyRot=yHeadRot=(float)(MathHelper.atan2(toward.z,toward.x)*180/Math.PI)-90;
        healingRequested=false;heldPosition=player.position();heldPlayer=player;start(AgitoAction.HEAL);
        // Stop attacks that were already charging/running, but leave the two guardian summons active.
        for(IAbility ability:new ArrayList<IAbility>(AbilityDataCapability.get(player).getEquippedAbilities())){
            if(ability!=null && !net.kazi.kazimod.shikigami.ShikigamiFeatures.awakeningCore(ability.getCore()))
                AbilityHelper.emergencyStopAbility(player,ability);
        }
        if(!isHealing() || heldPlayer!=player || !player.isAlive())return;
        player.stopUsingItem();player.setSprinting(false);
        player.addEffect(new EffectInstance(AgitoFeatures.EMBRACE.get(),40,0,false,false,false));
        player.displayClientMessage(new TranslationTextComponent("message.kazimod.agito.healing"),true);
    }
    public void holdOwner(PlayerEntity player){
        if(!isHealing() || heldPosition==null || player!=heldPlayer)return;
        if(!player.isAlive() || player.level!=level || player.isPassenger() || player.distanceToSqr(heldPosition)>4
                || !isAlive() || distanceToSqr(player)>9){stopHealing();return;}
        // The effect syncs the input lock; the server anchor also rejects movement from modified clients.
        player.setDeltaMovement(Vector3d.ZERO);player.fallDistance=0;player.setSprinting(false);
        if(player.distanceToSqr(heldPosition)>.0025 && player instanceof ServerPlayerEntity)
            ((ServerPlayerEntity)player).connection.teleport(heldPosition.x,heldPosition.y,heldPosition.z,player.yRot,player.xRot);
    }
    private void tickHealing(LivingEntity owner){
        if(!(owner instanceof PlayerEntity)){stopHealing();return;}
        PlayerEntity player=(PlayerEntity)owner;
        if(isHealing()){
            if(heldPosition==null || !player.hasEffect(AgitoFeatures.EMBRACE.get())){stopHealing();return;}
            navigation.stop();setTarget(null);setDeltaMovement(Vector3d.ZERO);holdOwner(player);
            if(!isHealing())return;
            int age=(int)actionAge(0);
            if(age%10==0)player.addEffect(new EffectInstance(AgitoFeatures.EMBRACE.get(),40,0,false,false,false));
            if(age>0 && age%20==0){
                player.heal(player.getMaxHealth()*OWNER_HEAL);
                playSound(SoundEvents.EXPERIENCE_ORB_PICKUP,.2F,1.3F);
            }
            return;
        }
        if(++healingRequestTicks>100 || distanceToSqr(player)>64 || !canSee(player)){
            stopHealing();return;
        }
        face(player);
        if(distanceToSqr(player)<=3.6 && Math.abs(player.getY()-getY())<.5 && isOnGround() && player.isOnGround())startHealing(player);
        else if(tickCount%5==0)navigation.moveTo(player,1.15);
    }

    @Override public boolean hurt(DamageSource source,float amount){
        if(level.isClientSide)return super.hurt(source,amount);
        if(amount<=0 || !Float.isFinite(amount) || (portal() && source!=DamageSource.OUT_OF_WORLD))return false;
        if(!isTaming()&&MahoragaFeatures.allied(owner(),source.getEntity()))return false;
        stopHealing();
        if(source.getEntity() instanceof LivingEntity)defend((LivingEntity)source.getEntity());
        return super.hurt(source,amount);
    }
    @Override public void aiStep(){
        super.aiStep();if(level.isClientSide || !isAlive())return;
        LivingEntity owner=owner();
        if(owner==null || !owner.isAlive() || !MahoragaAbility.awakened(owner)
                || !AbilityDataCapability.get(owner).hasEquippedAbility(AgitoAbility.INSTANCE)){
            if(owner!=null)preserveFor(owner);remove();return;
        }
        if(isDismissing()){
            navigation.stop();setDeltaMovement(Vector3d.ZERO);
            if(actionAge(0)>=AgitoAction.DISMISS.ticks){preserveFor(owner);remove();}
            return;
        }
        if(action()==AgitoAction.SUMMON){
            navigation.stop();setDeltaMovement(Vector3d.ZERO);
            if(actionAge(0)>=AgitoAction.SUMMON.ticks)start(AgitoAction.IDLE);
            return;
        }
        if(tickCount%20==0)heal(getMaxHealth()*SELF_HEAL);
        if(isHealing() || healingRequested){tickHealing(owner);return;}
        if(distanceToSqr(owner)>2500){returnNearOwner(owner);return;}
        if(attackCooldown>0)attackCooldown--;if(jumpCooldown>0)jumpCooldown--;
        if(tickCount%10==0){
            if(owner.tickCount-owner.getLastHurtByMobTimestamp()<200)defend(owner.getLastHurtByMob());
            if(owner.tickCount-owner.getLastHurtMobTimestamp()<120)defend(owner.getLastHurtMob());
        }
        LivingEntity target=getTarget();
        if(!validEnemy(target) || level.getGameTime()-lastThreat>600){setTarget(null);target=null;}
        if(isTaming()&&validEnemy(owner)){setTarget(owner);target=owner;lastThreat=level.getGameTime();}
        if(action()!=AgitoAction.IDLE){tickAction(target);return;}
        if(target==null){
            if(distanceToSqr(owner)>36){if(tickCount%8==0)navigation.moveTo(owner,distanceToSqr(owner)>144?1.45:1.05);}
            else {navigation.stop();getLookControl().setLookAt(owner,20,20);}
            return;
        }
        face(target);
        double distance=distanceTo(target);
        if(distance<3.5 && canSee(target) && attackCooldown==0){
            alternatePunch=!alternatePunch;
            start(random.nextFloat()<.3F?AgitoAction.BARRAGE:alternatePunch?AgitoAction.PUNCH_RIGHT:AgitoAction.PUNCH_LEFT);
        }else if(isOnGround() && jumpCooldown==0 && distance>3.5 && distance<11 && canSee(target)
                && (target.getY()>getY()+1.2 || distance>6)){
            start(AgitoAction.JUMP);jumpCooldown=65;
        }else if(tickCount%5==0)navigation.moveTo(target,1.5);
    }
    private void tickAction(LivingEntity target){
        int age=(int)actionAge(0);AgitoAction action=action();
        if(action==AgitoAction.JUMP){
            if(age<8){navigation.stop();if(target!=null)face(target);}
            if(age==8 && isOnGround() && validEnemy(target)){
                Vector3d delta=target.position().subtract(position()).multiply(1,0,1).normalize().scale(.8);
                setDeltaMovement(delta.x,.65+Math.min(.45,Math.max(0,target.getY()-getY())*.08),delta.z);hasImpulse=true;
                playSound(SoundEvents.ENDER_DRAGON_FLAP,.45F,1.1F);
            }
        }else{
            navigation.stop();
            if(age<8 && target!=null)face(target);
            if(action==AgitoAction.BARRAGE){
                if(age>=10 && age<=34 && (age-10)%6==0)strike(target,11,.15F,true);
            }else if(age==12)strike(target,(float)getAttributeValue(Attributes.ATTACK_DAMAGE),.6F,false);
        }
        if(age>=action.ticks){start(AgitoAction.IDLE);attackCooldown=7;}
    }
    private void strike(LivingEntity target,float damage,float knockback,boolean barrage){
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP,.65F,barrage?1.3F:.7F);
        if(!validEnemy(target) || distanceToSqr(target)>18 || !canSee(target))return;
        Vector3d delta=target.position().subtract(position());
        if(delta.multiply(1,0,1).normalize().dot(getLookAngle().multiply(1,0,1).normalize())<.15)return;
        ModDamageSource source=new AbilityDamageSource("agito_"+action().clip,this,AgitoAbility.INSTANCE);
        source.setSourceTypes(new ArrayList<>(Arrays.asList(SourceType.FIST,SourceType.PHYSICAL)));
        if(barrage)target.invulnerableTime=0;
        if(target.hurt(source,MahoragaHaki.infuse(this,source,damage))){
            target.knockback(knockback,-delta.x,-delta.z);
            playSound(SoundEvents.PLAYER_ATTACK_STRONG,.8F,.75F);
        }
    }
    private void face(LivingEntity target){
        Vector3d d=target.position().subtract(position());
        float angle=(float)(MathHelper.atan2(d.z,d.x)*180/Math.PI)-90;
        yRot=MathHelper.rotateIfNecessary(yRot,angle,22);yBodyRot=yRot;yHeadRot=yRot;
        getLookControl().setLookAt(target,30,25);
    }
    private void returnNearOwner(LivingEntity owner){
        navigation.stop();setTarget(null);
        for(int i=0;i<12;i++){
            double angle=i*Math.PI/6;BlockPos top=new BlockPos(owner.position().add(Math.cos(angle)*4,5,Math.sin(angle)*4));
            for(int down=0;down<12;down++){
                BlockPos ground=top.below(down);
                if(!level.hasChunkAt(ground) || !level.getBlockState(ground).isFaceSturdy(level,ground,Direction.UP))continue;
                Vector3d at=new Vector3d(ground.getX()+.5,ground.getY()+1.01,ground.getZ()+.5);
                if(!level.noCollision(this,getBoundingBox().move(at.subtract(position()))))continue;
                setPos(at.x,at.y,at.z);setDeltaMovement(Vector3d.ZERO);start(AgitoAction.IDLE);return;
            }
        }
        preserveFor(owner);remove();
    }
    public void preserveFor(LivingEntity owner){
        if(level.isClientSide || !isAlive() || isTaming())return;
        CompoundNBT saved=new CompoundNBT();saved.putFloat("Health",getHealth());saved.putFloat("MaxHealth",getMaxHealth());
        saved.putLong("LastHeal",level.getGameTime());owner.getPersistentData().put(AgitoFeatures.RESERVE_TAG,saved);
    }
    public void restoreFor(LivingEntity owner){
        if(isTaming())return;
        CompoundNBT saved=owner.getPersistentData().getCompound(AgitoFeatures.RESERVE_TAG);
        if(saved.contains("Health"))setHealth(MathHelper.clamp(saved.getFloat("Health"),1,getMaxHealth()));
    }
    @Override public void remove(){
        if(!level.isClientSide){
            stopHealing();LivingEntity owner=owner();
            if(owner!=null && owner.getPersistentData().hasUUID(AgitoFeatures.SUMMON_TAG)
                    && getUUID().equals(owner.getPersistentData().getUUID(AgitoFeatures.SUMMON_TAG)))owner.getPersistentData().remove(AgitoFeatures.SUMMON_TAG);
        }
        super.remove();
    }
    @Override public void die(DamageSource source){
        stopHealing();super.die(source);
        if(!level.isClientSide&&dead)TenShadowsTaming.died(this,source);
    }
    @Override public boolean causeFallDamage(float distance,float multiplier){return false;}
    @Override public AxisAlignedBB getBoundingBoxForCulling(){return getBoundingBox().inflate(3);}
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit){}
    @Override public void readAdditionalSaveData(CompoundNBT nbt){
        super.readAdditionalSaveData(nbt);
        if(!level.isClientSide)MahoragaHaki.initialize(this);
    }
    @Override public IPacket<?> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
