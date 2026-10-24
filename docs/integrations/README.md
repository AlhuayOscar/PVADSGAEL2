# Optional integrations — implementation status

## Tactical Hold Complete (3693258802)

Implemented in both 42 and 42.20, OFF by default. Enable **Tactical Hold
integrado** under **PVADSGAEL - Optional integrations** in Mod Options, apply,
and restart the game. H cycles the available rifle/pistol poses. This first
version changes idle holds, not the ADS aiming pose. Aiming, firing, sprinting,
sneaking and timed actions relinquish the masks immediately to existing systems.

The PVG controller is independent of the upstream Lua controllers. Animation
nodes and mask names are namespaced. No per-frame online-player sweep is used.
Original Tactical Hold Complete being active disables this integrated controller
to avoid duplicate ownership. Use only one version.

Verified with the game's Kahlua runtime using player mocks: idle rifle/pistol,
ADS release, shot release, timed-action release, disabled state and foreign-mask
preservation. XML nodes parse successfully. In-game animation blending and MP
replication still require testing. Not installed or published by the packaging
step.

## Original PVADSGael sling — experimental, not yet tested in game

Alice's Weapon Sling (3775549570) was a behavioral reference. No Alice geometry,
textures or Lua controllers are bundled. New assets are reproducible with
`tools/integrations/build_sling_assets.py`: a closed diagonal webbing loop,
separate flat world-item mesh, UVs, normals, olive woven texture and inventory icon.
Each mesh has 384 triangles. This is a rigid Spine-attached prototype, not a
skinned strap; visible alignment and clothing penetration require in-game tuning.

Enable **PVADSGael Sling propio** in Mod Options and restart in single-player.
In multiplayer the server's **IntegratedSling** sandbox option is authoritative
and defaults OFF; the local checkbox does not override the server. Craft
**PVADSGael Weapon Sling** (Tailoring 2, belt, four leather strips, thread,
needle and cutting tool), or spawn `Base.PVGaelSling` in debug for testing.
Equip it, then attach a rifle to the new PVADSGael Sling hotbar slot. This first
version supports Rifle attachment types, not shovels/melee weapons/extra styles.

The Java helper adds `pvg_sling_front` to MaleBody/FemaleBody at startup, without
replacing either script or modifying existing attachments. Requires the matching
JAR. If the Java API is missing, the extra hotbar slot is not registered and one
warning is logged. No continuous update/sweep or per-shot logging is introduced.
Item/registry definitions remain present when OFF for save compatibility. Detach
the rifle and remove the sling before disabling; nothing is automatically deleted.

Verified offline with native Assimp imports and Kahlua mocks: UVs/normals,
anchor idempotency, OFF behavior, old-Java guard, no duplicated slot, native back
slot preservation, recipe gate and server authority. Java bytecode verification
passes. Not installed or published yet. In-game wear/equip/crafting/replication
and visual fitting remain unverified.

## Pending module — not exposed as a working option

* Lethal Stealth (3531611692): includes crouching and prone. Integration needs
  isolated action/state handlers, namespaced variables, safe disabled behavior,
  and MP authority. Do not copy the aiming-perk mutation/buff subsystem into PVG.
  No stance module is loaded by this first stage.

## Attribution / permission record

Tactical Hold Complete animation resources are from Workshop item 3693258802;
credit remains with its original creators. Carlos explicitly stated in the
integration request that he has permission to incorporate these resources.
This records that statement; it does not assert an independently verified public
license or grant downstream redistribution rights. Asset hashes are recorded in
`tactical-assets.json`. Retain the original authors' credits when publishing.

Reference URLs:
https://steamcommunity.com/sharedfiles/filedetails/?id=3693258802
https://steamcommunity.com/sharedfiles/filedetails/?id=3775549570
https://steamcommunity.com/sharedfiles/filedetails/?id=3531611692
