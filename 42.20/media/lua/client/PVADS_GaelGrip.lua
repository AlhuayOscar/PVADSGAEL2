-- Recoil benefit for attached grips; handguards are not treated as grips.
PVADS_GaelGrip = PVADS_GaelGrip or {}
local aiming = require "GGS_AimingSystem"

local function isGrip(part)
    local identifier = (tostring(part:getPartType() or "") .. " "
        .. tostring(part:getFullType() or "") .. " "
        .. tostring(part:getName() or "")):lower():gsub("[%s_%-]", "")
    if identifier:find("handguard", 1, true)
        or identifier:find("guardamanos", 1, true)
        or identifier:find("foreend", 1, true)
        or identifier:find("forend", 1, true) then return false end
    return identifier:find("grip", 1, true) ~= nil
        or identifier:find("empunadura", 1, true) ~= nil
        or identifier:find("empuñadura", 1, true) ~= nil
end

function PVADS_GaelGrip.effects(weapon)
    if not weapon or not weapon.getAllWeaponParts then return 1, 1 end
    local parts = weapon:getAllWeaponParts()
    if not parts then return 1, 1 end
    local grip, stock, muzzle, laser = 1, 1, 1, false
    for index = 0, parts:size() - 1 do
        local part = parts:get(index)
        if part then
            local slot = tostring(part:getPartType() or ""):lower()
            local id = tostring(part:getFullType() or ""):lower()
            local data = part:getModData()
            local registered = aiming.partModifiers and aiming.partModifiers[part:getFullType()]
            local value = data and data.RealRecoil
            if value == nil then value = registered and registered.RealRecoil end
            local recoil = math.max(-75, math.min(150, tonumber(value) or 0))
            local factor = 1 + recoil / 100
            if slot == "grip" or isGrip(part) then
                grip = math.min(grip, math.min(0.60, factor)) -- preserve the existing grip benefit
            elseif slot == "stock" then
                stock = math.min(stock, recoil ~= 0 and factor or 0.80)
            elseif slot == "canon" or slot == "barrel" or slot == "muzzle"
                or id:find("silencer", 1, true) or id:find("suppressor", 1, true)
                or id:find("compensator", 1, true) or id:find("muzzle", 1, true) then
                -- Use Gael's existing part value once, not a second flat bonus.
                if value == nil then
                    if id:find("brake", 1, true) then factor = 0.80
                    elseif id:find("compensator", 1, true) then factor = 0.85
                    elseif id:find("silencer", 1, true) or id:find("suppressor", 1, true) then factor = 0.95 end
                end
                muzzle = muzzle * factor
            end
            if slot == "laser" or id:find("laser", 1, true) then laser = true end
        end
    end
    return math.max(0.25, math.min(1.5, grip * stock * muzzle)), laser and 1.25 or 1
end

function PVADS_GaelGrip.recoilMultiplier(weapon)
    local recoil = PVADS_GaelGrip.effects(weapon)
    return recoil
end

function PVADS_GaelGrip.recoveryMultiplier(weapon)
    local _, recovery = PVADS_GaelGrip.effects(weapon)
    return recovery
end
