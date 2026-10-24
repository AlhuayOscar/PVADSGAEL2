import java.nio.file.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
public class PatchIntegrationAnchors {
 public static void main(String[] args)throws Exception {
  boolean found=false;
  try(var zip=new ZipFile(args[0]);var out=new ZipOutputStream(Files.newOutputStream(Path.of(args[1])))) {
   var entries=zip.entries();
   while(entries.hasMoreElements()) {
    var entry=entries.nextElement();var bytes=zip.getInputStream(entry).readAllBytes();
    if(entry.getName().equals("pvads/IntegrationAnchors.class"))throw new IllegalStateException("Already patched");
    if(entry.getName().equals("pvads/Api.class")) {
     var node=new ClassNode();new ClassReader(bytes).accept(node,0);
     if(node.methods.stream().anyMatch(m->m.name.equals("setupSlingAnchors")))throw new IllegalStateException("Duplicate method");
     var method=new MethodNode(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"setupSlingAnchors","()Z",null,null);
     var annotation=method.visitAnnotation("Lse/krka/kahlua/integration/annotations/LuaMethod;",true);
     annotation.visit("name","PVADS_setupSlingAnchors");annotation.visit("global",true);annotation.visitEnd();
     method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"pvads/IntegrationAnchors","setup","()Z",false));
     method.instructions.add(new InsnNode(Opcodes.IRETURN));node.methods.add(method);
     var writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);node.accept(writer);bytes=writer.toByteArray();found=true;
    }
    out.putNextEntry(new ZipEntry(entry.getName()));out.write(bytes);out.closeEntry();
   }
   if(!found)throw new IllegalStateException("Api missing");
   out.putNextEntry(new ZipEntry("pvads/IntegrationAnchors.class"));out.write(Files.readAllBytes(Path.of(args[2])));out.closeEntry();
  }
  System.out.println("Added optional sling-anchor API; existing rendering/recoil bytecode preserved.");
 }
}
