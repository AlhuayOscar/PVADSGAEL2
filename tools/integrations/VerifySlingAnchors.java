import java.lang.reflect.*;
import zombie.scripting.objects.ModelScript;
public class VerifySlingAnchors {
 public static void main(String[] args)throws Exception {
  Class<?> api=Class.forName("pvads.Api",false,VerifySlingAnchors.class.getClassLoader());
  if(api.getDeclaredMethod("setupSlingAnchors").getReturnType()!=boolean.class)throw new AssertionError("API");
  Method add=Class.forName("pvads.IntegrationAnchors").getDeclaredMethod("add",ModelScript.class);add.setAccessible(true);
  ModelScript model=new ModelScript();add.invoke(null,model);
  var first=model.getAttachmentById("pvg_sling_front");
  if(first==null||!first.getBone().equals("Bip01_Spine"))throw new AssertionError("anchor");
  add.invoke(null,model);
  if(first!=model.getAttachmentById("pvg_sling_front"))throw new AssertionError("not idempotent");
  System.out.println("Anchor API verified; additive setup is idempotent and preserves existing anchors.");
 }
}
