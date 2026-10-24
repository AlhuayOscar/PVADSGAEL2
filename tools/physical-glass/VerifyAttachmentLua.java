import java.nio.file.*;
import se.krka.kahlua.j2se.J2SEPlatform;
import se.krka.kahlua.vm.*;
import se.krka.kahlua.luaj.compiler.LuaCompiler;
public class VerifyAttachmentLua {
 public static void main(String[] args)throws Exception {
  var platform=J2SEPlatform.getInstance();var env=platform.newEnvironment();var thread=new KahluaThread(platform,env);
  var owner=KahluaThread.class.getDeclaredField("debugOwnerThread");owner.setAccessible(true);owner.set(thread,Thread.currentThread());
  String tests="""
    local function part(slot,id)
      return {getPartType=function()return slot end,getFullType=function()return id end,
        getName=function()return id end,getModData=function()return {} end}
    end
    local function weapon(parts)
      return {getAllWeaponParts=function()return {size=function()return #parts end,get=function(_,i)return parts[i+1] end} end}
    end
    local function expect(parts,recoil,recovery)
      local r,c=PVADS_GaelGrip.effects(weapon(parts))
      assert(math.abs(r-recoil)<0.00001,'recoil '..r..' expected '..recoil)
      assert(c==recovery,'recovery')
    end
    expect({},1,1)
    expect({part('Grip','Base.TacticalGrip')},0.6,1)
    expect({part('Handguard','Base.Handguard')},1,1)
    expect({part('Stock','Base.Stock')},0.8,1)
    expect({part('Canon','Base.TestBrake')},0.8,1)
    expect({part('Canon','Base.TestSilencer')},0.95,1)
    expect({part('Canon','Base.KnownSuppressor')},0.97,1)
    expect({part('Laser','Base.ANPEQ_10')},1,1.25)
    expect({part('Laser','Base.ANPEQ_10'),part('Laser','Base.SecondLaser')},1,1.25)
    expect({part('Grip','Base.TacticalGrip'),part('Stock','Base.Stock'),part('Canon','Base.TestBrake')},0.384,1)
    expect({part('Grip','Base.TacticalGrip'),part('Grip','Base.SecondGrip')},0.6,1)
    """;
  String setup="require=function()return {partModifiers={['Base.KnownSuppressor']={RealRecoil=-3}}} end\n";
  Object[] result=thread.pcall(LuaCompiler.loadstring(setup+Files.readString(Path.of(args[0]))+"\n"+tests,"attachment-tests",env),new Object[0]);
  if(!Boolean.TRUE.equals(result[0]))throw new AssertionError(java.util.Arrays.toString(result));
  System.out.println("Attachment Lua tests OK: grip, stock, muzzle fallback/native modifiers, laser, combinations and no duplicate grip/laser stacking.");
 }
}
