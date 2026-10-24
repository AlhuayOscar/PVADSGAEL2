import java.nio.file.*;
import se.krka.kahlua.j2se.J2SEPlatform;
import se.krka.kahlua.vm.*;
import se.krka.kahlua.luaj.compiler.LuaCompiler;
public class VerifyTacticalLua {
 public static void main(String[] args)throws Exception {
  var platform=J2SEPlatform.getInstance(); var env=platform.newEnvironment(); var thread=new KahluaThread(platform,env);
  var owner=KahluaThread.class.getDeclaredField("debugOwnerThread");owner.setAccessible(true);owner.set(thread,Thread.currentThread());
  String setup="""
    local handlers={}
    Events=setmetatable({}, {__index=function(t,k)local e={Add=function(f)handlers[k]=f end};rawset(t,k,e);return e end})
    require=function()end
    Keyboard={KEY_H=35}
    ISTimedActionQueue={queues={}}
    isClient=function()return false end
    instanceof=function()return true end
    PVGIntegrations={enabled=function()return true end,key=function(_,fallback)return fallback end}
    local vars,data={},{}
    local aiming,attacking,rifleGun=false,false,true
    local gun={isRanged=function()return true end,isTwoHandWeapon=function()return rifleGun end}
    player={
     isLocal=function()return true end,isDead=function()return false end,isAsleep=function()return false end,
     isAiming=function()return aiming end,isPerformingAttackAnimation=function()return attacking end,
     isSneaking=function()return false end,isSprinting=function()return false end,
     getVariableString=function(_,k)return vars[k] end,getVariableBoolean=function()return false end,
     setVariable=function(_,k,v)vars[k]=v end,clearVariable=function(_,k)vars[k]=nil end,
     getModData=function()return data end,getPrimaryHandItem=function()return gun end,
     getSecondaryHandItem=function()return rifleGun and gun or nil end,
    }
    getSpecificPlayer=function()return player end
    """;
  String tests="""
    PVGIntegrations.updateTactical(player)
    assert(vars.RightHandMask=='PVG_TacGunPose','rifle idle')
    aiming=true;PVGIntegrations.updateTactical(player)
    assert(vars.RightHandMask==nil,'ADS must release rifle mask')
    aiming=false;PVGIntegrations.updateTactical(player)
    attacking=true;PVGIntegrations.updateTactical(player)
    assert(vars.RightHandMask==nil,'shot must release rifle mask')
    attacking=false;rifleGun=false;PVGIntegrations.updateTactical(player)
    assert(vars.RightHandMask=='PVG_TacPistolRight' and vars.LeftHandMask=='PVG_TacPistolLeft','pistol pair')
    aiming=true;PVGIntegrations.updateTactical(player)
    assert(vars.RightHandMask==nil and vars.LeftHandMask==nil,'ADS must release both pistol masks')
    vars.RightHandMask='OtherModMask';PVGIntegrations.updateTactical(player)
    assert(vars.RightHandMask=='OtherModMask','must not clear foreign masks')
    aiming=false;vars.RightHandMask=nil
    PVGIntegrations.updateTactical(player)
    ISTimedActionQueue.queues[player]={queue={{}}};PVGIntegrations.updateTactical(player)
    assert(vars.RightHandMask==nil and vars.LeftHandMask==nil,'timed action must release both masks')
    ISTimedActionQueue.queues[player]=nil
    PVGIntegrations.enabled=function()return false end
    PVGIntegrations.updateTactical(player)
    assert(vars.RightHandMask==nil,'disabled')
    """;
  Object[] result=thread.pcall(LuaCompiler.loadstring(setup+Files.readString(Path.of(args[0]))+"\n"+tests,"tactical-tests",env),new Object[0]);
  if(!Boolean.TRUE.equals(result[0]))throw new AssertionError(java.util.Arrays.toString(result));
  System.out.println("Tactical tests OK: rifle/pistol poses, ADS/firing/action release, foreign mask preservation, disabled module.");
 }
}
