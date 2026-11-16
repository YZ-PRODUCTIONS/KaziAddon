package net.kazi.kazimod.abilities.YamiRework;

import java.util.List;
import net.kazi.kazimod.yami.*;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import xyz.pixelatedw.mineminenomi.abilities.yami.AbsorbedBlocksAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceType;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.init.ModAnimations;
import xyz.pixelatedw.mineminenomi.init.ModBlocks;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class LiberationRework extends Ability {
    public enum Mode {ABSORB,RELEASE,STORM}
    public static final int MAX_SHOTS=48,DURATION=72,COOLDOWN=240;
    public static final int STORM_DURATION=120,STORM_COOLDOWN=500;
    private static final double STORM_RADIUS=15,STORM_HEIGHT=24;
    public static final AbilityCore<LiberationRework> INSTANCE=new AbilityCore.Builder<LiberationRework>(
            "Liberation Rework",AbilityCategory.DEVIL_FRUITS,LiberationRework::new)
            .addDescriptionLine(new StringTextComponent("Absorb stores your placed darkness. Release fires up to 48 stored blocks through a darkness portal in front of you, following your aim. Storm surrounds you with a tornado that pulls and stuns enemies within 15 blocks for six seconds, with a 25-second cooldown."))
            .addAdvancedDescriptionLine(AbilityDescriptionLine.NEW_LINE,CooldownComponent.getTooltip(80,STORM_COOLDOWN),ContinuousComponent.getTooltip(DURATION,STORM_DURATION))
            .addAdvancedDescriptionLine(ProjectileComponent.getProjectileTooltips())
            .setIcon(new ResourceLocation("mineminenomi","textures/abilities/liberation.png"))
            .setSourceType(SourceType.BLUNT).setSourceHakiNature(SourceHakiNature.IMBUING).build();
    private final AltModeComponent<Mode> mode=new AltModeComponent<Mode>(this,Mode.class,Mode.ABSORB) {
        @Override public void load(CompoundNBT tag) {
            super.load(tag);
            activeMode=getCurrentMode();
            updateDisplay(activeMode);
        }
    }.addChangeModeEvent(this::modeChanged);
    private final ContinuousComponent continuous=new ContinuousComponent(this,true).addStartEvent(this::start).addTickEvent(this::during).addEndEvent(this::end);
    private final AnimationComponent animation=new AnimationComponent(this);
    private final ProjectileComponent projectiles=new ProjectileComponent(this,owner->new LiberationBlockProjectile(owner.level,owner));
    private Mode activeMode=Mode.ABSORB;
    private int elapsed,shots;
    public LiberationRework(AbilityCore<LiberationRework> core) {
        super(core);isNew=true;addComponents(mode,continuous,animation,projectiles);
        addUseEvent(this::useLiberation);
        addCanUseCheck((owner,a)->owner.level.isClientSide||mode.getCurrentMode()!=Mode.RELEASE||continuous.isContinuous()||hasAmmo(owner)
                ?AbilityUseResult.success():AbilityUseResult.fail(new StringTextComponent("Absorb some darkness first.")));
        addRemoveEvent((owner,a)->continuous.stopContinuity(owner));
        addEquipEvent((owner,a)->updateDisplay(mode.getCurrentMode()));
        updateDisplay(mode.getCurrentMode());
    }
    private AbsorbedBlocksAbility storage(LivingEntity owner) {
        return AbilityDataCapability.get(owner).getPassiveAbility(AbsorbedBlocksAbility.INSTANCE);
    }
    private boolean hasAmmo(LivingEntity owner) {
        AbsorbedBlocksAbility store=storage(owner);
        return store!=null&&store.getAbsorbedBlocks().stream().anyMatch(AbsorbedBlocksAbility.BlockData::isCompressed);
    }
    private void modeChanged(LivingEntity owner,IAbility ability,Mode selected) {
        updateDisplay(selected);
    }
    private void updateDisplay(Mode selected) {
        if(selected==Mode.STORM) {
            setDisplayIcon(new ResourceLocation("kazimod","textures/abilities/darkness_storm.png"));
            setDisplayName(new TranslationTextComponent("ability.kazimod.liberation_storm"));
            return;
        }
        setDisplayIcon(new ResourceLocation("mineminenomi","textures/abilities/"+(selected==Mode.RELEASE?"liberation_release":"liberation")+".png"));
        setDisplayName(new TranslationTextComponent("ability.mineminenomi."+(selected==Mode.RELEASE?"liberation_release":"liberation")));
    }
    private void useLiberation(LivingEntity owner,IAbility ability) {
        if(owner.level.isClientSide)return;
        if(continuous.isContinuous()){continuous.stopContinuity(owner);return;}
        if(mode.getCurrentMode()==Mode.STORM){continuous.triggerContinuity(owner,STORM_DURATION);return;}
        if(mode.getCurrentMode()==Mode.RELEASE){continuous.triggerContinuity(owner,DURATION);return;}
        AbsorbedBlocksAbility store=storage(owner);if(store==null)return;
        // Inspect only this user's tracked blocks, avoiding an 81-cubed world scan.
        for(AbsorbedBlocksAbility.BlockData block:store.getAbsorbedBlocks()) {
            BlockPos pos=block.getBlockPos();
            if(block.isCompressed()||pos.distSqr(owner.blockPosition())>40*40||!owner.level.hasChunkAt(pos))continue;
            if(owner.level.getBlockState(pos).getBlock()==ModBlocks.DARKNESS.get()) {
                owner.level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());block.setCompressed(true);
            }
        }
        YamiVisualPacket.send(owner,YamiEffects.ABSORB,owner.position());
        cooldownComponent.startCooldown(owner,80);
    }
    private void start(LivingEntity owner,IAbility ability) {
        activeMode=mode.getCurrentMode();
        elapsed=shots=0;
        if(activeMode==Mode.STORM) {
            if(!owner.level.isClientSide) {
                YamiVisualPacket.send(owner,YamiEffects.TORNADO,owner.position());
                owner.level.playSound(null,owner.blockPosition(),SoundEvents.PORTAL_TRIGGER,SoundCategory.PLAYERS,.45F,.5F);
            }
            return;
        }
        animation.start(owner,ModAnimations.POINT_LEFT_ARM);
        if(!owner.level.isClientSide)YamiVisualPacket.send(owner,YamiEffects.PORTAL,YamiCombat.portal(owner));
    }
    private void during(LivingEntity owner,IAbility ability) {
        if(owner.level.isClientSide)return;
        if(activeMode==Mode.STORM){duringStorm(owner);return;}
        if(!owner.isAlive()||!hasAmmo(owner)||shots>=MAX_SHOTS){continuous.stopContinuity(owner);return;}
        if(elapsed%5==0)YamiVisualPacket.send(owner,YamiEffects.PORTAL,YamiCombat.portal(owner));
        if(elapsed++%3!=0)return;
        AbsorbedBlocksAbility store=storage(owner);
        List<AbsorbedBlocksAbility.BlockData> blocks=store.getAbsorbedBlocks().stream()
                .filter(AbsorbedBlocksAbility.BlockData::isCompressed).limit(2).collect(java.util.stream.Collectors.toList());
        Vector3d forward=owner.getLookAngle(),right=forward.cross(new Vector3d(0,1,0)).normalize();
        if(right.lengthSqr()<.01)right=new Vector3d(1,0,0);
        Vector3d up=right.cross(forward).normalize(),center=YamiCombat.portal(owner);
        Vector3d aim=WyHelper.rayTraceBlocksAndEntities(owner,96).getLocation();
        for(int i=0;i<2&&i<blocks.size()&&shots<MAX_SHOTS;i++) {
            AbsorbedBlocksAbility.BlockData block=blocks.get(i);
            double angle=random.nextDouble()*Math.PI*2,spread=Math.sqrt(random.nextDouble())*1.15;
            Vector3d origin=center.add(right.scale(Math.cos(angle)*spread)).add(up.scale(Math.sin(angle)*spread));
            Vector3d direction=aim.subtract(origin);
            if(direction.dot(forward)<=.1)direction=forward;
            LiberationBlockProjectile shot=(LiberationBlockProjectile)projectiles.getNewProjectile(owner);
            shot.setBlock(block.getBlockState());shot.setPos(origin.x,origin.y,origin.z);
            shot.shoot(direction.x,direction.y,direction.z,2.8F,1.5F);
            if(owner.level.addFreshEntity(shot)){store.removeAbsorbedBlock(block);shots++;}
        }
        if(elapsed%6==1)owner.level.playSound(null,owner.blockPosition(),SoundEvents.STONE_BREAK,SoundCategory.PLAYERS,.5F,.65F);
    }
    private void duringStorm(LivingEntity owner) {
        if(!owner.isAlive()){continuous.stopContinuity(owner);return;}
        Vector3d center=owner.position();
        if(elapsed++%5==0)YamiVisualPacket.send(owner,YamiEffects.TORNADO,center);
        AxisAlignedBB area=new AxisAlignedBB(center.x-STORM_RADIUS,center.y-2,center.z-STORM_RADIUS,
                center.x+STORM_RADIUS,center.y+STORM_HEIGHT,center.z+STORM_RADIUS);
        for(LivingEntity target:owner.level.getEntitiesOfClass(LivingEntity.class,area,t->YamiCombat.enemy(owner,t))) {
            Vector3d offset=center.subtract(target.position());
            double horizontal=offset.x*offset.x+offset.z*offset.z;
            if(horizontal>STORM_RADIUS*STORM_RADIUS||AbilityHelper.isDodging(target))continue;
            YamiCombat.stun(target);
            if(elapsed%4==1)AbilityHelper.disableAbilities(target,6,a->true);
            if(target instanceof MobEntity)((MobEntity)target).getNavigation().stop();
            Vector3d goal=center.add(0,.6,0).subtract(target.position());
            double distance=goal.length();
            Vector3d velocity=distance<1.5?target.getDeltaMovement().scale(.2)
                    :target.getDeltaMovement().scale(.45).add(goal.normalize().scale(Math.min(.85,.25+distance*.045)));
            if(velocity.lengthSqr()>1.44)velocity=velocity.normalize().scale(1.2);
            AbilityHelper.setDeltaMovement(target,velocity);target.hurtMarked=true;target.fallDistance=0;
        }
    }
    private void end(LivingEntity owner,IAbility ability) {
        animation.stop(owner);
        if(!owner.level.isClientSide) {
            YamiVisualPacket.send(owner,activeMode==Mode.STORM?-3:-2,owner.position());
            cooldownComponent.startCooldown(owner,activeMode==Mode.STORM?STORM_COOLDOWN:COOLDOWN);
        }
    }
}
