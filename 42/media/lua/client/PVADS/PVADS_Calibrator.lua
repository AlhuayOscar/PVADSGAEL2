-- Project Viewpoint: Recoil & ADS - (c) 2026 the author of Project Viewpoint (Workshop 3812612054). All rights reserved.
-- Not open source: do not copy, modify, reuse or redistribute without permission (see LICENSE.txt).
require "ISUI/ISCollapsableWindow"
require "ISUI/ISButton"
require "ISUI/ISTextEntryBox"
require "PVADS/PVADS_Overlay"

--[[
Developer calibration window (Viewpoint settings window > Recoil & ADS > Developer / calibration mode, then the calibrator key).
Aim the weapon, press "Hold ADS", and nudge the values until the cyan cross (Viewpoint's true aim ray)
sits on the front post / dot / reticle. Changes apply live. "Save" writes them to
Zomboid/Lua/PVADS_UserProfiles.txt (loaded automatically next time); "Copy" puts a ready-to-paste
PVADS.RegisterWeaponOpticOverride / RegisterWeaponProfile line on the clipboard for shipping in a mod.

Values (weapon units ~ 0.55 m; angles in degrees):
  X/Y/Z      shift of the sight point (Z up, Y forward)     Pitch/Yaw/Roll  sight-line correction
  Relief     eye distance behind the sight                  FOV            ADS zoom for irons/dots
  Height     optic sight-axis height above its mount        Lens           scope lens size
Picture-in-picture (1.5.6, per weapon + optic, saved the same way):
  PiP lens X/Y   move the lens picture (share of the lens radius, + = right / up)
  PiP lens size  lens radius multiplier       PiP zoom   zoom multiplier for this sight
  PiP reticle X/Y  move the reticle inside the lens (share of the lens radius)
  PiP depth      red dots / holos: extra clear distance past the glass (metres)
Every value can be typed into its box (Enter or click away applies). The step button cycles the +/- step size.
]]

local Cal = { stepLevel = 2 }
-- +/- step = field's base step x this; level 2 (x1) is the old "Fine" step, the old default "Coarse" was x10
Cal.STEPS = { { mul = 5, name = "Step x5" }, { mul = 1, name = "Step x1" }, { mul = 0.2, name = "Step x0.2" }, { mul = 0.05, name = "Step x0.05" } }
PVADS.Calibrator = Cal

-- base = step at "x1"; def = value when the profile has none
local FIELDS = {
    { key = "adjX", label = "Sight X", base = 0.0002 },
    { key = "adjY", label = "Sight Y", base = 0.0005 },
    { key = "adjZ", label = "Sight Z", base = 0.0002 },
    { key = "adjPitch", label = "Pitch", base = 0.025 },
    { key = "adjYaw", label = "Yaw", base = 0.025 },
    { key = "adjRoll", label = "Roll", base = 0.1 },
    { key = "relief", label = "Relief", base = 0.001 },
    { key = "fovMul", label = "ADS FOV", base = 0.005 },
    { key = "opticHeight", label = "Optic height", base = 0.0002 },
    { key = "lensScale", label = "Lens size", base = 0.01, def = 1 },
    { key = "adsTime", label = "ADS time", base = 0.005 },
    { section = "Picture-in-picture (scopes / red dots)" },
    { key = "pipX", label = "PiP lens X", base = 0.005 },
    { key = "pipY", label = "PiP lens Y", base = 0.005 },
    { key = "pipZ", label = "PiP lens Z", base = 0.002 },
    { key = "pipScale", label = "PiP lens size", base = 0.005, def = 1 },
    { key = "pipZoom", label = "PiP zoom", base = 0.01, def = 1 },
    { key = "pipRetX", label = "PiP reticle X", base = 0.005 },
    { key = "pipRetY", label = "PiP reticle Y", base = 0.005 },
    { key = "pipDepth", label = "PiP depth (dots)", base = 0.002 },
    { key = "pipCorner", label = "PiP corner (dots)", base = 0.01, def = 0.45 },
    { key = "pipAspect", label = "PiP height (dots)", base = 0.01, def = 1.0 },
    { key = "reticleRoll", label = "Reticle roll", base = 0.25 },
}

-- ------------------------------------------------------------------ persistence
local FILE = "PVADS_UserProfiles.txt"

function PVADS.loadUserProfiles()
    PVADS.User = {}
    local r = getFileReader(FILE, false)
    if not r then return end
    while true do
        local line = r:readLine()
        if not line then break end
        local key, rest = string.match(line, "^([^=]+)=>(.*)$")
        if key then
            local t = {}
            for k, v in string.gmatch(rest, "([%w_]+)=([%-%d%.eE]+)") do t[k] = tonumber(v) end
            PVADS.User[key] = t
        end
    end
    r:close()
    -- 1.8.0: per-weapon calibrations of an optic move to the optic (model + mount) the first time that gun is aimed
    -- with it; the optic's model and mount are only known then (the console names each one moved)
    local pending = {}
    for key, t in pairs(PVADS.User) do
        local optic = string.match(key, "^[^|]*|(.+)$")
        if optic and PVADS.OPTIC_FIELDS then
            for k in pairs(t) do if PVADS.OPTIC_FIELDS[k] then table.insert(pending, key); break end end
        end
    end
    if #pending > 0 then
        table.sort(pending)
        PVADS.log(#pending .. " per-weapon optic calibration(s) found; each is copied to its optic (for every other gun that mounts it) the first time that gun is aimed with it: " .. table.concat(pending, ", "))
    end
end

-- 1.8.10: PZ's Kahlua has no global next(); "is this table empty" is answered with pairs
local function hasAny(t)
    if type(t) ~= "table" then return false end
    for _ in pairs(t) do return true end
    return false
end

function PVADS.saveUserProfiles()
    local w = getFileWriter(FILE, true, false)
    if not w then return false end
    w:write("# Project Viewpoint ADS - personal calibration. Format: weapon|optic=>key=value;...\r\n")
    -- sorted, so the file reads the same after every save; entries left empty are dropped
    local keys, empty = {}, {}
    for key, t in pairs(PVADS.User) do
        if hasAny(t) then table.insert(keys, key) else table.insert(empty, key) end
    end
    for _, key in ipairs(empty) do PVADS.User[key] = nil end
    table.sort(keys)
    for _, key in ipairs(keys) do
        local fields = {}
        for k in pairs(PVADS.User[key]) do table.insert(fields, k) end
        table.sort(fields)
        local s = key .. "=>"
        for _, k in ipairs(fields) do s = s .. k .. "=" .. string.format("%.5f", PVADS.User[key][k]) .. ";" end
        w:write(s .. "\r\n")
    end
    w:close()
    return true
end

-- ------------------------------------------------------------------ helpers
local function current()
    local pl = getSpecificPlayer(0)
    local w = PVADS.Controller.weapon(pl)
    if not w then return nil end
    return w, PVADS.userKey(w)
end

-- 1.8.0: with an optic on the gun, the optic's values are calibrated for the optic model + mount (every gun that
-- mounts it); they live on the Java side (Zomboid cache dir/Lua/PVADS_OpticProfiles.txt). Iron sights and the
-- weapon's own values (ADS time ...) stay per weapon in PVADS_UserProfiles.txt.
local function opticMode()
    local p = PVADS.Controller.profile
    return p ~= nil and p.opticId ~= nil and PVADS_opticKey ~= nil and PVADS_userOpticSet ~= nil and PVADS_opticKey() ~= ""
end
local function opticField(key) return PVADS.OPTIC_FIELDS and PVADS.OPTIC_FIELDS[key] and opticMode() end

local function value(key, def)
    if opticField(key) and PVADS_effective then
        local e = PVADS_effective(key)
        if e ~= nil then return e end
    end
    local p = PVADS.Controller.profile
    local v = p and p[key]
    if v == nil then return def or 0 end
    return v
end

local function setValue(key, v)
    local w, uk = current()
    if not w then return end
    if opticField(key) then
        if PVADS_userOpticSet(key, v) then
            Cal.opticDirty = true
            -- this gun now follows the optic's value: its old per-weapon value of it goes
            local u = PVADS.User[uk]
            if u and u[key] ~= nil then u[key] = nil; Cal.luaDirty = true end
            PVADS.Controller.refresh(true)
            return
        end
    end
    PVADS.User[uk] = PVADS.User[uk] or {}
    PVADS.User[uk][key] = v
    Cal.luaDirty = true
    PVADS.Controller.refresh(true)
end

function Cal.snippet()
    local w, uk = current()
    if not w then return "-- no firearm equipped" end
    if opticMode() and PVADS_userOpticFields then
        local t = PVADS_userOpticFields({}) or {}
        local fields = {}
        for k, v in pairs(t) do table.insert(fields, k .. " = " .. string.format("%.5f", v)) end
        table.sort(fields)
        local out = string.format('PVADS.RegisterOpticModelProfile("%s", { %s })', PVADS_opticKey(), table.concat(fields, ", "))
        -- this gun's own values with the optic (ADS time ...), if any, on a second line
        local u = PVADS.User[uk]
        if hasAny(u) then
            local wf = {}
            for k, v in pairs(u) do table.insert(wf, k .. " = " .. string.format("%.4f", v)) end
            table.sort(wf)
            local weaponId, opticId = string.match(uk, "^([^|]*)|(.*)$")
            out = out .. string.format('\nPVADS.RegisterWeaponOpticOverride("%s", "%s", { %s })', weaponId, opticId or "", table.concat(wf, ", "))
        end
        return out
    end
    local u = PVADS.User[uk] or {}
    local fields = {}
    for k, v in pairs(u) do table.insert(fields, k .. " = " .. string.format("%.4f", v)) end
    table.sort(fields)
    local weaponId, opticId = string.match(uk, "^([^|]*)|(.*)$")
    -- irons use optic "" so the values stay with the iron sights (a weapon profile would carry them onto its optics)
    return string.format('PVADS.RegisterWeaponOpticOverride("%s", "%s", { %s })', weaponId, opticId or "", table.concat(fields, ", "))
end

-- ------------------------------------------------------------------ window
PVADSCalibWindow = ISCollapsableWindow:derive("PVADSCalibWindow")

function PVADSCalibWindow:createChildren()
    ISCollapsableWindow.createChildren(self)
    local y = self:titleBarHeight() + 6
    local fh = getTextManager():getFontHeight(UIFont.Small)
    local bh = fh + 6
    self.rows = {}
    self.sections = {}
    for _, f in ipairs(FIELDS) do
        if f.section then
            table.insert(self.sections, { text = f.section, y = y + 4 })
            y = y + fh + 8
        else
            -- typed value: Enter (or clicking elsewhere) applies it
            local e = ISTextEntryBox:new(string.format("%.5f", value(f.key, f.def)), 112, y, 92, bh)
            e.font = UIFont.Small
            e:initialise(); e:instantiate()
            e.pvField = f; e.pvWin = self
            e.onCommandEntered = function(box) box.pvWin:applyTyped(box) end
            e.onLostFocus = function(box) box.pvWin:applyTyped(box) end
            self:addChild(e)
            local minus = ISButton:new(210, y, 28, bh, "-", self, PVADSCalibWindow.onStep)
            minus.internal = { f = f, sign = -1 }
            minus:initialise(); self:addChild(minus)
            local plus = ISButton:new(242, y, 28, bh, "+", self, PVADSCalibWindow.onStep)
            plus.internal = { f = f, sign = 1 }
            plus:initialise(); self:addChild(plus)
            table.insert(self.rows, { f = f, y = y, entry = e })
            y = y + bh + 3
        end
    end
    local function btn(x, label, fn)
        local b = ISButton:new(x, y, 82, bh, label, self, fn)
        b:initialise(); self:addChild(b); return b
    end
    y = y + 4
    self.btnHold = btn(8, "Hold ADS", PVADSCalibWindow.onHold)
    self.btnFine = btn(94, Cal.STEPS[Cal.stepLevel].name, PVADSCalibWindow.onFine)
    btn(180, "Reset", PVADSCalibWindow.onReset)
    btn(266, "Copy", PVADSCalibWindow.onCopy)
    y = y + bh + 4
    btn(8, "Save", PVADSCalibWindow.onSave)
    btn(94, "Probe", PVADSCalibWindow.onProbe)
    btn(180, "Validate", PVADSCalibWindow.onValidate)
    btn(266, "Close", PVADSCalibWindow.close)
    y = y + bh + 4
    btn(8, "Undo", PVADSCalibWindow.onUndo)
    self.infoY = y + bh + 6
    self:setHeight(self.infoY + 180)
end

--- A value typed into a row's box. Anything that is not a number is put back to the current value.
function PVADSCalibWindow:applyTyped(box)
    local f = box.pvField
    if not f then return end
    local v = tonumber(box:getText())
    if v == nil then
        self.message = "Not a number for " .. f.label .. ": '" .. tostring(box:getText()) .. "'"
        box:setText(string.format("%.5f", value(f.key, f.def)))
        return
    end
    if math.abs(v - value(f.key, f.def)) > 1e-9 then
        setValue(f.key, v)
        self.message = f.label .. " = " .. string.format("%.5f", v)
    end
end

function PVADSCalibWindow:onStep(button)
    local f, sign = button.internal.f, button.internal.sign
    local step = f.base * Cal.STEPS[Cal.stepLevel].mul
    setValue(f.key, value(f.key, f.def) + sign * step)
end

function PVADSCalibWindow:onHold()
    PVADS.Controller.forceADS = not PVADS.Controller.forceADS
    self.btnHold:setTitle(PVADS.Controller.forceADS and "Release" or "Hold ADS")
end

function PVADSCalibWindow:onFine()
    Cal.stepLevel = Cal.stepLevel % #Cal.STEPS + 1
    self.btnFine:setTitle(Cal.STEPS[Cal.stepLevel].name)
end

function PVADSCalibWindow:onReset()
    local w, uk = current()
    if not uk then return end
    if opticMode() then
        -- the optic's calibration (undo brings it back) and this gun's old per-weapon values of the optic's fields
        local had = PVADS_userOpticClear()
        local u = PVADS.User[uk]
        if u then for k in pairs(PVADS.OPTIC_FIELDS) do if u[k] ~= nil then u[k] = nil; Cal.luaDirty = true end end end
        Cal.opticDirty = Cal.opticDirty or had
        self.message = "Reset " .. PVADS_opticLabel() .. " (Undo brings it back; Save to keep)"
    else
        PVADS.User[uk] = nil
        Cal.luaDirty = true
    end
    PVADS.Controller.refresh(true)
end

function PVADSCalibWindow:onUndo()
    if not PVADS_userOpticUndo then return end
    local k = PVADS_userOpticUndo()
    self.message = (k ~= "") and ("Undone: last change to " .. k .. " (Save to keep)") or "Nothing to undo"
    if k ~= "" then Cal.opticDirty = true end
    PVADS.Controller.refresh(true)
end

function PVADSCalibWindow:onCopy()
    local s = Cal.snippet()
    Clipboard.setClipboard(s)
    self.message = "Copied: " .. s
end

function PVADSCalibWindow:onSave()
    local ok = PVADS.saveUserProfiles()
    local okO = true
    if PVADS_userOpticSave then okO = PVADS_userOpticSave() end
    Cal.luaDirty = false; if okO then Cal.opticDirty = false end
    self.message = (ok and okO) and ("Saved: weapons to Zomboid/Lua/" .. FILE .. ", optics to " .. (PVADS_opticProfileFile and PVADS_opticProfileFile() or "?"))
        or ("Save failed" .. (ok and " (optics)" or " (weapons)"))
end

function PVADSCalibWindow:onProbe()
    self.message = PVADS_probe()
    print("[ProjectViewpoint] probe: " .. self.message)
end

function PVADSCalibWindow:onValidate()
    if PVADS.Validate then
        local n = PVADS.Validate()
        self.message = "Validation: " .. n .. " finding(s), see console / Zomboid/Lua/PVADS_validation.txt"
    end
end

function PVADSCalibWindow:close()
    PVADS.Controller.forceADS = false
    self:setVisible(false)
end

function PVADSCalibWindow:render()
    ISCollapsableWindow.render(self)
    if self.isCollapsed then return end
    local fh = getTextManager():getFontHeight(UIFont.Small)
    for _, sct in ipairs(self.sections) do
        self:drawText(sct.text, 8, sct.y, 0.55, 0.85, 1, 1, UIFont.Small)
    end
    for _, r in ipairs(self.rows) do
        self:drawText(r.f.label, 8, r.y + 3, 1, 1, 1, 1, UIFont.Small)
        -- keep the box showing the live value unless it is being typed in
        if r.entry and not r.entry:isFocused() then
            local txt = string.format("%.5f", value(r.f.key, r.f.def))
            if r.entry:getText() ~= txt then r.entry:setText(txt) end
        end
        self:drawText(string.format("%g", r.f.base * Cal.STEPS[Cal.stepLevel].mul), 276, r.y + 3, 0.5, 0.5, 0.5, 1, UIFont.Small)
    end
    local p = PVADS.Controller.profile
    local y = self.infoY
    local st = {}
    if PVADS_getState then PVADS_getState(st) end
    local target
    if not p then target = ""
    elseif opticMode() then
        target = "Calibrating optic: " .. PVADS_opticLabel() .. " (every gun that mounts it)"
    else
        target = "Calibrating iron sights / weapon: " .. p.weaponId
    end
    local src = (opticMode() and PVADS_opticSource) and ("optic values from: " .. PVADS_opticSource()) or ""
    local unsaved = ((Cal.luaDirty or Cal.opticDirty) and "unsaved changes - press Save" or "")
    local lines = {
        target, src, unsaved,
        p and (p.weaponId .. "  [" .. p.family .. "]") or "no firearm",
        p and ("mode " .. p.mode .. "  optic " .. tostring(p.opticId) .. "  model " .. tostring(p.opticModel)) or "",
        p and ("irons: " .. tostring(p.ironSource) .. "   reticle " .. tostring(p.reticle) .. "   mag " .. tostring(p.mag)) or "",
        string.format("ADS %.2f   misalign %.2f deg   fov %.1f -> %.1f", st.t or 0, st.misalign or 0, st.fovIn or 0, st.fovOut or 0),
        (st.issue and st.issue ~= "") and ("issue: " .. st.issue) or "",
        st.pipMode and string.format("PiP %s   lens (%.0f, %.0f) r %.0f px", st.pip and "drawing" or "not drawn (raise the sight)", st.pipX or 0, st.pipY or 0, st.pipR or 0) or "PiP: not used for this sight",
    }
    for _, l in ipairs(lines) do self:drawText(l, 8, y, 0.85, 0.85, 0.85, 1, UIFont.Small); y = y + fh + 1 end
    if self.message then
        self:drawText(string.sub(self.message, 1, 90), 8, y + 2, 1, 0.9, 0.4, 1, UIFont.Small)
    end
end

function PVADSCalibWindow:new(x, y)
    local o = ISCollapsableWindow.new(self, x, y, 360, 640)
    o.title = "Project Viewpoint - ADS calibrator"
    o.resizable = false
    return o
end

function Cal.toggle()
    if not Cal.window then
        Cal.window = PVADSCalibWindow:new(60, 120)
        Cal.window:initialise()
        Cal.window:addToUIManager()
        return
    end
    Cal.window:setVisible(not Cal.window:isVisible())
    if not Cal.window:isVisible() then PVADS.Controller.forceADS = false end
end
