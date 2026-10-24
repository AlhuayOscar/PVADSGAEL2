import jassimp.*;
import java.util.*;
public class VerifyGlass {
    public static void main(String[] args)throws Exception {
        Jassimp.setLibraryLoader(new JassimpLibraryLoader(){public void loadLibrary(){System.load("D:\\Steam\\steamapps\\common\\ProjectZomboid\\jassimp64.dll");}});
        for(String cls:List.of("pvads.PhysicalGlass","pvads.ADS","pvads.Api","pvads.Pip","pvads.MeshGeo")) {
            Class<?> c=Class.forName(cls,false,VerifyGlass.class.getClassLoader());
            c.getDeclaredMethods();System.out.println("Verified bytecode "+cls);
        }
        AiScene original=Jassimp.importFile(args[0],EnumSet.of(AiPostProcessSteps.TRIANGULATE));
        AiScene result=Jassimp.importFile(args[1],EnumSet.of(AiPostProcessSteps.TRIANGULATE));
        Map<String,AiMesh> before=new HashMap<>();for(AiMesh m:original.getMeshes())before.put(m.getName(),m);
        int references=0,unchanged=0;
        for(AiMesh mesh:result.getMeshes()) {
            if(mesh.getName().endsWith("_Vidrio_Referencia")) {
                references++;
                if(!mesh.hasTexCoords(0)||!mesh.hasNormals()||mesh.getNumFaces()!=22)throw new IllegalStateException("Bad reference "+mesh.getName());
            } else {
                AiMesh m=before.get(mesh.getName());
                if(m==null||m.getNumVertices()!=mesh.getNumVertices()||m.getNumFaces()!=mesh.getNumFaces()||m.hasTexCoords(0)!=mesh.hasTexCoords(0))throw new IllegalStateException("Original mesh changed "+mesh.getName());
                unchanged++;
            }
        }
        if(references!=55||unchanged!=55)throw new IllegalStateException("Counts "+references+" "+unchanged);
        System.out.println("Native Assimp import OK: 55 original meshes unchanged; 55 reference meshes with UV, normals and 22 triangles each.");
    }
}
