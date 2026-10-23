package net.kazi.kazimod.mahoraga;

import java.util.UUID;

/** Runs without Minecraft so timer and tactical regressions can be checked quickly. */
public final class MahoragaLogicTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static void near(float expected,float value,String message){check(Math.abs(expected-value)<.00001,message+": "+value);}
    private static void tick(AdaptationMemory memory,long from,long to){for(long now=from;now<=to;now++)memory.tick(now);}
    public static void main(String[] args){
        AdaptationMemory memory=new AdaptationMemory();
        tick(memory,0,1000);check(memory.turns()==0,"No adaptation before being hit");
        memory.observe("ability:fireball","element:FIRE",1000);
        tick(memory,1000,1199);check(memory.progress()==0,"Full 10-second quiet period");
        memory.tick(1200);check(memory.progress()==1,"Wheel starts after ten seconds");
        tick(memory,1201,1218);near(1,memory.multiplier("ability:fireball","element:FIRE"),"No early resistance");
        memory.observe("ability:fireball","element:FIRE",1219);
        check(memory.progress()==0,"Hit interrupts a turning wheel");
        tick(memory,1219,1418);check(memory.turns()==0,"Interrupted turn does not count");
        tick(memory,1419,1438);check(memory.turns()==1,"Turn completes after renewed quiet period");
        near(.67F,memory.multiplier("ability:fireball","element:FIRE"),"Exact attack reduces damage by 33 percent");
        near(.85F,memory.multiplier("ability:other_fire","element:FIRE"),"Different fire attack reduces by 15 percent");
        near(1,memory.multiplier("ability:ice","element:ICE"),"Unrelated attack unchanged");
        near(1,memory.multiplier("","element:FIRE"),"Unadaptable source bypasses all learned resistance");
        tick(memory,1439,1657);near(.34F,memory.multiplier("ability:fireball","element:FIRE"),"Second exact adaptation");
        tick(memory,1658,1876);near(.01F,memory.multiplier("ability:fireball","element:FIRE"),"Third exact adaptation");
        tick(memory,1877,2095);near(0,memory.multiplier("ability:fireball","element:FIRE"),"Fourth exact adaptation reaches immunity");
        tick(memory,2096,2752);near(0,memory.multiplier("ability:other_fire","element:FIRE"),"Seventh category adaptation reaches immunity");
        int turns=memory.turns();tick(memory,2753,4000);check(memory.turns()==turns,"Fully learned memory stops redundant turns");
        memory.observe("ability:ice","element:ICE",4000);tick(memory,4000,4219);
        near(.67F,memory.multiplier("ability:ice","element:ICE"),"New attack learns independently");
        near(0,memory.multiplier("ability:fireball","element:FIRE"),"Previous immunity retained");
        memory.observe("ability:cut","type:SLASH",4220);memory.observe("","",4300);
        tick(memory,4300,4499);check(memory.progress()==0,"Special hit resets timer too");
        tick(memory,4500,4519);near(.67F,memory.multiplier("ability:cut","type:SLASH"),"Special hit never becomes an adaptation target");
        AdaptationMemory bounded=new AdaptationMemory();long now=0;
        for(int i=0;i<160;i++){bounded.observe("attack:"+i,"type:"+i,now);tick(bounded,now,now+219);now+=220;}
        check(bounded.attacks.size()==128,"Attack memory is bounded");check(bounded.categories.size()==32,"Category memory is bounded");
        AdaptationMemory unknown=new AdaptationMemory();unknown.observe("environment:test",null,0);tick(unknown,0,1000);
        near(0,unknown.multiplier("environment:test",null),"Exact-only environmental adaptation");
        AdaptationMemory batch=new AdaptationMemory();
        batch.observe("ability:fireball","element:FIRE",0);
        batch.observe("ability:flame_lance","element:FIRE",40);
        batch.observe("ability:ice_spear","element:ICE",80);
        batch.observe("ability:slash","type:SLASH",100);
        batch.observe("ability:fireball","element:FIRE",120);
        tick(batch,120,319);check(batch.progress()==0,"Batch still requires ten quiet seconds after the last hit");
        tick(batch,320,339);check(batch.turns()==1,"One wheel turn commits the entire batch");
        for(String attack:new String[]{"ability:fireball","ability:flame_lance","ability:ice_spear","ability:slash"})
            near(.67F,batch.multiplier(attack,null),"Batch adapts every distinct attack: "+attack);
        near(.85F,batch.multiplier("ability:other_fire","element:FIRE"),"Shared fire category advances once, not once per attack");
        near(.85F,batch.multiplier("ability:other_ice","element:ICE"),"Ice category advances in the same turn");
        near(.85F,batch.multiplier("ability:other_slash","type:SLASH"),"Slash category advances in the same turn");
        batch.observe("ability:lightning","element:LIGHTNING",350);
        tick(batch,350,559);check(batch.progress()==10,"Second batch can begin turning");
        batch.observe("ability:punch","type:FIST",560);
        check(batch.progress()==0,"New attack interrupts the wheel without erasing the batch");
        tick(batch,560,778);check(batch.turns()==1,"Interrupted batch has no premature gains");
        batch.tick(779);check(batch.turns()==2,"Retained batch commits after the restarted quiet period");
        near(.67F,batch.multiplier("ability:lightning","element:LIGHTNING"),"Attack before interruption is retained");
        near(.67F,batch.multiplier("ability:punch","type:FIST"),"Interrupting attack joins the same batch");
        near(.34F,batch.multiplier("ability:fireball","element:FIRE"),"Older unfinished adaptations continue together");
        batch.observe("","element:SPECIAL",800);tick(batch,800,1019);
        check(!batch.categories.containsKey("element:SPECIAL"),"Unadaptable hits never enter the category batch");
        tick(batch,1020,3000);
        near(0,batch.multiplier("ability:lightning","element:LIGHTNING"),"All queued attacks can reach immunity");
        near(0,batch.multiplier("ability:other_fire","element:FIRE"),"Shared category continues after exact attacks are immune");
        int completed=batch.turns();batch.observe("ability:fireball","element:FIRE",3100);tick(batch,3100,4000);
        check(batch.turns()==completed,"Already immune hits do not schedule redundant turns");
        AdaptationMemory flood=new AdaptationMemory();
        for(int i=0;i<200;i++)flood.observe("ability:"+i,"type:"+i,i);
        tick(flood,199,418);
        check(flood.attacks.size()==128,"Pending attack batch is bounded");
        check(flood.categories.size()==32,"Pending category batch is bounded");
        near(.67F,flood.multiplier("ability:199",null),"Newest attack survives bounded batch eviction");
        near(.85F,flood.multiplier("unknown","type:199"),"Newest category survives bounded batch eviction");
        AdaptationMemory timed=new AdaptationMemory();
        timed.observe("ability:meteor","element:FIRE",0);
        tick(timed,0,219);
        long firstExpiry=timed.attackExpires.get("ability:meteor");
        check(firstExpiry==219+AdaptationMemory.ADAPTATION_TICKS,"Attack expiry starts at first adaptation");
        check(timed.categoryExpires.get("element:FIRE")==firstExpiry,"Category gets its own 30-minute expiry");
        timed.observe("ability:meteor","element:FIRE",220);
        tick(timed,220,439);
        near(.34F,timed.multiplier("ability:meteor","element:FIRE"),"Repeated adaptation increases resistance");
        check(timed.attackExpires.get("ability:meteor")==firstExpiry,"Repeated wheel turns do not refresh expiry");
        timed.tick(firstExpiry-1);
        check(timed.attacks.containsKey("ability:meteor"),"Adaptation remains until its deadline");
        timed.tick(firstExpiry);
        near(1,timed.multiplier("ability:meteor","element:FIRE"),"Exact and category adaptation expire at 30 minutes");
        timed.observe("ability:meteor","element:FIRE",firstExpiry+1);
        tick(timed,firstExpiry+1,firstExpiry+220);
        near(.67F,timed.multiplier("ability:meteor","element:FIRE"),"Expired attacks can be learned again");
        CombatMemory combat=new CombatMemory();CombatMemory.Profile aggressive=combat.opponent(UUID.randomUUID());
        for(int i=0;i<12;i++)aggressive.sample(4,true,false);
        check(aggressive.aggressive(),"Recognizes sustained melee pressure");
        CombatMemory.Profile ranged=combat.opponent(UUID.randomUUID());for(int i=0;i<8;i++)ranged.sample(14+i,false,true);
        check(ranged.kiting(),"Recognizes ranged retreating opponent");
        for(int i=0;i<100;i++)ranged.sample(10,false,false);check(!ranged.kiting(),"Old behavior decays");
        check(CombatMemory.incoming(10,0,0,-2,0,0,2.8),"Incoming projectile detected");
        check(!CombatMemory.incoming(10,0,0,2,0,0,2.8),"Receding projectile ignored");
        check(!CombatMemory.incoming(10,5,0,-2,0,0,2.8),"Overhead miss ignored");
        check(!CombatMemory.incoming(10,0,0,0,0,0,2.8),"Stationary projectile ignored");
        check(!CombatMemory.incoming(40,0,0,-2,0,0,2.8),"Distant projectile outside prediction horizon ignored");
        for(MahoragaAction action:MahoragaAction.values())check(MahoragaAction.from(action.ordinal())==action,"Action mapping "+action);
        check(MahoragaAction.from(-1)==MahoragaAction.IDLE,"Invalid action safely defaults");
        check(MahoragaAction.IDLE.canRecoverIntoGuard(0),"Idle can reactively guard");
        check(!MahoragaAction.SLASH_RIGHT.canRecoverIntoGuard(15),"Guard does not cancel a slash impact");
        check(MahoragaAction.SLASH_RIGHT.canRecoverIntoGuard(16),"Slash recovery can guard");
        check(MahoragaAction.SLASH_LEFT.canRecoverIntoGuard(16),"Both slash recoveries can guard");
        check(!MahoragaAction.OVERHEAD_SLAM.canRecoverIntoGuard(24),"Slam impact is not interrupted");
        check(MahoragaAction.OVERHEAD_SLAM.canRecoverIntoGuard(29),"Slam recovery can guard");
        check(!MahoragaAction.PUNCH_BARRAGE.canRecoverIntoGuard(30),"Last barrage hit remains committed");
        check(MahoragaAction.PUNCH_BARRAGE.canRecoverIntoGuard(33),"Barrage recovery can guard");
        check(!MahoragaAction.BLOCK.canRecoverIntoGuard(20),"Incoming hits cannot refresh active guard forever");
        check(!MahoragaAction.SUMMON.canRecoverIntoGuard(70),"Guard cannot interrupt summoning");
        check(!MahoragaAction.AIR_JUMP.canRecoverIntoGuard(12),"Guard cannot interrupt aerial movement");
        check(!MahoragaAction.DISMISS.canRecoverIntoGuard(30),"Guard cannot interrupt dismissal");
        check(MahoragaAction.DISMISS.ticks==60,"Dismissal completes after three seconds");
        System.out.println("Mahoraga logic: "+checks+" checks passed.");
    }
}
