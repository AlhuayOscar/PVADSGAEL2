import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public class PatchPhysicalGlass {
    static byte[] patch(byte[] bytes,String name) {
        ClassNode c=new ClassNode();new ClassReader(bytes).accept(c,0);
        int edits=0;
        for(MethodNode m:c.methods) {
            if(name.equals("pvads/ADS")&&m.name.equals("steadyZoomNow")&&m.desc.equals("()F")) {
                for(AbstractInsnNode i=m.instructions.getFirst();i!=null;i=i.getNext())if(i.getOpcode()==Opcodes.FRETURN) {
                    m.instructions.insertBefore(i,new MethodInsnNode(Opcodes.INVOKESTATIC,"pvads/PhysicalGlass","physicalFocus","(F)F",false));edits++;break;
                }
            }
            if(name.equals("pvads/ADS")&&(m.name.equals("align")||m.name.equals("fov"))) {
                for(AbstractInsnNode i=m.instructions.getFirst();i!=null;i=i.getNext())if(i instanceof FieldInsnNode f&&f.getOpcode()==Opcodes.GETFIELD&&f.owner.equals("pvads/ADS$Profile")) {
                    if(m.name.equals("align")&&f.name.equals("adjY")) {m.instructions.insert(i,new MethodInsnNode(Opcodes.INVOKESTATIC,"pvads/PhysicalGlass","adjustY","(F)F",false));edits++;break;}
                    if(m.name.equals("fov")&&f.name.equals("fovMul")) {m.instructions.insert(i,new MethodInsnNode(Opcodes.INVOKESTATIC,"pvads/PhysicalGlass","physicalFov","(F)F",false));edits++;break;}
                }
            }
            if(name.equals("pvads/Pip")&&m.name.equals("wanted")&&m.desc.equals("()Z")) {
                for(AbstractInsnNode i=m.instructions.getFirst();i!=null;i=i.getNext())if(i.getOpcode()==Opcodes.IRETURN) {
                    m.instructions.insertBefore(i,new MethodInsnNode(Opcodes.INVOKESTATIC,"pvads/PhysicalGlass","earlyWanted","(Z)Z",false));edits++;break;
                }
            }
            if(name.equals("pvads/ADS")&&m.name.equals("align")) {
                for(AbstractInsnNode i=m.instructions.getFirst();i!=null;i=i.getNext()) {
                    if(i instanceof FieldInsnNode f&&f.getOpcode()==Opcodes.PUTSTATIC&&f.owner.equals("pvads/Pip")&&f.name.equals("meshObj")) {
                        m.instructions.insert(i,new MethodInsnNode(Opcodes.INVOKESTATIC,"pvads/PhysicalGlass","bind","()V",false));edits++;break;
                    }
                }
            }
            if(name.equals("pvads/Api")&&m.name.equals("setProfile")) {
                InsnList list=new InsnList();list.add(new VarInsnNode(Opcodes.ALOAD,0));
                list.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"pvads/PhysicalGlass","profile","(Lse/krka/kahlua/vm/KahluaTable;)V",false));
                m.instructions.insert(list);edits++;
            }
            if(name.equals("pvads/MeshGeo")&&m.name.equals("onLoadedX")) {
                InsnList capture=new InsnList();capture.add(new VarInsnNode(Opcodes.ALOAD,0));capture.add(new VarInsnNode(Opcodes.ALOAD,1));
                capture.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"pvads/PhysicalGlass","loaded","(Ljava/lang/Object;Ljava/lang/Object;)V",false));
                m.instructions.insert(capture);edits++;
                for(AbstractInsnNode i=m.instructions.getFirst();i!=null;i=i.getNext()) {
                    if(i instanceof LdcInsnNode s&&"weapon".equals(s.cst)) {
                        AbstractInsnNode next=i.getNext();while(next!=null&&!(next instanceof MethodInsnNode))next=next.getNext();
                        if(!(next instanceof MethodInsnNode call)||!call.owner.equals("java/lang/String")||!call.name.equals("contains"))throw new IllegalStateException("capture anchor");
                        m.instructions.set(call,new MethodInsnNode(Opcodes.INVOKESTATIC,"pvads/PhysicalGlass","capturePath","(Ljava/lang/String;Ljava/lang/String;)Z",false));edits++;break;
                    }
                }
            }
            if(name.equals("pvads/Pip")&&m.name.equals("composite")) {
                if(!m.desc.equals("(IIIIFLorg/joml/Matrix4f;Lorg/joml/Matrix4f;[FLjava/lang/Object;Ljava/lang/Object;II)V"))throw new IllegalStateException(m.desc);
                InsnList list=new InsnList();LabelNode fallback=new LabelNode();
                for(int i=0;i<12;i++)list.add(new VarInsnNode(i<4||i>=10?Opcodes.ILOAD:(i==4?Opcodes.FLOAD:Opcodes.ALOAD),i));
                list.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"pvads/PhysicalGlass","render",m.desc.substring(0,m.desc.length()-1)+"Z",false));
                list.add(new JumpInsnNode(Opcodes.IFEQ,fallback));list.add(new InsnNode(Opcodes.RETURN));list.add(fallback);
                list.add(new FrameNode(Opcodes.F_SAME,0,null,0,null));m.instructions.insert(list);edits++;
            }
        }
        if(name.equals("pvads/Api")) {
            MethodNode wheel=new MethodNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"moveScopeY","(Ljava/lang/Object;)Z",null,null);
            AnnotationVisitor annotation=wheel.visitAnnotation("Lse/krka/kahlua/integration/annotations/LuaMethod;",true);
            annotation.visit("name","PVADS_moveScopeY");annotation.visit("global",true);annotation.visitEnd();
            wheel.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));
            wheel.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"pvads/PhysicalGlass","moveScopeY","(Ljava/lang/Object;)Z",false));
            wheel.instructions.add(new InsnNode(Opcodes.IRETURN));c.methods.add(wheel);edits++;
        }
        int expected=name.equals("pvads/ADS")?4:2;
        if(edits!=expected)throw new IllegalStateException(name+" expected "+expected+" patches, got "+edits);
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);c.accept(writer);return writer.toByteArray();
    }
    public static void main(String[] args)throws Exception {
        Path input=Path.of(args[0]),out=Path.of(args[1]),classes=Path.of(args[2]),resource=Path.of(args[3]);
        Set<String> targets=Set.of("pvads/ADS","pvads/Api","pvads/Pip","pvads/MeshGeo");
        int patched=0;
        try(ZipFile zip=new ZipFile(input.toFile());ZipOutputStream output=new ZipOutputStream(Files.newOutputStream(out))) {
            Enumeration<? extends ZipEntry> all=zip.entries();
            while(all.hasMoreElements()) {
                ZipEntry e=all.nextElement();if(e.getName().startsWith("pvads/PhysicalGlass")||e.getName().equals("pvads/gael-glass.csv"))throw new IllegalStateException("Already patched input");
                byte[] data=zip.getInputStream(e).readAllBytes();String name=e.getName().replace(".class","");
                if(targets.contains(name)){data=patch(data,name);patched++;}
                output.putNextEntry(new ZipEntry(e.getName()));output.write(data);output.closeEntry();
            }
            try(var stream=Files.list(classes.resolve("pvads"))) {
                for(Path f:stream.filter(p->p.getFileName().toString().startsWith("PhysicalGlass")&&p.toString().endsWith(".class")).toList()) {
                    output.putNextEntry(new ZipEntry("pvads/"+f.getFileName()));output.write(Files.readAllBytes(f));output.closeEntry();
                }
            }
            output.putNextEntry(new ZipEntry("pvads/gael-glass.csv"));output.write(Files.readAllBytes(resource));output.closeEntry();
        }
        if(patched!=4)throw new IllegalStateException("Missing targets "+patched);
        System.out.println("Patched 4 classes; added PhysicalGlass renderer and reference surface resource.");
    }
}
