# PhysicalGlass v2

Local experimental integration, 2026-10-24. Not yet visually validated in PZ.

## Assets

`media/models_X/PVADSGael/GaelScopes.fbx` preserves the 55 original Gael
housing meshes and adds 55 child reference meshes, each with 24 vertices,
22 triangles, normals and a `GlassUV` UV map. Placements are preliminary.
51 model script overrides retain existing item IDs and texture paths.
Gael remains a required dependency; its Workshop files are not modified.

## Renderer

`PhysicalGlass` receives the alignment hook after the gun model is assigned.
It finds the actual optic instance (original Gael or substituted mesh), requests
the FBX's selected `*_Vidrio_Referencia` through PZ ModelManager, and captures
its actual loaded positions and UV before VBO upload. The CSV is now only a
registry of supported mesh names: its coordinates are not used for rendering.
The renderer uses the captured *drawn optic* matrix, or the drawn gun matrix
and its optic attachment transform. No camera-centred substitution is used
when a bound physical surface is missing from the draw records.

The vertex shader preserves homogeneous clip coordinates, so UV interpolation
and fragment depth are perspective-correct. The fragment shader samples PiP
colour and reticle textures and discards fragments behind captured scene depth.
The PiP image alone is horizontally flipped to correct the reported mirror;
mesh coordinates, attachment transforms and reticle orientation are unchanged.
Render state is restored after the custom pass. No animation or camera pose
is changed, and the prior shot-cursor guard is retained.

The physical surface becomes eligible for rendering 0.2 seconds after ADS
animation begins, without waiting for the original 97% animation threshold.
The existing completed-ADS visibility remains valid for faster transitions.
The timer resets on release, invalid state or a weapon change. This is a PiP
render-only gate; ADS alignment, firing state and precision are unchanged.

The FBX child is loaded as the physical PiP surface, not as an opaque Gael disk.
Editing the installed FBX reference geometry/UV updates the runtime surface on
the next asset load/restart. Preserve its name and 24 perimeter UV vertices.

## Calibration

Existing `pipAnchorX/Y/Z` translate in the loaded optic's local coordinates.
`pipAnchorScale * pipScale` sizes the surface around the reference centre.
`pipX/Y` are additional local offsets; `pipZ` offsets along the local tube axis.
`glassPitch/Yaw/Roll` rotate about the reference centre in degrees, and are
exposed in the Glass calibrator tab and resolved from optic/weapon/user profiles.
`physicalGlass=0` disables this renderer for a resolved profile.
Neutral offsets/angles are 0; neutral scales are 1. Zoom remains independent.
`pipSpeed`, `pipMove`, and screen-fixed controls do not animate the physical
surface: it follows the actual drawn model directly.

## Verification performed

- JVM `-Xverify:all` on the four patched classes and new helper.
- Native PZ Assimp import: 55 original meshes retained; all 55 new reference
  meshes have UVs, normals and 22 triangles.
- PZ ProcessedAiScene: all 55 references selected individually expose actual
  loaded position/UV data with 24 unique perimeter vertices.
- Hidden OpenGL context on AMD Radeon RX 6750 XT: shaders compile/link;
  pixel tests pass for UV surface, aperture, transformed plane and occlusion.
- Modified Lua files compiled using PZ's own Kahlua compiler.

Still required: in-game check of instance binding, loaded axes, placement,
reticle orientation, occlusion, recoil/movement and performance.
Diagnostics are prefixed `PhysicalGlass:` and throttled to one every 2 seconds.

## Source

`tools/physical-glass` contains the renderer, ASM patcher, asset packager,
reference resource and verification tools. Patch an **unmodified pre-PhysicalGlass
PVADSGAEL JAR**; the patcher deliberately rejects an already patched JAR.
