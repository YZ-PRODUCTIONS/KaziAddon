NAGI DRAGON SUMMONS - EDITABLE BLOCKBENCH MODELS

Open any .bbmodel in Blockbench. Each contains its texture, named mesh parts,
an articulated body, a jaw rig, and two animations:
  swim: looping flight/body movement
  bite: attack motion, blended with swim by the game renderer

Models:
  first         Ivory scales, swept horns, forehead crown
  blind         Dark armor, plated eyeless brow, crown blade
  cobra         Single frontal eye, ribbed hood, long venom fangs
  four_eyed     Four eyes, green scales, swept frilled crown
  corpse_flame  Hollow ring-shaped maw and animated flame crest
  ice_crystal   Pale crystalline horns, mane, and spines
  spearhead     Black elongated spear-shaped snout and swept blades
  angel         Light gold scales, layered wings, feathered mane

The separate PNG files are copies of the embedded 256x256 texture atlases.
preview.png and head-details.png are software renders of the exported game
geometry, not screenshots from Minecraft or Blockbench.
The in-game renderer applies 55% opacity to all eight dragons. Inspection
previews remain opaque to make model details and seams easier to check.

Exporting an edited model back into the mod:
  From the project folder, run: node tools/build_nagi_dragons.cjs
  This preserves existing .bbmodel edits and exports all eight runtime models.
  Use --only=first (or another model ID) to export just one model.

The exporter supports mesh triangles/quads, the existing single 256x256
texture, named bone groups, and numeric position/rotation animation keys.
Keep the swim/bite animation names and the detail_/glow_ name prefixes.
Those prefixes control distant detail removal and full-bright accents.

WARNING: --regenerate rebuilds the authored models and overwrites manual
Blockbench changes. Do not use it after editing a model without a backup.

Verification:
  node tools/tests/test_nagi_dragon_assets.cjs --preview

The game renderer uses the matching runtime files under:
  src/main/resources/assets/kazimod/models/nagi/dragons/
  src/main/resources/assets/kazimod/textures/models/nagi/dragons/

This visual replacement does not change dragon AI, damage, targeting, or
individual powers. A live Minecraft visual test is still recommended.
