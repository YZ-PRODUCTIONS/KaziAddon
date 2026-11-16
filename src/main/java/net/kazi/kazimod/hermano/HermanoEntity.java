package net.kazi.kazimod.hermano;

import java.util.*;
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
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.*;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.ServerBossInfo;
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
import xyz.pixelatedw.mineminenomi.init.*;

public final class HermanoEntity extends OPEntity {
    public static final ResourceLocation TEXTURE=new ResourceLocation("kazimod","textures/entities/el_hermano.png");
    private static final DataParameter<Integer> ACTION=EntityDataManager.defineId(HermanoEntity.class,DataSerializers.INT);
    private static final DataParameter<Integer> START=EntityDataManager.defineId(HermanoEntity.class,DataSerializers.INT);
    private static final DataParameter<Boolean> POWER=EntityDataManager.defineId(HermanoEntity.class,DataSerializers.BOOLEAN);
    private final ServerBossInfo bar=new ServerBossInfo(new TranslationTextComponent("entity.kazimod.el_hermano"),BossInfo.Color.RED,BossInfo.Overlay.PROGRESS);
    private boolean initialized,natural,announced,counterTriggered;
    private int recovery,specialCooldown,counterCooldown,geppoTravelTicks;
    private BlockPos home;
    private Vector3d rushDirection=Vector3d.ZERO;
    private final Set<UUID> rushHit=new HashSet<>();

    public HermanoEntity(EntityType<? extends HermanoEntity> type,World world){
        super(type,world,new ResourceLocation[]{TEXTURE});maxUpStep=1.6F;xpReward=250;setPersistenceRequired();setFear(false);
    }
    public static AttributeModifierMap.MutableAttribute createAttributes(){
        return OPEntity.createAttributes().add(Attributes.MAX_HEALTH,1650).add(Attributes.ATTACK_DAMAGE,30)
                .add(Attributes.MOVEMENT_SPEED,.36).add(Attributes.FOLLOW_RANGE,96)
                .add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.ARMOR,0).add(Attributes.ARMOR_TOUGHNESS,0);
    }
    @Override protected void registerGoals(){goalSelector.addGoal(0,new SwimGoal(this));}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(ACTION,0);entityData.define(START,0);entityData.define(POWER,false);}
    public HermanoAction action(){return HermanoAction.from(entityData.get(ACTION));}
    public float actionAge(float partial){return Math.max(0,(int)level.getGameTime()-entityData.get(START)+partial);}
    public boolean fullPower(){return entityData.get(POWER);}
    public boolean isNaturalBoss(){return natural;}
    public void setNaturalBoss(){natural=true;home=blockPosition();}
    private void equip(EquipmentSlotType slot,Item item){ItemStack stack=new ItemStack(item);stack.enchant(Enchantments.ALL_DAMAGE_PROTECTION,4);stack.enchant(Enchantments.VANISHING_CURSE,1);stack.getOrCreateTag().putBoolean("Unbreakable",true);setItemSlot(slot,stack);setDropChance(slot,0);}
    private void initialize(boolean fresh){
        float healthFraction=getHealth()/Math.max(1,getMaxHealth());
        double scale=CommonConfig.INSTANCE.getDorikiLimit()/10000D;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(1650*Math.max(.1,scale));
        if(!fresh)setHealth(getMaxHealth()*healthFraction);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(30);
        EntityStatsCapability.get(this).setDoriki(50000*Math.max(.1,scale));
        EntityStatsCapability.get(this).setHeart(false);
        HakiDataCapability.get(this).setBusoshokuHakiExp(CommonConfig.INSTANCE.getHakiExpLimit());
        if(AbilityDataCapability.get(this).getEquippedAbility(GeppoAbility.INSTANCE)==null)
            MobsHelper.unlockAndEquipAbility(this,GeppoAbility.INSTANCE);
        equip(EquipmentSlotType.HEAD,Items.NETHERITE_HELMET);equip(EquipmentSlotType.CHEST,Items.NETHERITE_CHESTPLATE);
        equip(EquipmentSlotType.LEGS,Items.NETHERITE_LEGGINGS);equip(EquipmentSlotType.FEET,Items.NETHERITE_BOOTS);
        if(fresh){setHealth(getMaxHealth());home=blockPosition();begin(HermanoAction.ARRIVAL);}
        initialized=true;
    }
    public boolean enemy(LivingEntity target){return target!=null&&target!=this&&target.isAlive()&&target.level==level
            &&!(target instanceof HermanoEntity)&&!isAlliedTo(target)
            &&(!(target instanceof PlayerEntity)||!((PlayerEntity)target).isCreative()&&!target.isSpectator());}
    private void begin(HermanoAction action){
        entityData.set(ACTION,action.ordinal());entityData.set(START,(int)level.getGameTime());navigation.stop();counterTriggered=false;rushHit.clear();geppoTravelTicks=0;
        if(action==HermanoAction.FULL_POWER){entityData.set(POWER,true);playSound(SoundEvents.ENDER_DRAGON_GROWL,1,.7F);}
        if(action==HermanoAction.IMPACT)playSound(SoundEvents.BEACON_POWER_SELECT,.8F,.6F);
        if(action==HermanoAction.ARRIVAL)playSound(SoundEvents.IRON_GOLEM_REPAIR,1,.6F);
    }
    private void face(LivingEntity target,float speed){
        Vector3d d=target.getBoundingBox().getCenter().subtract(getEyePosition(1));
        yRot=MathHelper.rotateIfNecessary(yRot,(float)(MathHelper.atan2(d.z,d.x)*180/Math.PI)-90,speed);
        yBodyRot=yHeadRot=yRot;xRot=MathHelper.clamp((float)(-MathHelper.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))*180/Math.PI),-65,65);
    }
    @Override public void aiStep(){
        super.aiStep();if(level.isClientSide||!isAlive())return;
        if(!initialized)initialize(true);
        if(!announced){HermanoSpawning.announce(this);announced=true;}
        if(natural){HermanoSpawning.get((ServerWorld)level).track(this);if(level.getDifficulty()==Difficulty.PEACEFUL){HermanoSpawning.get((ServerWorld)level).defeated(this);remove();return;}}
        bar.setPercent(MathHelper.clamp(getHealth()/getMaxHealth(),0,1));
        if(recovery>0)recovery--;if(specialCooldown>0)specialCooldown--;if(counterCooldown>0)counterCooldown--;
        LivingEntity target=getTarget();
        if(!enemy(target)||target.distanceToSqr(this)>144*144){setTarget(null);target=null;}
        if(tickCount%10==0&&target==null){
            target=level.getEntitiesOfClass(PlayerEntity.class,getBoundingBox().inflate(64),this::enemy).stream()
                    .filter(this::canSee).min(Comparator.comparingDouble(this::distanceToSqr)).orElse(null);setTarget(target);
        }
        if(home!=null&&distanceToSqr(Vector3d.atCenterOf(home))>160*160){
            setTarget(null);target=null;if(action()!=HermanoAction.IDLE)begin(HermanoAction.IDLE);
            if(pursueWithGeppo(Vector3d.atBottomCenterOf(home)))return;
            if(tickCount%10==0)navigation.moveTo(home.getX()+.5,home.getY(),home.getZ()+.5,1.25);return;
        }
        if(action()!=HermanoAction.IDLE){tickAction(target);return;}
        if(target==null){
            if(home!=null&&(getY()<home.getY()-3||!isOnGround()&&distanceToSqr(Vector3d.atBottomCenterOf(home))>16)
                    &&pursueWithGeppo(Vector3d.atBottomCenterOf(home)))return;
            geppoTravelTicks=0;if(tickCount%20==0)navigation.stop();return;
        }
        if(!fullPower()&&getHealth()<=getMaxHealth()*.45F){begin(HermanoAction.FULL_POWER);return;}
        face(target,18);double distance=distanceTo(target);
        if(pursueWithGeppo(target.position()))return;
        if(recovery==0&&canSee(target)){
            if(specialCooldown==0&&distance>6){begin(random.nextFloat()<.62F?HermanoAction.IMPACT:HermanoAction.GLARE);specialCooldown=fullPower()?100:160;return;}
            if(distance>4&&distance<24&&Math.abs(target.getY()-getY())<6){begin(HermanoAction.RUSH);return;}
            if(distance<4.4){begin(random.nextFloat()<.18F&&counterCooldown==0?HermanoAction.COUNTER:HermanoAction.COMBO);return;}
        }
        if(tickCount%6==0)navigation.moveTo(target,fullPower()?1.45:1.2);
    }
    private boolean pursueWithGeppo(Vector3d destination){
        if(!GoalUtil.canMove(this)||isPassenger()){geppoTravelTicks=0;return false;}
        Vector3d delta=destination.subtract(position());
        double horizontal=Math.sqrt(delta.x*delta.x+delta.z*delta.z);
        boolean climbing=delta.y>3||horizontalCollision;
        double pursuitRange=isOnGround()?12:4.5;
        if(!climbing&&horizontal<pursuitRange){geppoTravelTicks=0;return false;}
        // Let each air kick carry him before another attack can brake its momentum.
        if(geppoTravelTicks>0){geppoTravelTicks--;navigation.stop();return true;}
        IAbility geppo=AbilityDataCapability.get(this).getEquippedAbility(GeppoAbility.INSTANCE);
        if(geppo==null||!geppo.canUse(this).isSuccess())return false;
        AxisAlignedBB bounds=getBoundingBox().deflate(.05);
        if(!level.noCollision(this,bounds.expandTowards(0,.6,0)))return false;
        Vector3d direction=delta.multiply(1,0,1).normalize();
        boolean wall=!level.noCollision(this,bounds.expandTowards(direction.scale(1.8)));
        // Rise vertically until the entire body clears the rim, then kick toward the target.
        double speed=wall?0:Math.min(fullPower()?1.5:1.2,horizontal/8);
        double lift=climbing||wall?(isOnGround()?1.65:1.25):MathHelper.clamp(delta.y*.12+.45,-.25,.8);
        geppo.use(this);
        setDeltaMovement(direction.x*speed,lift,direction.z*speed);hasImpulse=true;
        navigation.stop();geppoTravelTicks=12;return true;
    }
    private void tickAction(LivingEntity target){
        HermanoAction action=action();int age=(int)actionAge(0);
        navigation.stop();
        if(target!=null&&(action!=HermanoAction.RUSH||age<14)&&(action!=HermanoAction.GLARE||age<16))face(target,action==HermanoAction.COMBO?9:12);
        if(action!=HermanoAction.RUSH)setDeltaMovement(getDeltaMovement().multiply(.55,1,.55));
        switch(action){
            case ARRIVAL:if(age==45)HermanoEffect.spawn(this,HermanoEffect.SHOCK,position(),22,5);break;
            case COMBO:
                if(age==12||age==20||age==29){melee(target,age==29?17:10,age==29?1.8:.15);}
                break;
            case RUSH:
                if(age==14){rushDirection=getLookAngle().normalize();playSound(SoundEvents.PLAYER_ATTACK_SWEEP,1,.5F);}
                if(age>=14&&age<30){
                    setDeltaMovement(rushDirection.scale(fullPower()?1.7:1.35).add(0,isOnGround()?.12:-.04,0));hasImpulse=true;
                    for(LivingEntity victim:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(.9),this::enemy))
                        if(rushHit.add(victim.getUUID())&&canSee(victim))hit(victim,"power_rush",26,1.5,false);
                }else setDeltaMovement(getDeltaMovement().multiply(.5,1,.5));
                break;
            case GLARE:
                if(age==18){
                    Vector3d at=getEyePosition(1),forward=getLookAngle();
                    HermanoEffect.spawn(this,HermanoEffect.GLARE,at,14,18).direction(yRot,xRot);
                    for(LivingEntity victim:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(18),this::enemy)){
                        Vector3d delta=victim.getBoundingBox().getCenter().subtract(at);
                        if(delta.lengthSqr()<324&&delta.normalize().dot(forward)>.88&&canSee(victim))hit(victim,"invisible_pressure",22,2.4,true);
                    }
                    playSound(SoundEvents.GENERIC_EXPLODE,.9F,1.4F);
                }break;
            case IMPACT:
                if(age==40&&target!=null&&canSee(target)){
                    Vector3d aim=target.getBoundingBox().getCenter().add(target.getDeltaMovement().scale(4));
                    double yaw=yRot*Math.PI/180;
                    Vector3d at=position().add(.8*Math.cos(yaw)-1.9*Math.sin(yaw),2.45,.8*Math.sin(yaw)+1.9*Math.cos(yaw));
                    HermanoProjectile shot=new HermanoProjectile(this,fullPower());
                    shot.setPos(at.x,at.y,at.z);shot.setDeltaMovement(aim.subtract(at).normalize().scale(fullPower()?1.7:1.4));level.addFreshEntity(shot);
                    playSound(SoundEvents.FIREWORK_ROCKET_LAUNCH,1,.65F);
                }break;
            case COUNTER:
                if(age==22&&counterTriggered){burst(position().add(0,1,0),5,18,"counter_impact");HermanoEffect.spawn(this,HermanoEffect.SHOCK,position(),20,5);}
                break;
            case FULL_POWER:
                if(age==52){burst(position().add(0,1,0),9,24,"full_power");HermanoEffect.spawn(this,HermanoEffect.BLAST,position().add(0,1,0),30,9);}
                break;
            default:break;
        }
        if(age>=action.ticks){if(action==HermanoAction.COUNTER)counterCooldown=160;begin(HermanoAction.IDLE);recovery=fullPower()?14:24;}
    }
    private void melee(LivingEntity victim,float damage,double knockback){
        if(!enemy(victim)||distanceToSqr(victim)>25||!canSee(victim))return;
        Vector3d d=victim.position().subtract(position()).multiply(1,0,1).normalize();
        if(d.dot(getLookAngle().multiply(1,0,1).normalize())<.25)return;
        // Each separate punch is intentional; area attacks never reset invulnerability on each body part.
        victim.invulnerableTime=0;hit(victim,"heavy_punch",damage,knockback,false);
        HermanoEffect.spawn(this,HermanoEffect.HIT,victim.getBoundingBox().getCenter(),10,1.3F);playSound(SoundEvents.PLAYER_ATTACK_STRONG,1,.7F);
    }
    public float infused(ModDamageSource source,float base){return 2*MahoragaHaki.infuse(this,source,base*(fullPower()?1.2F:1));}
    public ModDamageSource source(String name,boolean energy){
        ModDamageSource source=new ModDamageSource("el_hermano_"+name).causeEntityDamageFromSource(this);
        source.setSourceElement(energy?SourceElement.SHOCKWAVE:SourceElement.NONE);
        source.setSourceTypes(new ArrayList<>(Arrays.asList(energy?SourceType.PROJECTILE:SourceType.FIST,SourceType.PHYSICAL)));
        return source;
    }
    private void hit(LivingEntity target,String attack,float damage,double knockback,boolean energy){
        ModDamageSource source=source(attack,energy);
        if(target.hurt(source,infused(source,damage))){Vector3d d=target.position().subtract(position());target.knockback((float)knockback,-d.x,-d.z);}
    }
    public void burst(Vector3d at,float radius,float damage,String attack){
        ModDamageSource source=source(attack,true).setAreaOfEffect();float power=infused(source,damage);
        for(LivingEntity victim:level.getEntitiesOfClass(LivingEntity.class,new AxisAlignedBB(at,at).inflate(radius),this::enemy)){
            double distance=victim.getBoundingBox().getCenter().distanceTo(at);if(distance>radius)continue;
            if(level.clip(new RayTraceContext(at,victim.getBoundingBox().getCenter(),RayTraceContext.BlockMode.COLLIDER,RayTraceContext.FluidMode.NONE,this)).getType()!=RayTraceResult.Type.MISS)continue;
            if(victim.hurt(source,power*(float)(1-.4*distance/radius))){Vector3d d=victim.position().subtract(at);victim.knockback(2,-d.x,-d.z);}
        }
        playSound(SoundEvents.GENERIC_EXPLODE,1.3F,.65F);
    }
    @Override public boolean hurt(DamageSource source,float amount){
        if(level.isClientSide)return super.hurt(source,amount);
        if(!initialized)initialize(true);
        if(source.getEntity() instanceof HermanoEntity)return false;
        LivingEntity attacker=source.getEntity() instanceof LivingEntity?(LivingEntity)source.getEntity():null;
        if(enemy(attacker))setTarget(attacker);
        if(!source.isBypassInvul()&&attacker!=null){
            if(action()==HermanoAction.IDLE&&counterCooldown==0&&random.nextFloat()<.22F){begin(HermanoAction.COUNTER);counterCooldown=160;}
            if(action()==HermanoAction.COUNTER&&actionAge(0)<22){amount*=.5F;counterTriggered=true;}
        }
        return super.hurt(source,amount);
    }
    @Override public boolean doHurtTarget(Entity entity){if(entity instanceof LivingEntity){melee((LivingEntity)entity,15,1);return true;}return false;}
    @Override public void die(DamageSource source){
        super.die(source);bar.removeAllPlayers();
        if(!level.isClientSide&&natural)HermanoSpawning.get((ServerWorld)level).defeated(this);
    }
    @Override public void startSeenByPlayer(ServerPlayerEntity player){super.startSeenByPlayer(player);bar.addPlayer(player);}
    @Override public void stopSeenByPlayer(ServerPlayerEntity player){super.stopSeenByPlayer(player);bar.removePlayer(player);}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public boolean canChangeDimensions(){return false;}
    @Override public boolean causeFallDamage(float distance,float multiplier){return false;}
    @Override protected boolean canRide(Entity entity){return false;}
    @Override public AxisAlignedBB getBoundingBoxForCulling(){return getBoundingBox().inflate(5);}
    @Override protected void dropCustomDeathLoot(DamageSource source,int looting,boolean recentlyHit){
        if(recentlyHit)spawnAtLocation(net.kazi.kazimod.init.KaziItems2.AWAKENING_ESSENCE.get());
    }
    @Override public void addAdditionalSaveData(CompoundNBT n){super.addAdditionalSaveData(n);n.putBoolean("HermanoInitialized",initialized);n.putBoolean("NaturalHermano",natural);n.putBoolean("Announced",announced);n.putBoolean("FullPower",fullPower());if(home!=null)n.putLong("Home",home.asLong());}
    @Override public void readAdditionalSaveData(CompoundNBT n){
        super.readAdditionalSaveData(n);natural=n.getBoolean("NaturalHermano");announced=n.getBoolean("Announced");home=n.contains("Home")?BlockPos.of(n.getLong("Home")):blockPosition();entityData.set(POWER,n.getBoolean("FullPower"));
        if(!level.isClientSide){
            initialized=n.getBoolean("HermanoInitialized");
            if(initialized){initialize(false);begin(HermanoAction.IDLE);recovery=40;}
            else home=null; // /summon applies its final position after reading the initial NBT.
        }
    }
    @Override public IPacket<?> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
