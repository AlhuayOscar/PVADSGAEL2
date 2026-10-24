require "PVG/Config"
local I = PVGIntegrations
-- Keep registries available even when disabled so existing saves can load items.
local location = ItemBodyLocation.get(ResourceLocation.of("pvg:slingfront"))
if location then BodyLocations.getGroup("Human"):getOrCreateLocation(location) end
AttachedLocations.getGroup("Human"):getOrCreateLocation("PVG Sling Rifle"):setAttachmentName("pvg_sling_front")
function I.canCraftSling()
    return I.enabled("sling")
end
