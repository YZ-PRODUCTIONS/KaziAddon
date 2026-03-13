package net.kazi.kazimod.abilities.BomuRework;

import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.network.play.server.SAnimateHandPacket;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCategory;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityCore;
import xyz.pixelatedw.mineminenomi.api.abilities.AbilityDescriptionLine;
import xyz.pixelatedw.mineminenomi.api.abilities.DropHitAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.ExplosionAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.AbilityComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.CooldownComponent;
import xyz.pixelatedw.mineminenomi.api.abilities.components.DealDamageComponent;
import xyz.pixelatedw.mineminenomi.api.damagesource.AbilityDamageSource;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceElement;
import xyz.pixelatedw.mineminenomi.api.damagesource.SourceHakiNature;
import xyz.pixelatedw.mineminenomi.api.helpers.AbilityHelper;
import xyz.pixelatedw.mineminenomi.init.ModDamageSource;
import xyz.pixelatedw.mineminenomi.particles.effects.CommonExplosionParticleEffect;
import xyz.pixelatedw.mineminenomi.wypi.WyHelper;

public class KickBombRework extends DropHitAbility {

    private static final ITextComponent[] DESCRIPTION = AbilityHelper.registerDescriptionText(
            "mineminenomi", "kick_bomb",
            new Pair[]{ImmutablePair.of("The user leaps into the air and kicks the ground on landing, detonating their leg in a massive explosion", (Object) null)}
    );

    private static final float COOLDOWN = 400.0F;
    private static final float DAMAGE = 70.0F;
    private static final int AOE_RADIUS = 5; // 5 each direction = 10x10x10

    public static final AbilityCore<KickBombRework> INSTANCE;

    private final DealDamageComponent dealDamageComponent = new DealDamageComponent(this);

    public KickBombRework(AbilityCore<KickBombRework> core) {
        super(core);
        this.isNew = true;
        this.addComponents(new AbilityComponent[]{this.dealDamageComponent});
        this.continuousComponent.addStartEvent(100, this::onStartContinuityEvent);
        this.continuousComponent.addTickEvent(100, this::onContinuityTick);
        this.continuousComponent.addEndEvent(100, this::endContinuityEvent);
    }

    // Called by DropHitAbility when the entity lands — this is where we detonate
    @Override
    public void onLanding(LivingEntity entity) {
        if (!entity.level.isClientSide) {
            // Animate hand swing like Hiryu Kaen
            ((ServerWorld) entity.level).getChunkSource().broadcastAndSend(entity, new SAnimateHandPacket(entity, 0));

            double cx = entity.getX();
            double cy = entity.getY();
            double cz = entity.getZ();
            World world = entity.level;

            // Core explosion at landing point
            ExplosionAbility explosion = AbilityHelper.newExplosion(entity, world, cx, cy, cz, 7.0F);
            explosion.setStaticDamage(DAMAGE);
            explosion.setSmokeParticles(new CommonExplosionParticleEffect(8));
            explosion.doExplosion();

            // 10x10x10 AOE shockwave
            doAoeBlast(entity, world, cx, cy, cz);
        }
    }

    // Leap into the air on activation
    private void onStartContinuityEvent(LivingEntity entity, IAbility ability) {
        Vector3d speed = WyHelper.propulsion(entity, 1.1, 1.1);
        AbilityHelper.setDeltaMovement(entity, speed.x, 1.75F, speed.z);
    }

    // While in the air, drive the entity downward to slam (mirrors Hiryu Kaen tick)
    private void onContinuityTick(LivingEntity entity, IAbility ability) {
        if (this.continuousComponent.getContinueTime() >= 15.0F) {
            Vector3d speed = entity.getLookAngle().multiply(1.25F, 1.0F, 1.25F);
            AbilityHelper.setDeltaMovement(entity, speed.x, -3.0F, speed.z);
        }
    }

    private void endContinuityEvent(LivingEntity entity, IAbility ability) {
        this.cooldownComponent.startCooldown(entity, COOLDOWN);
    }

    private void doAoeBlast(LivingEntity source, World world, double cx, double cy, double cz) {
        // Damage entities — same pattern as Hiryu Kaen
        AbilityDamageSource dmgSource = (AbilityDamageSource) ModDamageSource
                .causeAbilityDamage(source, this.getCore())
                .setExplosion();
        dmgSource.setUnavoidable();


        world.getEntitiesOfClass(LivingEntity.class,
                new AxisAlignedBB(
                        cx - AOE_RADIUS, cy - AOE_RADIUS, cz - AOE_RADIUS,
                        cx + AOE_RADIUS, cy + AOE_RADIUS, cz + AOE_RADIUS
                )
        ).forEach(target -> {
            if (target != source) {
                this.dealDamageComponent.hurtTarget(source, target, DAMAGE, dmgSource);
            }
        });

        // Block destruction + explosion particles across full 10x10x10
        int icx = (int) Math.floor(cx);
        int icy = (int) Math.floor(cy);
        int icz = (int) Math.floor(cz);

        for (int x = icx - AOE_RADIUS; x <= icx + AOE_RADIUS; x++) {
            for (int y = icy - AOE_RADIUS; y <= icy + AOE_RADIUS; y++) {
                for (int z = icz - AOE_RADIUS; z <= icz + AOE_RADIUS; z++) {
                    BlockPos pos = new BlockPos(x, y, z);

                    // Destroy non-air, non-bedrock blocks
                    if (!world.isEmptyBlock(pos) && world.getBlockState(pos).getBlock() != Blocks.BEDROCK) {
                        world.destroyBlock(pos, true, source);
                    }

                    // Explosion particle at every block in the AOE
                    if (world instanceof ServerWorld) {
                        ((ServerWorld) world).sendParticles(
                                ParticleTypes.EXPLOSION,
                                x + 0.5, y + 0.5, z + 0.5,
                                1, 0, 0, 0, 0.0
                        );
                    }
                }
            }
        }

        // Big emitter burst at epicenter
        if (world instanceof ServerWorld) {
            ((ServerWorld) world).sendParticles(
                    ParticleTypes.EXPLOSION_EMITTER,
                    cx, cy, cz,
                    3, 1.0, 1.0, 1.0, 0.5
            );
        }
    }

    static {
        INSTANCE = (new AbilityCore.Builder("Kick Bomb", AbilityCategory.DEVIL_FRUITS, KickBombRework::new))
                .addDescriptionLine(DESCRIPTION)
                .addAdvancedDescriptionLine(new AbilityDescriptionLine.IDescriptionLine[]{
                        AbilityDescriptionLine.NEW_LINE,
                        CooldownComponent.getTooltip(COOLDOWN),
                        DealDamageComponent.getTooltip(DAMAGE)
                })
                .setSourceHakiNature(SourceHakiNature.HARDENING)
                .setSourceElement(SourceElement.EXPLOSION)
                .build();
    }
}