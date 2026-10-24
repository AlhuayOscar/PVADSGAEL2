import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import java.nio.*;
import java.lang.reflect.*;
public class VerifyGlassShader {
    public static void main(String[] args)throws Exception {
        if(!GLFW.glfwInit())throw new IllegalStateException("GLFW init");
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE,GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR,3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR,3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE,GLFW.GLFW_OPENGL_CORE_PROFILE);
        long window=GLFW.glfwCreateWindow(64,64,"PVADS shader verification",0,0);
        if(window==0)throw new IllegalStateException("Hidden GL context");
        try {
            GLFW.glfwMakeContextCurrent(window);GL.createCapabilities();
            Class<?> c=Class.forName("pvads.PhysicalGlass");
            Method m=c.getDeclaredMethod("build");m.setAccessible(true);m.invoke(null);
            Field pf=c.getDeclaredField("program"),vf=c.getDeclaredField("vao");pf.setAccessible(true);vf.setAccessible(true);
            int program=pf.getInt(null);GL20.glUseProgram(program);GL30.glBindVertexArray(vf.getInt(null));
            float[] clip=new float[104],uv=new float[52];
            for(int i=0;i<26;i++){
                double a=(i-1)*Math.PI*2/24;float x=i==0?0:(float)Math.cos(a),y=i==0?0:(float)Math.sin(a);
                clip[i*4]=x*0.8f;clip[i*4+1]=y*0.8f;clip[i*4+3]=1;
                uv[i*2]=x*0.5f+0.5f;uv[i*2+1]=y*0.5f+0.5f;
            }
            GL20.glUniform4fv(GL20.glGetUniformLocation(program,"uClip"),clip);GL20.glUniform2fv(GL20.glGetUniformLocation(program,"uUV"),uv);
            GL20.glUniform2f(GL20.glGetUniformLocation(program,"uPic"),1,1);
            GL20.glUniform2f(GL20.glGetUniformLocation(program,"uImageScale"),1,1);
            GL20.glUniform2f(GL20.glGetUniformLocation(program,"uDepthSize"),64,64);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);int tex=GL11.glGenTextures();GL11.glBindTexture(GL11.GL_TEXTURE_2D,tex);
            ByteBuffer white=ByteBuffer.allocateDirect(4);white.put(new byte[]{-1,-1,-1,-1}).flip();
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL11.GL_RGBA8,1,1,0,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,white);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
            GL20.glUniform1i(GL20.glGetUniformLocation(program,"uTex"),0);
            GL11.glViewport(0,0,64,64);GL11.glClearColor(0,0,0,1);GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN,0,26);
            if(pixel(32,32)<240||pixel(1,1)!=0)throw new IllegalStateException("Surface UV/aperture pixel check");
            // An inclined plane must move its aperture with geometry, not screen centre.
            for(int i=0;i<26;i++){clip[i*4]=clip[i*4]*0.25f+0.35f;clip[i*4+2]=clip[i*4+1]*0.2f;}
            GL20.glUniform4fv(GL20.glGetUniformLocation(program,"uClip"),clip);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN,0,26);
            if(pixel(32,32)!=0||pixel(43,32)<240)throw new IllegalStateException("Transformed surface pixel check");
            GL13.glActiveTexture(GL13.GL_TEXTURE4);int dt=GL11.glGenTextures();GL11.glBindTexture(GL11.GL_TEXTURE_2D,dt);
            FloatBuffer depth=ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asFloatBuffer();depth.put(0.2f).flip();
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_R32F,1,1,0,GL11.GL_RED,GL11.GL_FLOAT,depth);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_NEAREST);
            GL20.glUniform1i(GL20.glGetUniformLocation(program,"uDepthTex"),4);
            GL20.glUniform1i(GL20.glGetUniformLocation(program,"uHasDepth"),1);
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN,0,26);
            if(pixel(43,32)!=0)throw new IllegalStateException("Occlusion pixel check");
            int err=GL11.glGetError();if(err!=0)throw new IllegalStateException("GL error "+err);
            System.out.println("PhysicalGlass vertex/fragment shaders compiled and linked on "+GL11.glGetString(GL11.GL_RENDERER));
            System.out.println("GPU pixel tests OK: UV surface, aperture, transformed plane, depth occlusion.");
        } finally {GLFW.glfwDestroyWindow(window);GLFW.glfwTerminate();}
    }
    static int pixel(int x,int y){ByteBuffer b=ByteBuffer.allocateDirect(4);GL11.glReadPixels(x,y,1,1,GL11.GL_RGBA,GL11.GL_UNSIGNED_BYTE,b);return b.get(0)&255;}
}
