import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public class PatchRecoilPhysics {
    public static void main(String[] args)throws Exception {
        try(ZipFile zip=new ZipFile(args[0]);ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(Path.of(args[1])))) {
            var entries=zip.entries();boolean patched=false;
            while(entries.hasMoreElements()){
                ZipEntry e=entries.nextElement();byte[] bytes=zip.getInputStream(e).readAllBytes();
                if(e.getName().equals("pvads/RecoilPhysics.class"))throw new IllegalStateException("Already patched");
                if(e.getName().equals("pvads/ADS.class")){
                    ClassNode c=new ClassNode();new ClassReader(bytes).accept(c,0);int edits=0;
                    for(MethodNode m:c.methods){
                        if(m.name.equals("onFire")){
                            for(AbstractInsnNode i=m.instructions.getFirst();i!=null;i=i.getNext())if(i instanceof FieldInsnNode f&&f.getOpcode()==Opcodes.GETFIELD&&f.name.equals("recoilAmount")){
                                m.instructions.insert(i,new MethodInsnNode(Opcodes.INVOKESTATIC,"pvads/RecoilPhysics","shotScale","(F)F",false));edits++;break;
                            }
                        }
                        if(m.name.equals("lookEffects")&&m.desc.equals("(F)V")){
                            AbstractInsnNode start=null,end=null;
                            for(AbstractInsnNode i=m.instructions.getFirst();i!=null;i=i.getNext())if(i instanceof FieldInsnNode f&&f.owner.equals("pvads/ADS")){
                                if(start==null&&f.name.equals("vKickV")&&f.getOpcode()==Opcodes.GETSTATIC)start=i;
                                if(start!=null&&f.name.equals("gBack")&&f.getOpcode()==Opcodes.PUTSTATIC){end=i;break;}
                            }
                            if(start==null||end==null)throw new IllegalStateException("Spring block missing");
                            InsnList add=new InsnList();add.add(new VarInsnNode(Opcodes.FLOAD,0));add.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"pvads/RecoilPhysics","stepSprings","(F)V",false));m.instructions.insertBefore(start,add);
                            for(AbstractInsnNode i=start;;){AbstractInsnNode next=i.getNext();m.instructions.remove(i);if(i==end)break;i=next;}
                            edits++;
                        }
                    }
                    if(edits!=2)throw new IllegalStateException("Expected 2 patches, got "+edits);
                    ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(writer);bytes=writer.toByteArray();patched=true;
                }
                out.putNextEntry(new ZipEntry(e.getName()));out.write(bytes);out.closeEntry();
            }
            if(!patched)throw new IllegalStateException("ADS missing");
            out.putNextEntry(new ZipEntry("pvads/RecoilPhysics.class"));out.write(Files.readAllBytes(Path.of(args[2])));out.closeEntry();
        }
    }
}
