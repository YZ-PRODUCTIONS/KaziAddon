package net.kazi.kazimod.shikigami;

import java.util.*;
import net.kazi.kazimod.mahoraga.*;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.haki.HakiDataCapability;
import xyz.pixelatedw.mineminenomi.entities.mobs.OPEntity;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;

public final class ShikigamiEntity extends OPEntity implements TenShadowsBeast {
    public enum Action {IDLE,SUMMON,DISMISS,SLASH_LEFT,SLASH_RIGHT,STOMP,TONGUE,RUNUP,CHARGE,DROP,LAND}
    private static final DataParameter<Optional<UUID>> OWNER=EntityDataManager.defineId(ShikigamiEntity.class,DataSerializers.OPTIONAL_UUID);
    private static final DataParameter<Boolean> TAMING=EntityDataManager.defineId(ShikigamiEntity.class,DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> ACTION=EntityDataManager.defineId(ShikigamiEntity.class,DataSerializers.INT),START=EntityDataManager.defineId(ShikigamiEntity.class,DataSerializers.INT),PREY=EntityDataManager.defineId(ShikigamiEntity.class,DataSerializers.INT);
    public final ShikigamiKind kind;
    private int cooldown,chargeStuckTicks;
    private long lastThreat;
    private boolean alternate,impacted;
    private double runDistance;
    private Vector3d lastRun,runGoal,chargeDirection,lastChargePosition;
    public ShikigamiEntity(EntityType<? extends ShikigamiEntity> type,World world,ShikigamiKind kind){
        super(type,world,new ResourceLocation[]{new ResourceLocation("kazimod","textures/entities/"+kind.id+".png")});this.kind=kind;
        maxUpStep=kind==ShikigamiKind.OX?3F:kind==ShikigamiKind.ELEPHANT?1.5F:1.1F;setPersistenceRequired();
    }
    public static AttributeModifierMap.MutableAttribute attributes(ShikigamiKind k){return OPEntity.createAttributes()
            .add(Attributes.MAX_HEALTH,k.health).add(Attributes.ATTACK_DAMAGE,k.damage).add(Attributes.MOVEMENT_SPEED,k.speed)
            .add(Attributes.FOLLOW_RANGE,50).add(Attributes.KNOCKBACK_RESISTANCE,k==ShikigamiKind.ELEPHANT?1:.4)
            .add(Attributes.ARMOR,0).add(Attributes.ARMOR_TOUGHNESS,0);}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new SwimGoal(this));}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(OWNER,Optional.empty());entityData.define(TAMING,false);entityData.define(ACTION,0);entityData.define(START,0);entityData.define(PREY,-1);}
    @Override public String shadowId(){return kind.id;}
    @Override public boolean isTaming(){return entityData.get(TAMING);}
    @Override public void setTaming(boolean taming){entityData.set(TAMING,taming);}
    public UUID ownerId(){return entityData.get(OWNER).orElse(null);}
    public LivingEntity owner(){UUID id=ownerId();if(id==null)return null;Entity e=level instanceof ServerWorld?((ServerWorld)level).getEntity(id):level.getPlayerByUUID(id);return e instanceof LivingEntity?(LivingEntity)e:null;}
    public void setOwner(LivingEntity owner){
        entityData.set(OWNER,Optional.of(owner.getUUID()));EntityStatsCapability.get(this).setHeart(false);
        equip(EquipmentSlotType.HEAD,Items.NETHERITE_HELMET);equip(EquipmentSlotType.CHEST,Items.NETHERITE_CHESTPLATE);
        equip(EquipmentSlotType.LEGS,Items.NETHERITE_LEGGINGS);equip(EquipmentSlotType.FEET,Items.NETHERITE_BOOTS);setHealth(getMaxHealth());
    }
    private void equip(EquipmentSlotType slot,Item item){ItemStack stack=new ItemStack(item);stack.enchant(Enchantments.ALL_DAMAGE_PROTECTION,4);stack.enchant(Enchantments.VANISHING_CURSE,1);stack.getOrCreateTag().putBoolean("Unbreakable",true);setItemSlot(slot,stack);setDropChance(slot,0);}
    public Action action(){return Action.values()[MathHelper.clamp(entityData.get(ACTION),0,Action.values().length-1)];}
    public float age(float partial){return Math.max(0,level.getGameTime()-entityData.get(START)+partial);}
    public LivingEntity prey(){Entity e=level.getEntity(entityData.get(PREY));return e instanceof LivingEntity?(LivingEntity)e:null;}
    public boolean dismissing(){return action()==Action.DISMISS;}
    public boolean portal(){return action()==Action.SUMMON||dismissing();}
    private void start(Action action){entityData.set(ACTION,action.ordinal());entityData.set(START,(int)level.getGameTime());navigation.stop();if(action!=Action.TONGUE&&action!=Action.RUNUP&&action!=Action.CHARGE)entityData.set(PREY,-1);chargeStuckTicks=0;lastChargePosition=null;}
    public void beginSummon(){start(Action.SUMMON);setNoGravity(true);setDeltaMovement(Vector3d.ZERO);playSound(SoundEvents.EVOKER_PREPARE_SUMMON,.7F,.7F);}
    public void beginDismiss(){if(level.isClientSide||dismissing()||!isAlive())return;start(Action.DISMISS);setTarget(null);setNoGravity(true);setDeltaMovement(Vector3d.ZERO);}
    public boolean validEnemy(LivingEntity e){return e!=null&&e!=this&&e.isAlive()&&owner()!=null&&e.level==level&&(isTaming()?e==owner():!MahoragaFeatures.allied(owner(),e))&&!AbilityHelper.isInCreativeOrSpectator(e)&&e.distanceToSqr(owner())<=2500;}
    public void defend(LivingEntity enemy){if(validEnemy(enemy)&&!portal()){setTarget(enemy);lastThreat=level.getGameTime();}}
    @Override public boolean isAlliedTo(Entity other){return other==this||!isTaming()&&(other==owner()||MahoragaFeatures.allied(owner(),other));}
    @Override public boolean hurt(DamageSource source,float amount){
        if(!level.isClientSide){if((portal()||action()==Action.DROP)&&source!=DamageSource.OUT_OF_WORLD)return false;if(!isTaming()&&MahoragaFeatures.allied(owner(),source.getEntity()))return false;
            if(source.getEntity() instanceof LivingEntity)defend((LivingEntity)source.getEntity());}
        return super.hurt(source,amount);
    }
    @Override public void aiStep(){
        super.aiStep();if(level.isClientSide||!isAlive())return;
        LivingEntity owner=owner();
        if(owner==null||!owner.isAlive()||!MahoragaAbility.awakened(owner)||!AbilityDataCapability.get(owner).hasEquippedAbility(ShikigamiAbility.core(kind))){if(owner!=null)preserveFor(owner);remove();return;}
        int age=(int)age(0);
        if(portal()){
            navigation.stop();setDeltaMovement(Vector3d.ZERO);
            if(dismissing()){if(age>=50){preserveFor(owner);remove();}return;}
            if(age>=(kind==ShikigamiKind.ELEPHANT?24:70)){setNoGravity(false);start(kind==ShikigamiKind.ELEPHANT?Action.DROP:Action.IDLE);}
            return;
        }
        if(action()==Action.DROP){
            navigation.stop();setDeltaMovement(0,Math.max(-4.5,getDeltaMovement().y-.22),0);hasImpulse=true;
            if(isOnGround()&&!impacted){impacted=true;slam();start(Action.LAND);}
            else if(age>240||getY()<-20){preserveFor(owner);remove();}
            return;
        }
        if(action()==Action.LAND){if(age>=35)start(Action.IDLE);return;}
        if(distanceToSqr(owner)>2500){returnToOwner(owner);return;}
        if(cooldown>0)cooldown--;
        if(tickCount%10==0){if(owner.tickCount-owner.getLastHurtByMobTimestamp()<100)defend(owner.getLastHurtByMob());if(owner.tickCount-owner.getLastHurtMobTimestamp()<100)defend(owner.getLastHurtMob());}
        LivingEntity target=getTarget();if(!validEnemy(target)||level.getGameTime()-lastThreat>600){setTarget(null);target=null;}
        if(isTaming()&&validEnemy(owner)){setTarget(owner);target=owner;lastThreat=level.getGameTime();}
        if(action()!=Action.IDLE){tickAttack(target,age);return;}
        if(target==null){if(distanceToSqr(owner)>36){if(tickCount%10==0)navigation.moveTo(owner,1.1);assistOxStep(owner.position(),.33);}else navigation.stop();return;}
        face(target,12);
        double distance=distanceTo(target);
        if(kind==ShikigamiKind.OX&&cooldown==0){beginRunup(target);return;}
        if(kind==ShikigamiKind.TOAD&&distance<=24&&distance>3&&cooldown==0&&canSee(target)){start(Action.TONGUE);entityData.set(PREY,target.getId());return;}
        if(kind!=ShikigamiKind.TOAD&&distance<kind.width+2&&cooldown==0&&canSee(target)){alternate=!alternate;start(kind==ShikigamiKind.ELEPHANT?Action.STOMP:alternate?Action.SLASH_LEFT:Action.SLASH_RIGHT);return;}
        if(kind==ShikigamiKind.TOAD&&distance<=5)navigation.stop();else if(tickCount%6==0)navigation.moveTo(target,kind==ShikigamiKind.TOTALITY?1.5:1);
        assistOxStep(target.position(),.3);
    }
    private void tickAttack(LivingEntity target,int age){
        if(!validEnemy(target)){start(Action.IDLE);return;}
        switch(action()){
            case RUNUP:
                if(lastRun!=null&&isOnGround())runDistance+=Math.min(1.8,position().subtract(lastRun).multiply(1,0,1).length());lastRun=position();
                if(tickCount%6==0)navigation.moveTo(runGoal.x,runGoal.y,runGoal.z,1.6);
                assistOxStep(runGoal,.48);
                if(runDistance>=12||age>=65||distanceToSqr(runGoal)<3){chargeDirection=target.position().subtract(position()).multiply(1,0,1).normalize();start(Action.CHARGE);}
                break;
            case CHARGE:
                navigation.stop();if(chargeDirection==null||chargeDirection.lengthSqr()<.1){start(Action.IDLE);break;}
                Vector3d toward=target.position().subtract(position()).multiply(1,0,1);
                if(toward.lengthSqr()>.01){float current=movementYaw(chargeDirection),wanted=movementYaw(toward);
                    float yaw=MathHelper.approachDegrees(current,wanted,12);double radians=Math.toRadians(yaw);
                    chargeDirection=new Vector3d(-Math.sin(radians),0,Math.cos(radians));}
                entityData.set(PREY,target.getId());
                // A step can briefly report a collision. End only if the charge stops making progress.
                boolean stalled=horizontalCollision&&lastChargePosition!=null&&position().subtract(lastChargePosition).multiply(1,0,1).lengthSqr()<.0025;
                chargeStuckTicks=stalled?chargeStuckTicks+1:0;lastChargePosition=position();
                faceOxMovement(chargeDirection);
                Vector3d velocity=chargeDirection.scale(.75+Math.min(20,runDistance)*.018);setDeltaMovement(velocity.x,getDeltaMovement().y,velocity.z);hasImpulse=true;
                AxisAlignedBB sweep=getBoundingBox().expandTowards(velocity).inflate(.35);
                if(sweep.intersects(target.getBoundingBox())&&canSee(target)){strike(target,ShikigamiKind.chargeDamage(runDistance),1.8F);start(Action.IDLE);cooldown=14;setDeltaMovement(0,getDeltaMovement().y,0);}
                else if(age>65||chargeStuckTicks>=8){start(Action.IDLE);cooldown=14;}
                break;
            case TONGUE:
                navigation.stop();face(target,20);
                if(distanceToSqr(target)>24*24||!canSee(target)){start(Action.IDLE);cooldown=35;break;}
                if(age>=7&&age<=32){Vector3d pull=position().add(0,.6,0).subtract(target.position());if(pull.length()>3){Vector3d drag=pull.normalize().scale(.85);target.setDeltaMovement(drag.x,MathHelper.clamp(drag.y+.07,-.3,.35),drag.z);target.hasImpulse=true;target.hurtMarked=true;}
                    if(age==8)strike(target,2,0);}
                if(age>=40){start(Action.IDLE);cooldown=60;}
                break;
            default:
                navigation.stop();if(age<8)face(target,18);
                int hit=action()==Action.STOMP?17:11;
                if(age==hit&&distanceToSqr(target)<Math.pow(kind.width+2.7,2)&&canSee(target))strike(target,(float)kind.damage,kind==ShikigamiKind.ELEPHANT?1.1F:.45F);
                if(age>=(action()==Action.STOMP?38:24)){start(Action.IDLE);cooldown=kind==ShikigamiKind.ELEPHANT?22:8;}
        }
    }
    private void beginRunup(LivingEntity target){
        Vector3d away=position().subtract(target.position()).multiply(1,0,1).normalize();if(away.lengthSqr()<.1)away=getLookAngle().scale(-1);
        runGoal=position().add(away.scale(12));if(runGoal.distanceToSqr(owner().position())>2200)runGoal=owner().position().add(away.scale(8));
        runDistance=0;lastRun=position();start(Action.RUNUP);entityData.set(PREY,target.getId());navigation.moveTo(runGoal.x,runGoal.y,runGoal.z,1.6);
    }
    private void assistOxStep(Vector3d destination,double speed){
        if(kind!=ShikigamiKind.OX||!isOnGround()||(!horizontalCollision&&!navigation.isDone()))return;
        Vector3d direction=destination.subtract(position()).multiply(1,0,1).normalize();if(direction.lengthSqr()<.1)return;
        AxisAlignedBB body=getBoundingBox().deflate(.001),ahead=body.move(direction.scale(.75));
        if(level.noCollision(this,ahead))return;
        // Bypass the path finder's one-block ledge limit only when there is a clear, supported step.
        for(double rise=.5;rise<=maxUpStep;rise+=.5){
            if(!level.noCollision(this,body.expandTowards(0,rise,0)))break;
            AxisAlignedBB landing=ahead.move(0,rise,0);
            if(level.noCollision(this,landing)&&!level.noCollision(this,landing.move(0,-.1,0))){
                faceOxMovement(direction);setDeltaMovement(direction.x*speed,getDeltaMovement().y,direction.z*speed);hasImpulse=true;return;
            }
        }
    }
    private static float movementYaw(Vector3d direction){return (float)(MathHelper.atan2(direction.z,direction.x)*180/Math.PI)-90;}
    private void faceOxMovement(Vector3d direction){yRot=yBodyRot=yHeadRot=movementYaw(direction);}
    @Override protected float tickHeadTurn(float yaw,float movement){
        if(kind==ShikigamiKind.OX&&action()==Action.CHARGE){
            // Keep client body smoothing from turning the rushing model away from its travel direction.
            yBodyRot=yRot;LivingEntity target=prey();
            yHeadRot=target==null?yRot:yRot+MathHelper.clamp(MathHelper.wrapDegrees(movementYaw(target.position().subtract(position()))-yRot),-25,25);
            return movement;
        }
        return super.tickHeadTurn(yaw,movement);
    }
    private void strike(LivingEntity target,float damage,float knockback){
        if(!validEnemy(target))return;
        ModDamageSource source=new AbilityDamageSource(kind.id,this,ShikigamiAbility.core(kind));source.setSourceTypes(new ArrayList<>(Arrays.asList(SourceType.PHYSICAL)));
        if(kind==ShikigamiKind.OX){
            HakiDataCapability.get(this).setBusoshokuHakiExp(CommonConfig.INSTANCE.getHakiExpLimit());
            damage=MahoragaHaki.infuse(this,source,damage);
        }else source.setInternal().setHakiNature(SourceHakiNature.SPECIAL);
        if(target.hurt(source,damage)&&knockback>0){Vector3d d=target.position().subtract(position());target.knockback(knockback,-d.x,-d.z);}
        playSound(kind==ShikigamiKind.TOTALITY?SoundEvents.PLAYER_ATTACK_SWEEP:SoundEvents.RAVAGER_ATTACK,.65F,.85F);
    }
    private void slam(){
        for(LivingEntity target:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(16,8,16)))
            if(validEnemy(target)&&target.position().distanceToSqr(position())<=256){strike(target,200,2.2F);target.setDeltaMovement(target.getDeltaMovement().add(0,.65,0));target.hurtMarked=true;}
        playSound(SoundEvents.GENERIC_EXPLODE,3,.65F);level.broadcastEntityEvent(this,(byte)61);
    }
    private void face(LivingEntity target,float speed){Vector3d d=target.position().subtract(position());float angle=(float)(MathHelper.atan2(d.z,d.x)*180/Math.PI)-90;yRot=MathHelper.rotateIfNecessary(yRot,angle,speed);yBodyRot=yHeadRot=yRot;getLookControl().setLookAt(target,25,20);}
    private void returnToOwner(LivingEntity owner){
        navigation.stop();setTarget(null);for(int i=0;i<16;i++){
            BlockPos top=new BlockPos(owner.position().add(Math.cos(i)*5,5,Math.sin(i)*5));
            for(int d=0;d<14;d++){BlockPos ground=top.below(d);if(!level.hasChunkAt(ground)||!level.getBlockState(ground).isFaceSturdy(level,ground,Direction.UP))continue;
                Vector3d at=new Vector3d(ground.getX()+.5,ground.getY()+1.01,ground.getZ()+.5);if(!level.noCollision(this,getBoundingBox().move(at.subtract(position()))))continue;
                setPos(at.x,at.y,at.z);setDeltaMovement(Vector3d.ZERO);start(Action.IDLE);return;}}
        preserveFor(owner);remove();
    }
    public void preserveFor(LivingEntity owner){if(level.isClientSide||!isAlive()||isTaming())return;CompoundNBT n=new CompoundNBT();n.putFloat("Health",getHealth());n.putFloat("MaxHealth",getMaxHealth());n.putLong("LastHeal",level.getGameTime());owner.getPersistentData().put(kind.reserveTag(),n);}
    public void restoreFor(LivingEntity owner){if(isTaming())return;CompoundNBT n=owner.getPersistentData().getCompound(kind.reserveTag());if(n.contains("Health"))setHealth(MathHelper.clamp(n.getFloat("Health"),1,getMaxHealth()));}
    @Override public void remove(){if(!level.isClientSide){LivingEntity owner=owner();if(owner!=null&&owner.getPersistentData().hasUUID(kind.summonTag())&&getUUID().equals(owner.getPersistentData().getUUID(kind.summonTag())))owner.getPersistentData().remove(kind.summonTag());}super.remove();}
    @Override public void die(DamageSource source){super.die(source);if(!level.isClientSide&&dead)TenShadowsTaming.died(this,source);}
    @Override public boolean causeFallDamage(float distance,float multiplier){return false;}
    @Override public AxisAlignedBB getBoundingBoxForCulling(){return getBoundingBox().inflate(kind==ShikigamiKind.TOAD?26:5);}
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean hit){}
    @Override public IPacket<?> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
