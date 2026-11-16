import net.kazi.kazimod.entities.asriel.ChaosBeamAttackEntity;
import net.kazi.kazimod.abilities.YagiAwakening.ShockerBreakerAbility;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosBeamHealing;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosBeamRecoil;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosBeamDamage;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.server.ServerWorld;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;

/** Runs the production entity against small deterministic server fixtures. */
public final class ChaosBeamEntityTest {
    private static int checks;
    public static void main(String[] args) {
        Fixture full = new Fixture();
        LivingEntity enemy = full.target(0,20,true), ally = full.target(0,20,false);
        enemy.health=enemy.maxHealth=500;
        LivingEntity enlargedArea = full.target(25,80,true);
        LivingEntity outside = full.target(70,20,true), behind = full.target(0,-10,true);
        for (int age=0;age<40;age++) full.tick(age);
        require(enemy.successfulHits == 0, "Charging orbs must never cause damage");
        full.world.gameTime=40; full.field.beginBeam();
        // Delayed first entity tick still yields twelve distinct pulses, never duplicate hits in a tick.
        for (int age=41;age<280;age++) { full.tick(age); full.field.tick(); }
        require(enemy.successfulHits==12 && enemy.damageAmounts.stream().mapToDouble(Float::doubleValue).sum()==240,
                "Twelve twenty-damage pulses, including a delayed first tick");
        require(enemy.health==260&&enemy.actualDamageAmounts.stream().mapToDouble(Float::doubleValue).sum()==240,
                "Enemies with sufficient health still take all twelve full damage pulses");
        require(ally.successfulHits==0 && behind.successfulHits==0 && outside.successfulHits==0, "Allies and entities outside the cone remain safe");
        require(enlargedArea.successfulHits>0, "The doubled cone hits enemies beyond its previous maximum width at this distance");
        require(full.owner.damageAttempts==0&&full.owner.health==250,
                "The caster takes no self damage during charge or any beam pulse");
        full.tick(280); require(full.field.removed, "Field expires at the end of firing");
        full.field.finishFiring(full.owner);
        require(full.owner.damageAttempts==0,"Completing the beam defers recoil until after the cutscene");
        require(net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsCutscenes.starts == 1,
                "A completed beam starts one finale, even if both cleanup paths run");
        Fixture interrupted = new Fixture();
        interrupted.field.beginBeam(); interrupted.tick(80); interrupted.field.finishFiring(interrupted.owner); interrupted.field.remove();
        require(net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsCutscenes.starts == 1,
                "Stopping a beam early does not start the finale");
        Fixture wall = new Fixture(); wall.world.wallZ=12;
        LivingEntity blocked=wall.target(0,20,true), visible=wall.target(0,8,true);
        wall.field.beginBeam(); wall.tick(0);
        require(blocked.successfulHits==0 && visible.successfulHits==1, "Solid wall clips visual and combat length");
        require(wall.field.getLength()<=10.21, "Synced visual endpoint is the wall");
        Fixture unloaded = new Fixture(); unloaded.world.loadedUntil=16;
        LivingEntity distant=unloaded.target(0,25,true); unloaded.field.beginBeam(); unloaded.tick(0);
        require(distant.successfulHits==0 && unloaded.field.getLength()<16, "No attack through unloaded terrain");
        Fixture death=new Fixture(); death.owner.alive=false; death.tick(1); require(death.field.removed,"Owner death clears field");
        Fixture lost=new Fixture(); AbilityDataCapability.DATA.remove(lost.owner); lost.tick(1); require(lost.field.removed,"Lost ability clears field");
        Fixture disabled=new Fixture(); disabled.ability.enabled=false; disabled.tick(1); require(disabled.field.removed,"Interrupted or disabled ability clears field");
        Fixture waiting=new Fixture(); waiting.tick(45); require(waiting.field.removed,"Abandoned charge cannot leave permanent orbs");
        Fixture palette=new Fixture(); require(palette.field.hasDarkPalette(),"Low-health appearance at spawn");
        palette.owner.health=750; palette.tick(1); require(!palette.field.hasDarkPalette(),"Healing updates the appearance without cancelling the cast");
        palette.owner.health=250; palette.tick(2); require(palette.field.hasDarkPalette(),"Low health restores flowing dark palette");
        require(!palette.field.removed,"Health changes preserve the chosen move");
        Fixture locked = new Fixture();
        locked.owner.yRot=90; locked.owner.xRot=30; locked.tick(1);
        require(locked.owner.yRot==0 && locked.owner.xRot==0 && locked.field.getAttackDirection().z==1,
                "Turning during charge keeps camera and attack on the cast direction");
        locked.world.gameTime=40; locked.field.beginBeam();
        locked.owner.yRot=-120; locked.owner.xRot=-45; locked.owner.setPos(3,0,0); locked.tick(50);
        require(locked.owner.yRot==0 && locked.owner.xRot==0 && locked.field.getAttackDirection().z==1,
                "Turning during firing cannot redirect the beam");
        require(locked.owner.getX()==0 && locked.field.getX()==0,"Caster and muzzle stay at the original cast position");
        LivingEntity other=locked.target(0,10,true); other.yRot=70; locked.field.lockAim(other);
        require(other.yRot==70,"Aim lock must never affect another player");
        locked.tick(280); locked.owner.yRot=80; locked.field.lockAim(locked.owner);
        require(locked.owner.yRot==80 && !locked.field.hasActiveAimLock(),"Expired field releases camera control");
        Fixture vertical = new Fixture(63,-90);
        vertical.owner.yRot=0; vertical.owner.xRot=0; vertical.tick(1);
        require(vertical.owner.yRot==63 && vertical.owner.xRot==-90 && vertical.field.getAttackDirection().y>.999,
                "A straight-up cast preserves yaw as well as pitch");
        Fixture movement=new Fixture();
        require(movement.owner.isNoGravity(),"The charge immediately suspends gravity");
        movement.owner.setPos(4,6,2);movement.owner.setDeltaMovement(new net.minecraft.util.math.vector.Vector3d(1,2,3));
        movement.owner.jumping=movement.owner.sprinting=movement.owner.passenger=true;
        movement.owner.xxa=movement.owner.zza=1;movement.owner.fallDistance=9;movement.tick(20);
        require(movement.owner.position().lengthSqr()==0 && movement.owner.getDeltaMovement().lengthSqr()==0,
                "Charge blocks jumping, motion and external displacement in every axis");
        require(!movement.owner.jumping&&!movement.owner.sprinting&&!movement.owner.passenger
                && movement.owner.xxa==0&&movement.owner.zza==0&&movement.owner.fallDistance==0,
                "Clear movement controls, mounts and accumulated falling");
        int corrections=movement.owner.teleports;movement.tick(21);
        require(movement.owner.teleports==corrections,"A stationary caster does not get teleported every tick");
        movement.world.gameTime=40;movement.field.beginBeam();movement.owner.setPos(1,-2,4);movement.tick(140);
        require(movement.owner.position().lengthSqr()==0&&movement.owner.isNoGravity(),"Firing keeps the same position lock");
        movement.tick(280);require(!movement.owner.isNoGravity(),"Normal completion restores gravity");
        movement.owner.setPos(7,3,2);movement.field.lockMovement(movement.owner);
        require(movement.owner.getX()==7,"An expired field cannot pull the player back");
        Fixture cancelled=new Fixture();cancelled.field.remove();
        require(!cancelled.owner.isNoGravity(),"Cancellation immediately restores gravity");
        require(!death.owner.isNoGravity()&&!lost.owner.isNoGravity()&&!disabled.owner.isNoGravity(),
                "Death, lost ability and disabled ability all release gravity");
        ServerWorld floatingWorld=new ServerWorld();LivingEntity floating=new LivingEntity(floatingWorld);floating.setNoGravity(true);
        ChaosBeamAttackEntity floatingField=ChaosBeamAttackEntity.spawn(floating);floatingField.remove();
        require(floating.isNoGravity(),"Preserve pre-existing no-gravity from another source");
        Fixture recovered=new Fixture();ChaosBeamAttackEntity.clearMovementLock(recovered.owner);
        require(!recovered.owner.isNoGravity(),"Recovery clears saved Chaos Beam gravity lock");
        Fixture collateral=new Fixture();LivingEntity unaffected=collateral.target(5,12,true);
        collateral.field.lockMovement(unaffected);
        require(unaffected.getX()==5&&!unaffected.isNoGravity(),"Movement lock only affects the caster");
        healing();
        recoil();
        nonlethal();
        healthWriteFloor();
        System.out.println("Chaos Beam production entity: "+checks+" scenarios passed.");
    }

    private static void healthWriteFloor() {
        Fixture cast=new Fixture();LivingEntity sample=cast.target(0,10,true);
        ChaosBeamDamage.hurt(cast.owner,sample);
        net.minecraft.util.DamageSource beam=sample.lastDamageSource;
        net.minecraft.util.DamageSource wrapped=new net.minecraft.util.DamageSource(beam.getMsgId());
        ChaosBeamRecoil.hurtCaster(cast.owner);
        net.minecraft.util.DamageSource recoil=cast.owner.lastDamageSource;
        float[] values={Float.NEGATIVE_INFINITY,-50F,-0F,0F,.5F,.999F,1F,1.25F,20F,Float.NaN,Float.POSITIVE_INFINITY};
        for(net.minecraft.util.DamageSource source:new net.minecraft.util.DamageSource[]{beam,wrapped}) {
            for(float value:values)
                require(Float.floatToIntBits(ChaosBeamDamage.clampHealth(source,value))==Float.floatToIntBits(value>=1?value:1),
                        "Original and wrapped beam sources clamp every below-floor or NaN health write");
        }
        for(net.minecraft.util.DamageSource source:new net.minecraft.util.DamageSource[]{
                new net.minecraft.util.DamageSource("ordinary"),
                new net.minecraft.util.DamageSource(ShockerBreakerAbility.INSTANCE.getId()),recoil,null}) {
            for(float value:values)
                require(Float.floatToIntBits(ChaosBeamDamage.clampHealth(source,value))==Float.floatToIntBits(value),
                        "Other damage and recoil retain their original health writes, including lethal and invalid values");
        }
        for(boolean player:new boolean[]{false,true}) {
            java.lang.reflect.Method redirect=player?testsupport.BeamHealthWriter.PLAYER_WRITE:testsupport.BeamHealthWriter.LIVING_WRITE;
            org.spongepowered.asm.mixin.injection.Redirect annotation=redirect.getAnnotation(org.spongepowered.asm.mixin.injection.Redirect.class);
            String receiver=player?"player/PlayerEntity":"LivingEntity";
            require(annotation.require()==1&&annotation.method()[0].equals("actuallyHurt(Lnet/minecraft/util/DamageSource;F)V")
                    &&annotation.at().value().equals("INVOKE")
                    &&annotation.at().target().equals("Lnet/minecraft/entity/"+receiver+";setHealth(F)V"),
                    "Each real redirect targets the actual health write in its respective damage method");
            for(boolean skipEvents:new boolean[]{false,true})for(boolean wrap:new boolean[]{false,true}) {
                LivingEntity target=player?new net.minecraft.entity.player.PlayerEntity(cast.world):new LivingEntity(cast.world);
                target.health=30;target.skipDamageEvents=skipEvents;target.wrapDamageSource=wrap;
                target.incomingMultiplier=5;target.lateDamageMultiplier=50;
                ChaosBeamDamage.hurt(cast.owner,target);
                require(target.isAlive()&&target.health==1&&target.minimumHealthWritten==1
                        &&target.healthWrites==1&&target.deathCallbacks==0,
                        "The real health-write redirect prevents death before cleanup, even after late amplification or skipped final events");
                require(!ChaosBeamDamage.hurt(cast.owner,target)&&target.healthWrites==1,
                        "Further pulses at one HP do not produce another health write");
            }
            LivingEntity target=player?new net.minecraft.entity.player.PlayerEntity(cast.world):new LivingEntity(cast.world);
            for(float value:new float[]{-100F,.25F,Float.NaN}) {
                target.health=10;testsupport.BeamHealthWriter.write(target,value,wrapped,20F);
                require(target.health==1,"Both actual redirect bodies clamp fractional, negative and NaN proposed health");
            }
            target.health=10;testsupport.BeamHealthWriter.write(target,-90F,recoil,100F);
            require(target.health==0,"Both actual redirect bodies leave lethal recoil unchanged");
        }
        Fixture full=new Fixture();
        net.minecraft.entity.player.PlayerEntity victim=new net.minecraft.entity.player.PlayerEntity(full.world);
        victim.setPos(0,0,20);victim.health=35;victim.skipDamageEvents=true;victim.wrapDamageSource=true;victim.lateDamageMultiplier=50;
        full.world.addFreshEntity(victim);full.world.gameTime=40;full.field.beginBeam();
        for(int tick=40;tick<=280;tick++)full.tick(tick);
        require(victim.isAlive()&&victim.health==1&&victim.healthWrites==1&&victim.deathCallbacks==0,
                "A full production beam never briefly kills or revives a player when final damage caps are bypassed");
    }

    private static void nonlethal() {
        Fixture cast=new Fixture();
        LivingEntity low=cast.target(0,20,true),fractional=cast.target(1,20,true);
        LivingEntity one=cast.target(2,20,true),belowOne=cast.target(3,20,true),boosted=cast.target(4,20,true);
        low.health=20;fractional.health=1.25F;one.health=1;belowOne.health=.5F;boosted.health=30;
        boosted.incomingMultiplier=5;boosted.armorMultiplier=.5F;boosted.finalDamageMultiplier=3;
        LivingEntity ally=friend(cast,5,20,100,30);
        cast.world.gameTime=40;cast.field.beginBeam();
        for(int tick=40;tick<280;tick++)cast.tick(tick);
        require(low.isAlive()&&low.health==1&&low.damageAttempts==1,"Repeated beam pulses stop once a low-health enemy reaches one HP");
        require(fractional.isAlive()&&fractional.health==1&&fractional.actualDamageAmounts.get(0)==.25F,
                "Fractional health above one only loses the fraction");
        require(one.isAlive()&&one.health==1&&one.damageAttempts==0&&belowOne.isAlive()&&belowOne.health==.5F&&belowOne.damageAttempts==0,
                "Enemies already at or below one HP receive no further hits or healing");
        require(boosted.isAlive()&&boosted.health==1&&boosted.actualDamageAmounts.get(0)==29,
                "Both final event handlers prevent amplified damage from killing an enemy");
        require(ally.health==30&&ally.damageAttempts==0&&ally.heals==0,
                "Allies are neither damaged nor healed while the beam fires");
        low.health=2;
        require(ChaosBeamDamage.hurt(cast.owner,low)&&low.health==1,"An enemy healed above the floor can take damage again");
        LivingEntity armored=cast.target(0,10,true);armored.health=100;armored.armorMultiplier=.25F;armored.magicMultiplier=.5F;
        ChaosBeamDamage.hurt(cast.owner,armored);
        require(armored.health==97.5F,"The beam still respects ordinary armor and magic damage reductions");
        net.minecraft.util.DamageSource beamSource=low.lastDamageSource;
        LivingEntity eventTarget=cast.target(0,10,true);eventTarget.health=7;
        xyz.pixelatedw.mineminenomi.api.events.WyLivingDamageEvent nativeEvent=
                new xyz.pixelatedw.mineminenomi.api.events.WyLivingDamageEvent(eventTarget,beamSource,1000F);
        ChaosBeamDamage.nativeDamage(nativeEvent);
        require(nativeEvent.getAmount()==6,"The MMNM final damage handler independently enforces one HP");
        net.minecraft.util.DamageSource wrapped=new net.minecraft.util.DamageSource(beamSource.getMsgId());
        xyz.pixelatedw.mineminenomi.api.events.WyLivingDamageEvent wrappedNative=
                new xyz.pixelatedw.mineminenomi.api.events.WyLivingDamageEvent(eventTarget,wrapped,1000F);
        ChaosBeamDamage.nativeDamage(wrappedNative);
        require(wrappedNative.getAmount()==6,"MMNM source wrapping retains the native nonlethal cap through the unique message ID");
        net.minecraftforge.event.entity.living.LivingDamageEvent wrappedForge=
                new net.minecraftforge.event.entity.living.LivingDamageEvent(eventTarget,wrapped,1000F);
        ChaosBeamDamage.damage(wrappedForge);
        require(wrappedForge.getAmount()==6,"The Forge cap also recognizes the wrapped beam source");
        for(float amount:new float[]{1000F,Float.POSITIVE_INFINITY,Float.NaN,-1F}) {
            net.minecraftforge.event.entity.living.LivingDamageEvent event=
                    new net.minecraftforge.event.entity.living.LivingDamageEvent(eventTarget,beamSource,amount);
            ChaosBeamDamage.damage(event);
            require(event.getAmount()==(amount>0?6F:0F),"The Forge final handler caps boosted and invalid beam damage safely");
        }
        LivingEntity ordinary=cast.target(0,10,true);ordinary.health=10;
        ordinary.hurt(new xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource(
                ShockerBreakerAbility.INSTANCE.getId(),cast.owner,ShockerBreakerAbility.INSTANCE),20);
        require(!ordinary.isAlive()&&ordinary.health==0,"Ordinary Shocker Breaker damage with the same ability ID remains lethal");
        int previous=cast.owner.damageAttempts;
        require(!ChaosBeamDamage.hurt(cast.owner,cast.owner)&&cast.owner.damageAttempts==previous,
                "The offensive helper cannot apply its nonlethal floor to the caster");
        LivingEntity foreign=new LivingEntity(new ServerWorld());
        require(!ChaosBeamDamage.hurt(cast.owner,foreign)&&foreign.damageAttempts==0,"The beam damage helper excludes other dimensions");
        require(!ChaosBeamDamage.hurt(null,low)&&!ChaosBeamDamage.hurt(cast.owner,null),"Missing damage participants are harmless");
        cast.world.isClientSide=true;eventTarget.health=20;
        require(!ChaosBeamDamage.hurt(cast.owner,eventTarget)&&eventTarget.damageAttempts==0,"Only the server inflicts beam damage");
    }

    private static void healing() {
        Fixture cast=new Fixture();
        LivingEntity small=friend(cast,5,8,40,5),large=friend(cast,10,8,200,40);
        LivingEntity boundary=friend(cast,-200,0,100,30),outside=friend(cast,200.01,0,100,30);
        LivingEntity diagonal=friend(cast,150,150,100,30),capped=friend(cast,0,190,100,95);
        LivingEntity vertical=friend(cast,0,0,100,30),above=friend(cast,0,0,100,30);
        vertical.setPos(0,200,0);above.setPos(0,200.01,0);
        LivingEntity forwardTeam=cast.target(0,-20,false),reverseTeam=cast.target(0,-30,false);
        forwardTeam.health=reverseTeam.health=30;
        cast.owner.allies.add(forwardTeam);reverseTeam.allies.add(cast.owner);
        LivingEntity enemy=cast.target(5,5,true),neutral=cast.target(6,6,false);
        enemy.health=neutral.health=20;
        LivingEntity dead=friend(cast,2,2,100,0),removed=friend(cast,3,3,100,30),spectator=friend(cast,4,4,100,30);
        dead.alive=false;removed.removed=true;spectator.spectator=true;
        LivingEntity foreign=new LivingEntity(new ServerWorld());foreign.friendlyFaction=true;foreign.health=30;
        cast.world.entities.add(foreign);
        LivingEntity invalid=friend(cast,7,7,Float.NaN,30);
        LivingEntity departed=friend(cast,20,0,100,30);
        LivingEntity newAlly=cast.target(0,-40,false);newAlly.health=30;
        cast.world.wallZ=1;
        cast.tick(20);
        require(small.heals==0&&large.heals==0,"Charging never heals allies early");
        cast.world.gameTime=40;cast.field.beginBeam();
        cast.field.beginBeam();
        for(int tick=40;tick<280;tick++)cast.tick(tick);
        require(small.heals==0&&large.heals==0&&forwardTeam.heals==0,
                "Firing, duplicate releases and all beam pulses defer ally healing");
        cast.tick(280);cast.field.finishFiring(cast.owner);
        require(small.heals==0&&capped.heals==0,"Completing the beam leaves healing for the cutscene coordinator");
        LivingEntity late=friend(cast,0,10,200,30);
        departed.setPos(300,0,0);newAlly.friendlyFaction=true;
        cast.world.gameTime=632;
        ChaosBeamHealing.healAllies(cast.owner);
        require(small.health==15&&large.health==90,"Each ally receives one quarter of its own maximum health");
        require(boundary.health==55&&vertical.health==55,"The inclusive 200-block sphere reaches behind and above the caster");
        require(outside.heals==0&&above.heals==0&&diagonal.heals==0,"Horizontal, vertical and diagonal targets beyond 200 blocks stay unchanged");
        require(forwardTeam.health==55&&reverseTeam.health==55,"Either direction of the team's ally relation qualifies");
        require(capped.health==100&&capped.healAmounts.get(0)==25,"The normal heal path caps health at the target's maximum");
        require(small.heals==1&&capped.heals==1,"Walls and the attack's narrower damage range do not block allied healing");
        require(enemy.heals==0&&neutral.heals==0,"Enemies and unaffiliated non-enemies never receive healing");
        require(dead.heals==0&&removed.heals==0&&spectator.heals==0,"Dead, removed and spectator allies are excluded");
        require(foreign.heals==0&&invalid.heals==0,"Another world and invalid maximum health cannot receive healing");
        require(cast.owner.heals==0&&cast.owner.health==250,"The ally heal and firing do not alter the caster's health");
        require(late.health==80&&newAlly.health==55&&departed.heals==0,
                "Healing uses allies, maximum health and range at the finale deadline rather than the firing snapshot");
        cast.field.remove();cast.field.beginBeam();
        require(small.heals==1,"A retired beam cannot add a second healing pulse");

        Fixture moved=new Fixture();LivingEntity nowNear=friend(moved,290,0,100,30),nowFar=friend(moved,-150,0,100,30);
        moved.owner.setPos(100,0,0);ChaosBeamHealing.healAllies(moved.owner);
        require(nowNear.health==55&&nowFar.heals==0,"The healing sphere is centered on the caster's current position");

        Fixture cancelled=new Fixture();LivingEntity cancelledAlly=friend(cancelled,0,10,100,30);
        cancelled.field.remove();cancelled.field.beginBeam();
        require(cancelledAlly.heals==0,"A cancelled charge cannot award healing");
        Fixture gone=new Fixture();LivingEntity goneAlly=friend(gone,0,10,100,30);
        gone.owner.alive=false;ChaosBeamHealing.healAllies(gone.owner);
        require(goneAlly.heals==0,"A dead caster cannot award healing");
        ChaosBeamHealing.healAllies(null);
        net.minecraft.world.World client=new net.minecraft.world.World();client.isClientSide=true;
        LivingEntity localCaster=new LivingEntity(client),localAlly=new LivingEntity(client);
        localAlly.friendlyFaction=true;localAlly.health=30;client.addFreshEntity(localCaster);client.addFreshEntity(localAlly);
        ChaosBeamHealing.healAllies(localCaster);
        require(localAlly.heals==0,"Healing never runs on the client");
    }

    private static void recoil() {
        Fixture cast=new Fixture();cast.owner.health=500;cast.owner.invulnerableTime=40;
        cast.tick(20);
        require(cast.owner.damageAttempts==0&&cast.owner.health==500,"Charging never damages the caster");
        cast.world.gameTime=40;cast.field.beginBeam();
        cast.field.beginBeam();cast.tick(41);cast.tick(60);cast.tick(280);
        require(cast.owner.damageAttempts==0&&cast.owner.health==500,
                "Firing, repeated release and beam completion never inflict early recoil");
        cast.world.gameTime=632;
        ChaosBeamRecoil.hurtCaster(cast.owner);
        require(cast.owner.health==400&&cast.owner.successfulHits==1&&cast.owner.damageAmounts.get(0)==100,
                "The recoil helper submits one hundred-base damage hit when invoked after the cutscene");
        require(cast.owner.observedHurtWindow==0&&cast.owner.invulnerableTime==40,
                "Recoil bypasses stale hurt cooldown without shortening existing protection");
        cast.field.remove();cast.field.beginBeam();
        require(cast.owner.damageAttempts==1,"The retired beam cannot add an extra recoil hit");
        ChaosBeamRecoil.hurtCaster(cast.owner);
        require(cast.owner.health==300&&cast.owner.successfulHits==2,"The helper can apply recoil for a later completed cutscene");

        Fixture shortWindow=new Fixture();shortWindow.owner.invulnerableTime=5;ChaosBeamRecoil.hurtCaster(shortWindow.owner);
        require(shortWindow.owner.invulnerableTime==20,"The hurt window newly granted by damage is preserved");
        Fixture reduced=new Fixture();reduced.owner.armorMultiplier=.25F;reduced.owner.magicMultiplier=.5F;ChaosBeamRecoil.hurtCaster(reduced.owner);
        require(reduced.owner.damageAmounts.get(0)==100&&reduced.owner.health==150,
                "Armor and magic damage reductions cannot reduce the hundred-point recoil");
        net.minecraft.util.DamageSource source=reduced.owner.lastDamageSource;
        require(source.isBypassArmor()&&source.isBypassMagic()&&!source.isBypassInvul(),
                "The true-damage source bypasses defenses without bypassing invulnerability");
        recoilReduction(source,reduced.owner);
        Fixture immune=new Fixture();immune.owner.invulnerable=true;immune.owner.invulnerableTime=30;ChaosBeamRecoil.hurtCaster(immune.owner);
        require(immune.owner.damageAttempts==1&&immune.owner.successfulHits==0&&immune.owner.health==250
                &&immune.owner.invulnerableTime==30,"External damage immunity is respected and its hurt timer restored");

        Fixture lethal=new Fixture();lethal.owner.health=75;lethal.owner.onDeath=()->lethal.field.remove();
        LivingEntity saved=friend(lethal,5,10,100,30);lethal.field.beginBeam();
        require(saved.health==30&&saved.heals==0&&lethal.owner.health==75&&lethal.owner.damageAttempts==0,
                "Both healing and recoil remain deferred while a low-health caster fires");
        ChaosBeamHealing.healAllies(lethal.owner);
        require(saved.health==55,"The final heal can complete before the caster's potentially lethal recoil");
        ChaosBeamRecoil.hurtCaster(lethal.owner);
        require(!lethal.owner.isAlive()&&lethal.owner.health==0&&lethal.field.removed&&!lethal.owner.isNoGravity(),
                "Lethal recoil can synchronously clean up the field and movement lock");
        ChaosBeamRecoil.hurtCaster(lethal.owner);require(lethal.owner.damageAttempts==1,"A repeated helper invocation cannot damage a dead caster");

        Fixture cancelled=new Fixture();cancelled.field.remove();cancelled.field.beginBeam();
        require(cancelled.owner.damageAttempts==0,"Cancelled charging never inflicts self damage");
        Fixture invalid=new Fixture();invalid.owner.alive=false;ChaosBeamRecoil.hurtCaster(invalid.owner);
        require(invalid.owner.damageAttempts==0,"A dead caster cannot be damaged again");
        invalid.owner.alive=true;invalid.owner.removed=true;ChaosBeamRecoil.hurtCaster(invalid.owner);
        invalid.owner.removed=false;invalid.owner.spectator=true;ChaosBeamRecoil.hurtCaster(invalid.owner);
        require(invalid.owner.damageAttempts==0,"Removed and spectator casters are excluded");
        ChaosBeamRecoil.hurtCaster(null);
        new ChaosBeamAttackEntity(new net.minecraft.entity.EntityType<ChaosBeamAttackEntity>(),new ServerWorld()).beginBeam();
        net.minecraft.world.World client=new net.minecraft.world.World();client.isClientSide=true;
        LivingEntity local=new LivingEntity(client);ChaosBeamRecoil.hurtCaster(local);
        require(local.damageAttempts==0,"Client state never applies recoil health damage");
        require(!ChaosBeamRecoil.isRecoil(new net.minecraft.util.DamageSource("kazimod.chaos_beam_recoil"))
                &&!ChaosBeamRecoil.isRecoil(null),"Only the dedicated recoil source gets the beam protection exception");
    }

    private static void recoilReduction(net.minecraft.util.DamageSource recoil,LivingEntity target) {
        try {
            Class<?> mixin=Class.forName("net.kazi.kazimod.mixin.ChaosBeamRecoilMixin");
            java.lang.reflect.Method method=mixin.getDeclaredMethod("kazi$keepChaosBeamRecoil",LivingEntity.class,
                    net.minecraft.util.DamageSource.class,float.class,
                    org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable.class);
            method.setAccessible(true);
            org.spongepowered.asm.mixin.injection.Inject injection=method.getAnnotation(org.spongepowered.asm.mixin.injection.Inject.class);
            require(injection.cancellable()&&injection.at().value().equals("HEAD")
                    &&injection.method()[0].equals("calcDamageReduction(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/util/DamageSource;F)F"),
                    "The real mixin replaces only the MMNM reduction calculation at its entry");
            for(float amount:new float[]{1F,37.5F,100F}) {
                org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> callback=
                        new org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<>("reduction",true,-999F);
                method.invoke(null,target,recoil,amount,callback);
                require(callback.isCancelled()&&callback.getReturnValue()==amount,
                        "The real mixin preserves incoming recoil damage without MMNM toughness reduction");
            }
            for(net.minecraft.util.DamageSource ordinary:new net.minecraft.util.DamageSource[]{
                    new net.minecraft.util.DamageSource("ordinary"),
                    new net.minecraft.util.DamageSource("kazimod.chaos_beam_recoil"),null}) {
                org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Float> callback=
                        new org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<>("reduction",true,12.5F);
                method.invoke(null,target,ordinary,100F,callback);
                require(!callback.isCancelled()&&callback.getReturnValue()==12.5F,
                        "Unrelated damage retains normal MMNM reduction, including a matching source name");
            }
        } catch(ReflectiveOperationException failure) {throw new AssertionError("Unable to exercise production recoil mixin",failure);}
    }

    private static LivingEntity friend(Fixture fixture,double x,double z,float maximum,float health) {
        LivingEntity ally=fixture.target(x,z,false);ally.friendlyFaction=true;ally.maxHealth=maximum;ally.health=health;return ally;
    }
    private static void require(boolean pass,String message) { checks++;if(!pass)throw new AssertionError(message); }
    private static final class Fixture {
        final ServerWorld world=new ServerWorld(); final LivingEntity owner=new LivingEntity(world);
        final ShockerBreakerAbility ability=new ShockerBreakerAbility(); final ChaosBeamAttackEntity field;
        Fixture(){this(0,0);}
        Fixture(float yaw,float pitch){owner.health=250;owner.maxHealth=1000;owner.yRot=yaw;owner.xRot=pitch;world.addFreshEntity(owner);AbilityDataCapability.DATA.put(owner,ability);field=ChaosBeamAttackEntity.spawn(owner);ability.field=field;}
        LivingEntity target(double x,double z,boolean hostile){LivingEntity e=new LivingEntity(world);e.setPos(x,0,z);e.hostile=hostile;world.addFreshEntity(e);return e;}
        void tick(int time){world.gameTime=time;field.tick();}
    }
}
