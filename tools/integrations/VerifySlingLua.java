import java.nio.file.*;
import se.krka.kahlua.j2se.J2SEPlatform;
import se.krka.kahlua.vm.*;
import se.krka.kahlua.luaj.compiler.LuaCompiler;
public class VerifySlingLua {
 public static void main(String[] args)throws Exception {
  var platform=J2SEPlatform.getInstance();var env=platform.newEnvironment();var thread=new KahluaThread(platform,env);
  var owner=KahluaThread.class.getDeclaredField("debugOwnerThread");owner.setAccessible(true);owner.set(thread,Thread.currentThread());
  String setup="""
    local eventHandlers={}
    Events=setmetatable({}, {__index=function(t,k)local e={Add=function(f)
      eventHandlers[k]=eventHandlers[k] or {};table.insert(eventHandlers[k],f)
    end};rawset(t,k,e);return e end})
    require=function()end
    local bodyCount,attachCount=0,0
    ItemBodyLocation={get=function()return {} end}
    ResourceLocation={of=function(id)return id end}
    BodyLocations={getGroup=function()return {getOrCreateLocation=function()bodyCount=bodyCount+1 end} end}
    AttachedLocations={getGroup=function()return {getOrCreateLocation=function(_,id)
      assert(id=='PVG Sling Rifle');attachCount=attachCount+1
      return {setAttachmentName=function(_,name)assert(name=='pvg_sling_front') end}
    end} end}
    ISHotbarAttachDefinition={{type='Back',attachments={Rifle='Rifle On Back'}}}
    local anchorCalls=0
    """;
  String sources="";
  // Config returns I: isolate source files as modules, rather than concatenate after a return.
  sources="";
  for(String file:args)sources+="do local module=function()\n"+Files.readString(Path.of(file))+"\nend module() end\n";
  String tests="""
    assert(bodyCount==1 and attachCount==1,'save-compatible body/attachment registries')
    assert(not PVGIntegrations.canCraftSling(),'off by default')
    local startSling=eventHandlers.OnGameStart[2]
    startSling();assert(#ISHotbarAttachDefinition==1,'off must not add a slot')
    PVGIntegrations.requested.sling=true
    startSling();assert(#ISHotbarAttachDefinition==1,'missing Java must not add an invisible slot')
    PVADSJ={setupSlingAnchors=function()anchorCalls=anchorCalls+1;return true end}
    startSling();startSling()
    assert(#ISHotbarAttachDefinition==2 and anchorCalls==1,'exactly one native initialization and slot')
    assert(ISHotbarAttachDefinition[1].attachments.Rifle=='Rifle On Back','native back slot preserved')
    assert(ISHotbarAttachDefinition[2].attachments.Rifle=='PVG Sling Rifle','own anchor')
    assert(PVGIntegrations.canCraftSling(),'enabled recipe')
    isClient=function()return true end
    SandboxVars={PVADSGAEL2={IntegratedSling=false}}
    assert(not PVGIntegrations.canCraftSling(),'server disabled overrides local setting')
    SandboxVars.PVADSGAEL2.IntegratedSling=true
    assert(PVGIntegrations.canCraftSling(),'server enabled')
    """;
  Object[] result=thread.pcall(LuaCompiler.loadstring(setup+sources+tests,"sling-tests",env),new Object[0]);
  if(!Boolean.TRUE.equals(result[0]))throw new AssertionError(java.util.Arrays.toString(result));
  System.out.println("Sling Lua tests OK: OFF default, missing-Java guard, one-time slot, native-slot preservation, recipe and MP authority.");
 }
}
