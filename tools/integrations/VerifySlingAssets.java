import jassimp.*;
import java.util.*;
public class VerifySlingAssets {
 public static void main(String[] args)throws Exception {
  Jassimp.setLibraryLoader(new JassimpLibraryLoader(){public void loadLibrary(){System.load("D:\\Steam\\steamapps\\common\\ProjectZomboid\\jassimp64.dll");}});
  for(String path:args) {
   var scene=Jassimp.importFile(path,EnumSet.of(AiPostProcessSteps.TRIANGULATE));
   if(scene.getMeshes().size()!=1)throw new AssertionError("mesh count");
   var mesh=scene.getMeshes().get(0);
   if(!mesh.hasNormals()||!mesh.hasTexCoords(0)||mesh.getNumFaces()!=384)throw new AssertionError("mesh data "+mesh.getNumFaces());
   System.out.println("Sling native import OK: "+mesh.getName()+", 384 triangles, normals + UVs");
  }
 }
}
