"""Bake the supplied Collada skull and its authored expressions into the game mesh.

Only the HYPER_GONER geometry is imported, never the scene camera, lights or cubes.
Uses Python's standard library; the game does not need Blender or a Collada parser.
"""
import argparse
from collections import defaultdict
import gzip
import hashlib
import json
import math
from pathlib import Path
import struct
import xml.etree.ElementTree as ET

NS = {'c': 'http://www.collada.org/2005/11/COLLADASchema'}
POSES = ('srsly', 'LUAGH')


def source(mesh, ref):
    element = mesh.find("c:source[@id='%s']" % ref.lstrip('#'), NS)
    accessor = element.find('c:technique_common/c:accessor', NS)
    stride = int(accessor.get('stride', '1'))
    values = list(map(float, element.find('c:float_array', NS).text.split()))
    return [values[i:i + stride] for i in range(0, len(values), stride)]


def geometry(root, name):
    mesh = root.find("c:library_geometries/c:geometry[@id='%s']/c:mesh" % name, NS)
    positions = source(mesh, mesh.find('c:vertices/c:input', NS).get('source'))
    corners = []
    for poly in mesh.findall('c:polylist', NS):
        inputs = {i.get('semantic'): i for i in poly.findall('c:input', NS)}
        width = max(int(i.get('offset')) for i in inputs.values()) + 1
        normals = source(mesh, inputs['NORMAL'].get('source'))
        pos_offset, norm_offset = [int(inputs[key].get('offset')) for key in ('VERTEX', 'NORMAL')]
        counts = list(map(int, poly.find('c:vcount', NS).text.split()))
        if any(n != 3 for n in counts):
            raise ValueError('Expected the original triangulated model')
        indices = list(map(int, poly.find('c:p', NS).text.split()))
        material = poly.get('material')
        for i in range(0, len(indices), width):
            corners.append((indices[i + pos_offset], normals[indices[i + norm_offset]], material))
    return positions, corners


def corrected_normals(positions, corners):
    """Repair the source's mirrored/inconsistent shading without changing its mesh.

    The export has inward-facing patches even within each horn. Follow manifold
    edge adjacency to orient normals; leave the authored triangle order intact.
    Normal splits and material boundaries remain separate when smoothing.
    """
    def subtract(a, b):
        return tuple(x - y for x, y in zip(a, b))

    def dot(a, b):
        return sum(x * y for x, y in zip(a, b))

    def cross(a, b):
        return (a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0])

    def length(a):
        return math.sqrt(dot(a, a))

    def normalized(a):
        magnitude = length(a)
        return tuple(x / magnitude for x in a) if magnitude > 1e-12 else (0., 0., 0.)

    triangles = [tuple(corners[i + j][0] for j in range(3))
                 for i in range(0, len(corners), 3)]
    edges = defaultdict(list)
    face_crosses = []
    for face, triangle in enumerate(triangles):
        a, b, c = [positions[index] for index in triangle]
        face_crosses.append(cross(subtract(b, a), subtract(c, a)))
        for a, b in zip(triangle, (triangle[1], triangle[2], triangle[0])):
            edges[tuple(sorted((a, b)))].append((face, 1 if a < b else -1))

    adjacent = defaultdict(list)
    for faces in edges.values():
        if len(faces) != 2:
            # Nonmanifold junctions have no unique opposite face.
            continue
        (a, direction_a), (b, direction_b) = faces
        relation = -direction_a * direction_b
        adjacent[a].append((b, relation))
        adjacent[b].append((a, relation))

    signs = [0] * len(triangles)
    islands = []
    for seed in range(len(triangles)):
        if signs[seed]:
            continue
        signs[seed] = 1
        pending, island = [seed], []
        while pending:
            face = pending.pop()
            island.append(face)
            for neighbor, relation in adjacent[face]:
                expected = signs[face] * relation
                if not signs[neighbor]:
                    signs[neighbor] = expected
                    pending.append(neighbor)
                elif signs[neighbor] != expected:
                    raise ValueError('Non-orientable source surface')
        islands.append(island)

    mesh_center = tuple((min(p[axis] for p in positions) + max(p[axis] for p in positions)) / 2
                        for axis in range(3))
    for island in islands:
        used = {index for face in island for index in triangles[face]}
        center = tuple(sum(positions[index][axis] for index in used) / len(used)
                       for axis in range(3))
        # Signed volume about the island's own center handles mirrored closed
        # pieces and curved open pieces without depending on scene translation.
        volume = sum(signs[face] * dot(subtract(positions[triangles[face][0]], center),
                                       face_crosses[face]) for face in island)
        extent = max(max(positions[index][axis] for index in used)
                     - min(positions[index][axis] for index in used) for axis in range(3))
        if abs(volume) <= max(1., extent ** 3) * 1e-9:
            # Planar patches have no signed volume. Face their exposed side away
            # from the skull's center; black cavity surfaces keep their material.
            volume = sum(signs[face] * dot(subtract(positions[triangles[face][0]], mesh_center),
                                           face_crosses[face]) for face in island)
        if volume < 0:
            for face in island:
                signs[face] = -signs[face]

    sums = defaultdict(lambda: [0., 0., 0.])
    keys, face_normals = [], []
    for face, triangle in enumerate(triangles):
        face_normal = tuple(signs[face] * value for value in normalized(face_crosses[face]))
        face_normals.append(face_normal)
        for corner in range(3):
            index, authored_normal, material = corners[face * 3 + corner]
            # The normal is per corner in Collada: distinct values on the same
            # position identify authored hard seams. Tiny export noise is not a seam.
            key = (index, material, tuple(round(value, 5) for value in authored_normal))
            keys.append(key)
            p = positions[index]
            a = subtract(positions[triangle[(corner + 1) % 3]], p)
            b = subtract(positions[triangle[(corner + 2) % 3]], p)
            angle = math.atan2(length(cross(a, b)), dot(a, b))
            for axis in range(3):
                sums[key][axis] += face_normal[axis] * angle

    repaired = []
    for corner, (index, authored_normal, material) in enumerate(corners):
        normal = normalized(sums[keys[corner]])
        if normal == (0., 0., 0.):
            normal = face_normals[corner // 3]
        if normal == (0., 0., 0.):
            # The laughing expression collapses a few black cavity triangles.
            # Their zero area has no geometric normal; retain the exported one.
            normal = normalized(authored_normal)
        repaired.append((index, normal, material))
    return (positions, repaired), {'islands': len(islands),
                                  'reoriented_faces': sum(sign < 0 for sign in signs),
                                  'normal_groups': len(sums)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('source', type=Path)
    parser.add_argument('--output', type=Path, default=Path('src/main/resources/assets/kazimod/models/cinematics/hyper_goner.hgm.gz'))
    parser.add_argument('--manifest', type=Path, default=Path('art/hyper_goner/import_manifest.json'))
    args = parser.parse_args()
    root = ET.parse(args.source).getroot()
    corrected = [corrected_normals(*geometry(root, 'HYPER_GONER-mesh_morph_' + name)) for name in POSES]
    poses = [pose for pose, _ in corrected]
    if len(poses[0][1]) != len(poses[1][1]):
        raise ValueError('Morph topology differs')
    # Preserve authored proportions. Blender Z is up and the face points toward -Y.
    all_positions = [v for positions, _ in poses for v in positions]
    lo = [min(v[a] for v in all_positions) for a in range(3)]
    hi = [max(v[a] for v in all_positions) for a in range(3)]
    scale = 8.1 / (hi[2] - lo[2])
    center_x = (hi[0] + lo[0]) / 2
    center_up = (hi[2] + lo[2]) / 2
    forward_offset = 1.7 + lo[1] * scale
    materials = {}
    for material in root.findall('c:library_materials/c:material', NS):
        effect = material.find('c:instance_effect', NS).get('url')[1:]
        color = root.find("c:library_effects/c:effect[@id='%s']/c:profile_COMMON/c:technique/c:phong/c:diffuse/c:color" % effect, NS)
        materials[material.get('id')] = list(map(float, color.text.split()))[:3]

    def vertex(pose, corner):
        positions, corners = poses[pose]
        index, normal, material = corners[corner]
        x, y, z = positions[index]
        nx, ny, nz = normal[0], normal[2], -normal[1]
        length = math.sqrt(nx * nx + ny * ny + nz * nz) or 1
        light = .36 + .64 * max(0, (-nx * .35 + ny * .55 + nz * .76) / length)
        rgb = [round(min(255, max(0, c * 245 * light))) for c in materials[material]]
        # Keep cavity surfaces dark, but avoid an artificial full-scene black plane.
        color = (rgb[0] << 16) | (rgb[1] << 8) | rgb[2]
        return ((x - center_x) * scale, (z - center_up) * scale + .45, -y * scale + forward_offset, color)

    vertices, indices, lookup = [], [], {}
    for corner in range(len(poses[0][1])):
        if poses[0][1][corner][0] != poses[1][1][corner][0] or poses[0][1][corner][2] != poses[1][1][corner][2]:
            raise ValueError('Morph correspondence or material changed')
        # Deduplicate the pair of authored positions/shading, including sharp edges.
        pair = tuple(vertex(pose, corner) for pose in range(2))
        packed = b''.join(struct.pack('>fffI', *v) for v in pair)
        if packed not in lookup:
            lookup[packed] = len(vertices)
            vertices.append(packed)
        indices.append(lookup[packed])
    data = struct.pack('>4sII', b'HGM1', len(vertices), len(indices)) + b''.join(vertices)
    data += struct.pack('>%dI' % len(indices), *indices)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_bytes(gzip.compress(data, mtime=0))
    report = {'source': args.source.name, 'source_sha256': hashlib.sha256(args.source.read_bytes()).hexdigest(),
              'poses': POSES, 'triangles': len(indices) // 3, 'vertices': len(vertices),
              'bytes': args.output.stat().st_size, 'uniform_scale': scale,
              'normal_correction': {name: stats for name, (_, stats) in zip(POSES, corrected)},
              'bounds': [[min(vertex(p, c)[a] for p in range(2) for c in range(len(indices))) for a in range(3)],
                         [max(vertex(p, c)[a] for p in range(2) for c in range(len(indices))) for a in range(3)]]}
    args.manifest.parent.mkdir(parents=True, exist_ok=True)
    args.manifest.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
