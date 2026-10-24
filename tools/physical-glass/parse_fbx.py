import struct
import sys
import zlib


def unpack(data, offset, fmt):
    size = struct.calcsize(fmt)
    return struct.unpack_from(fmt, data, offset)[0], offset + size


def prop(data, offset):
    typ = chr(data[offset])
    offset += 1
    scalar = {"Y": "<h", "C": "<?", "I": "<i", "F": "<f", "D": "<d", "L": "<q"}
    if typ in scalar:
        return unpack(data, offset, scalar[typ])
    if typ in "SR":
        count, offset = unpack(data, offset, "<I")
        raw = data[offset:offset + count]
        offset += count
        return (raw.decode("utf-8", "replace") if typ == "S" else raw), offset
    if typ in "fdi lb".replace(" ", ""):
        count, offset = unpack(data, offset, "<I")
        encoding, offset = unpack(data, offset, "<I")
        length, offset = unpack(data, offset, "<I")
        raw = data[offset:offset + length]
        offset += length
        if encoding:
            raw = zlib.decompress(raw)
        item = {"f": "f", "d": "d", "i": "i", "l": "q", "b": "b"}[typ]
        return list(struct.unpack("<" + str(count) + item, raw)), offset
    raise ValueError("unknown property type " + typ)


def nodes(data, offset, end, wide=False):
    out = []
    hdr = "<QQQB" if wide else "<IIIB"
    hsize = struct.calcsize(hdr)
    while offset + hsize <= end:
        vals = struct.unpack_from(hdr, data, offset)
        node_end, count, plen, nlen = vals
        offset += hsize
        if node_end == 0:
            break
        name = data[offset:offset + nlen].decode("utf-8", "replace")
        offset += nlen
        props = []
        for _ in range(count):
            value, offset = prop(data, offset)
            props.append(value)
        children = nodes(data, offset, node_end, wide)
        out.append((name, props, children))
        offset = node_end
    return out


def walk(items):
    for item in items:
        yield item
        yield from walk(item[2])


raw = open(sys.argv[1], "rb").read()
target = sys.argv[2] if len(sys.argv) > 2 else "PistolScope"
version = struct.unpack_from("<I", raw, 23)[0]
tree = nodes(raw, 27, len(raw), version >= 7500)
models = []
geometries = []
connections = []
for name, props, children in walk(tree):
    if name == "Model": models.append(props)
    if name == "Geometry": geometries.append((props, children))
    if name == "C" and len(props) >= 3: connections.append(props)
for props in models:
    if len(props) > 1 and target in str(props[1]): print(target + " model", props)
for props in models:
    if len(props) > 1 and target in str(props[1]):
        model_id = props[0]
        print("links", [c for c in connections if model_id in c])
        for c in connections:
            if len(c) >= 3 and c[2] == model_id:
                geometry_id = c[1]
                for gprops, children in geometries:
                    if gprops[0] == geometry_id:
                        print("linked geometry", gprops[:3])
                        for child, cprops, _ in children:
                            if child in ("Vertices", "PolygonVertexIndex"):
                                values = cprops[0]
                                print(child, "count", len(values), "range", (min(values), max(values)))
                                if child == "Vertices":
                                    triples = list(zip(values[::3], values[1::3], values[2::3]))
                                    print("xyz min", tuple(min(v[i] for v in triples) for i in range(3)))
                                    print("xyz max", tuple(max(v[i] for v in triples) for i in range(3)))
                                    from collections import defaultdict
                                    levels = defaultdict(list)
                                    for x, y, z in triples:
                                        levels[round(y, 6)].append((x, z))
                                    for level, points in sorted(levels.items(), key=lambda pair: -len(pair[1]))[:12]:
                                        print("y", level, "vertices", len(points), "xz", (min(p[0] for p in points), max(p[0] for p in points), min(p[1] for p in points), max(p[1] for p in points)))
                                    if target == "PistolScope":
                                        ring = levels[-0.065667]
                                        print("ring24", ", ".join("%.6f, %.6f" % p for p in ring[::2]))
                                        def cross(a, b, c): return (b[0]-a[0])*(c[1]-a[1]) - (b[1]-a[1])*(c[0]-a[0])
                                        ordered = sorted(set(ring))
                                        hull = []
                                        for point in ordered:
                                            while len(hull) >= 2 and cross(hull[-2], hull[-1], point) <= 0: hull.pop()
                                            hull.append(point)
                                        lower_len = len(hull)
                                        for point in reversed(ordered[:-1]):
                                            while len(hull) > lower_len and cross(hull[-2], hull[-1], point) <= 0: hull.pop()
                                            hull.append(point)
                                        hull.pop()
                                        print("hull", len(hull), ", ".join("%.6f, %.6f" % p for p in hull))

for name, props, children in walk(tree):
    if name == "Geometry" and len(props) > 1 and target in str(props[1]):
        print("Geometry:", props[:3])
        for child, cprops, _ in children:
            if child in ("Vertices", "PolygonVertexIndex"):
                values = cprops[0]
                print(child, "count", len(values), "range", (min(values), max(values)))
                if child == "Vertices":
                    triples = list(zip(values[::3], values[1::3], values[2::3]))
                    print("xyz min", tuple(min(v[i] for v in triples) for i in range(3)))
                    print("xyz max", tuple(max(v[i] for v in triples) for i in range(3)))
