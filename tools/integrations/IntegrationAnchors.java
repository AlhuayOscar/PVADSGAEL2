package pvads;
import zombie.scripting.ScriptManager;
import zombie.scripting.objects.ModelScript;
import zombie.scripting.objects.ModelAttachment;

/** Additive anchors only; never replace MaleBody/FemaleBody scripts. */
public final class IntegrationAnchors {
    public static boolean setup() {
        ModelScript male=ScriptManager.instance.getModelScript("MaleBody");
        ModelScript female=ScriptManager.instance.getModelScript("FemaleBody");
        if(male==null||female==null)return false;
        add(male);add(female);return true;
    }
    private static void add(ModelScript model) {
        if(model.getAttachmentById("pvg_sling_front")!=null)return;
        ModelAttachment anchor=new ModelAttachment("pvg_sling_front");
        anchor.setBone("Bip01_Spine");
        anchor.getOffset().set(.125f,-.045f,.11f);
        anchor.getRotate().set(280f,25f,80f);
        model.addAttachment(anchor);
    }
}
