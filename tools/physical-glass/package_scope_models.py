"""Preserve FBX properties verbatim; append editable child glass meshes."""
from pathlib import Path
import struct, sys, runpy, contextlib, io, re, hashlib, json

ROOT = Path(r'C:\Users\carlos\Downloads\GaelScopesModels')
REPO = Path(r'C:\Users\carlos\Documents\Codex\2026-10-18\fijate-en-github-la-ultima-rama\work\PVADSGAEL2-latest')
source = (ROOT / 'Scopes.fbx').read_bytes()
sys.argv = ['parse_fbx.py', str(ROOT / 'Scopes.fbx')]
with contextlib.redirect_stdout(io.StringIO()):
    parser = runpy.run_path(str(Path(__file__).with_name('parse_fbx.py')))
wide = struct.unpack_from('<I', source, 23)[0] >= 7500
fmt = '<QQQB' if wide else '<IIIB'
header_size = struct.calcsize(fmt)

def read_nodes(start, end):
    result = []
    while start + header_size <= end:
        stop, count, length, namesize = struct.unpack_from(fmt, source, start)
        if not stop:
            return result, start + header_size
        pos = start + header_size
        name = source[pos:pos + namesize]
        pos += namesize
        raw = source[pos:pos + length]
        children, _ = read_nodes(pos + length, stop)
        result.append([name, count, raw, children])
        start = stop
    return result, start

def values(node):
    out, pos = [], 0
    for _ in range(node[1]):
        value, pos = parser['prop'](node[2], pos)
        out.append(value)
    return out

def prop(value):
    if isinstance(value, bool):
        return b'C'+struct.pack('<?',value)
    if isinstance(value, str):
        raw = value.encode('utf8')
        return b'S' + struct.pack('<I', len(raw)) + raw
    if isinstance(value, int):
        return b'I' + struct.pack('<i', value)
    kind, items = value
    if kind == 'L':
        return b'L'+struct.pack('<q',items)
    raw = struct.pack('<' + str(len(items)) + kind, *items)
    return kind.encode() + struct.pack('<III', len(items), 0, len(raw)) + raw

def node(name, props=(), children=None):
    return [name.encode(), len(props), b''.join(prop(p) for p in props), children or []]

def encode(n, offset):
    name, count, raw, children = n
    pos = offset + header_size + len(name) + len(raw)
    body = bytearray()
    for child in children:
        encoded = encode(child, pos)
        body.extend(encoded)
        pos += len(encoded)
    if children:
        body.extend(bytes(header_size))
        pos += header_size
    return struct.pack(fmt, pos, count, len(raw), len(name)) + name + raw + body

tree, footer_start = read_nodes(27, len(source))
objects = next(n for n in tree if n[0] == b'Objects')
connections = next(n for n in tree if n[0] == b'Connections')
original_objects = list(objects[3])
ids = [values(n)[0] for n in original_objects if n[1] and isinstance(values(n)[0], int)]
next_id = max(ids) + 100
manifest = []
for original in original_objects:
    if original[0] != b'Model':
        continue
    props = values(original)
    name = props[1].split('\0')[0]
    obj = ROOT / 'Con_Vidrio_Referencia' / (name + '.obj')
    if not obj.exists():
        continue
    points, active = [], False
    for line in obj.read_text().splitlines():
        if line.startswith('o '):
            active = line.endswith('_Vidrio_Referencia')
        elif active and line.startswith('v '):
            points.append(tuple(map(float, line.split()[1:])))
    if len(points) != 24:
        raise ValueError(f'Unexpected reference: {obj}')
    gid, mid = next_id, next_id + 1
    next_id += 2
    faces = []
    for i in range(1, len(points)-1):
        faces.extend([0, i, -(i+1)-1])
    cx = sum(p[0] for p in points)/len(points)
    cz = sum(p[2] for p in points)/len(points)
    rx = max(abs(p[0]-cx) for p in points)
    rz = max(abs(p[2]-cz) for p in points)
    uv = [v for p in points for v in ((p[0]-cx)/(2*rx)+0.5, (p[2]-cz)/(2*rz)+0.5)]
    uv_indices = [i if i >= 0 else -i-1 for i in faces]
    # Separate geometry: never bake an opaque disk into the housing mesh.
    objects[3].append(node('Geometry', [('L',gid), name+'_Vidrio_Referencia\0\1Geometry', 'Mesh'], [
        node('GeometryVersion', [124]),
        node('Vertices', [('d', [v for p in points for v in p])]),
        node('PolygonVertexIndex', [('i', faces)]),
        node('LayerElementNormal', [0], [node('Version',[101]),node('Name',['']),
            node('MappingInformationType',['ByPolygonVertex']),node('ReferenceInformationType',['Direct']),
            node('Normals',[('d',[v for _ in faces for v in (0.0,-1.0,0.0)])])]),
        node('LayerElementUV', [0], [node('Version',[101]),node('Name',['GlassUV']),
            node('MappingInformationType',['ByPolygonVertex']),node('ReferenceInformationType',['IndexToDirect']),
            node('UV',[('d',uv)]),node('UVIndex',[('i',uv_indices)])]),
        node('Layer', [0], [node('Version',[100]),
            node('LayerElement',[],[node('Type',['LayerElementNormal']),node('TypedIndex',[0])]),
            node('LayerElement',[],[node('Type',['LayerElementUV']),node('TypedIndex',[0])])]),
    ]))
    objects[3].append(node('Model', [('L',mid), name+'_Vidrio_Referencia\0\1Model', 'Mesh'], [
        node('Version', [232]), node('Properties70'), node('Shading', [True]), node('Culling', ['CullingOff'])
    ]))
    connections[3].extend([node('C', ['OO', ('L',gid), ('L',mid)]), node('C', ['OO', ('L',mid), ('L',props[0])])])
    manifest.append({'optic':name, 'reference':name+'_Vidrio_Referencia', 'vertices':points,
                     'calibration':'preliminary', 'parentModelId':props[0]})

# Update informational object counts to match the appended objects.
definitions = next(n for n in tree if n[0] == b'Definitions')
for child in definitions[3]:
    if child[0] == b'Count':
        replacement = node('Count', [values(child)[0] + len(manifest)*2])
        child[:] = replacement
    if child[0] == b'ObjectType' and values(child)[0] in ('Model','Geometry'):
        for count in child[3]:
            if count[0] == b'Count':
                count[:] = node('Count', [values(count)[0] + len(manifest)])

output = bytearray(source[:27])
for n in tree:
    output.extend(encode(n, len(output)))
output.extend(bytes(header_size))
output.extend(source[footer_start:])
# Parse generated file structurally, checking every original object property byte.
parsed = parser['nodes'](output, 27, len(output), wide)
new_objects = next(ch for name,p,ch in parsed if name=='Objects')
assert len(new_objects) == len(original_objects) + 2*len(manifest)
assert len(manifest) == 55
pack = ROOT / 'PVADSGAEL_Integrados'
pack.mkdir(exist_ok=True)
(pack/'GaelScopes.fbx').write_bytes(output)
(pack/'glass-manifest.json').write_text(json.dumps(manifest, indent=2), encoding='utf8')
(pack/'gael-glass.csv').write_text('\n'.join(m['optic']+','+','.join(str(v) for p in m['vertices'] for v in p) for m in manifest)+'\n', encoding='utf8')
blocks = []
for script in (ROOT/'scripts').glob('*.txt'):
    for match in re.finditer(r'\bmodel\s+(\w+)\s*\{([^{}]*)\}', script.read_text(encoding='utf-8-sig'), re.S):
        if 'Scope/Scopes|' in match[2]:
            blocks.append('    model '+match[1]+'\n    {'+match[2].replace('Scope/Scopes|','PVADSGael/GaelScopes|')+'    }')
overrides = '// PVADSGAEL-owned geometry; original item IDs and texture paths preserved.\nmodule Base\n{\n'+'\n'.join(blocks)+'\n}\n'
for version in ('42','42.20'):
    media = REPO/version/'media'
    target = media/'models_X'/'PVADSGael'
    target.mkdir(parents=True, exist_ok=True)
    (target/'GaelScopes.fbx').write_bytes(output)
    (media/'scripts'/'ZZ_PVADSGael_ScopeModels.txt').write_text(overrides, encoding='utf8')
    references = media/'lua/client/ZZ_PVADS_GaelGlassReferences.lua'
    if references.exists():
        references.write_text(references.read_text(encoding='utf8').replace('Scope/Scopes|','PVADSGael/GaelScopes|'), encoding='utf8')
(pack/'LEEME.txt').write_text('55 miras originales con mallas hijas *_Vidrio_Referencia, UV GlassUV y normales.\nLos discos tienen ubicaciones preliminares: requieren calibracion visual.\nSe preservan UV, normales, materiales y transformaciones de las miras originales.\nLas texturas se resuelven desde Gael (dependencia requerida).\nImportar GaelScopes.fbx en Blender; editar solo las referencias.\nPhysicalGlass Java usa los mismos vertices exportados en gael-glass.csv dentro del JAR.\nEditar Blender no actualiza el JAR automaticamente: hay que reexportar las coordenadas.\nEn el calibrador: Anchor XYZ, Glass crop size y Glass pitch/yaw/roll ajustan la superficie local.\nValores base: posicion y angulos 0, escala 1. El zoom es independiente.\nVerificado con importador nativo Assimp y pruebas GPU; la integracion requiere prueba en PZ.\n', encoding='utf8')
print(json.dumps({'references':len(manifest),'modelOverrides':len(blocks),'bytes':len(output),'sha256':hashlib.sha256(output).hexdigest(),'download':str(pack)},indent=2))
