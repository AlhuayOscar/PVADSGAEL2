require "PVG/Config"
require "PZAPI/ModOptions"
local I = PVGIntegrations
if not I.options and PZAPI and PZAPI.ModOptions then
    local opts = PZAPI.ModOptions:create(I.optionId, "PVADSGAEL - Optional integrations")
    I.options = opts
    opts:addDescription("Modules are OFF by default. Enable, apply and restart PZ. Do not combine Tactical Hold versions. In multiplayer the sling uses the server's IntegratedSling sandbox option. Stance integration is not included yet.")
    opts:addTickBox("tactical", "Tactical Hold integrado", I.requested.tactical)
    opts:addTickBox("sling", "PVADSGael Sling propio (experimental)", I.requested.sling)
    opts:addKeyBind("tacticalKey", "Cambiar postura Tactical Hold", Keyboard.KEY_H)
    opts.apply = function()
        for _, key in ipairs({ "tactical", "sling" }) do
            I.requested[key] = opts:getOption(key):getValue() == true
        end
        I.save()
    end
end
