package net.kazi.kazimod.events;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.kazi.kazimod.KaziMod;
import net.kazi.kazimod.abilities.ZushiRework.ZushiAbilityBehavior;
import net.kazi.kazimod.entities.ZushiVfxEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import xyz.pixelatedw.mineminenomi.api.abilities.IAbility;
import xyz.pixelatedw.mineminenomi.api.abilities.components.ContinuousComponent;
import xyz.pixelatedw.mineminenomi.api.events.ability.AbilityTickEvent;
import xyz.pixelatedw.mineminenomi.api.events.ability.AbilityUseEvent;
import xyz.pixelatedw.mineminenomi.api.events.ability.EquipAbilityEvent;
import xyz.pixelatedw.mineminenomi.entities.projectiles.AbilityProjectileEntity;
import xyz.pixelatedw.mineminenomi.entities.projectiles.zushi.SagariNoRyuseiProjectile;
import xyz.pixelatedw.mineminenomi.init.ModAbilityKeys;

/** Full friend-jar Zushi integration through public events/callbacks, without new ability IDs or mixins. */
@Mod.EventBusSubscriber(modid = "kazimod")
public final class ZushiVisualEvents {
    private static final Map<IAbility, Visuals> ACTIVE = new WeakHashMap<>();
    private static final Map<SagariNoRyuseiProjectile, Boolean> METEORS = new WeakHashMap<>();
    private static boolean warnedMeteorTick;

    private ZushiVisualEvents() { }

    @SubscribeEvent
    public static void equipped(EquipAbilityEvent event) {
        // Saved slots can be equipped before AbilityDataBase has a data owner.
        // Throwing here aborts MMNM's entire remaining ability-data load.
        if (isSupportedServerEvent(event.getEntityLiving(), event.getAbility())) {
            ZushiAbilityBehavior.install(event.getAbility());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void beforeUse(AbilityUseEvent.Pre event) {
        if (isSupportedServerEvent(event.getEntityLiving(), event.getAbility())) {
            ZushiAbilityBehavior.install(event.getAbility());
        }
    }

    @SubscribeEvent
    public static void abilityTick(AbilityTickEvent event) {
        LivingEntity caster = event.getEntityLiving();
        IAbility ability = event.getAbility();
        if (!isSupportedServerEvent(caster, ability)) return;
        // Covers saved/command-created abilities as well as normally equipped ones.
        ZushiAbilityBehavior.install(ability);
        ContinuousComponent continuous = ability.getComponent(ModAbilityKeys.CONTINUOUS).orElse(null);
        if (!caster.isAlive() || continuous == null || !continuous.isContinuous()
                || ability.getComponent(ModAbilityKeys.DISABLE).map(disable -> disable.isDisabled()).orElse(false)) {
            clear(ability);
        }
    }

    private static boolean isSupportedServerEvent(LivingEntity caster, IAbility ability) {
        return caster != null && caster.level != null && !caster.level.isClientSide
                && ZushiAbilityBehavior.supports(ability);
    }

    public static void clear(IAbility ability) {
        Visuals old = ACTIVE.remove(ability);
        if (old != null) old.clear();
    }

    public static void refreshZone(LivingEntity caster, IAbility ability, float radius) {
        Visuals visuals = ACTIVE.computeIfAbsent(ability, unused -> new Visuals());
        if (visuals.zone != null && visuals.zone.level != caster.level) {
            visuals.zone.remove();
            visuals.zone = null;
        }
        if (visuals.zone == null || !visuals.zone.isAlive()) visuals.zone = ZushiVfxEntity.graviZone(caster, radius);
        visuals.zone.refresh(radius);
    }

    public static void refreshPressure(LivingEntity caster, IAbility ability, List<LivingEntity> targets, float size) {
        Visuals visuals = ACTIVE.computeIfAbsent(ability, unused -> new Visuals());
        visuals.pressure.values().removeIf(effect -> {
            if (effect.level != caster.level) effect.remove();
            return !effect.isAlive();
        });
        for (LivingEntity target : targets) {
            ZushiVfxEntity effect = visuals.pressure.get(target.getId());
            if (effect == null && visuals.pressure.size() < 32) {
                effect = ZushiVfxEntity.gravity(target, size);
                visuals.pressure.put(target.getId(), effect);
            }
            if (effect != null) effect.refresh(size);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void meteorJoined(EntityJoinWorldEvent event) {
        if (event.getWorld().isClientSide || !(event.getEntity() instanceof SagariNoRyuseiProjectile)) return;
        SagariNoRyuseiProjectile meteor = (SagariNoRyuseiProjectile) event.getEntity();
        if (METEORS.put(meteor, Boolean.TRUE) != null) return;
        // The friend's mixin suppresses the 25 smoke/fire particles per tick.
        // This is the complete remaining base tick body, including its collision adjustment.
        if (meteor.onTickEvent.getClass().getName().startsWith(SagariNoRyuseiProjectile.class.getName() + "$$Lambda$")) {
            meteor.onTickEvent = () -> {
                if (1 > meteor.tickCount) {
                    meteor.setBoundingBox(meteor.getBoundingBox().inflate(meteor.getSize() / 30.0F));
                }
            };
        } else if (!warnedMeteorTick) {
            warnedMeteorTick = true;
            KaziMod.LOGGER.warn("Another addon owns the meteor tick callback; keeping it instead of removing its behavior.");
        }
        AbilityProjectileEntity.IOnBlockImpact original = meteor.onBlockImpactEvent;
        boolean[] shown = {false};
        meteor.onBlockImpactEvent = hit -> {
            if (!shown[0]) {
                shown[0] = true;
                ZushiVfxEntity.impact(meteor.level, meteor.position(), Math.max(0.1F, meteor.getSize() / 30.0F));
            }
            // Always run the original callback; damage, terrain, and debris are untouched.
            original.onImpact(hit);
        };
    }

    private static final class Visuals {
        ZushiVfxEntity zone;
        final Map<Integer, ZushiVfxEntity> pressure = new HashMap<>();

        void clear() {
            if (zone != null) zone.remove();
            pressure.values().forEach(ZushiVfxEntity::remove);
            pressure.clear();
            zone = null;
        }
    }
}
