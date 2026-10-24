-- One-time migration from the old screen-space calibrations.
require "PVADS_GaelVisualChoices"
require "ZZ_PVADS_GaelGlassReferences"

local zero = {
    "pipX", "pipY", "pipZ", "pipRetX", "pipRetY", "pipDepth",
    "pipAnchorX", "pipAnchorY", "pipAnchorZ", "pipCropX", "pipCropY",
    "reticleRoll",
}
local unit = { "pipScale", "pipZoom", "pipAnchorScale", "lensScale", "windowScale" }

function PVADS.GaelResetLegacyGlassProfiles()
    if PVADS.GaelUseGlassReferences == false then return 0 end
    local count = 0
    for key, user in pairs(PVADS.User or {}) do
        local optic = string.match(key, "^[^|]+|(.+)$")
        local global = key == "__GaelGlobalOffsets__|"
        if type(user) == "table" and (global or (optic and PVADS.GaelGlassReferences[optic]))
            and user.gaelGlassBaseVersion ~= 1 then
            for _, field in ipairs(zero) do user[field] = global and nil or 0 end
            for _, field in ipairs(unit) do
                -- Global values are additive deltas, unlike per-optic scales.
                if global then user[field] = nil else user[field] = 1 end
            end
            if not global then
                user.pip2DBackup = 0
                user.pipAnchorClip = 0
                user.pipFixed = 1
                user.pipMove = 0
            end
            user.gaelGlassBaseVersion = 1
            count = count + 1
        end
    end
    if count > 0 then print("[PVADSGAEL2] Reset legacy glass calibration: " .. count .. " profiles") end
    return count
end

local load = PVADS.loadUserProfiles
PVADS.loadUserProfiles = function(...)
    load(...)
    PVADS.GaelResetLegacyGlassProfiles()
end

local function resetAtStart()
    if PVADS.GaelResetLegacyGlassProfiles() > 0 and PVADS.saveUserProfiles then
        PVADS.saveUserProfiles()
    end
end
if Events and Events.OnGameStart then Events.OnGameStart.Add(resetAtStart) end
