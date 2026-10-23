KAKE KAKE NO MI - CASINO VFX REWORK

Slot Spin: floating red-and-gold slot cabinet, animated lever, three cylindrical
number reels, sequential braking, animated bulbs, reveal chime and final result.
The reels resolve to the server's roll; the matching HUD shows that same number.

Coin Flick: embossed gold coin model, luminous motion ribbon and coin-burst hit.
Loaded Dice: tumbling gold-edged dice with pips and custom dice-shard impact.
Card Slash: two-sided suit-patterned cards, teal trails and crossed hit slashes.
Double Down: paired orbiting dice and roulette seal.
Jackpot Shot: rotating coin-ring muzzle with individual gold coin projectiles.
Casino Chip Rain: overhead roulette ring, downward light trails, red/teal chips.
Lucky Seven: gold-edged giant dice, synchronized hover/drop/impact phases, floor
warning seals and seven markings, impact shards, and a short model collapse.
Casino Storm: fixed-origin 40-block roulette boundary, tall luminous spirals,
orbiting cards/chips, and reworked visuals for all its existing projectiles.
Jackpot: mobile 30-block boundary, personal coin halo, golden rays and 777 crown.
Slot Spin, Casino Roll and Lucky Slot have newly rendered transparent icons.

Damage, cooldowns, roll probabilities, durations and projectile counts unchanged.
Chips have their own entity type so clients no longer render them as coins.
Native small explosions still calculate damage and knockback; their vanilla
particle broadcast is replaced by model-based impacts. Status-effect bubbles
are hidden for Kake's hit effects; the status effects themselves remain.
Legacy particle registry entries remain for world compatibility, but Kake's
ability/projectile code no longer emits them.

Performance: one synchronized controller per aura, deterministic client motion,
no per-frame network updates, distance-reduced geometry, impact-controller limit
of 24 per world tick. Models use reusable vertex transforms. Mesh tests enforce
a 16,000-vertex ceiling per effect; current largest tested mesh is 14,480 vertices.
These are code/geometry limits, not an in-game FPS measurement.

Validation: tools/KakeVisualCheck.java tests reel results for all rolls, smooth
braking, finite geometry/opacity and mesh budgets; generates the preview/icons.
The preview uses the exact model/VFX geometry with a software renderer.
Live multiplayer rendering, shaders and FPS still require an in-game test.
