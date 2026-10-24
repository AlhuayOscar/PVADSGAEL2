require "PVG/Config"
require "Hotbar/ISHotbarAttachDefinition"
local I = PVGIntegrations
local function register()
    if not I.enabled("sling") or I.loaded.sling then return end
    -- Do not offer an invisible weapon slot if an old Java build is installed.
    if not PVADSJ or not PVADSJ.setupSlingAnchors or not PVADSJ.setupSlingAnchors() then
        if not I.slingWarned then
            I.slingWarned = true
            print("[PVADSGael] Sling disabled: matching Java anchor API is required.")
        end
        return
    end
    for _, slot in ipairs(ISHotbarAttachDefinition) do
        if slot.type == "PVGSling" then I.loaded.sling = true; return end
    end
    table.insert(ISHotbarAttachDefinition, {
        type = "PVGSling", name = "PVADSGael Sling", animset = "belt left",
        attachments = { Rifle = "PVG Sling Rifle" },
    })
    I.loaded.sling = true
    -- Single refresh after registration; normal clothing events handle later changes.
    if getNumActivePlayers and getPlayerHotbar then
        for i = 0, getNumActivePlayers() - 1 do
            local bar = getPlayerHotbar(i)
            if bar then bar:refresh() end
        end
    end
end
Events.OnGameStart.Add(register)
Events.OnCreatePlayer.Add(function() register() end)
