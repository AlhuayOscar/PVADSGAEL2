-- Physical surface: local position/size use the existing Anchor XYZ and size.
-- Numeric user fields persist through the existing generic profile serializer.
require "PVADS_GaelVisualChoices"
if not PVADS.physicalGlassResolverInstalled then
    PVADS.physicalGlassResolverInstalled = true
    for _, key in ipairs({ "glassPitch", "glassYaw", "glassRoll" }) do
        PVADS.GaelGlobalFields[key] = true
    end
    local resolve = PVADS.ResolveProfile
    PVADS.ResolveProfile = function(weapon)
        local profile = resolve(weapon)
        local id = profile.opticId or ""
        local optic = PVADS.Optics and PVADS.Optics[id]
        local specific = PVADS.WeaponOptic and PVADS.WeaponOptic[profile.weaponId]
        specific = specific and specific[id]
        local user = PVADS.User and PVADS.User[tostring(profile.weaponId) .. "|" .. id]
        for _, key in ipairs({ "glassPitch", "glassYaw", "glassRoll", "physicalGlass" }) do
            local value = optic and optic[key]
            if specific and specific[key] ~= nil then value = specific[key] end
            if user and user[key] ~= nil then value = user[key] end
            if key == "physicalGlass" then
                if value ~= nil then profile[key] = tonumber(value) end
            else
                profile[key] = (tonumber(value) or 0) + PVADS.GaelEffectiveGlobalValue(profile, key)
            end
        end
        return profile
    end
end
