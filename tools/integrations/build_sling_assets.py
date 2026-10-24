"""Original PVG sling geometry, UVs and procedural webbing. No upstream assets."""
from pathlib import Path
import math, struct, zlib, argparse, json, hashlib

def png(path, size, pixel):
    def chunk(k, v):
        return struct.pack('>I',len(v))+k+v+struct.pack('>I',zlib.crc32(k+v)&0xffffffff)
    raw=b''.join(b'\0'+bytes(c for x in range(size) for c in pixel(x,y)) for y in range(size))
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',size,size,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b''))

def mesh(flat=False):
    rings=[]; n=48
    for i in range(n):
        a=2*math.pi*i/n;c,s=math.cos(a),math.sin(a)
        if flat:
            center=(.25*c,.002,.15*s); side=(0,1,0); radial=(c,0,s)
        else:
            center=(.34*c/math.sqrt(2),.145*s,.08+.34*c/math.sqrt(2))
            side=(-1/math.sqrt(2),0,1/math.sqrt(2));radial=(c/math.sqrt(2),s,c/math.sqrt(2))
        rings.append([tuple(center[k]+w*side[k]+t*radial[k] for k in range(3))
                      for w,t in ((-.0175,-.0015),(.0175,-.0015),(.0175,.0015),(-.0175,.0015))])
    faces=[]
    for i in range(n):
        for j in range(4):
            faces.append([rings[i][j],rings[(i+1)%n][j],rings[(i+1)%n][(j+1)%4],rings[i][(j+1)%4]])
    return faces

def fbx(path, name, faces):
    verts=[];indices=[];normals=[];uv=[]
    for face in faces:
        a,b,c=face[:3];u=[b[k]-a[k] for k in range(3)];v=[c[k]-a[k] for k in range(3)]
        normal=(u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]);length=math.sqrt(sum(x*x for x in normal))
        normal=tuple(x/length for x in normal)
        start=len(verts)//3
        for p in face: verts.extend(p);normals.extend(normal)
        indices.extend((start,start+1,start+2,-(start+4)))
        uv.extend((0,0,1,0,1,1,0,1))
    def arr(label,items):return f'{label}: *{len(items)} {{ a: '+','.join(str(round(x,8)) if isinstance(x,float) else str(x) for x in items)+' }'
    text='''; FBX 7.4.0 project file - original PVADSGael sling
FBXHeaderExtension: { FBXHeaderVersion: 1003 FBXVersion: 7400 }
GlobalSettings: {
 Version: 1000
 Properties70: {
  P: "UpAxis", "int", "Integer", "", 1
  P: "UpAxisSign", "int", "Integer", "", 1
  P: "FrontAxis", "int", "Integer", "", 2
  P: "FrontAxisSign", "int", "Integer", "", 1
  P: "CoordAxis", "int", "Integer", "", 0
  P: "CoordAxisSign", "int", "Integer", "", 1
  P: "UnitScaleFactor", "double", "Number", "", 100
 }
}
Objects: {
 Geometry: 1001, "Geometry::NAME", "Mesh" {
 GeometryVersion: 124
 VERTICES
 INDICES
 LayerElementNormal: 0 { Version: 101 Name: "" MappingInformationType: "ByPolygonVertex" ReferenceInformationType: "Direct" NORMALS }
 LayerElementUV: 0 { Version: 101 Name: "WebbingUV" MappingInformationType: "ByPolygonVertex" ReferenceInformationType: "Direct" UVS }
 Layer: 0 { Version: 100 LayerElement: { Type: "LayerElementNormal" TypedIndex: 0 } LayerElement: { Type: "LayerElementUV" TypedIndex: 0 } }
 }
 Model: 1002, "Model::NAME", "Mesh" { Version: 232 Properties70: {
 P: "Lcl Translation", "Lcl Translation", "", "A", 0,0,0
 P: "Lcl Rotation", "Lcl Rotation", "", "A", 0,0,0
 P: "Lcl Scaling", "Lcl Scaling", "", "A", 1,1,1
 } Shading: T Culling: "CullingOff" }
}
Connections: { C: "OO",1001,1002 C: "OO",1002,0 }
'''.replace('NAME',name).replace('VERTICES',arr('Vertices',verts)).replace('INDICES',arr('PolygonVertexIndex',indices)).replace('NORMALS',arr('Normals',normals)).replace('UVS',arr('UV',uv))
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text,encoding='utf-8')

def build(repo):
    for version in ('42','42.20'):
        media=repo/version/'media'
        fbx(media/'models_X/PVADSGael/Sling/PVGSling.fbx','PVGSling',mesh())
        fbx(media/'models_X/PVADSGael/Sling/PVGSlingFlat.fbx','PVGSlingFlat',mesh(True))
        def weave(x,y):
            edge=10 if x%32 in (0,1,30,31) else 0
            shade=(5 if (x//2+y//2)%2 else -5)+edge
            return (65+shade,76+shade,45+shade,255)
        png(media/'textures/PVADSGael/SlingWebbing.png',64,weave)
        def icon(x,y):
            r=((x-32)/23)**2+((y-32)/16)**2
            return weave(x,y) if .55<r<1 else (0,0,0,0)
        png(media/'textures/Item_PVGSling.png',64,icon)
    print('Original sling FBX/UVs and textures generated for 42 + 42.20.')

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--repo',type=Path,required=True);build(p.parse_args().repo)
