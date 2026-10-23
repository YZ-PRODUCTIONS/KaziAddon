package net.kazi.kazimod.abilities.DokuRework;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.MrMagicalCart.cartaddon.init.CartMorphs;
import net.kazi.kazimod.doku.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.*;
import xyz.pixelatedw.mineminenomi.api.abilities.components.*;
import xyz.pixelatedw.mineminenomi.api.damagesource.*;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.protection.DefaultProtectionRules;
import xyz.pixelatedw.mineminenomi.data.entity.ability.*;
import xyz.pixelatedw.mineminenomi.init.*;
import xyz.pixelatedw.mineminenomi.packets.server.SSyncAbilityDataPacket;
import xyz.pixelatedw.mineminenomi.wypi.WyNetwork;

public class NewVenomRoadRework extends Ability {
    private static final ITextComponent[] DESCRIPTION=AbilityHelper.registerDescriptionText("kazimod","venom_road",new Pair[]{ImmutablePair.of("Ride a flowing road of venom. Steer freely with your aim and movement keys, sprint to accelerate, and use again to cancel.",null)});
    public static final int MIN_COOLDOWN=100,MAX_COOLDOWN=500,DURATION=500;
    public static final AbilityCore<NewVenomRoadRework> INSTANCE=new AbilityCore.Builder<>("Venom Road",AbilityCategory.DEVIL_FRUITS,NewVenomRoadRework::new)
            .addDescriptionLine(DESCRIPTION).addAdvancedDescriptionLine(AbilityDescriptionLine.NEW_LINE,CooldownComponent.getTooltip(MIN_COOLDOWN,MAX_COOLDOWN),ContinuousComponent.getTooltip(DURATION),DealDamageComponent.getTooltip(8,15))
            .setSourceHakiNature(SourceHakiNature.SPECIAL).setSourceElement(SourceElement.POISON).build();
    private final AltModeComponent<Mode> altModeComponent=new AltModeComponent<>(this,Mode.class,Mode.NORMAL,true).addChangeModeEvent(this::onAltModeChange);
    private final AnimationComponent animationComponent=new AnimationComponent(this);
    private final ContinuousComponent continuousComponent=new ContinuousComponent(this,true).addStartEvent(this::start).addTickEvent(this::onContinuityTick).addEndEvent(this::onContinuityEnd);
    private final DealDamageComponent damage=new DealDamageComponent(this);
    private final RangeComponent range=new RangeComponent(this);
    private final Set<UUID> hit=new HashSet<>();
    private DokuGasEntity road;
    public NewVenomRoadRework(AbilityCore<NewVenomRoadRework> core){
        super(core);isNew=true;addComponents(altModeComponent,animationComponent,continuousComponent,damage,range);
        addCanUseCheck((user,a)->continuousComponent.isContinuous()||user instanceof PlayerEntity&&AbilityHelper.canUseMomentumAbilities(user)?AbilityUseResult.success():AbilityUseResult.fail(null));
        addUseEvent((user,a)->continuousComponent.triggerContinuity(user,DURATION));
        addRemoveEvent((user,a)->{if(continuousComponent.isContinuous())continuousComponent.stopContinuity(user);});
    }
    private void start(LivingEntity user,IAbility ability){
        hit.clear();
        if(!user.level.isClientSide){
            IAbilityData data=AbilityDataCapability.get(user);
            if(data.getPassiveAbility(VenomRoadFlightAbility.INSTANCE)==null){
                // Existing Doku saves predate this passive; unlocking must precede equipping.
                data.addUnlockedAbility(VenomRoadFlightAbility.INSTANCE,AbilityUnlock.PROGRESSION);
                if(data.getPassiveAbility(VenomRoadFlightAbility.INSTANCE)==null)data.addPassiveAbility(VenomRoadFlightAbility.INSTANCE.createAbility());
                WyNetwork.sendToAllTrackingAndSelf(new SSyncAbilityDataPacket(user.getId(),data),user);
            }
            animationComponent.start(user,ModAnimations.SHOOT_SELF_FORWARD);
        }
    }
    public void onContinuityTick(LivingEntity user,IAbility ability){
        if(!VenomRoadFlightAbility.allowed(user)){continuousComponent.stopContinuity(user);return;}
        user.fallDistance=0;if(user.level.isClientSide)return;
        boolean demon=CartMorphs.VENOM_DEMON2.get().isActive(user);
        road=DokuGasEntity.refresh(road,user,DokuGasEntity.ROAD,demon?2.5F:1.8F,demon,true);
        for(LivingEntity target:range.getTargetsInArea(user,2.5F)){
            if(!user.getBoundingBox().inflate(.8).intersects(target.getBoundingBox())||!hit.add(target.getUUID()))continue;
            if(damage.hurtTarget(user,target,demon?15:8)){
                target.addEffect(new EffectInstance(ModEffects.DOKU_POISON.get(),300,demon?3:1));
                target.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN,400,demon?2:1));
                target.addEffect(new EffectInstance(Effects.DIG_SLOWDOWN,400,demon?2:1));
                target.addEffect(new EffectInstance(Effects.CONFUSION,400,demon?2:1));
            }
        }
        if(user.tickCount%5==0){BlockPos pos=user.blockPosition();if(user.level.getBlockState(pos.below()).getMaterial().isSolid())AbilityHelper.placeBlockIfAllowed(user,pos,(demon?ModBlocks.DEMON_POISON:ModBlocks.POISON).get().defaultBlockState(),DefaultProtectionRules.AIR_FOLIAGE);}
    }
    public void onContinuityEnd(LivingEntity user,IAbility ability){
        if(road!=null)road.finish();road=null;hit.clear();
        IAbility flight=AbilityDataCapability.get(user).getPassiveAbility(VenomRoadFlightAbility.INSTANCE);
        if(flight instanceof VenomRoadFlightAbility&&user instanceof PlayerEntity)((VenomRoadFlightAbility)flight).release((PlayerEntity)user);
        if(!user.level.isClientSide){
            float flightTime=Math.max(0,Math.min(DURATION,continuousComponent.getContinueTime()));
            float cooldown=MIN_COOLDOWN+(MAX_COOLDOWN-MIN_COOLDOWN)*flightTime/DURATION;
            cooldownComponent.startCooldown(user,cooldown);animationComponent.stop(user);
        }
    }
    public static boolean active(LivingEntity user){IAbility a=AbilityDataCapability.get(user).getEquippedAbility(INSTANCE);return a instanceof NewVenomRoadRework&&((NewVenomRoadRework)a).continuousComponent.isContinuous();}
    public void setNormalMode(LivingEntity user){altModeComponent.setMode(user,Mode.NORMAL);}
    public void setVenomMode(LivingEntity user){altModeComponent.setMode(user,Mode.VENOM);}
    private void onAltModeChange(LivingEntity user,IAbility ability,Mode mode){boolean demon=mode==Mode.VENOM;setDisplayName(new TranslationTextComponent(demon?"ability.mineminenomi.venom_road_venom":"ability.mineminenomi.venom_road"));setDisplayIcon(new ResourceLocation("mineminenomi","textures/abilities/venom_road"+(demon?"_venom":"")+".png"));}
    private enum Mode{NORMAL,VENOM}
}
