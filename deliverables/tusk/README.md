# Tusk: Guru Awakening

Use one Awakening Essence while holding Guru Guru no Mi's power. The server must
have Mine Mine no Mi awakenings enabled. Existing Essence cooldown and configured
kill requirements still apply.

Awakening removes every original Guru ability from unlocked, equipped, and passive
slots, and blocks them from being unlocked or used while awakened. Other fighting
styles and Haki are preserved. Existing awakened Guru players receive Act 1 on
login. Losing the fruit removes the Tusk loadout; saved mastery is retained.

## Acts and Mastery

- Act 1: Tusk, Nail Shot, Nail Shotgun, Spin Glide.
- Act 2: Golden Nail upgrade, Pursuing Hole, Spiral Burst.
  Unlock with 40 Nail Shot hits, including 10 hits from at least 8 blocks away.
- Act 3: Wormhole Slip, Wormhole Shot, Dimensional Dodge.
  Unlock with 24 pursuing-hole hits and 8 successful Spiral Burst casts.
- Act 4: Stand Barrage, Golden Beatdown, Infinite Rotation.
  Unlock with 20 Wormhole Shot hits and 12 successful Dimensional Dodge windows.

Use Tusk to manifest/dismiss the Stand and show mastery progress. Its normal
alternate-mode control cycles unlocked Acts. Later Acts can use earlier moves.

The HUD shows ten regenerating nails and Golden Spin. Landing attacks generates
Spin; sprinting in combat generates additional Spin. Spin slowly decays out of
combat. Nail costs, Spin costs, and cooldowns are shown on each ability.

Infinite Rotation consumes a full Spin meter, charges for 2.5 seconds, then fires
a golden nail. It applies ten seconds of damage and slowing, does not stack or
refresh an active effect, and has a 120-second cooldown. It is not a permanent stun.

## Models

Open any `tusk_act_1.bbmodel` through `tusk_act_4.bbmodel` in Blockbench. The models
use editable cubes and low-poly meshes, with embedded 512 x 512 pixel-texture
atlases. Each contains 15 clips: idle, follow, summon, dismiss, shoot, shotgun,
hole, wormhole, glide, burst, slip, dodge, barrage, beatdown, infinite.

Part counts: Act 1 = 24; Act 2 = 26; Act 3 = 47; Act 4 = 83. A part may contain
multiple faces, so these are not cube or polygon counts.

The refined models have more distinct silhouettes, tapered tusks and claws,
faceted armor, cleaner face markings, and solid hanging armor plates on Act 4.
Stepped pixel shading, seams, joints, and small star motifs supply surface detail.
All existing animation tracks and the bone hierarchy are retained.

The runtime adds smooth transitions, following motion, portal meshes, spirals,
projectile trails, impact rings, and barrage streaks.

After saving edits, run `node tools/build_tusk.cjs` from the project root. The
default builder preserves saved Blockbench geometry, textures, and animation
tracks, and exports matching runtime rigs and textures. Do not pass `--refine`
when exporting your own edits: that option replaces geometry and textures with
the authored refinement. The exporter uses `build_shikigami.cjs`,
`tusk_model_refinement.cjs`, and Sharp; set `SHARP_MODULE` to a local Sharp
installation when building on another machine.

Client HUD placement is configurable in `kazimod-tusk-client.toml`.

## Verification

The isolated Forge client checks cover awakening replacement, mastery progression,
resource accounting, native ability use/cooldowns, Haki projectile flags, projectile
hits, surface-following holes, burst deduplication, dodge exclusions, damage-over-time
expiry, state persistence, and bounded model/VFX geometry. Model previews are rendered
through Minecraft's actual rendering pipeline. The test world stubs networking and
region storage; live multiplayer balance remains to be play-tested.

The refinement also checks preserved animation data, bone hierarchy, valid UVs,
embedded textures, and lossless default export of saved assets.

The models and atlases are newly authored Minecraft-style interpretations, based on
Tusk reference imagery, not extracted game assets. Existing Minecraft sounds are used.
