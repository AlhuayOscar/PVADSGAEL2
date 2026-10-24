# Local release snapshot — 2026-10-24

Included in both 42 and 42.20:

- Physical reference mesh rendering with UV, perspective and depth occlusion.
- Horizontal PiP-image mirror correction; dot at 10% of prior diameter.
- Physical glass visible from 0.2 seconds into ADS (or earlier completed ADS).
- Mouse wheel controls session-only Mira Y, not scope magnification.
- Mira Y wheel range halved to +/-0.075; step remains 0.005.
- Focus retains stabilization but suppresses its extra visual zoom for physical optics.
- Circular reference apertures for 38 closed/tubular optics; 17 others unchanged.
- 45 AK74 calibration entries shipped as Lua defaults, combining saved weapon
  values with the corresponding available optic calibration. User values win.

Checks: bytecode verification, PZ native FBX import/selection, shader pixel tests,
timing/range checks and Kahlua compilation. In-game confirmation remains required
for the latest focus/half-range changes; the previous installed JAR may differ.

Workshop release is not certified ready:

- Confirm redistribution permissions for third-party code and Gael meshes.
  Steam prohibits uploading content without the necessary rights:
  https://help.steampowered.com/en/faqs/view/6862-8119-C23E-EA7B
- The source includes third-party restrictive copyright notices. Credits alone
  are not evidence of a redistribution license. GitHub does not replace permission.
- Small-sight excess glass clipping remains open (see optics-pending.md).
- Exclude scratch JARs, build output, local backups and user configuration files
  from an actual Workshop package. No Steam upload was performed here.
