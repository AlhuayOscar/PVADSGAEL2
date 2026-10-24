# Recoil correction candidate — 2026-10-24

MP5 logs show `reversePitchYaw=true` without a saved inversion override,
despite a tiny custom angular impulse (~0.01 degrees). Native ADS shot impulses
did not use the same strength/aiming attenuation as the Lua angular layer.
The native spring return used a single explicit update with dt up to 0.1 s,
which can overshoot severely at low FPS. These are code/log findings; the
in-game cause of every visual jolt has not been confirmed.

Changes in both variants:

- Automatic pitch/yaw inversion is no longer implicit. Explicit profile 1
  remains honoured; calibrator default now reports false consistently.
- Strength control multiplied by 1.5 at every level: previous recoil multiplier
  divided by 1.5 (33.3% lower amplitude, not a flat 50% amplitude cut).
- Aiming attenuates angular and native shot impulse by `1/(1+0.08*level)`;
  existing aiming recovery and weapon/attachment coefficients are preserved.
- Native gun/camera impulse now receives the skill attenuation too.
- Native recoil spring return substeps at <=1/240 s, retaining original
  spring stiffness/damping. No camera pose, alignment or glass geometry changes.

Verification: bytecode and Lua compilation; all strength levels 0–10;
bounded native spring recovery at 10, 15, 30, 60 and 144 FPS.
Still required: MP5 singles and bursts in-game, low/high skills, low FPS,
and explicit inversion opt-in. Installed in both variants with backup;
Carlos reported the model remains stable in gameplay after this correction.
