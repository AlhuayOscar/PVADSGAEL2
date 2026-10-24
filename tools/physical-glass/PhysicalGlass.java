package pvads;

import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.*;
import se.krka.kahlua.vm.KahluaTable;
import zombie.core.skinnedmodel.model.ModelInstance;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelMesh;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.skinnedmodel.ModelManager;
import zombie.core.textures.Texture;

/** Perspective glass surface exported from the very same Blender reference discs.
 * Does not mutate the character, camera, gun pose, or the original Gael assets. */
public final class PhysicalGlass {
    private static final Map<String,float[]> REFERENCES = new HashMap<>();
    private static final Map<Object,Float> AXIS_SIGNS = Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile Surface surface;
    private static volatile String ownedKey;
    private static final Map<String,Model> referenceModels = new HashMap<>();
    private static final Map<Object,float[]> loadedVertices = Collections.synchronizedMap(new WeakHashMap<>());
    private static volatile float pitch, yaw, roll;
    private static volatile boolean enabled = true;
    private static Field scModels, instances, meshes, values, count;
    private static int stride;
    private static int program, vao;
    private static long lastNote;
    private static String lastBound = "";
    private static boolean failed;
    private static long earlyStartNs;
    private static Object earlyWeapon;
    private static final Map<String,Float> eyeDistances = new java.util.concurrent.ConcurrentHashMap<>();
    private static boolean physicalScope() {
        return enabled && !failed && ADS.P!=null && ADS.P.mode==2 && ADS.S.pip && ownedKey!=null;
    }
    private static String distanceKey(){return ADS.P.weaponId+"|"+ADS.P.opticIdStr;}
    public static boolean moveScopeY(Object direction) {
        if(!physicalScope() || !VP.firstPerson() || !ADS.want || ADS.suspended || Presence.adsBlocked())return false;
        float dir=(float)Api.num(direction,0);
        if(dir==0)return false;
        String key=distanceKey();
        float offset=eyeDistances.getOrDefault(key,0f);
        // Same axis as Mira Y. Wheel up brings the optic closer to the eye.
        offset=distanceStep(offset,dir);
        eyeDistances.put(key,offset);
        note("wheel Mira Y offset="+offset+" (session-only; PiP magnification unchanged)");
        return true;
    }
    public static float adjustY(float base){return base+(physicalScope()?eyeDistances.getOrDefault(distanceKey(),0f):0f);}
    public static float physicalFov(float original){return physicalScope()?1f:original;}
    public static float physicalFocus(float original){return ADS.P!=null && ADS.P.mode!=0?1f:original;}
    static float distanceStep(float offset,float dir){
        return Math.max(-0.075f,Math.min(0.075f,offset+(dir>0?0.005f:-0.005f)));
    }
    private static final Map<String,Field> PIP_FIELDS = new HashMap<>();
    private static final class Surface {
        final ModelInstance gun, optic;
        final float[] gunPoints, opticPoints;
        final String key;
        Surface(ModelInstance g, ModelInstance o, float[] gp, float[] op, String k) {
            gun=g; optic=o; gunPoints=gp; opticPoints=op; key=k;
        }
    }
    static {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                PhysicalGlass.class.getResourceAsStream("/pvads/gael-glass.csv"), StandardCharsets.UTF_8))) {
            for (String line; (line=reader.readLine())!=null;) {
                String[] cells=line.split(",");
                float[] points=new float[cells.length-1];
                for(int i=1;i<cells.length;i++) points[i-1]=Float.parseFloat(cells[i]);
                REFERENCES.put(cells[0],points);
            }
            Log.info("PhysicalGlass v2: registered "+REFERENCES.size()+" reference mesh names; geometry comes from loaded FBX");
        } catch (Throwable e) { failed=true; Log.error("PhysicalGlass reference data",e); }
    }
    public static void profile(KahluaTable table) {
        if(table==null)return;
        pitch=Api.f(table,"glassPitch",0); yaw=Api.f(table,"glassYaw",0); roll=Api.f(table,"glassRoll",0);
        enabled=Api.f(table,"physicalGlass",1)>0.5f;
    }
    /** Render-only visibility: leave ADS.lensUp, gun pose and firing logic alone. */
    public static boolean earlyWanted(boolean original) {
        boolean eligible=enabled && !failed && !Pip.broken && ADS.P!=null && ADS.P.valid && ADS.P.mode==2
            && ADS.S.enabled && ADS.S.pip && ADS.want && !ADS.suspended
            && VP.firstPerson() && !Presence.adsBlocked() && ADS.t>0;
        boolean elapsed=visibilityDelay(eligible,ADS.lastWeaponInst,System.nanoTime());
        return original || (eligible && elapsed && surface!=null);
    }
    static boolean visibilityDelay(boolean eligible,Object weapon,long now) {
        if(!eligible || weapon==null){earlyStartNs=0;earlyWeapon=null;return false;}
        if(earlyStartNs==0 || earlyWeapon!=weapon){earlyStartNs=now;earlyWeapon=weapon;}
        return now-earlyStartNs>=200_000_000L;
    }
    public static boolean capturePath(String path,String ignored) {
        String p=path.toLowerCase(Locale.ROOT);
        return p.contains("weapon") || p.contains("gaelscopes") || p.contains("/scope/");
    }
    /** Capture the selected FBX mesh's real position/UV data before VBO upload. */
    public static void loaded(Object mesh,Object processed) {
        if(!(mesh instanceof ModelMesh) || processed==null)return;
        try {
            String path=((ModelMesh)mesh).getPath().getPath().toLowerCase(Locale.ROOT);
            if(!path.contains("gaelscopes"))return;
            List<?> list=(List<?>)field(processed.getClass(),"staticMeshes").get(processed);
            if(list==null || list.isEmpty())return;
            Object selected=list.get(0);
            VertexBufferObject.VertexArray a=(VertexBufferObject.VertexArray)field(selected.getClass(),"verticesUnskinned").get(selected);
            int pos=a.format.indexOf(VertexBufferObject.VertexType.VertexArray);
            int uv=a.format.indexOf(VertexBufferObject.VertexType.TextureCoordArray);
            if(pos<0||uv<0)return;
            float[] out=new float[a.numVertices*5];
            for(int i=0;i<a.numVertices;i++){
                for(int j=0;j<3;j++)out[i*5+j]=a.getElementFloat(i,pos,j);
                out[i*5+3]=a.getElementFloat(i,uv,0);out[i*5+4]=a.getElementFloat(i,uv,1);
            }
            loadedVertices.put(mesh,out);
            Log.info("PhysicalGlass: captured FBX mesh "+path+" vertices="+a.numVertices+" uv=true");
        }catch(Throwable e){Log.error("PhysicalGlass FBX capture",e);}
    }
    /** Called on the existing alignment thread, after the gun instance is assigned. */
    public static void bind() {
        if(failed || !enabled || ADS.P==null || ADS.P.mode!=2 || !(Pip.meshInst instanceof ModelInstance)) {
            surface=null;ownedKey=null;return;
        }
        try {
            ModelInstance gun=(ModelInstance)Pip.meshInst;
            ModelInstance optic=ADS.findOptic(gun);
            if(optic==null || optic.modelScript==null || optic.model==null) {surface=null;ownedKey=null;note("bind: optic instance/script/model unavailable");return;}
            String mesh=optic.modelScript.getMeshName();
            if(mesh==null) {surface=null;ownedKey=null;note("bind: optic mesh name unavailable");return;}
            String key=mesh.substring(mesh.lastIndexOf('|')+1);
            String actualKey=null;
            for(String candidate:REFERENCES.keySet())if(candidate.equalsIgnoreCase(key)){actualKey=candidate;break;}
            if(actualKey==null) {surface=null;ownedKey=null;note("bind: unrecognised optic mesh "+mesh);return;}
            key=actualKey;
            ownedKey=key;
            Surface previous=surface;
            if(previous!=null && (previous.gun!=gun || previous.optic!=optic))surface=null;
            Model reference=referenceModels.get(key);
            if(reference==null){
                reference=ModelManager.instance.loadStaticModel("PVADSGael/GaelScopes|"+key+"_Vidrio_Referencia",optic.modelScript.getTextureName(),"basicEffect");
                if(reference!=null)referenceModels.put(key,reference);
                note("requested real reference mesh "+key+"; housing="+mesh);
            }
            if(reference==null||!reference.mesh.isReady()){note("waiting for reference asset "+key);return;}
            float[] imported=loadedVertices.get(reference.mesh);
            if(imported==null){note("reference ready but missing captured UV/vertices "+key);return;}
            // Deduplicate the triangulated mesh by UV, order its boundary, and keep
            // all three loaded position coordinates. No CSV coordinate substitute.
            TreeMap<Double,float[]> perimeter=new TreeMap<>();
            for(int i=0;i<imported.length;i+=5){
                float u=imported[i+3],v=imported[i+4];
                if(Math.abs(u-0.5f)+Math.abs(v-0.5f)<0.001f)continue;
                perimeter.put(Math.atan2(v-0.5,u-0.5),Arrays.copyOfRange(imported,i,i+5));
            }
            if(perimeter.size()!=24){note("reference boundary must contain 24 UV points: "+key+" got "+perimeter.size());return;}
            float[] raw=new float[24*5];int ri=0;
            Matrix4f relative=new Matrix4f();
            if(optic.model.mesh.transform!=null)relative.set(optic.model.mesh.transform).invert();
            if(reference.mesh.transform!=null)relative.mul(reference.mesh.transform);
            Vector3f sourcePoint=new Vector3f();
            for(float[] vertex:perimeter.values()){
                relative.transformPosition(sourcePoint.set(vertex[0],vertex[1],vertex[2]));
                raw[ri++]=sourcePoint.x;raw[ri++]=sourcePoint.y;raw[ri++]=sourcePoint.z;raw[ri++]=vertex[3];raw[ri++]=vertex[4];
            }
            float cx=0,cy=0,cz=0,rx=0,rz=0;
            int n=raw.length/5;
            for(int i=0;i<n;i++){ cx+=raw[i*5];cy+=raw[i*5+1];cz+=raw[i*5+2]; }
            cx/=n;cy/=n;cz/=n;
            for(int i=0;i<n;i++){rx=Math.max(rx,Math.abs(raw[i*5]-cx));rz=Math.max(rz,Math.abs(raw[i*5+2]-cz));}
            float scale=Math.max(0.01f,ADS.P.pipAnchorScale)*Math.max(0.01f,ADS.P.pipScale);
            Matrix4f offset=new Matrix4f().translation(cx+ADS.P.pipAnchorX+ADS.P.pipX*rx,
                cy+ADS.P.pipAnchorY+ADS.P.pipZ,cz+ADS.P.pipAnchorZ+ADS.P.pipY*rz)
                .rotateXYZ((float)Math.toRadians(pitch),(float)Math.toRadians(yaw),(float)Math.toRadians(roll)).scale(scale);
            Matrix4f attachment=ADS.partMatrix(optic,gun.getAttachmentById(optic.attachmentNameParent),null,false,new Matrix4f());
            // 26 fan vertices: centre, perimeter, repeated first perimeter vertex.
            float[] op=new float[(n+2)*5],gp=new float[op.length];
            Vector3f point=new Vector3f();
            for(int i=0;i<n+2;i++) {
                int j=(i-1+n)%n;
                float x=i==0?0:raw[j*5]-cx,y=i==0?0:raw[j*5+1]-cy,z=i==0?0:raw[j*5+2]-cz;
                offset.transformPosition(point.set(x,y,z));
                int at=i*5;
                op[at]=point.x;op[at+1]=point.y;op[at+2]=point.z;
                op[at+3]=i==0?0.5f:raw[j*5+3]; op[at+4]=i==0?0.5f:raw[j*5+4];
                attachment.transformPosition(point);
                gp[at]=point.x;gp[at+1]=point.y;gp[at+2]=point.z;
                gp[at+3]=op[at+3];gp[at+4]=op[at+4];
            }
            Surface next=new Surface(gun,optic,gp,op,key);
            surface=next;
            // Also supply a physical centre to the existing PiP pass diagnostics.
            Pip.meshEye=new float[]{gp[0],gp[1],gp[2],0,1,0,Math.max(rx,rz)*scale};
            Pip.scopeLens2DBackup=false;
            if(!lastBound.equals(key)) {lastBound=key;Log.info("PhysicalGlass: bound real FBX "+key+"_Vidrio_Referencia; housing="+mesh+" (loaded vertices + UV)");}
        } catch(Throwable e) { surface=null; note("bind failed: "+e); }
    }
    private static float fit(float[] raw,float[] mesh,float sign) {
        float total=0;
        for(int i=0;i<raw.length;i+=3){
            float best=Float.MAX_VALUE;
            for(int j=0;j<mesh.length;j+=3){
                float x=raw[i]-mesh[j],y=raw[i+1]*sign-mesh[j+1],z=raw[i+2]*sign-mesh[j+2];
                best=Math.min(best,x*x+y*y+z*z);
            }
            total+=best;
        }
        return total;
    }
    private static Field field(Class<?> c,String name) throws Exception {Field f=c.getDeclaredField(name);f.setAccessible(true);return f;}
    private static Object pip(String name) throws Exception {
        Field f=PIP_FIELDS.get(name);if(f==null){f=field(Pip.class,name);PIP_FIELDS.put(name,f);}return f.get(null);
    }
    private static void note(String message) {
        long now=System.nanoTime();if(now-lastNote>2_000_000_000L){lastNote=now;Log.info("PhysicalGlass: "+message);}
    }
    /** True means this surface owns the composite, even if offscreen or not drawn.
     * Never show a camera-centred fallback for a recognised physical optic. */
    public static boolean render(int vx,int vy,int width,int height,float zoom,Matrix4f projection,
            Matrix4f view,float[] eye,Object frame,Object scene,int draw,int read) {
        Surface s=surface;
        if(failed || !enabled || ADS.P==null || ADS.P.mode!=2) return false;
        if(s==null){if(ownedKey!=null){note("physical reference pending "+ownedKey+"; legacy circle blocked");return true;}return false;}
        try {
            if(scModels==null){
                scModels=scene.getClass().getField("models");
                Class<?> c=scModels.getType();
                instances=field(c,"instances");meshes=field(c,"meshes");values=field(c,"values");count=field(c,"count");
                stride=field(c,"VALUES").getInt(null);
                if(stride<16)throw new IllegalStateException("Invalid model stride "+stride);
            }
            Object models=scModels.get(scene);
            Object[] inst=(Object[])instances.get(models);
            float[] data=(float[])values.get(models);
            int n=count.getInt(models),index=-1;
            boolean optic=false;
            for(int i=0;i<n;i++)if(inst[i]==s.optic){index=i;optic=true;break;}
            if(index<0)for(int i=0;i<n;i++)if(inst[i]==s.gun){index=i;break;}
            if(index<0){note("no drawn instance for "+s.key+"; camera fallback suppressed");return true;}
            Matrix4f model=new Matrix4f().set(data,index*stride);
            Matrix4f mvp=new Matrix4f(projection).mul(view).mul(model);
            float[] local=optic?s.opticPoints:s.gunPoints;
            float[] clips=new float[26*4],uv=new float[26*2];
            Vector4f p=new Vector4f();
            float minX=Float.MAX_VALUE,maxX=-Float.MAX_VALUE,minY=Float.MAX_VALUE,maxY=-Float.MAX_VALUE;
            for(int i=0;i<26;i++){
                mvp.transform(p.set(local[i*5],local[i*5+1],local[i*5+2],1));
                if(!Float.isFinite(p.w)||p.w<=0.0001f){note("surface behind near plane "+s.key);return true;}
                clips[i*4]=p.x;clips[i*4+1]=p.y;clips[i*4+2]=p.z;clips[i*4+3]=p.w;
                uv[i*2]=local[i*5+3];uv[i*2+1]=local[i*5+4];
                float x=p.x/p.w,y=p.y/p.w;
                minX=Math.min(minX,x);maxX=Math.max(maxX,x);minY=Math.min(minY,y);maxY=Math.max(maxY,y);
            }
            if(maxX < -1 || minX > 1 || maxY < -1 || minY > 1)return true;
            int color=(Integer)pip("colorTex");if(color==0)return true;
            drawSurface(clips,uv,color,width,height,(maxX-minX)*0.5f,(maxY-minY)*0.5f);
            note("draw "+s.key+" via "+(optic?"optic":"gun")+" matrix, depth="+WeaponDepth.ready()+
                " ndc=["+minX+","+minY+".."+maxX+","+maxY+"]");
            return true;
        } catch(Throwable e) { note("render failed (no screen fallback): "+e);return true; }
    }
    private static int shader(int type,String source) {
        int s=GL20.glCreateShader(type);GL20.glShaderSource(s,source);GL20.glCompileShader(s);
        if(GL20.glGetShaderi(s,GL20.GL_COMPILE_STATUS)==0)throw new IllegalStateException(GL20.glGetShaderInfoLog(s));return s;
    }
    private static void build() {
        int vs=shader(GL20.GL_VERTEX_SHADER,"#version 330\nuniform vec4 uClip[26];uniform vec2 uUV[26];out vec2 uv;void main(){gl_Position=uClip[gl_VertexID];uv=uUV[gl_VertexID];}");
        int fs=shader(GL20.GL_FRAGMENT_SHADER,"""
            #version 330
            in vec2 uv; out vec4 fragColor;
            uniform sampler2D uTex,uRet,uGlow,uDepthTex;
            uniform vec2 uPic,uImageScale,uDepthSize,uRetOffset,uCrop;
            uniform vec4 uRetUV,uGlowUV,uRetColor;
            uniform int uRetMode,uHasGlow,uHasDepth;
            uniform float uRetRoll;
            void main(){
                if(uHasDepth!=0){float depth=texture(uDepthTex,gl_FragCoord.xy/uDepthSize).r;
                    if(depth+0.00035<gl_FragCoord.z)discard;}
                vec2 d=(uv-0.5)*2.0;
                float r=length(d-uCrop);float edge=max(fwidth(r),0.0001);
                float alpha=1.0-smoothstep(1.0-edge,1.0,r);if(alpha<=0.0)discard;
                // Correct the mirrored PiP image only; retain mesh UV and reticle orientation.
                vec2 imageUV=vec2(1.0-uv.x,uv.y);
                vec3 c=texture(uTex,(vec2(0.5)+(imageUV-0.5)*uImageScale)*uPic).rgb;
                vec2 q=(d-uRetOffset)/(uRetMode==2?0.098:0.98);
                q=mat2(cos(uRetRoll),sin(uRetRoll),-sin(uRetRoll),cos(uRetRoll))*q;
                if(uRetMode!=0 && abs(q.x)<=1.0 && abs(q.y)<=1.0){
                    vec2 f=vec2(q.x*0.5+0.5,0.5-q.y*0.5);
                    float a=texture(uRet,mix(uRetUV.xy,uRetUV.zw,f)).a;
                    c=mix(c,uRetMode==1?vec3(0.03):uRetColor.rgb,a*(uRetMode==1?1.0:uRetColor.a));
                    if(uHasGlow!=0)c=mix(c,uRetColor.rgb,texture(uGlow,mix(uGlowUV.xy,uGlowUV.zw,f)).a*uRetColor.a*0.9);
                }
                c*=0.35+0.65*(1.0-smoothstep(0.8,1.0,r));
                fragColor=vec4(c,alpha);
            }
            """);
        int p=GL20.glCreateProgram();GL20.glAttachShader(p,vs);GL20.glAttachShader(p,fs);GL20.glLinkProgram(p);
        GL20.glDeleteShader(vs);GL20.glDeleteShader(fs);
        if(GL20.glGetProgrami(p,GL20.GL_LINK_STATUS)==0)throw new IllegalStateException(GL20.glGetProgramInfoLog(p));
        program=p;vao=GL30.glGenVertexArrays();Log.info("PhysicalGlass: perspective UV shader linked");
    }
    private static int u(String name){return GL20.glGetUniformLocation(program,name);}
    private static void texture(int unit,int id,String uniform){GL13.glActiveTexture(GL13.GL_TEXTURE0+unit);GL11.glBindTexture(GL11.GL_TEXTURE_2D,id);GL20.glUniform1i(u(uniform),unit);}
    private static void atlas(String uniform,Texture t){GL20.glUniform4f(u(uniform),t.getXStart(),t.getYStart(),t.getXEnd(),t.getYEnd());}
    private static void restore(int cap,boolean on){if(on)GL11.glEnable(cap);else GL11.glDisable(cap);}
    private static void drawSurface(float[] clips,float[] uv,int color,int w,int h,float sx,float sy) throws Exception {
        if(program==0)build();
        int oldProgram=GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),oldVao=GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int srcRGB=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),dstRGB=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcA=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),dstA=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        int eqRGB=GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB),eqA=GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
        boolean depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),blend=GL11.glIsEnabled(GL11.GL_BLEND),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE),scissor=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        boolean mask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int[] units={0,1,2,4},bindings=new int[4];
        for(int i=0;i<4;i++){GL13.glActiveTexture(GL13.GL_TEXTURE0+units[i]);bindings[i]=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);}
        try {
            GL11.glDisable(GL11.GL_DEPTH_TEST);GL11.glDisable(GL11.GL_CULL_FACE);GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glEnable(GL11.GL_BLEND);GL11.glDepthMask(false);
            GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD,GL14.GL_FUNC_ADD);
            GL14.glBlendFuncSeparate(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA,GL11.GL_ONE,GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL20.glUseProgram(program);GL30.glBindVertexArray(vao);
            GL20.glUniform4fv(u("uClip"),clips);GL20.glUniform2fv(u("uUV"),uv);
            texture(0,color,"uTex");texture(4,WeaponDepth.texture(),"uDepthTex");
            GL20.glUniform1i(u("uHasDepth"),WeaponDepth.ready()?1:0);
            GL20.glUniform2f(u("uDepthSize"),WeaponDepth.width(),WeaponDepth.height());
            GL20.glUniform2f(u("uPic"),(Float)pip("picSX"),(Float)pip("picSY"));
            GL20.glUniform2f(u("uImageScale"),sx,sy);
            GL20.glUniform2f(u("uRetOffset"),ADS.P.pipRetX,ADS.P.pipRetY);
            GL20.glUniform2f(u("uCrop"),ADS.P.pipCropX,ADS.P.pipCropY);
            GL20.glUniform1f(u("uRetRoll"),(float)Math.toRadians(ADS.P.reticleRoll));
            Texture ret=(Texture)pip("retTex"),glow=(Texture)pip("glowTex");
            int mode=(Integer)pip("retMode");if(ret==null||ret.getID()<=0)mode=0;
            GL20.glUniform1i(u("uRetMode"),mode);
            GL20.glUniform1i(u("uHasGlow"),glow!=null&&glow.getID()>0&&mode==1?1:0);
            if(mode!=0){texture(1,ret.getID(),"uRet");atlas("uRetUV",ret);}
            if(glow!=null&&glow.getID()>0&&mode==1){texture(2,glow.getID(),"uGlow");atlas("uGlowUV",glow);}
            GL20.glUniform4f(u("uRetColor"),(Float)pip("retR"),(Float)pip("retG"),(Float)pip("retB"),Math.min(1,(Float)pip("retBright")));
            GL11.glDrawArrays(GL11.GL_TRIANGLE_FAN,0,26);
        } finally {
            for(int i=0;i<4;i++){GL13.glActiveTexture(GL13.GL_TEXTURE0+units[i]);GL11.glBindTexture(GL11.GL_TEXTURE_2D,bindings[i]);}
            GL13.glActiveTexture(active);GL30.glBindVertexArray(oldVao);GL20.glUseProgram(oldProgram);
            GL14.glBlendFuncSeparate(srcRGB,dstRGB,srcA,dstA);GL20.glBlendEquationSeparate(eqRGB,eqA);
            GL11.glDepthMask(mask);restore(GL11.GL_DEPTH_TEST,depth);restore(GL11.GL_BLEND,blend);restore(GL11.GL_CULL_FACE,cull);restore(GL11.GL_SCISSOR_TEST,scissor);
        }
    }
}
