package net.kazi.kazimod.longbob;

import java.util.*;
import net.kazi.kazimod.doku.DokuGasEntity;
import net.kazi.kazimod.mahoraga.MahoragaHaki;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.*;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.*;
import net.minecraft.potion.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.*;
import net.minecraft.world.server.*;
import net.minecraftforge.fml.network.NetworkHooks;
import xyz.pixelatedw.mineminenomi.abilities.rokushiki.GeppoAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;
import xyz.pixelatedw.mineminenomi.api.entities.GoalUtil;
import xyz.pixelatedw.mineminenomi.api.helpers.MobsHelper;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.entitystats.EntityStatsCapability;
import xyz.pixelatedw.mineminenomi.data.entity.haki.HakiDataCapability;
import xyz.pixelatedw.mineminenomi.entities.mobs.OPEntity;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.init.ModEffects;

/** Boss adaptations of Doku's normal techniques. No Venom Demon morph or red poison. */
public final class LongbobEntity extends OPEntity {
    public static final ResourceLocation TEXTURE=new ResourceLocation("kazimod","textures/entities/longbobthegod.png");
    private static final DataParameter<Integer> ACTION=EntityDataManager.defineId(LongbobEntity.class,DataSerializers.INT);
    private static final DataParameter<Integer> START=EntityDataManager.defineId(LongbobEntity.class,DataSerializers.INT);
    private static final DataParameter<Boolean> COATED=EntityDataManager.defineId(LongbobEntity.class,DataSerializers.BOOLEAN);
    private final ServerBossInfo bar=new ServerBossInfo(new TranslationTextComponent("entity.kazimod.longbobthegod"),BossInfo.Color.GREEN,BossInfo.Overlay.PROGRESS);
    private final int[] cooldowns=new int[LongbobAction.values().length];
    private boolean initialized,natural,announced,retaliating;
    private int recovery,coatingTicks,geppoTravelTicks;
    private BlockPos home;
    private DokuGasEntity gas,roadGas;

    public LongbobEntity(EntityType<? extends LongbobEntity> type,World world){
        super(type,world,new ResourceLocation[]{TEXTURE});maxUpStep=1.5F;xpReward=250;setPersistenceRequired();setFear(false);
    }
    public static AttributeModifierMap.MutableAttribute createAttributes(){
        return OPEntity.createAttributes().add(Attributes.MAX_HEALTH,1650).add(Attributes.ATTACK_DAMAGE,30)
                .add(Attributes.MOVEMENT_SPEED,.36).add(Attributes.FOLLOW_RANGE,96)
                .add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.ARMOR,0).add(Attributes.ARMOR_TOUGHNESS,0);
    }
    @Override protected void registerGoals(){goalSelector.addGoal(0,new SwimGoal(this));}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(ACTION,0);entityData.define(START,0);entityData.define(COATED,false);}
    public LongbobAction action(){return LongbobAction.from(entityData.get(ACTION));}
    public float actionAge(float partial){return Math.max(0,(int)level.getGameTime()-entityData.get(START)+partial);}
    public boolean coated(){return entityData.get(COATED);}
    public boolean isNaturalBoss(){return natural;}
    public void setNaturalBoss(){natural=true;home=blockPosition();}
    private void equip(EquipmentSlotType slot,Item item){ItemStack stack=new ItemStack(item);stack.enchant(Enchantments.ALL_DAMAGE_PROTECTION,4);stack.enchant(Enchantments.VANISHING_CURSE,1);stack.getOrCreateTag().putBoolean("Unbreakable",true);setItemSlot(slot,stack);setDropChance(slot,0);}
    private void initialize(boolean fresh){
        float healthFraction=getHealth()/Math.max(1,getMaxHealth());
        double scale=Math.max(.1,CommonConfig.INSTANCE.getDorikiLimit()/10000D);
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(1650*scale);
        if(!fresh)setHealth(getMaxHealth()*healthFraction);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(30);
        EntityStatsCapability.get(this).setDoriki(50000*scale);EntityStatsCapability.get(this).setHeart(false);
        HakiDataCapability.get(this).setBusoshokuHakiExp(CommonConfig.INSTANCE.getHakiExpLimit());
        if(AbilityDataCapability.get(this).getEquippedAbility(GeppoAbility.INSTANCE)==null)
            MobsHelper.unlockAndEquipAbility(this,GeppoAbility.INSTANCE);
        equip(EquipmentSlotType.HEAD,Items.NETHERITE_HELMET);equip(EquipmentSlotType.CHEST,Items.NETHERITE_CHESTPLATE);
        equip(EquipmentSlotType.LEGS,Items.NETHERITE_LEGGINGS);equip(EquipmentSlotType.FEET,Items.NETHERITE_BOOTS);
        if(fresh){setHealth(getMaxHealth());home=blockPosition();begin(LongbobAction.ARRIVAL);}
        initialized=true;
    }
    public boolean enemy(LivingEntity target){return target!=null&&target!=this&&target.isAlive()&&target.level==level&&!isAlliedTo(target)
            &&(!(target instanceof PlayerEntity)||!((PlayerEntity)target).isCreative()&&!target.isSpectator());}
    @Override public boolean isAlliedTo(Entity other){return other instanceof LongbobEntity||super.isAlliedTo(other);}
    private void begin(LongbobAction next){
        if(action()==LongbobAction.ROAD){setNoGravity(false);if(roadGas!=null)roadGas.finish();roadGas=null;}
        if(action()==LongbobAction.GUMO){if(gas!=null)gas.finish();gas=null;}
        entityData.set(ACTION,next.ordinal());entityData.set(START,(int)level.getGameTime());navigation.stop();geppoTravelTicks=0;
        if(next==LongbobAction.HYDRA||next==LongbobAction.CHLORO)playSound(SoundEvents.SLIME_SQUISH,.9F,.65F);
        if(next==LongbobAction.GUMO)playSound(SoundEvents.FIRE_EXTINGUISH,1,.5F);
    }
    private void face(LivingEntity target,float speed){
        Vector3d d=target.getBoundingBox().getCenter().subtract(getEyePosition(1));
        yRot=MathHelper.rotateIfNecessary(yRot,(float)(MathHelper.atan2(d.z,d.x)*180/Math.PI)-90,speed);
        yBodyRot=yHeadRot=yRot;xRot=MathHelper.clamp((float)(-MathHelper.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))*180/Math.PI),-70,70);
    }
    private void use(LongbobAction action,int cooldown){cooldowns[action.ordinal()]=cooldown;begin(action);}
    @Override public void aiStep(){
        super.aiStep();if(level.isClientSide||!isAlive())return;
        if(!initialized)initialize(true);
        if(!announced){LongbobSpawning.announce(this);announced=true;}
        if(natural){LongbobSpawning.get((ServerWorld)level).track(this);if(level.getDifficulty()==Difficulty.PEACEFUL){LongbobSpawning.get((ServerWorld)level).defeated(this);remove();return;}}
        bar.setPercent(MathHelper.clamp(getHealth()/getMaxHealth(),0,1));
        if(recovery>0)recovery--;for(int i=0;i<cooldowns.length;i++)if(cooldowns[i]>0)cooldowns[i]--;
        if(coatingTicks>0)coatingTicks--;entityData.set(COATED,coatingTicks>0);
        if(coatingTicks>0)addEffect(new EffectInstance(ModEffects.PHYSICAL_MOVING_GUARD.get(),2,2,false,false));
        LivingEntity target=getTarget();
        if(!enemy(target)||distanceToSqr(target)>144*144){setTarget(null);target=null;}
        if(target==null&&tickCount%10==0){target=level.getEntitiesOfClass(PlayerEntity.class,getBoundingBox().inflate(64),this::enemy).stream()
                .filter(this::canSee).min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);setTarget(target);}
        if(!GoalUtil.canMove(this)){if(action()==LongbobAction.ROAD)begin(LongbobAction.IDLE);return;}
        if(home!=null&&distanceToSqr(Vector3d.atCenterOf(home))>160*160){
            setTarget(null);if(action()!=LongbobAction.IDLE)begin(LongbobAction.IDLE);
            if(pursueWithGeppo(Vector3d.atBottomCenterOf(home)))return;
            if(tickCount%10==0)navigation.moveTo(home.getX()+.5,home.getY(),home.getZ()+.5,1.25);return;
        }
        if(action()!=LongbobAction.IDLE){tickAction(target);return;}
        if(target==null){
            if(home!=null&&(getY()<home.getY()-3||!isOnGround()&&distanceToSqr(Vector3d.atBottomCenterOf(home))>16)
                    &&pursueWithGeppo(Vector3d.atBottomCenterOf(home)))return;
            geppoTravelTicks=0;navigation.stop();return;
        }
        face(target,18);double distance=distanceTo(target);
        if(geppoTravelTicks>0&&pursueWithGeppo(target.position()))return;
        if(recovery==0&&(distance>18||target.getY()>getY()+4||horizontalCollision)&&cooldowns[LongbobAction.ROAD.ordinal()]==0){use(LongbobAction.ROAD,240);return;}
        if(pursueWithGeppo(target.position()))return;
        if(recovery==0){
            if(canSee(target)){
                if(distance<8&&!coated()&&cooldowns[LongbobAction.FUGU.ordinal()]==0){use(LongbobAction.FUGU,360);return;}
                if(distance<10&&cooldowns[LongbobAction.GUMO.ordinal()]==0){use(LongbobAction.GUMO,420);return;}
                if(distance>6&&cooldowns[LongbobAction.HYDRA.ordinal()]==0){use(LongbobAction.HYDRA,160);return;}
                if(distance>4&&cooldowns[LongbobAction.CHLORO.ordinal()]==0){use(LongbobAction.CHLORO,140);return;}
                if(distance<4){begin(LongbobAction.MELEE);return;}
            }
        }
        if(tickCount%6==0)navigation.moveTo(target,1.2);
    }
    private boolean pursueWithGeppo(Vector3d destination){
        if(!GoalUtil.canMove(this)||isPassenger()){geppoTravelTicks=0;return false;}
        Vector3d delta=destination.subtract(position());
        double horizontal=Math.sqrt(delta.x*delta.x+delta.z*delta.z);
        boolean climbing=delta.y>3||horizontalCollision;
        if(!climbing&&horizontal<(isOnGround()?12:4.5)){geppoTravelTicks=0;return false;}
        // Let each air kick carry him before a cast can brake its momentum.
        if(geppoTravelTicks>0){geppoTravelTicks--;navigation.stop();return true;}
        IAbility geppo=AbilityDataCapability.get(this).getEquippedAbility(GeppoAbility.INSTANCE);
        if(geppo==null||!geppo.canUse(this).isSuccess())return false;
        AxisAlignedBB bounds=getBoundingBox().deflate(.05);
        if(!level.noCollision(this,bounds.expandTowards(0,.6,0)))return false;
        Vector3d direction=delta.multiply(1,0,1).normalize();
        boolean wall=!level.noCollision(this,bounds.expandTowards(direction.scale(1.8)));
        // Clear the rim vertically before applying forward movement.
        double speed=wall?0:Math.min(1.2,horizontal/8);
        double lift=climbing||wall?(isOnGround()?1.65:1.25):MathHelper.clamp(delta.y*.12+.45,-.25,.8);
        geppo.use(this);
        setDeltaMovement(direction.x*speed,lift,direction.z*speed);hasImpulse=true;
        navigation.stop();geppoTravelTicks=12;return true;
    }
    private void tickAction(LivingEntity target){
        LongbobAction action=action();int age=(int)actionAge(0);
        if(target!=null)face(target,action==LongbobAction.ROAD?25:14);
        if(action!=LongbobAction.ROAD&&action!=LongbobAction.GUMO){navigation.stop();setDeltaMovement(getDeltaMovement().multiply(.55,1,.55));}
        switch(action){
            case ARRIVAL:
                if(age<35)gas=DokuGasEntity.refresh(gas,this,DokuGasEntity.BURST,2.5F,false,false).green(true);
                else if(gas!=null){gas.finish();gas=null;}break;
            case MELEE:
                if((age==12||age==22)&&enemy(target)&&distanceToSqr(target)<25&&canSee(target)){
                    target.invulnerableTime=0;hit(target,age==22?18:12,"paw_strike",false);
                    Vector3d d=target.position().subtract(position());target.knockback(age==22?1.2F:.2F,-d.x,-d.z);
                    playSound(SoundEvents.PLAYER_ATTACK_STRONG,.9F,.8F);
                }break;
            case HYDRA:if(age==24||age==36||age==48)shoot(target,LongbobProjectile.HYDRA,age/12-3);break;
            case CHLORO:if(age==22||age==30)shoot(target,LongbobProjectile.CHLORO,age==22?-1:1);break;
            case GUMO:
                if(age>=24){
                    gas=DokuGasEntity.refresh(gas,this,DokuGasEntity.CLOUD,9,false,true).green(true);
                    if(target!=null&&tickCount%10==0)navigation.moveTo(target,.65);
                    if(age%20==4)poisonArea(position().add(0,1,0),9,5,"doku_gumo");
                }break;
            case FUGU:if(age==18){coatingTicks=200;entityData.set(COATED,true);playSound(SoundEvents.SLIME_SQUISH,.9F,.65F);}break;
            case ROAD:
                navigation.stop();
                if(target==null){begin(LongbobAction.IDLE);recovery=20;return;}
                if(age>=16&&age<86){
                    setNoGravity(true);Vector3d d=target.position().add(0,1.2,0).subtract(position());
                    Vector3d desired=d.normalize().scale(Math.min(1.25,d.length()*.13));
                    if(isOnGround()||horizontalCollision)desired=new Vector3d(desired.x,Math.max(.65,desired.y),desired.z);
                    Vector3d motion=getDeltaMovement().scale(.7).add(desired.scale(.3));
                    setDeltaMovement(motion);hasImpulse=true;fallDistance=0;
                    roadGas=DokuGasEntity.refresh(roadGas,this,DokuGasEntity.ROAD,1.8F,false,true).green(true);
                    if(age%20==0)poisonArea(position(),2.5F,7,"venom_road");
                }else{setNoGravity(false);if(roadGas!=null){roadGas.finish();roadGas=null;}}
                break;
            default:break;
        }
        if(age>=action.ticks){begin(LongbobAction.IDLE);recovery=18;}
    }
    private void shoot(LivingEntity target,int kind,int side){
        if(!enemy(target)||!canSee(target))return;
        Vector3d forward=target.getBoundingBox().getCenter().subtract(getEyePosition(1)).normalize();
        Vector3d right=new Vector3d(-forward.z,0,forward.x);
        Vector3d muzzle=getEyePosition(1).add(forward.scale(2)).add(right.scale(side*.65));
        if(level.clip(new RayTraceContext(getEyePosition(1),muzzle,RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,this)).getType()!=RayTraceResult.Type.MISS)muzzle=getEyePosition(1);
        LongbobProjectile shot=new LongbobProjectile(this,kind);shot.setPos(muzzle.x,muzzle.y,muzzle.z);
        Vector3d aim=target.getBoundingBox().getCenter().add(target.getDeltaMovement().scale(3));
        shot.setDeltaMovement(aim.subtract(muzzle).normalize().scale(kind==LongbobProjectile.HYDRA?1.4:.95));level.addFreshEntity(shot);
        playSound(SoundEvents.SLIME_JUMP,1,.7F);
    }
    public ModDamageSource source(String attack,boolean projectile){
        ModDamageSource source=new ModDamageSource("longbob_"+attack).causeEntityDamageFromSource(this);
        source.setSourceElement(SourceElement.POISON).setSourceTypes(new ArrayList<>(Arrays.asList(projectile?SourceType.PROJECTILE:SourceType.FIST,SourceType.PHYSICAL)));
        return source;
    }
    public float infused(ModDamageSource source,float amount){return 2*MahoragaHaki.infuse(this,source,amount);}
    private void hit(LivingEntity target,float amount,String name,boolean projectile){ModDamageSource source=source(name,projectile);if(target.hurt(source,infused(source,amount)))poison(target,160,0);}
    // Doku poison deals 10 + amplifier damage; this doubles only this boss's poison.
    public static void poison(LivingEntity target,int ticks,int amplifier){target.addEffect(new EffectInstance(ModEffects.DOKU_POISON.get(),ticks,10+2*amplifier,false,false));}
    private void poisonArea(Vector3d at,float radius,float amount,String name){
        for(LivingEntity target:level.getEntitiesOfClass(LivingEntity.class,new AxisAlignedBB(at,at).inflate(radius),this::enemy)){
            if(target.getBoundingBox().getCenter().distanceToSqr(at)>radius*radius||!canSee(target))continue;
            hit(target,amount,name,true);
        }
    }
    @Override public boolean canBeAffected(EffectInstance effect){return effect.getEffect()!=Effects.POISON&&effect.getEffect()!=ModEffects.DOKU_POISON.get()&&super.canBeAffected(effect);}
    @Override public boolean hurt(DamageSource source,float amount){
        if(source==ModDamageSource.POISON||source instanceof ModDamageSource&&((ModDamageSource)source).getElement()==SourceElement.POISON)return false;
        if(level.isClientSide)return super.hurt(source,amount);
        if(!initialized)initialize(true);
        LivingEntity attacker=source.getEntity() instanceof LivingEntity?(LivingEntity)source.getEntity():null;
        if(enemy(attacker))setTarget(attacker);
        boolean damaged=super.hurt(source,amount);
        if(damaged&&coated()&&!retaliating&&enemy(attacker)&&source.getDirectEntity()==attacker){
            retaliating=true;try{hit(attacker,5,"doku_fugu",false);}finally{retaliating=false;}
        }
        return damaged;
    }
    @Override public boolean doHurtTarget(Entity target){if(target instanceof LivingEntity&&enemy((LivingEntity)target)){hit((LivingEntity)target,15,"paw_strike",false);return true;}return false;}
    @Override public void die(DamageSource source){super.die(source);bar.removeAllPlayers();finishGas();if(!level.isClientSide&&natural)LongbobSpawning.get((ServerWorld)level).defeated(this);}
    private void finishGas(){setNoGravity(false);if(gas!=null)gas.finish();if(roadGas!=null)roadGas.finish();gas=roadGas=null;}
    @Override public void remove(){finishGas();super.remove();}
    @Override public void startSeenByPlayer(ServerPlayerEntity player){super.startSeenByPlayer(player);bar.addPlayer(player);}
    @Override public void stopSeenByPlayer(ServerPlayerEntity player){super.stopSeenByPlayer(player);bar.removePlayer(player);}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public boolean canChangeDimensions(){return false;}
    @Override public boolean causeFallDamage(float distance,float multiplier){return false;}
    @Override protected boolean canRide(Entity entity){return false;}
    @Override public AxisAlignedBB getBoundingBoxForCulling(){return getBoundingBox().inflate(3);}
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit){if(recentlyHit)spawnAtLocation(net.kazi.kazimod.init.KaziItems2.AWAKENING_ESSENCE.get());}
    @Override public void addAdditionalSaveData(CompoundNBT n){super.addAdditionalSaveData(n);n.putBoolean("LongbobInitialized",initialized);n.putBoolean("NaturalLongbob",natural);n.putBoolean("Announced",announced);n.putInt("Coating",coatingTicks);n.putIntArray("TechniqueCooldowns",cooldowns);if(home!=null)n.putLong("Home",home.asLong());}
    @Override public void readAdditionalSaveData(CompoundNBT n){
        super.readAdditionalSaveData(n);natural=n.getBoolean("NaturalLongbob");announced=n.getBoolean("Announced");home=n.contains("Home")?BlockPos.of(n.getLong("Home")):null;
        coatingTicks=MathHelper.clamp(n.getInt("Coating"),0,200);int[] saved=n.getIntArray("TechniqueCooldowns");for(int i=0;i<Math.min(saved.length,cooldowns.length);i++)cooldowns[i]=MathHelper.clamp(saved[i],0,600);
        if(!level.isClientSide){initialized=n.getBoolean("LongbobInitialized");if(initialized){initialize(false);begin(LongbobAction.IDLE);recovery=30;}else home=null;}
        setNoGravity(false);
    }
    @Override public IPacket<?> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
