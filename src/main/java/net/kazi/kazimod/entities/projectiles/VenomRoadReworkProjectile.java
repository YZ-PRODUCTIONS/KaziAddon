//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.kazi.kazimod.entities.projectiles;

import net.MrMagicalCart.cartaddon.entities.projectiles.dokuextra.NewDokuProjectiles;
import net.kazi.kazimod.abilities.DokuRework.VenomRoadRework;
import net.minecraft.block.Block;
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
import xyz.pixelatedw.mineminenomi.api.protection.DefaultProtectionRules;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;
import xyz.pixelatedw.mineminenomi.init.ModBlocks;
import xyz.pixelatedw.mineminenomi.init.ModEffects;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;


public class VenomRoadReworkProjectile extends AbilityProjectileEntity implements IEntityAdditionalSpawnData {
    private Vector3d startPos;
    private VenomRoadRework parent;
    private int spread = 4;

    public VenomRoadReworkProjectile(EntityType type, World world) {
        super(type, world);
    }

    public VenomRoadReworkProjectile(World world, LivingEntity owner, VenomRoadRework ability) {
        super((EntityType) NewDokuProjectiles.NEW_VENOM_ROAD.get(), world, owner, VenomRoadRework.INSTANCE);
        this.parent = ability;
        this.setOwner(owner);
        this.setDamage(8.0F);
        this.noCulling = true;
        this.setPassThroughEntities();
        this.startPos = owner.position();
        this.onEntityImpactEvent = this::onEntityImpactEvent;
        this.onBlockImpactEvent = this::onBlockImpactEvent;
    }

    private void onBlockImpactEvent(BlockPos pos) {
        LivingEntity thrower = this.getThrower();
        if (thrower != null) {
            if (AbilityHelper.canUseMomentumAbilities(thrower)) {
                thrower.level.getBlockState(pos);
            }

            int fails = 0;
            BlockPos.Mutable mutpos = new BlockPos.Mutable();
            int i = 0;

            while(i < this.getPoisonBlockAmount() && fails <= 100) {
                double offsetX = WyHelper.randomWithRange(-this.spread, this.spread);
                double offsetY = WyHelper.randomWithRange(-2, 2);
                double offsetZ = WyHelper.randomWithRange(-this.spread, this.spread);
                mutpos.set(this.getX() + offsetX, this.getY() + offsetY, this.getZ() + offsetZ);
                if (this.level.getBlockState(mutpos.below()).getMaterial().isSolid() && AbilityHelper.placeBlockIfAllowed(this.getThrower(), mutpos, this.getPoisonBlock().defaultBlockState(), DefaultProtectionRules.AIR_FOLIAGE)) {
                    ++i;
                } else {
                    ++fails;
                }
            }
        }

        this.parent.getComponent(ModAbilityKeys.COOLDOWN).ifPresent((comp) -> comp.startCooldown(this.getThrower(), 240.0F));
    }

    private void onEntityImpactEvent(LivingEntity hitEntity) {
        hitEntity.addEffect(new EffectInstance((Effect)ModEffects.DOKU_POISON.get(), 300, 1));
        hitEntity.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 400, 1));
        hitEntity.addEffect(new EffectInstance(Effects.DIG_SLOWDOWN, 400, 1));
        hitEntity.addEffect(new EffectInstance(Effects.CONFUSION, 400, 1));
        this.onBlockImpactEvent.onImpact(hitEntity.blockPosition());
    }

    public void writeSpawnData(PacketBuffer buffer) {
        super.writeSpawnData(buffer);
        buffer.writeBoolean(false);
        buffer.writeDouble(this.startPos != null ? this.startPos.x : (double)0.0F);
        buffer.writeDouble(this.startPos != null ? this.startPos.y : (double)0.0F);
        buffer.writeDouble(this.startPos != null ? this.startPos.z : (double)0.0F);
    }

    public void readSpawnData(PacketBuffer buffer) {
        super.readSpawnData(buffer);
        buffer.readBoolean(); // always false, renderer will use normal visuals
        this.startPos = new Vector3d(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    public int getPoisonBlockAmount() {
        return 40;
    }

    public Block getPoisonBlock() {
        return (Block)ModBlocks.POISON.get();
    }

    public Vector3d getStartPos() {
        return this.startPos;
    }

    public boolean isDemonMode() {
        return false;
    }
}