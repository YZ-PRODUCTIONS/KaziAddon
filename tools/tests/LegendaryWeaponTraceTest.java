import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import net.kazi.kazimod.abilities.SupaRework.LegendaryWeaponTrace;
import net.kazi.kazimod.abilities.SupaRework.ProjectionAbility;
import net.kazi.kazimod.abilities.SupaRework.RealityMarbleAbility;
import net.kazi.kazimod.testing.TraceNetwork;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.World;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import xyz.pixelatedw.mineminenomi.api.abilities.Ability;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityUnlock;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.events.ability.AbilityUseEvent;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.config.CommonConfig;
import xyz.pixelatedw.mineminenomi.data.entity.ability.AbilityDataCapability;
import xyz.pixelatedw.mineminenomi.data.entity.ability.IAbilityData;
import xyz.pixelatedw.mineminenomi.data.entity.devilfruit.DevilFruitCapability;

/** Runs the production manager; fixtures provide only the surrounding game APIs. */
public final class LegendaryWeaponTraceTest {
    private static int checks;
    private static int unrelatedId;

    public static void main(String[] args) {
        poolAndGates();commandGrantActivation();shuffledCycles();unusedExpiry();instantAndChargedUse();cleanup();ownershipAndSlots();recoveryAndRollback();returnCallbacksAndBans();nativeCooldowns();clientSynchronization();
        System.out.println("Legendary Weapon Trace behavior: " + checks + " checks passed.");
    }

    private static void poolAndGates() {
        Set<AbilityCore<?>> selected=new HashSet<>();
        {Fixture f=new Fixture(0);
        for(int index=0;index<11;index++) {
            IAbility rolled=f.roll();selected.add(rolled.getCore());
            require(rolled.getCore()!=ProjectionAbility.INSTANCE,"The first press equips a borrowed move rather than firing it");
            require(LegendaryWeaponTrace.hasActiveRoll(f.user),"An active roll is tracked independently from Projection");
            require(LegendaryWeaponTrace.isRolledMove(f.user,rolled),"The equipped borrowed instance is recognized");
            require(!LegendaryWeaponTrace.isRolledMove(f.user,rolled.getCore().createAbility()),"Another instance with the same core receives no borrowed-use privilege");
            require(f.data.hasUnlockedAbility(rolled.getCore()),"The selected move is temporarily available in ability data");
            require(f.data.hasUnlockedAbility(ProjectionAbility.INSTANCE),"Swapping the slot preserves Projection ownership");
            require(f.user.syncs>0,"The slot replacement is synchronized to the player");
            expire(f);
        }}
        require(selected.size()==11,"All eleven configured moves can be rolled");
        for(RealityMarbleAbility.Mode mode:RealityMarbleAbility.Mode.values()) {
            Fixture gated=new Fixture(0);gated.marble.mode=mode;
            boolean allowed=mode!=RealityMarbleAbility.Mode.VOID_CREATION_OF_SWORDS;
            require(LegendaryWeaponTrace.isMarbleActive(gated.user)==allowed,"Only Mugen and Infinite Creation satisfy the marble gate");
            LegendaryWeaponTrace.roll(gated.user,gated.projection);
            require(LegendaryWeaponTrace.hasActiveRoll(gated.user)==allowed,"Lost Works cannot create a trace roll");
        }
        rejected("inactive marble",f->f.marble.active=false);
        rejected("unequipped marble",f->f.data.equipped.set(0,null));
        rejected("client",f->f.world.isClientSide=true);
        rejected("dead caster",f->f.user.alive=false);
        rejected("spectator",f->f.user.spectator=true);
        rejected("unawakened fruit",f->DevilFruitCapability.get(f.user).awakened=false);
        rejected("different fruit",f->DevilFruitCapability.get(f.user).supa=false);
        rejected("unequipped Projection",f->f.data.equipped.set(1,null));
        Fixture active=new Fixture(0);IAbility first=active.roll();
        LegendaryWeaponTrace.roll(active.user,active.projection);
        require(active.data.equipped.get(1)==first,"A stale Projection reference cannot reroll an active borrowed move");
    }

    private static void commandGrantActivation() {
        for (RealityMarbleAbility.Mode mode : RealityMarbleAbility.Mode.values()) {
            Fixture f = new Fixture(0);
            f.marble.mode = mode;
            f.data.unlocked.put(ProjectionAbility.INSTANCE, AbilityUnlock.COMMAND);
            DevilFruitCapability.get(f.user).awakened = false;
            DevilFruitCapability.get(f.user).supa = false;
            boolean allowed = mode != RealityMarbleAbility.Mode.VOID_CREATION_OF_SWORDS;
            require(LegendaryWeaponTrace.isMarbleActive(f.user) == allowed,
                    "Command-granted Projection works without an eaten fruit, but still requires Mugen or Infinite");
            if (!allowed) {
                LegendaryWeaponTrace.roll(f.user, f.projection);
                require(!LegendaryWeaponTrace.hasActiveRoll(f.user), "Command grants do not enable Trace in Lost Works");
                continue;
            }
            IAbility rolled = f.roll();
            require(LegendaryWeaponTrace.canUseRolledMove(f.user, rolled),
                    "A command-granted roll remains usable without fruit progression");
            f.world.isClientSide = true;
            require(LegendaryWeaponTrace.canUseRolledMove(f.user, rolled),
                    "Client-side borrowed-move checks honor the same command grant");
            f.world.isClientSide = false;
            f.world.gameTime++;
            f.tick();
            require(LegendaryWeaponTrace.hasActiveRoll(f.user), "The next tick does not immediately remove the command-granted roll");
            expire(f);
            require(f.data.getUnlockTypeForAbility(ProjectionAbility.INSTANCE) == AbilityUnlock.COMMAND,
                    "Returning Projection preserves the command grant");
            f.marble.active = false;
            LegendaryWeaponTrace.roll(f.user, f.data.getEquippedAbility(ProjectionAbility.INSTANCE));
            require(!LegendaryWeaponTrace.hasActiveRoll(f.user), "Command grants still require an active marble");
        }
    }

    private static void shuffledCycles() {
        // A constant RNG used to repeat the same move forever. A cycle must still
        // visit every candidate, including Divine Departure, before any repeat.
        Fixture f=new Fixture(0);
        for(int round=0;round<3;round++) {
            Set<AbilityCore<?>> cycle=new HashSet<>();
            for(int i=0;i<11;i++) {
                IAbility move=f.roll();
                require(cycle.add(move.getCore()),"A shuffled cycle never repeats an available move before all eleven appear");
                if(i%3==0) {
                    LegendaryWeaponTrace.logout(new PlayerEvent.PlayerLoggedOutEvent(f.user));
                    LegendaryWeaponTrace.login(new PlayerEvent.PlayerLoggedInEvent(f.user));
                } else expire(f);
                require(!LegendaryWeaponTrace.hasActiveRoll(f.user),"Returning a loan leaves the next cycle draw available");
            }
            require(cycle.contains(net.kazi.kazimod.abilities.SaberRework.DivineDepartureClone.INSTANCE),"Divine Departure appears in every complete cycle");
            require(cycle.contains(net.MrMagicalCart.cartaddon.abilities.dagger.FinalExecutionAbility.INSTANCE),"Final Execution remains in every complete cycle");
        }

        Fixture original=new Fixture(0);Set<AbilityCore<?>> beforeDeath=new HashSet<>();
        for(int i=0;i<4;i++){beforeDeath.add(original.roll().getCore());expire(original);}
        Fixture clone=new Fixture(0);
        LegendaryWeaponTrace.clone(new PlayerEvent.Clone(clone.user,original.user));
        for(int i=4;i<11;i++) {
            require(beforeDeath.add(clone.roll().getCore()),"Respawning preserves which moves remain in the current cycle");
            expire(clone);
        }
        require(beforeDeath.size()==11,"The pre-respawn and post-respawn draws finish one complete cycle");

        Fixture filtered=new Fixture(0);Set<AbilityCore<?>> available=new HashSet<>();
        AbilityCore<?> banned=net.kazi.kazimod.abilities.SaberRework.DivineDepartureClone.INSTANCE;
        AbilityHelper.BANNED.add(banned);
        try {
            for(int i=0;i<10;i++) {
                IAbility move=filtered.roll();
                require(move.getCore()!=banned&&available.add(move.getCore()),"Banned entries are omitted without repeating eligible entries");
                expire(filtered);
            }
        } finally {AbilityHelper.BANNED.remove(banned);}
        Set<AbilityCore<?>> afterUnban=new HashSet<>();
        for(int i=0;i<11;i++){afterUnban.add(filtered.roll().getCore());expire(filtered);}
        require(afterUnban.contains(banned),"A move that becomes available returns to the cycle");

        Fixture probe=new Fixture(0);probe.roll();expire(probe);AbilityCore<?> second=probe.roll().getCore();
        for(boolean failGrant:new boolean[]{true,false}) {
            Fixture failure=new Fixture(0);IAbility first=failure.roll();expire(failure);
            if(failGrant)failure.data.denyGrant=true;else failure.data.denyEquipCore=second;
            LegendaryWeaponTrace.roll(failure.user,failure.data.getEquippedAbility(ProjectionAbility.INSTANCE));
            require(!LegendaryWeaponTrace.hasActiveRoll(failure.user),"A rejected draw cannot install a loan");
            failure.data.denyGrant=false;failure.data.denyEquipCore=null;
            Set<AbilityCore<?>> recovered=new HashSet<>();recovered.add(first.getCore());
            for(int i=1;i<11;i++) {
                require(recovered.add(failure.roll().getCore()),"An equip/grant failure does not consume a cycle entry");
                expire(failure);
            }
            require(recovered.size()==11,"Failed draws still permit all eleven moves in the same cycle");
        }
    }

    private static void expire(Fixture f) {
        f.world.gameTime=f.user.getPersistentData().getCompound(LegendaryWeaponTrace.TAG).getLong("expiry");
        f.tick();
        ProjectionAbility restored=f.data.getEquippedAbility(ProjectionAbility.INSTANCE);
        if(restored!=null)restored.cooldown.stopCooldown(f.user);
    }

    private static void rejected(String reason,Consumer<Fixture> configure) {
        Fixture f=new Fixture(0);configure.accept(f);LegendaryWeaponTrace.roll(f.user,f.projection);
        require(!LegendaryWeaponTrace.hasActiveRoll(f.user),"Cannot roll with "+reason);
    }

    private static void unusedExpiry() {
        Fixture f=new Fixture(0);IAbility rolled=f.roll();f.world.gameTime=f.start+299;f.tick();
        require(f.data.equipped.get(1)==rolled&&LegendaryWeaponTrace.canUseRolledMove(f.user,rolled),"The unused move remains usable through tick 299");
        f.world.gameTime=f.start+300;
        require(!LegendaryWeaponTrace.canUseRolledMove(f.user,rolled),"The deadline denies use at tick 300 before tick cleanup runs");
        AbilityUseEvent.Pre late=new AbilityUseEvent.Pre(f.user,rolled);LegendaryWeaponTrace.beforeUse(late);
        require(late.canceled,"The server blocks a late activation at the 15 second boundary");
        f.tick();f.assertReturned("unused expiry");
        require(!f.data.hasUnlockedAbility(rolled.getCore()),"Unused expiry removes the temporary unlock");
        f.tick();f.assertReturned("repeated cleanup");
    }

    private static void instantAndChargedUse() {
        Fixture f=new Fixture(0);Ability rolled=(Ability)f.roll();
        AbilityUseEvent.Pre pre=new AbilityUseEvent.Pre(f.user,rolled);LegendaryWeaponTrace.beforeUse(pre);
        require(!pre.canceled,"The first borrowed activation is permitted");
        LegendaryWeaponTrace.afterUse(new AbilityUseEvent.Post(f.user,rolled));
        require(!LegendaryWeaponTrace.canUseRolledMove(f.user,rolled),"A successful instant use consumes permission immediately");
        AbilityUseEvent.Pre again=new AbilityUseEvent.Pre(f.user,rolled);LegendaryWeaponTrace.beforeUse(again);
        require(again.canceled,"Repeated same-tick instant activation is denied");
        f.world.gameTime++;f.tick();f.assertReturned("instant completion");
        for(boolean charge:new boolean[]{true,false}) {
            Fixture active=new Fixture(0);Ability move=(Ability)active.roll();active.world.gameTime=active.start+299;
            if(charge)move.charge.charging=true;else move.continuous.continuous=true;
            LegendaryWeaponTrace.afterUse(new AbilityUseEvent.Post(active.user,move));
            AbilityUseEvent.Pre release=new AbilityUseEvent.Pre(active.user,move);LegendaryWeaponTrace.beforeUse(release);
            require(!release.canceled,"A running move retains its normal charge-release or continuity-stop input");
            active.world.gameTime=active.start+301;active.tick();
            require(active.data.equipped.get(1)==move,"A move started before expiry can finish its "+(charge?"charge":"continuity"));
            move.charge.charging=false;move.continuous.continuous=false;active.world.gameTime++;active.tick();
            active.assertReturned(charge?"charge completion":"continuity completion");
        }
        Fixture nonmatching=new Fixture(0);IAbility waiting=nonmatching.roll();
        LegendaryWeaponTrace.afterUse(new AbilityUseEvent.Post(nonmatching.user,unrelated()));nonmatching.world.gameTime++;nonmatching.tick();
        require(nonmatching.data.equipped.get(1)==waiting&&LegendaryWeaponTrace.canUseRolledMove(nonmatching.user,waiting),"Using another equipped ability does not consume the roll");
    }

    private static void cleanup() {
        for(String reason:new String[]{"marble ended","lost works","dead","logout","dimension","login","fruit lost"}) {
            Fixture f=new Fixture(0);Ability move=(Ability)f.roll();move.charge.charging=true;
            LegendaryWeaponTrace.afterUse(new AbilityUseEvent.Post(f.user,move));f.world.gameTime++;
            if(reason.equals("marble ended")){f.marble.active=false;f.tick();}
            else if(reason.equals("lost works")){f.marble.mode=RealityMarbleAbility.Mode.VOID_CREATION_OF_SWORDS;f.tick();}
            else if(reason.equals("dead")){f.user.alive=false;LegendaryWeaponTrace.death(new LivingDeathEvent(f.user));}
            else if(reason.equals("logout"))LegendaryWeaponTrace.logout(new PlayerEvent.PlayerLoggedOutEvent(f.user));
            else if(reason.equals("login"))LegendaryWeaponTrace.login(new PlayerEvent.PlayerLoggedInEvent(f.user));
            else if(reason.equals("fruit lost")){DevilFruitCapability.get(f.user).supa=false;f.tick();}
            else {f.world.dimension="minecraft:the_nether";LegendaryWeaponTrace.dimension(new PlayerEvent.PlayerChangedDimensionEvent(f.user));}
            require(!move.charge.charging&&!move.continuous.continuous,"Cleanup stops a running borrowed move on "+reason);
            f.assertReturned(reason);require(!f.data.hasUnlockedAbility(move.getCore()),"Cleanup removes only the temporary grant on "+reason);
        }
        Fixture inactive=new Fixture(0);IAbility move=inactive.roll();inactive.marble.active=false;
        AbilityUseEvent.Pre event=new AbilityUseEvent.Pre(inactive.user,move);LegendaryWeaponTrace.beforeUse(event);
        require(event.canceled,"Marble loss blocks a borrowed activation before the next cleanup tick");
        Fixture sneaking=new Fixture(0);Ability discarded=(Ability)sneaking.roll();sneaking.user.shift=true;
        AbilityUseEvent.Pre discard=new AbilityUseEvent.Pre(sneaking.user,discarded);LegendaryWeaponTrace.beforeUse(discard);
        require(discard.canceled,"Sneak use discards instead of firing the borrowed move");
        sneaking.world.gameTime++;sneaking.tick();sneaking.assertReturned("sneak discard");
        Fixture lost=new Fixture(0);IAbility loan=lost.roll();DevilFruitCapability.get(lost.user).supa=false;
        lost.data.removeUnlockedAbility(ProjectionAbility.INSTANCE);lost.tick();
        require(!LegendaryWeaponTrace.hasActiveRoll(lost.user)&&!lost.data.hasUnlockedAbility(loan.getCore()),"Losing Projection ownership still cleans up the borrowed grant");
        require(!lost.data.hasUnlockedAbility(ProjectionAbility.INSTANCE)&&lost.data.getEquippedAbility(ProjectionAbility.INSTANCE)==null,"Cleanup never regrants Projection after the fruit system removes it");
    }

    private static void ownershipAndSlots() {
        Fixture discovery=new Fixture(0);AbilityCore<?> selected=discovery.roll().getCore();
        for(AbilityUnlock ownership:AbilityUnlock.values()) {
            Fixture f=new Fixture(0);f.data.addUnlockedAbility(selected,ownership);IAbility rolled=f.roll();
            require(rolled.getCore()==selected,"An already-unlocked move remains eligible for the fixed pool");
            f.world.gameTime=f.start+300;f.tick();f.assertReturned("owned move expiry");
            require(f.data.hasUnlockedAbility(selected)&&f.data.getUnlockTypeForAbility(selected)==ownership,"Existing unlock and its provenance survive the temporary roll");
        }
        Fixture moved=new Fixture(0);IAbility rolled=moved.roll();IAbility unrelated=unrelated();
        moved.data.equipped.set(4,rolled);moved.data.equipped.set(1,unrelated);moved.world.gameTime=moved.start+300;moved.tick();
        require(moved.data.equipped.get(1)==unrelated,"Expiry does not overwrite a replacement in the original slot");
        require(moved.data.getEquippedAbility(ProjectionAbility.INSTANCE)!=null,"A moved borrowed slot can safely return Projection elsewhere");
        require(!moved.data.equipped.contains(rolled),"Moving a borrowed move cannot retain it past expiry");
        moved.assertReturned("rebound slot");

        Fixture full=new Fixture(0);IAbility removed=full.roll();
        for(int slot=1;slot<full.data.equipped.size();slot++)full.data.equipped.set(slot,unrelated());
        java.util.List<IAbility> occupants=new java.util.ArrayList<>(full.data.equipped);
        full.world.gameTime=full.start+300;full.tick();
        require(full.data.equipped.equals(occupants),"Restoration never overwrites a full bar of unrelated abilities");
        require(!full.data.hasUnlockedAbility(removed.getCore()),"The borrowed grant is removed even when Projection must await a free slot");
        full.data.equipped.set(3,null);full.world.gameTime++;full.tick();full.assertReturned("free slot after deferred restoration");
        for(int i=0;i<occupants.size();i++)if(i!=3)require(full.data.equipped.get(i)==occupants.get(i),"Deferred restoration preserves every occupied unrelated slot");
    }

    private static void recoveryAndRollback() {
        Fixture discovery=new Fixture(0);AbilityCore<?> first=discovery.roll().getCore();
        for(boolean grant:new boolean[]{true,false}) {
            Fixture f=new Fixture(0);if(grant)f.data.denyGrant=true;else f.data.denyEquipCore=first;
            LegendaryWeaponTrace.roll(f.user,f.projection);
            require(f.data.equipped.get(1)==f.projection,"A rejected "+(grant?"unlock":"equip")+" leaves the original Projection in its slot");
            require(!LegendaryWeaponTrace.hasActiveRoll(f.user)&&!f.data.hasUnlockedAbility(first),"A rejected roll leaves no temporary state or leaked unlock");
        }
        Fixture excluded=new Fixture(0);IAbility existing=first.createAbility();excluded.data.setEquippedAbility(3,existing);
        IAbility rolled=excluded.roll();require(rolled.getCore()!=first&&excluded.data.equipped.get(3)==existing,"Already-equipped cores are excluded without disturbing the existing ability");
        Fixture passive=new Fixture(0);passive.data.passives.add(first);require(passive.roll().getCore()!=first,"A passive instance also prevents an incompatible duplicate loan");
        Fixture old=new Fixture(0);IAbility loan=old.roll();
        Fixture replacement=new Fixture(0);replacement.data.equipped.set(1,loan);replacement.data.addUnlockedAbility(loan.getCore(),AbilityUnlock.COMMAND);
        LegendaryWeaponTrace.clone(new PlayerEvent.Clone(replacement.user,old.user));replacement.assertReturned("respawn clone recovery");
        require(!replacement.data.hasUnlockedAbility(loan.getCore()),"Respawn recovery removes a persisted borrowed unlock");
        Fixture phase=new Fixture(0);IAbility pending=phase.roll();phase.world.gameTime=phase.start+300;
        LegendaryWeaponTrace.tick(new TickEvent.PlayerTickEvent(phase.user,TickEvent.Phase.START));
        require(phase.data.equipped.get(1)==pending,"The manager mutates ability data only at the end of a player tick");
        phase.world.isClientSide=true;phase.tick();require(phase.data.equipped.get(1)==pending,"Client ticks cannot independently expire a server-owned loan");
        Fixture unavailable=new Fixture(0);IAbility unavailableLoan=unavailable.roll();
        unavailable.world.gameTime=unavailable.start+300;AbilityDataCapability.DATA.remove(unavailable.user);unavailable.tick();
        require(LegendaryWeaponTrace.hasActiveRoll(unavailable.user),"Temporarily missing ability data preserves the recovery record");
        AbilityDataCapability.DATA.put(unavailable.user,unavailable.data);unavailable.tick();unavailable.assertReturned("capability recovery");
        require(!unavailable.data.hasUnlockedAbility(unavailableLoan.getCore())&&!unavailable.data.equipped.contains(unavailableLoan),"A capability that returns later still has its borrowed move and temporary unlock removed");
    }

    private static void returnCallbacksAndBans() {
        Fixture f=new Fixture(0);Ability move=(Ability)f.roll();move.charge.charging=true;
        require(!LegendaryWeaponTrace.isReturningMove(f.user,move),"An ordinary active roll is not treated as forced cleanup");
        final boolean[] callback={false};
        AbilityHelper.onEmergencyStop=(user,ability)->{
            callback[0]=true;
            require(LegendaryWeaponTrace.isReturningMove(user,ability),"The actual loan is marked returning before emergency end callbacks execute");
            require(!LegendaryWeaponTrace.isRolledMove(user,ability)&&!LegendaryWeaponTrace.canUseRolledMove(user,ability),"Emergency callbacks cannot reopen ordinary borrowed-use permission");
            require(!LegendaryWeaponTrace.isReturningMove(user,ability.getCore().createAbility()),"Returning permission is restricted to the equipped instance");
            require(!LegendaryWeaponTrace.isReturningMove(user,unrelated()),"An unrelated move never receives the forced-return exception");
            LegendaryWeaponTrace.afterUse(new AbilityUseEvent.Post(user,ability));
        };
        try {LegendaryWeaponTrace.logout(new PlayerEvent.PlayerLoggedOutEvent(f.user));}
        finally {AbilityHelper.onEmergencyStop=null;}
        require(callback[0],"Forced cleanup invokes the ability framework's end callbacks");
        require(!LegendaryWeaponTrace.isReturningMove(f.user,move),"Returning permission clears once restoration finishes");
        f.assertReturned("emergency callback completion");

        Fixture discover=new Fixture(0);AbilityCore<?> banned=discover.roll().getCore();AbilityHelper.BANNED.add(banned);
        try {
            Fixture excluded=new Fixture(0);require(excluded.roll().getCore()!=banned,"The random pool excludes a banned move while fraud checks are enabled");
            CommonConfig.INSTANCE.fraudChecks=false;Fixture disabled=new Fixture(0);
            require(disabled.roll().getCore()==banned,"Disabling the configured ban check preserves the server's intended pool policy");
            CommonConfig.INSTANCE.fraudChecks=true;
            for(AbilityCore<?> core:xyz.pixelatedw.mineminenomi.api.ModRegistries.ABILITIES.getValues())AbilityHelper.BANNED.add(core);
            Fixture empty=new Fixture(0);LegendaryWeaponTrace.roll(empty.user,empty.projection);
            require(empty.data.equipped.get(1)==empty.projection&&!LegendaryWeaponTrace.hasActiveRoll(empty.user),"An entirely unavailable pool leaves Projection and ownership intact");
        } finally {AbilityHelper.BANNED.clear();CommonConfig.INSTANCE.fraudChecks=true;}
    }

    private static void nativeCooldowns() {
        for(float nativeTicks:new float[]{900,1400}) {
            Fixture instant=new Fixture(0);Ability move=(Ability)instant.roll();
            move.cooldown.startCooldown(instant.user,nativeTicks);
            LegendaryWeaponTrace.afterUse(new AbilityUseEvent.Post(instant.user,move));
            instant.world.gameTime++;instant.tick();instant.assertReturned("native instant cooldown",nativeTicks/2);

            Fixture elapsed=new Fixture(0);Ability ticking=(Ability)elapsed.roll();ticking.cooldown.startCooldown(elapsed.user,nativeTicks);
            ticking.cooldown.ticks=25;
            LegendaryWeaponTrace.afterUse(new AbilityUseEvent.Post(elapsed.user,ticking));elapsed.world.gameTime++;elapsed.tick();
            elapsed.assertReturned("elapsed native cooldown",nativeTicks/2);

            Fixture charged=new Fixture(0);Ability charging=(Ability)charged.roll();charging.charge.charging=true;
            LegendaryWeaponTrace.afterUse(new AbilityUseEvent.Post(charged.user,charging));charged.world.gameTime+=40;charged.tick();
            require(charged.data.equipped.get(1)==charging,"A charge waits for its native completion before selecting the return cooldown");
            charging.charge.onStop=()->charging.cooldown.startCooldown(charged.user,nativeTicks);
            charging.charge.stopCharging(charged.user);charged.world.gameTime++;charged.tick();
            charged.assertReturned("cooldown established by charge end",nativeTicks/2);

            Fixture emergency=new Fixture(0);Ability finished=(Ability)emergency.roll();finished.cooldown.startCooldown(emergency.user,nativeTicks);
            LegendaryWeaponTrace.afterUse(new AbilityUseEvent.Post(emergency.user,finished));finished.cooldown.ticks=0;
            emergency.world.gameTime++;emergency.tick();emergency.assertReturned("native cooldown survives emergency helper",nativeTicks/2);
            require(finished.cooldown.getStartCooldown()==40,"The fixture invokes the framework's emergency 40-tick cooldown after capturing the actual native duration");
        }

        Fixture pending=new Fixture(0);Ability move=(Ability)pending.roll();move.cooldown.startCooldown(pending.user,1400);
        LegendaryWeaponTrace.afterUse(new AbilityUseEvent.Post(pending.user,move));
        for(int slot=1;slot<pending.data.equipped.size();slot++)pending.data.equipped.set(slot,unrelated());
        pending.world.gameTime++;pending.tick();require(LegendaryWeaponTrace.hasActiveRoll(pending.user),"A full bar retains the captured native cooldown with its pending restoration");
        pending.world.gameTime+=1000;pending.data.equipped.set(3,null);pending.tick();pending.assertReturned("deferred dynamic cooldown restoration",700);

        Fixture canceled=new Fixture(0);Ability charging=(Ability)canceled.roll();charging.charge.charging=true;
        LegendaryWeaponTrace.afterUse(new AbilityUseEvent.Post(canceled.user,charging));canceled.marble.active=false;canceled.tick();
        canceled.assertReturned("canceled charge without native cooldown",200);
        require(charging.cooldown.getStartCooldown()==40,"A canceled charge still exercises the emergency helper cooldown without using it as a native attack cooldown");
    }

    private static void clientSynchronization() {
        TraceNetwork.events.clear();Fixture server=new Fixture(0);Ability move=(Ability)server.roll();
        require(TraceNetwork.events.size()==2,"Rolling sends one state snapshot and one ability snapshot");
        require("state".equals(TraceNetwork.events.get(0).kind)&&"abilities".equals(TraceNetwork.events.get(1).kind),"Roll state reaches clients before the newly equipped ability snapshot");
        for(TraceNetwork.Record packet:TraceNetwork.events)require("tracking_and_self".equals(packet.audience)&&packet.subject==server.user.getId(),"Both roll snapshots reach the owner and tracking players for the same caster");
        net.minecraft.nbt.CompoundNBT visible=TraceNetwork.events.get(0).state;
        Set<String> expectedKeys=new HashSet<>(java.util.Arrays.asList("selected","dimension","expiry","used","returning"));
        require(visible.getAllKeys().equals(expectedKeys),"The client snapshot exposes only the five fields needed to recognize the loan");
        require(!visible.contains("projection")&&!visible.contains("hadUnlock")&&!visible.contains("slot"),"Restoration data and unlock ownership stay on the server");
        Fixture client=new Fixture(0);client.world.isClientSide=true;IAbility clientMove=move.getCore().createAbility();client.data.equipped.set(1,clientMove);
        applyState(client,visible);require(LegendaryWeaponTrace.canUseRolledMove(client.user,clientMove),"A client can recognize the server's unused borrowed move");
        TraceNetwork.events.clear();LegendaryWeaponTrace.afterUse(new AbilityUseEvent.Post(server.user,move));
        require(TraceNetwork.events.size()==1&&"state".equals(TraceNetwork.events.get(0).kind),"After use only the small state snapshot is sent, preserving native client component startup");
        require(TraceNetwork.events.get(0).state.getBoolean("used"),"Successful use is included in the client snapshot");
        applyState(client,TraceNetwork.events.get(0).state);
        require(!((Ability)clientMove).charge.charging&&!((Ability)clientMove).continuous.continuous,"The client fixture has not received its native charge or continuity start yet");
        require(LegendaryWeaponTrace.canUseRolledMove(client.user,clientMove),"A used-state packet arriving first cannot block the subsequent native visual start on clients");
        require(!LegendaryWeaponTrace.canUseRolledMove(server.user,move),"The server still rejects a second instant cast after consuming the loan");

        TraceNetwork.events.clear();final boolean[] stopped={false};
        AbilityHelper.onEmergencyStop=(entity,ability)->{
            stopped[0]=true;
            require(TraceNetwork.events.size()==1&&"state".equals(TraceNetwork.events.get(0).kind),"The returning marker is sent before emergency callbacks and before a full ability snapshot");
            require(TraceNetwork.events.get(0).state.getBoolean("returning"),"The first forced-return packet carries the cancellation marker");
            applyState(client,TraceNetwork.events.get(0).state);
            require(LegendaryWeaponTrace.isReturningMove(client.user,clientMove)&&!LegendaryWeaponTrace.canUseRolledMove(client.user,clientMove),"The client can suppress native attack end callbacks while the returning marker is active");
        };
        try {LegendaryWeaponTrace.logout(new PlayerEvent.PlayerLoggedOutEvent(server.user));}
        finally {AbilityHelper.onEmergencyStop=null;}
        require(stopped[0],"The packet-order test executes an emergency callback");
        require(TraceNetwork.events.size()==3,"Forced return sends its marker, the cleared state, and the final ability snapshot");
        require("state".equals(TraceNetwork.events.get(1).kind)&&TraceNetwork.events.get(1).state.isEmpty(),"The borrowed state clears only after emergency callbacks have completed");
        require("abilities".equals(TraceNetwork.events.get(2).kind),"The final full snapshot follows completed restoration");
        applyState(client,TraceNetwork.events.get(1).state);require(!LegendaryWeaponTrace.hasActiveRoll(client.user)&&!LegendaryWeaponTrace.isReturningMove(client.user,clientMove),"A clear snapshot removes the client's temporary privileges");

        Fixture target=new Fixture(0);target.roll();ServerPlayerEntity viewer=new ServerPlayerEntity(target.world);TraceNetwork.events.clear();
        LegendaryWeaponTrace.tracking(new PlayerEvent.StartTracking(viewer,target.user));
        require(TraceNetwork.events.size()==2&&"state".equals(TraceNetwork.events.get(0).kind)&&"abilities".equals(TraceNetwork.events.get(1).kind),"A new tracking viewer receives loan state before the target's abilities");
        for(TraceNetwork.Record packet:TraceNetwork.events)require("player".equals(packet.audience)&&packet.recipient==viewer&&packet.subject==target.user.getId(),"Tracking snapshots go only to the new viewer and identify the target caster");
        TraceNetwork.events.clear();Fixture idle=new Fixture(0);LegendaryWeaponTrace.tracking(new PlayerEvent.StartTracking(viewer,idle.user));
        require(TraceNetwork.events.isEmpty(),"Tracking a player without a loan sends no trace-specific snapshots");
    }

    private static void applyState(Fixture client,net.minecraft.nbt.CompoundNBT visible) {
        if(visible.isEmpty())client.user.getPersistentData().remove(LegendaryWeaponTrace.TAG);
        else client.user.getPersistentData().put(LegendaryWeaponTrace.TAG,visible.copy());
    }

    private static Ability unrelated() {
        return new AbilityCore<Ability>("fixture:unrelated_"+(++unrelatedId),Ability::new).createAbility();
    }

    private static void require(boolean condition,String message) {
        checks++;if(!condition)throw new AssertionError(message);
    }

    private static final class Fixture {
        final World world=new World();final ServerPlayerEntity user=new ServerPlayerEntity(world);
        final IAbilityData data=new IAbilityData();final ProjectionAbility projection=ProjectionAbility.INSTANCE.createAbility();
        final RealityMarbleAbility marble=RealityMarbleAbility.INSTANCE.createAbility();final long start=100;
        Fixture(final int index) {
            world.gameTime=start;user.random=new java.util.Random(){@Override public int nextInt(int bound){return index%bound;}};
            projection.mode="LEGENDARY_WEAPON_TRACE";AbilityDataCapability.DATA.put(user,data);
            data.addUnlockedAbility(ProjectionAbility.INSTANCE,AbilityUnlock.PROGRESSION);data.setEquippedAbility(0,marble);data.setEquippedAbility(1,projection);
        }
        IAbility roll() {ProjectionAbility current=data.getEquippedAbility(ProjectionAbility.INSTANCE);LegendaryWeaponTrace.roll(user,current);IAbility result=data.equipped.get(1);require(result!=null&&result.getCore()!=ProjectionAbility.INSTANCE,"A valid first press successfully rolls a move");return result;}
        void tick(){LegendaryWeaponTrace.tick(new TickEvent.PlayerTickEvent(user,TickEvent.Phase.END));}
        void assertReturned(String reason) {assertReturned(reason,200);}
        void assertReturned(String reason,float cooldownTicks) {
            ProjectionAbility restored=data.getEquippedAbility(ProjectionAbility.INSTANCE);
            require(restored!=null,"Projection returns after "+reason);
            require("LEGENDARY_WEAPON_TRACE".equals(restored.mode),"The selected Projection mode survives "+reason);
            require(restored.cooldown.ticks==cooldownTicks,"Projection receives the expected "+cooldownTicks+" tick return cooldown after "+reason+"; actual "+restored.cooldown.ticks);
            require(!LegendaryWeaponTrace.hasActiveRoll(user),"The roll state clears after "+reason);
        }
    }
}
