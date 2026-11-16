import java.util.UUID;
import net.kazi.kazimod.abilities.YagiAwakening.ChaosBeamAnnouncement;
import net.kazi.kazimod.network.ChaosBeamTitlePacket;
import net.kazi.kazimod.init.KaziPacketHandler;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

/** Exercise production announcement audience and packet direction, without Minecraft runtime. */
public final class ChaosBeamAnnouncementTest {
    private static int checks;
    public static void main(String[] args) {
        ServerWorld world = new ServerWorld("minecraft:overworld"), other = new ServerWorld("minecraft:the_nether");
        world.time = 417;
        ServerPlayerEntity caster = player(world,0,0,0);
        ServerPlayerEntity near = player(world,10,0,0), edge = player(world,0,100,0);
        ServerPlayerEntity far = player(world,100.01,0,0), diagonal = player(world,80,80,0);
        ServerPlayerEntity dead = player(world,1,0,0), removed = player(world,2,0,0);
        dead.alive = false; removed.removed = true;
        ServerPlayerEntity foreign = player(other,0,0,0);
        ChaosBeamAnnouncement.send(caster);
        require(KaziPacketHandler.CHANNEL.sends.size()==3,"Exactly caster, nearby player and inclusive radius edge receive title");
        for (ServerPlayerEntity target : new ServerPlayerEntity[]{caster,near,edge})
            require(KaziPacketHandler.CHANNEL.count(target)==1,"Eligible players receive one title each");
        for (ServerPlayerEntity target : new ServerPlayerEntity[]{far,diagonal,dead,removed,foreign})
            require(KaziPacketHandler.CHANNEL.count(target)==0,"Out-of-sphere, dead, removed and other dimensions excluded");
        ChaosBeamTitlePacket first=KaziPacketHandler.CHANNEL.sends.get(0).packet;
        require(first.text.equals("WHY WONT YOU LET ME WIN"),"Exact user text retained");
        require(first.startTick==417 && first.dimension.equals(world.dimension().location()),"Authoritative cast clock and dimension included");
        for(KaziPacketHandler.Send send:KaziPacketHandler.CHANNEL.sends)
            require(send.packet.session.equals(first.session),"One announcement identity shared by whole audience");
        ServerPlayerEntity late=player(world,0,0,0);
        require(KaziPacketHandler.CHANNEL.count(late)==0,"Late arrivals do not retroactively join an announcement");
        ChaosBeamAnnouncement.send(caster);
        require(KaziPacketHandler.CHANNEL.count(late)==1,"Next cast selects the new audience");
        require(!first.session.equals(KaziPacketHandler.CHANNEL.sends.get(3).packet.session),"Separate casts use separate identities");
        int sent=KaziPacketHandler.CHANNEL.sends.size();
        caster.alive=false;ChaosBeamAnnouncement.send(caster);
        require(KaziPacketHandler.CHANNEL.sends.size()==sent,"Dead caster cannot announce");

        PacketBuffer buffer = new PacketBuffer();
        ChaosBeamTitlePacket.encode(first,buffer);
        ChaosBeamTitlePacket copy=ChaosBeamTitlePacket.decode(buffer);
        require(copy.session.equals(first.session)&&copy.dimension.equals(first.dimension)
                &&copy.startTick==first.startTick&&copy.text.equals(first.text),"Packet round-trip preserves every field");
        String maximum=repeat('W',128);
        new ChaosBeamTitlePacket(UUID.randomUUID(),new ResourceLocation("minecraft:overworld"),0,maximum);
        rejects("");rejects(repeat('W',129));
        NetworkEvent.Context server = new NetworkEvent.Context(NetworkDirection.PLAY_TO_SERVER);
        ChaosBeamTitlePacket.handle(first,()->server);
        require(server.handled&&server.queued==0,"Wrong-direction packet is handled without touching client renderer");
        NetworkEvent.Context client = new NetworkEvent.Context(NetworkDirection.PLAY_TO_CLIENT);
        ChaosBeamTitlePacket.handle(first,()->client);
        require(client.handled&&client.queued==1&&net.kazi.kazimod.client.asriel.ChaosBeamTitleOverlay.last==first,
                "Client packet reaches overlay on enqueued work");
        System.out.println("Chaos Beam announcement: "+checks+" checks passed.");
    }
    private static ServerPlayerEntity player(ServerWorld world,double x,double y,double z) {
        ServerPlayerEntity p=new ServerPlayerEntity(world,x,y,z);world.players.add(p);return p;
    }
    private static String repeat(char c,int count){StringBuilder b=new StringBuilder();while(count-->0)b.append(c);return b.toString();}
    private static void rejects(String text){try{new ChaosBeamTitlePacket(UUID.randomUUID(),new ResourceLocation("minecraft:overworld"),0,text);throw new AssertionError("Invalid length accepted");}catch(IllegalArgumentException expected){checks++;}}
    private static void require(boolean pass,String message){checks++;if(!pass)throw new AssertionError(message);}
}
