-- Optional integrations. Off by default; changes take effect on next game load.
PVGIntegrations = PVGIntegrations or {}
local I = PVGIntegrations
I.requested = I.requested or { tactical = false, sling = false }
if I.requested.sling == nil then I.requested.sling = false end
I.active = I.active or {}
I.loaded = I.loaded or {}
I.optionId = "PVADSGael_Integrations"
local file = "PVADSGael_Integrations.txt"
if not I.readConfig then
    I.readConfig = true
    if getFileReader then
        local reader = getFileReader(file, false)
        if reader then
            while true do
                local line = reader:readLine()
                if not line then break end
                local key, value = string.match(line, "^(%w+)=(%d+)$")
                if key and I.requested[key] ~= nil then I.requested[key] = value == "1" end
            end
            reader:close()
        end
    end
end
function I.save()
    local writer = getFileWriter(file, true, false)
    if not writer then return false end
    for _, key in ipairs({ "tactical", "sling" }) do
        writer:write(key .. "=" .. (I.requested[key] and "1" or "0") .. "\n")
    end
    writer:close()
    return true
end
function I.external(id)
    local mods = getActivatedMods and getActivatedMods()
    return mods and (mods:contains(id) or mods:contains("\\" .. id)) or false
end
function I.enabled(key)
    if I.active[key] ~= nil then return I.active[key] end
    local external = { tactical = "TacHold Complete", stances = "RET_LethalStealth" }
    if external[key] and I.external(external[key]) then return false end
    -- Gameplay-changing stance systems must be server-authoritative in MP.
    if (key == "stances" or key == "sling") and ((isClient and isClient()) or (isServer and isServer())) then
        local option = key == "sling" and "IntegratedSling" or "IntegratedStances"
        return SandboxVars and SandboxVars.PVADSGAEL2 and SandboxVars.PVADSGAEL2[option] == true or false
    end
    return I.requested[key] == true
end
function I.key(name, fallback)
    local opts = PZAPI and PZAPI.ModOptions and PZAPI.ModOptions:getOptions(I.optionId)
    local option = opts and opts:getOption(name)
    return option and option:getValue() or fallback
end
-- Snapshot at startup: Apply only affects the next session.
Events.OnGameStart.Add(function()
    I.active.tactical = I.enabled("tactical")
    I.active.sling = I.enabled("sling")
end)
return I
