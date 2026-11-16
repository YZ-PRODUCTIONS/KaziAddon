import net.kazi.kazimod.client.asriel.ChaosBeamCameraClient;
import net.kazi.kazimod.entities.asriel.ChaosBeamAttackEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraft.util.MovementInput;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

/** Exercise the real client event handlers and field state, without a graphics window. */
public final class ChaosBeamCameraTest {
    private static int checks;
    private static final Minecraft MC = Minecraft.getInstance();
    public static void main(String[] args) {
        Fixture own = new Fixture(); own.join(); own.turn();
        ChaosBeamCameraClient.render(new TickEvent.RenderTickEvent(TickEvent.Phase.END));
        require(own.player.yRot==123,"No after-render camera mutation");
        render(); require(own.locked(),"Reset mouse yaw and pitch before rendering");
        require(own.player.yRotO==40 && own.player.xRotO==-15,"Interpolation cannot shake between mouse and locked angles");
        for (TickEvent.Phase phase:TickEvent.Phase.values()) {
            own.turn();ChaosBeamCameraClient.tick(new TickEvent.ClientTickEvent(phase));
            require(own.locked(),"View stays locked around client simulation and movement packets");
        }
        own.world.isClientSide=false; own.world.gameTime=40;own.field.beginBeam();own.world.isClientSide=true;
        own.world.gameTime=200;own.turn();render();require(own.locked(),"Lock persists through firing");
        own.world.gameTime=280;own.turn();render();require(own.player.yRot==123,"Timer releases lock even before removal packet");

        Fixture removed = new Fixture();removed.join();removed.field.remove();removed.turn();render();
        require(removed.player.yRot==123,"Interrupted field releases immediately");
        Fixture death = new Fixture();death.join();death.player.alive=false;death.turn();render();
        require(death.player.yRot==123,"Death releases the camera");
        Fixture observer = new Fixture();observer.join();
        LivingEntity other=new LivingEntity(observer.world);other.yRot=70;MC.player=other;MC.camera=other;render();
        require(other.yRot==70,"Other players' beams never lock the viewer");
        Fixture scene = new Fixture();scene.join();MC.camera=new LivingEntity(scene.world);scene.turn();render();
        require(scene.player.yRot==123,"Do not take over a cutscene's camera");
        MC.camera=scene.player;render();require(scene.locked(),"Resume the caster lock when its own camera returns");
        scene.player.spectator=true;scene.turn();render();require(scene.player.yRot==123,"Spectator camera stays free");
        Fixture travel = new Fixture();travel.join();MC.level=new ServerWorld();travel.turn();render();
        require(travel.player.yRot==123,"Dimension changes release the old world's lock");
        Fixture logout = new Fixture();logout.join();ChaosBeamCameraClient.logout(new ClientPlayerNetworkEvent.LoggedOutEvent());
        logout.turn();render();require(logout.player.yRot==123,"Logout clears all tracked locks");
        Fixture movement=new Fixture();movement.join();
        MovementInput input=new MovementInput();input.leftImpulse=input.forwardImpulse=1;
        input.up=input.down=input.left=input.right=input.jumping=input.shiftKeyDown=true;
        movement.player.setPos(2,4,6);movement.player.setDeltaMovement(new Vector3d(1,2,3));
        ChaosBeamCameraClient.input(new InputUpdateEvent(movement.player,input));
        require(input.leftImpulse==0&&input.forwardImpulse==0&&!input.up&&!input.down&&!input.left&&!input.right
                &&!input.jumping&&!input.shiftKeyDown,"Block every movement input, including jump and flying descent");
        require(movement.player.position().lengthSqr()==0&&movement.player.getDeltaMovement().lengthSqr()==0,
                "Local simulation uses the same anchored position as the server");
        movement.player.setPos(1,2,3);render();
        require(movement.player.position().lengthSqr()==0&&movement.player.xOld==0&&movement.player.yo==0,
                "Frame interpolation cannot display movement or gravity jitter");
        movement.field.remove();input.forwardImpulse=1;input.jumping=true;movement.player.setPos(3,2,1);
        ChaosBeamCameraClient.input(new InputUpdateEvent(movement.player,input));render();
        require(input.forwardImpulse==1&&input.jumping&&movement.player.getX()==3,"Movement returns immediately on cancellation");
        Fixture different=new Fixture();different.join();LivingEntity stranger=new LivingEntity(different.world);
        input.forwardImpulse=1;ChaosBeamCameraClient.input(new InputUpdateEvent(stranger,input));
        require(input.forwardImpulse==1,"Never suppress another player's movement event");
        System.out.println("Chaos Beam camera: "+checks+" checks passed.");
    }
    private static void render(){ChaosBeamCameraClient.render(new TickEvent.RenderTickEvent(TickEvent.Phase.START));}
    private static void require(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    private static final class Fixture {
        final ServerWorld world=new ServerWorld(); final LivingEntity player=new LivingEntity(world);
        final ChaosBeamAttackEntity field;
        Fixture(){
            ChaosBeamCameraClient.logout(new ClientPlayerNetworkEvent.LoggedOutEvent());
            player.health=250;player.maxHealth=1000;
            player.yRot=40;player.xRot=-15;world.addFreshEntity(player);field=ChaosBeamAttackEntity.spawn(player);
            world.isClientSide=true;MC.level=world;MC.player=player;MC.camera=player;
        }
        void join(){ChaosBeamCameraClient.joined(new EntityJoinWorldEvent(field));}
        void turn(){player.yRot=123;player.xRot=70;}
        boolean locked(){return player.yRot==40&&player.xRot==-15;}
    }
}
