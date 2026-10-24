# Next integrations requested by Carlos — 2026-10-24

Checkpoint first; integrations below are not yet implemented.

1. TacHold integration for firearm handling/aiming poses.
2. Real Crouching Stance integration for firing while crouched.
3. An integrated sling system inspired by the Workshop sling mod, using
   our own models and textures. Identify the exact reference mod before
   implementation; do not assume permission to copy third-party code/assets.

Preserve the stable recoil, physical optic glass, calibrated profiles and
shot-cursor guard. Verify transition poses, firing, first/third-person views
and compatibility before publishing these integrations.

Performance follow-up (reviewed, not yet implemented): cache reference geometry
and shader uniform locations; make remaining diagnostics opt-in; evaluate the
dynamic/full-profile double update before changing its behaviour. Do not remove
actual error reporting or safety guards as part of diagnostic cleanup.

Completed in this checkpoint: focus no longer scales PiP glass or adds optic
focus zoom; steadying remains. GaelPoseDiagnostics removed from the active
local installation and retained in a recoverable backup; frequent aim/recoil
shot diagnostics are opt-in. Native spring return uses stable substeps and
both recoil layers receive strength/aiming attenuation.
