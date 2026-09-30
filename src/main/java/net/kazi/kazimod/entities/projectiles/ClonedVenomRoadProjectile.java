package net.kazi.kazimod.entities.projectiles;

import net.kazi.kazimod.abilities.DokuRework.NewVenomRoadRework;
import net.MrMagicalCart.cartaddon.entities.projectiles.dokuextra.NewDokuProjectiles;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.api.protection.BlockProtectionRule;
import xyz.pixelatedw.mineminenomi.api.protection.DefaultProtectionRules;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.init.ModBlocks;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class ClonedVenomRoadProjectile
extends AbilityProjectileEntity
implements IEntityAdditionalSpawnData {
    private boolean isDemonMode = false;
    private Vector3d startPos;
    private NewVenomRoadRework parent;
    private int spread = 4;

    public ClonedVenomRoadProjectile(EntityType type, World world) {
        super(type, world);
    }

    public ClonedVenomRoadProjectile(World world, LivingEntity player, NewVenomRoadRework parent, boolean isDemonMode) {
        super((EntityType)NewDokuProjectiles.NEW_VENOM_ROAD.get(), world, player, NewVenomRoadRework.INSTANCE);
        this.parent = parent;
        this.setDamage(isDemonMode ? 15.0f : 8.0f);
        this.noCulling = true;
        this.setPassThroughEntities();
        this.isDemonMode = isDemonMode;
        this.startPos = player.position();
        this.onEntityImpactEvent = this::onEntityImpactEvent;
        this.onBlockImpactEvent = this::onBlockImpactEvent;
    }

    private void onBlockImpactEvent(BlockPos pos) {
        LivingEntity thrower = this.getThrower();
        if (thrower != null) {
            if (AbilityHelper.canUseMomentumAbilities((LivingEntity)thrower)) {
                BlockState blockState = thrower.level.getBlockState(pos);
            }
            int fails = 0;
            BlockPos.Mutable mutpos = new BlockPos.Mutable();
            int i = 0;
            while (i < this.getPoisonBlockAmount() && fails <= 100) {
                double offsetX = WyHelper.randomWithRange((int)(-this.spread), (int)this.spread);
                double offsetY = WyHelper.randomWithRange((int)-2, (int)2);
                double offsetZ = WyHelper.randomWithRange((int)(-this.spread), (int)this.spread);
                mutpos.set(this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ);
                if (this.level.getBlockState(mutpos.below()).getMaterial().isSolid() && AbilityHelper.placeBlockIfAllowed((LivingEntity)this.getThrower(), (BlockPos)mutpos, (BlockState)this.getPoisonBlock().defaultBlockState(), (BlockProtectionRule)DefaultProtectionRules.AIR_FOLIAGE)) {
                    ++i;
                    continue;
                }
                ++fails;
            }
        }
        this.parent.getComponent(ModAbilityKeys.COOLDOWN).ifPresent(comp -> comp.startCooldown(this.getThrower(), 120.0f));
    }

    private void onEntityImpactEvent(LivingEntity hitEntity) {
        hitEntity.addEffect(new EffectInstance((Effect)ModEffects.DOKU_POISON.get(), 300, this.isDemonMode ? 3 : 1));
        hitEntity.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 400, this.isDemonMode ? 2 : 1));
        hitEntity.addEffect(new EffectInstance(Effects.DIG_SLOWDOWN, 400, this.isDemonMode ? 2 : 1));
        hitEntity.addEffect(new EffectInstance(Effects.CONFUSION, 400, this.isDemonMode ? 2 : 1));
        this.onBlockImpactEvent.onImpact(hitEntity.blockPosition());
    }

    public void writeSpawnData(PacketBuffer buffer) {
        super.writeSpawnData(buffer);
        buffer.writeBoolean(this.isDemonMode);
        buffer.writeDouble(this.startPos != null ? this.startPos.x : 0.0);
        buffer.writeDouble(this.startPos != null ? this.startPos.y : 0.0);
        buffer.writeDouble(this.startPos != null ? this.startPos.z : 0.0);
    }

    public void readSpawnData(PacketBuffer buffer) {
        super.readSpawnData(buffer);
        this.isDemonMode = buffer.readBoolean();
        this.startPos = new Vector3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public int getPoisonBlockAmount() {
        return 40;
    }

    public Block getPoisonBlock() {
        return this.isDemonMode ? (Block)ModBlocks.DEMON_POISON.get() : (Block)ModBlocks.POISON.get();
    }

    public Vector3d getStartPos() {
        return this.startPos;
    }

    public boolean isDemonMode() {
        return this.isDemonMode;
    }
}

