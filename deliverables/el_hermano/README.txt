EL HERMANO WORLD BOSS

Model: el_hermano.bbmodel (editable Blockbench geometry, embedded texture, nine animation clips).
The model is 3.6 blocks tall, with 25 low-poly parts and 16 animation bones.
Its palette, tall head, muscular shape, uniform, gloves and boots follow the supplied reference.

SPAWNING
- One natural El Hermano at a time, in the Overworld.
- Random 30-60 minute delay, counting active server time while survival/adventure players are present.
- Timer pauses while a natural boss is alive or while no eligible players are in the Overworld.
- Spawns 64-112 blocks from a player on clear, dry ground in an already loaded chunk.
- If there is no suitable location, retries after one minute without generating terrain.
- Global chat announces XYZ coordinates and dimension. Boss has a red health bar.
- Timer, active boss identity, health and full-power state persist in world saves.
- The next 30-60 minute timer starts after death. Unloaded bosses keep their occupied slot.
- Configure in config/kazimod.toml under [kazimod.elHermano]:
  naturalSpawning, minimumSpawnMinutes, maximumSpawnMinutes.

COMBAT
- 825 base HP and 50,000 Doriki, scaled using the same Doriki-limit ratio as the Sukuna boss.
- Actual full netherite equipment with Protection IV, unbreakable and not dropped/rendered.
- Internal Destruction damage source plus the native Haoshoku Infusion damage bonus on all attacks.
- Heavy Punch Combo: three timed strikes, finishing with knockback.
- Power Rush: telegraphed forward rush; each victim can be hit once per rush.
- Invisible Pressure: eye-pressure attack in a forward cone, blocked by terrain.
- Power Impact: charged red/orange Ki projectile that explodes on blocks or entities.
- Counter Impact: guarded stance halves incoming attack damage and retaliates with a shockwave.
- Full Power: activates below 45% HP; explosive power-up, red aura, faster attacks, 20% more damage.
- Boss can jump toward airborne enemies, steps over terrain, and stays within 160 blocks of its home.
- No destructive terrain explosions. A player kill awards an Awakening Essence.

TESTING / MANUAL SPAWN
/summon kazimod:el_hermano ~ ~ ~
Command-spawned bosses announce themselves but do not occupy the natural-spawn slot.

ASSETS
Runtime model: assets/kazimod/models/entities/el_hermano.json
Texture: assets/kazimod/textures/entities/el_hermano.png
Generator: tools/build_hermano.cjs (uses the existing build_shikigami.cjs bake helpers).
Regenerating overwrites the generated Blockbench source; keep manual model edits before regenerating.
Effects are bounded animated meshes rather than particle swarms.
