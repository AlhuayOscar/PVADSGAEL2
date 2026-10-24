import java.nio.file.*;
import se.krka.kahlua.luaj.compiler.LuaCompiler;
public class VerifyGlassLua {
    public static void main(String[] args)throws Exception {
        for(String arg:args){LuaCompiler.loadstring(Files.readString(Path.of(arg)),arg,null);System.out.println("Lua compiled: "+arg);}
    }
}
