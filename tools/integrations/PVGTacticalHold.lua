-- Independent, event-driven controller; animation assets adapted with permission.
require "PVG/Config"
require "TimedActions/ISTimedActionQueue"
local I = PVGIntegrations
local state = setmetatable({}, { __mode = "k" })
local rifle = { "PVG_TacGunPose", "PVG_TacGunPoseHighReady", "PVG_TacGunPoseLowready", "PVG_TacGunResting", "PVG_TacGunClassicStockGrip", "" }
local pistol = {
    { "PVG_TacPistolRight", "PVG_TacPistolLeft" },
    { "PVG_TacPistolRightHigh", "PVG_TacPistolLeftHigh" },
    { "PVG_TacPistolRightLow", "PVG_TacPistolLeftLow" }, { "", "" },
}
local function clear(player, s)
    for _, key in ipairs({ "RightHandMask", "LeftHandMask" }) do
        local current = player:getVariableString(key)
        if current and string.sub(current, 1, 7) == "PVG_Tac" then
            player:clearVariable(key)
        end
    end
    if s.right ~= "" or s.left ~= "" then
        s.right, s.left = "", ""
        player:getModData().PVGTacticalPose = nil
        if player:isLocal() and isClient() then player:transmitModData() end
    end
end
function I.updateTactical(player)
    if not player then return end
    local s = state[player]
    if not s then s = { index = 1, right = "", left = "" }; state[player] = s end
    if not player:isLocal() then
        local pose = player:getModData().PVGTacticalPose
        if I.enabled("tactical") and pose and not player:isAiming() then
            if type(pose) ~= "table" or type(pose[1]) ~= "string" or type(pose[2]) ~= "string" then return end
            local validRight, validLeft = pose[1] == "", pose[2] == ""
            for _, value in ipairs(rifle) do if value == pose[1] then validRight = true end end
            for _, value in ipairs(pistol) do
                if value[1] == pose[1] then validRight = true end
                if value[2] == pose[2] then validLeft = true end
            end
            if not validRight or not validLeft then clear(player, s); return end
            if player:getVariableString("RightHandMask") ~= pose[1] then player:setVariable("RightHandMask", pose[1]) end
            if player:getVariableString("LeftHandMask") ~= pose[2] then player:setVariable("LeftHandMask", pose[2]) end
        else clear(player, s) end
        return
    end
    local gun = player:getPrimaryHandItem()
    local queue = ISTimedActionQueue.queues[player]
    local action = queue and queue.queue and queue.queue[1]
    -- Relinquish masks immediately to ADS, firing, timed actions and stance animations.
    if not I.enabled("tactical") or player:isDead() or player:isAsleep()
        or player:isAiming() or player:isPerformingAttackAnimation() or player:isSneaking()
        or player:isSprinting() or action or player:getVariableBoolean("pvgstanceproneposition")
        or not gun or not instanceof(gun, "HandWeapon") or not gun:isRanged() then clear(player, s); return end
    local right, left
    if gun:isTwoHandWeapon() then
        if player:getSecondaryHandItem() ~= gun then clear(player, s); return end
        right, left = rifle[(s.index - 1) % #rifle + 1], ""
    else
        local pose = pistol[(s.index - 1) % #pistol + 1]
        right, left = pose[1], player:getSecondaryHandItem() and "" or pose[2]
    end
    if right == s.right and left == s.left then return end
    clear(player, s)
    if right ~= "" then player:setVariable("RightHandMask", right) end
    if left ~= "" then player:setVariable("LeftHandMask", left) end
    s.right, s.left = right, left
    player:getModData().PVGTacticalPose = { right, left }
    if isClient() then player:transmitModData() end
end
Events.OnPlayerUpdate.Add(I.updateTactical)
Events.OnKeyPressed.Add(function(key)
    if not I.enabled("tactical") or key ~= I.key("tacticalKey", Keyboard.KEY_H) then return end
    local player = getSpecificPlayer(0)
    if not player then return end
    I.updateTactical(player)
    local s = state[player]
    if s then s.index = s.index + 1; I.updateTactical(player) end
end)
