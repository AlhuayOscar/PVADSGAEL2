# Next integrations requested by Carlos — 2026-10-24

Optional hold, sling and stance integrations cancelled and removed at Carlos's
request. Keep the ADS foundation and its original attribution.

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
