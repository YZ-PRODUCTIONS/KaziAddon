import net.kazi.kazimod.abilities.NagiAwakening.InnerDragonsCutscenes;
import net.kazi.kazimod.abilities.cinematics.AwakeningCutsceneType;
import net.kazi.kazimod.abilities.cinematics.ChaosBeamFinaleLift;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosBeamRecoil;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosBeamHealing;
import net.kazi.kazimod.init.KaziPacketHandler;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import xyz.pixelatedw.mineminenomi.api.events.*;

/** Production scene membership and damage event boundaries without launching Minecraft. */
public final class ChaosFinaleProtectionTest {
    private static int checks;
    private static final AwakeningCutsceneType FINALE=AwakeningCutsceneType.CHAOS_BEAM_FINALE;

    public static void main(String[] args) {
        boundaries();
        otherScenes();
        audience();
        lifecycle();
        replacement();
        delayedRecoil();
        recoilLifecycle();
        recoilCallbacks();
        System.out.println("Chaos finale protection: "+checks+" checks passed.");
    }

    private static void boundaries() {
        Fixture f=new Fixture();f.start();
        for(int age=0;age<352;age++) {
            f.world.gameTime=age;
            require(protectedByScene(f.caster),"Caster protected at tick "+age+" before END");
            require(protectedByScene(f.viewer),"Viewer protected at tick "+age+" before END");
            require(!protectedByScene(f.outsider),"Unselected players are never protected");
            if(age==311||age==312||age==351)allDamageEvents(f.viewer,true);
            tick();
            require(noEffects(),"No healing or self-damage before the entire fade completes at tick "+age);
            require(protectedByScene(f.caster)&&protectedByScene(f.viewer),"No protection gap during scene-to-fade handoff");
            if(age>=312)require(ChaosBeamFinaleLift.finishes==1,"Lift finished exactly once while tail remains");
        }
        f.world.gameTime=352;
        require(!protectedByScene(f.caster)&&!protectedByScene(f.viewer),"Protection ends exactly when return fade reaches zero");
        allDamageEvents(f.viewer,false);
        tick();
        require(ChaosBeamRecoil.hits.size()==1,"The completed finale applies recoil exactly once at the deadline");
        ChaosBeamRecoil.Hit recoil=ChaosBeamRecoil.hits.get(0);
        require(recoil.caster==f.caster&&recoil.tick==352&&!recoil.protectedAtHit,"Only the caster is hurt, after fade protection expires");
        pairedEffects(1);
        tick();f.world.gameTime=400;tick();
        require(ChaosBeamRecoil.hits.size()==1,"Repeated and later ticks never replay the penalty");
        pairedEffects(1);
        require(ChaosBeamFinaleLift.ticks==312,"Lift ends with camera rather than running through return fade");
        require(ChaosBeamFinaleLift.completed==1&&ChaosBeamFinaleLift.aborted==0,"Normal cleanup retains correct completion kind");
    }

    private static void otherScenes() {
        for(AwakeningCutsceneType type:AwakeningCutsceneType.values()) {
            if(type==FINALE)continue;
            Fixture f=new Fixture();InnerDragonsCutscenes.start(f.caster,0,type);
            f.world.gameTime=type.durationTicks-1;
            require(protectedByScene(f.viewer),"Existing scene keeps original final protected tick");
            f.world.gameTime=type.durationTicks;
            require(!protectedByScene(f.viewer),"Other scenes gain no fade protection extension");
            tick();
            require(!protectedByScene(f.viewer),"Other scene cleanup stays expired");
            f.world.gameTime=1000;tick();
            require(noEffects(),"Other cinematic types never award Chaos Beam healing or recoil");
        }
    }

    private static void audience() {
        Fixture f=new Fixture();f.start();
        ServerPlayerEntity late=f.world.add(10);
        require(!protectedByScene(late),"Late arrival is not part of the cinematic audience");
        f.world.gameTime=312;tick();
        require(!protectedByScene(late),"Late arrival gains no return-fade immunity");
        f.viewer.x=200;
        require(protectedByScene(f.viewer),"Original viewer stays protected after moving beyond the original radius");

        f=new Fixture();
        ServerPlayerEntity earlierCaster=f.world.add(180);
        f.viewer.x=90;
        InnerDragonsCutscenes.start(earlierCaster,0,AwakeningCutsceneType.INNER_DRAGONS);
        f.start();
        f.world.gameTime=160;tick();
        require(!protectedByScene(f.viewer),"Viewer busy with another scene was excluded from finale");
        f.world.gameTime=312;tick();
        require(!protectedByScene(f.viewer),"Excluded viewer receives no finale tail");
    }

    private static void lifecycle() {
        Fixture f=new Fixture();f.start();f.world.gameTime=312;
        f.caster.eligible=false;
        require(protectedByScene(f.viewer),"Eligibility loss after cinematic cannot truncate other viewers' fade");
        tick();require(protectedByScene(f.viewer),"Fade survives caster eligibility loss at handoff");
        f.world.gameTime=320;InnerDragonsCutscenes.logout(new PlayerEvent.PlayerLoggedOutEvent(f.caster));
        require(protectedByScene(f.viewer),"Caster logout preserves other viewers' fade protection");
        require(!protectedByScene(f.caster),"Departing caster loses its own tail membership");

        f=new Fixture();f.start();f.world.gameTime=312;
        InnerDragonsCutscenes.logout(new PlayerEvent.PlayerLoggedOutEvent(f.caster));
        require(protectedByScene(f.viewer),"Caster logout exactly at boundary preserves tail before normal END migration");
        require(ChaosBeamFinaleLift.completed==1,"Boundary logout completes the lift once");

        f=new Fixture();f.start();f.world.gameTime=312;tick();
        ServerWorld destination=new ServerWorld();destination.gameTime=312;f.viewer.level=destination;
        InnerDragonsCutscenes.changedDimension(new PlayerEvent.PlayerChangedDimensionEvent(f.viewer));
        require(!protectedByScene(f.viewer),"Dimension travel removes individual fade protection");
        f.viewer.level=f.world;
        require(!protectedByScene(f.viewer),"Returning to old dimension does not resurrect fade protection");
        require(protectedByScene(f.caster),"Viewer departure preserves caster immunity");

        f=new Fixture();f.start();f.world.gameTime=312;tick();
        InnerDragonsCutscenes.logout(new PlayerEvent.PlayerLoggedOutEvent(f.viewer));
        require(!protectedByScene(f.viewer),"Viewer logout removes its own return-fade membership");
        require(protectedByScene(f.caster),"Viewer logout cannot strip the caster's protection");

        f=new Fixture();f.start();f.world.gameTime=312;tick();f.viewer.alive=false;
        require(!protectedByScene(f.viewer),"Dead viewer is not protected");tick();f.viewer.alive=true;
        require(!protectedByScene(f.viewer),"Dead-body tail is removed during cleanup");
        ServerPlayerEntity respawn=f.world.add(10);respawn.uuid=f.viewer.uuid;
        require(!protectedByScene(respawn),"Respawned body with same UUID cannot inherit old tail");
        InnerDragonsCutscenes.stopped(new FMLServerStoppedEvent());
        require(!protectedByScene(f.caster),"Server stop clears remaining protection");

        f=new Fixture();f.start();f.world.gameTime=100;InnerDragonsCutscenes.cancel(f.caster);
        require(!protectedByScene(f.caster)&&!protectedByScene(f.viewer),"Early cancellation starts no return-fade immunity");
        require(ChaosBeamFinaleLift.aborted==1,"Early cancellation releases lift once");
    }

    private static void replacement() {
        Fixture f=new Fixture();f.start();f.world.gameTime=312;tick();f.world.gameTime=320;
        f.viewer.x=200;
        InnerDragonsCutscenes.start(f.caster,320,AwakeningCutsceneType.INNER_DRAGONS);
        InnerDragonsCutscenes.cancel(f.caster);
        require(protectedByScene(f.viewer),"New cast/cancellation cannot erase previous viewers' fade immunity");
        f.world.gameTime=352;require(!protectedByScene(f.viewer),"Previous tail still ends at its own fixed deadline");

        f=new Fixture();f.start();f.world.gameTime=312;
        f.viewer.x=200;
        InnerDragonsCutscenes.start(f.caster,312,AwakeningCutsceneType.INNER_DRAGONS);
        InnerDragonsCutscenes.cancel(f.caster);
        require(protectedByScene(f.viewer),"Replacement exactly at boundary migrates old audience before replacing scene");

        f=new Fixture();f.start();f.world.gameTime=312;tick();f.world.gameTime=320;
        KaziPacketHandler.CHANNEL.sent.clear();
        InnerDragonsCutscenes.start(f.caster,320,AwakeningCutsceneType.INNER_DRAGONS);
        boolean sawNewScene=false;
        for(KaziPacketHandler.Delivery sent:KaziPacketHandler.CHANNEL.sent)
            if(sent.player==f.viewer&&!sent.packet.cancel)sawNewScene=true;
        require(sawNewScene,"Fade-only protection does not mark a viewer as still watching the old scene");
    }

    private static void delayedRecoil() {
        Fixture f=new Fixture();f.start();f.world.gameTime=400;tick();
        require(ChaosBeamRecoil.hits.size()==1&&ChaosBeamRecoil.hits.get(0).tick==400,
                "A skipped completion tick still applies one overdue recoil");
        require(!ChaosBeamRecoil.hits.get(0).protectedAtHit,"Overdue recoil is outside protection");
        tick();require(ChaosBeamRecoil.hits.size()==1,"Overdue recoil cannot repeat");
        pairedEffects(1);

        f=new Fixture();f.start();f.world.gameTime=100;InnerDragonsCutscenes.cancel(f.caster);
        f.world.gameTime=400;tick();
        require(noEffects(),"An early cancelled cutscene queues no healing or penalty");

        f=new Fixture();f.start();f.world.gameTime=312;
        InnerDragonsCutscenes.cancel(f.caster);InnerDragonsCutscenes.cancel(f.caster);tick();
        require(noEffects(),"Completion through the cancel boundary still waits for fade");
        f.world.gameTime=352;tick();
        require(ChaosBeamRecoil.hits.size()==1,"Repeated cleanup paths queue only one completed-cast penalty");
        pairedEffects(1);

        f=new Fixture();f.start();f.world.gameTime=312;tick();f.world.gameTime=320;
        InnerDragonsCutscenes.start(f.caster,320,AwakeningCutsceneType.INNER_DRAGONS);
        f.world.gameTime=352;tick();
        require(noEffects()&&protectedByScene(f.caster),"A later cinematic defers healing and recoil while the caster remains protected");
        f.world.gameTime=479;tick();
        require(noEffects(),"Healing and recoil wait through the later scene's last protected tick");
        f.world.gameTime=480;tick();
        require(ChaosBeamRecoil.hits.size()==1&&ChaosBeamRecoil.hits.get(0).tick==480
                &&!ChaosBeamRecoil.hits.get(0).protectedAtHit,"Deferred recoil fires when the later scene releases protection");
        pairedEffects(1);

        f=new Fixture();f.start();f.world.gameTime=312;tick();f.world.gameTime=320;
        InnerDragonsCutscenes.start(f.caster,320,FINALE);
        f.world.gameTime=352;tick();f.world.gameTime=632;tick();f.world.gameTime=671;tick();
        require(noEffects(),"A second finale's return fade also defers the first healing and penalty");
        f.world.gameTime=672;tick();
        require(ChaosBeamRecoil.hits.size()==2,"Two completed casts each retain their own one-time penalty");
        for(ChaosBeamRecoil.Hit hit:ChaosBeamRecoil.hits)
            require(hit.caster==f.caster&&hit.tick==672&&!hit.protectedAtHit,"Both completed-cast penalties wait for the latest fade");
        pairedEffects(2);
    }

    private static void recoilLifecycle() {
        for(int mode=0;mode<7;mode++) {
            Fixture f=new Fixture();f.start();f.world.gameTime=312;tick();f.world.gameTime=320;
            switch(mode) {
                case 0: InnerDragonsCutscenes.logout(new PlayerEvent.PlayerLoggedOutEvent(f.caster));break;
                case 1: InnerDragonsCutscenes.changedDimension(new PlayerEvent.PlayerChangedDimensionEvent(f.caster));break;
                case 2:
                    ServerPlayerEntity respawn=f.world.add(0);respawn.uuid=f.caster.uuid;
                    InnerDragonsCutscenes.cloned(new PlayerEvent.Clone(f.caster));break;
                case 3: InnerDragonsCutscenes.stopped(new FMLServerStoppedEvent());break;
                case 4: f.caster.alive=false;break;
                case 5: f.caster.removed=true;break;
                default: f.caster.level=new ServerWorld();break;
            }
            f.world.gameTime=352;tick();
            require(noEffects(),"Lifecycle cleanup drops pending healing and recoil, case "+mode);
            f.caster.alive=true;f.caster.removed=false;f.caster.level=f.world;
            f.world.gameTime=400;tick();
            require(noEffects(),"Returning to the old body cannot restore discarded healing and recoil, case "+mode);
        }

        Fixture f=new Fixture();f.start();f.world.gameTime=312;tick();
        InnerDragonsCutscenes.logout(new PlayerEvent.PlayerLoggedOutEvent(f.viewer));
        f.world.gameTime=352;tick();
        require(ChaosBeamRecoil.hits.size()==1&&ChaosBeamRecoil.hits.get(0).caster==f.caster,
                "A viewer's departure cannot remove the caster's penalty");
        pairedEffects(1);
    }

    private static void recoilCallbacks() {
        Fixture f=new Fixture();f.start();
        final ServerPlayerEntity secondCaster=f.world.add(300),nextCaster=f.world.add(600);
        InnerDragonsCutscenes.start(secondCaster,0,FINALE);
        f.world.gameTime=312;tick();
        ChaosBeamRecoil.onHit=caster->{
            require(ChaosBeamHealing.heals.stream().anyMatch(heal->heal.caster==caster&&heal.casterAlive),
                    "Allies are healed before a lethal recoil callback can kill their caster");
            caster.alive=false;
            InnerDragonsCutscenes.cancel(caster);
            InnerDragonsCutscenes.logout(new PlayerEvent.PlayerLoggedOutEvent(caster));
            InnerDragonsCutscenes.start(nextCaster,352,AwakeningCutsceneType.INNER_DRAGONS);
        };
        f.world.gameTime=352;tick();
        require(ChaosBeamRecoil.hits.size()==2,"Death cleanup and new scenes inside recoil cannot invalidate iteration or skip another caster");
        require(ChaosBeamRecoil.hits.get(0).caster!=ChaosBeamRecoil.hits.get(1).caster,
                "Each caster receives its own penalty exactly once during synchronous callbacks");
        for(ChaosBeamRecoil.Hit hit:ChaosBeamRecoil.hits)
            require(!hit.protectedAtHit,"Callbacks always see the completed caster outside protection");
        pairedEffects(2);
        ChaosBeamRecoil.onHit=null;f.world.gameTime=353;tick();
        require(ChaosBeamRecoil.hits.size()==2,"Reentrant cleanup cannot leave pending penalties behind");
        pairedEffects(2);
        require(protectedByScene(nextCaster),"A scene created during recoil remains valid on the next tick");
    }

    private static boolean noEffects() {
        return ChaosBeamRecoil.hits.isEmpty()&&ChaosBeamHealing.heals.isEmpty()&&ChaosBeamHealing.order.isEmpty();
    }

    private static void pairedEffects(int expected) {
        require(ChaosBeamHealing.heals.size()==expected,"Each completed cast heals allies exactly once");
        require(ChaosBeamRecoil.hits.size()==expected&&ChaosBeamHealing.order.size()==expected*2,
                "Healing and recoil remain paired without duplicate callbacks");
        for(int i=0;i<expected;i++) {
            ChaosBeamHealing.Heal heal=ChaosBeamHealing.heals.get(i);
            ChaosBeamRecoil.Hit hit=ChaosBeamRecoil.hits.get(i);
            require(heal.caster==hit.caster&&heal.tick==hit.tick,
                    "The ally-healing helper receives the same caster context and deadline as recoil");
            require(heal.casterAlive&&!heal.protectedAtHeal,"Healing occurs while the caster is alive after protection has ended");
            require(ChaosBeamHealing.order.get(i*2).equals("heal:"+hit.caster.getUUID())
                    &&ChaosBeamHealing.order.get(i*2+1).equals("hurt:"+hit.caster.getUUID()),
                    "Every cast heals allies before its potentially lethal self-damage");
        }
    }

    private static void allDamageEvents(ServerPlayerEntity player,boolean expected) {
        LivingAttackEvent a=new LivingAttackEvent(player);InnerDragonsCutscenes.attack(a);require(a.canceled==expected,"Vanilla attack gate");
        LivingHurtEvent h=new LivingHurtEvent(player);InnerDragonsCutscenes.hurt(h);require(h.canceled==expected,"Vanilla hurt gate");
        LivingDamageEvent d=new LivingDamageEvent(player);InnerDragonsCutscenes.damage(d);require(d.canceled==expected,"Vanilla damage gate");
        WyLivingAttackEvent wa=new WyLivingAttackEvent(player);InnerDragonsCutscenes.nativeAttack(wa);require(wa.canceled==expected,"MMNM attack gate");
        WyLivingHurtEvent wh=new WyLivingHurtEvent(player);InnerDragonsCutscenes.nativeHurt(wh);require(wh.canceled==expected,"MMNM hurt gate");
        WyLivingDamageEvent wd=new WyLivingDamageEvent(player);InnerDragonsCutscenes.nativeDamage(wd);require(wd.canceled==expected,"MMNM damage gate");
    }

    private static void tick(){InnerDragonsCutscenes.tick(new TickEvent.ServerTickEvent(TickEvent.Phase.END));}
    private static boolean protectedByScene(ServerPlayerEntity p){return InnerDragonsCutscenes.isProtected(p);}
    private static void require(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static final class Fixture {
        final ServerWorld world=new ServerWorld();final ServerPlayerEntity caster,viewer,outsider;
        Fixture(){
            InnerDragonsCutscenes.stopped(new FMLServerStoppedEvent());ChaosBeamFinaleLift.reset();ChaosBeamRecoil.reset();ChaosBeamHealing.reset();KaziPacketHandler.CHANNEL.sent.clear();
            caster=world.add(0);viewer=world.add(20);outsider=world.add(101);
        }
        void start(){InnerDragonsCutscenes.start(caster,0,FINALE);}
    }
}
