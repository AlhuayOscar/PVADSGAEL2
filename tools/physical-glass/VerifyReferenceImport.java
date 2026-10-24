import jassimp.*;
import java.lang.reflect.*;
import java.util.*;
import zombie.core.skinnedmodel.model.jassimp.*;
import zombie.core.skinnedmodel.model.VertexBufferObject;

public class VerifyReferenceImport {
    public static void main(String[] args)throws Exception {
        Jassimp.setLibraryLoader(new JassimpLibraryLoader(){public void loadLibrary(){System.load("D:\\Steam\\steamapps\\common\\ProjectZomboid\\jassimp64.dll");}});
        AiScene scene=Jassimp.importFile(args[0],EnumSet.of(AiPostProcessSteps.TRIANGULATE));
        int verified=0;
        for(AiMesh mesh:scene.getMeshes())if(mesh.getName().endsWith("_Vidrio_Referencia")) {
            ProcessedAiSceneParams p=ProcessedAiSceneParams.create();p.scene=scene;p.mode=JAssImpImporter.LoadMode.StaticMesh;p.meshName=mesh.getName();p.animBonesScaleModifier=1;
            ProcessedAiScene selected=ProcessedAiScene.process(p);
            Field f=selected.getClass().getDeclaredField("staticMeshes");f.setAccessible(true);
            List<?> list=(List<?>)f.get(selected);
            if(list.size()!=1)throw new IllegalStateException("Expected selected mesh only: "+mesh.getName()+" got "+list.size());
            Object first=list.get(0);Field vertices=first.getClass().getDeclaredField("verticesUnskinned");vertices.setAccessible(true);
            VertexBufferObject.VertexArray a=(VertexBufferObject.VertexArray)vertices.get(first);
            int position=a.format.indexOf(VertexBufferObject.VertexType.VertexArray),uv=a.format.indexOf(VertexBufferObject.VertexType.TextureCoordArray);
            if(position<0||uv<0)throw new IllegalStateException("Missing position/UV "+mesh.getName());
            TreeSet<Double> angles=new TreeSet<>();
            for(int i=0;i<a.numVertices;i++) angles.add(Math.atan2(a.getElementFloat(i,uv,1)-0.5,a.getElementFloat(i,uv,0)-0.5));
            if(angles.size()!=24)throw new IllegalStateException("Boundary "+mesh.getName()+" "+angles.size());
            verified++;
        }
        if(verified!=55)throw new IllegalStateException("Missing references");
        System.out.println("PZ ProcessedAiScene selected all 55 reference meshes individually; real loaded positions/UV expose 24 boundary vertices each.");
    }
}
