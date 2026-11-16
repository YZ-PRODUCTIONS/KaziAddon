Hyper Goner model supplied by the user

Original folder: hyper_goner_by_dj_cool_2000_db2lxb0
Original files: hyper goner.blend, hyper goner.dae, hyper goner.fbx
Creator credited by the supplied folder name: DJ Cool 2000
The unmodified Collada source is retained here as hyper_goner.dae.
No new license or ownership claim is made by this conversion.

Rebuild from the repository root:
  python tools/import_hyper_goner_model.py art/hyper_goner/hyper_goner.dae

The game asset includes only the original HYPER_GONER mesh, with its white
and black materials and authored srsly / LUAGH shape keys. Normals provide
baked neutral lighting; inconsistent mirrored surface directions are repaired
for shading only, preserving authored hard edges, positions and triangle order.
The mesh is uniformly scaled and reoriented from
Blender Z-up / -Y-forward to the cinematic's Y-up / +Z-forward coordinates.
Cameras, lights, and the four loose Cube props are scene extras and are
not part of the imported skull.

The basis includes a black triangle with vertex 6112 incorrectly extended
31.26271 units forward. The supplied OMG shape extends the same feature
further. The neutral srsly and laughing LUAGH shapes have the corrected
authored positions, so they are used without modifying their geometry.
The cutscene interpolates between these two original shapes.
HyperGonerMouth adds a separate opaque, recessed interior behind the original
mesh. Its perimeter follows fourteen source vertex anchors through both poses;
dark side walls and a capped back prevent the world showing through the mouth.
The original packaged model data is unchanged by this additional lining.
Source hash, exact bounds, triangle count and normal-repair details are recorded
in import_manifest.json. The compact model and attribution ship with the mod;
the source file and importer remain in the project for reproducible updates.
