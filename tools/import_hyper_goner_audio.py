"""Import the original Hyper Goner recordings with encoding headroom, preserving speed/pitch.

Cached source recordings and provenance are retained. This only converts to Minecraft-compatible
mono Vorbis; the cutscene performs its own short ending fade and lifecycle cleanup.
"""
from array import array
from pathlib import Path
import hashlib
import json
import math
import subprocess
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
WORK = ROOT / 'build/hyper-goner-audio'
OUT = ROOT / 'src/main/resources/assets/kazimod/sounds/asriel'
FFMPEG = ROOT / 'build/asriel-audio/deps/imageio_ffmpeg/binaries/ffmpeg-win-x86_64-v7.1.exe'
BASE = 'https://raw.githubusercontent.com/AspectFX/Undertale-Source-Code/master/'
SOURCES = [
    ('mus_sfx_hypergoner_laugh.ogg', 'hyper_goner_laugh.ogg', .82),
    ('mus_sfx_hypergoner_charge.ogg', 'hyper_goner_charge.ogg', .68),
    ('mus_create.ogg', 'hyper_goner_return.ogg', .82),
]

def ffmpeg(*args):
    return subprocess.run([str(FFMPEG), '-hide_banner', '-loglevel', 'error', '-y', *map(str,args)],
                          stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True).stdout

def decode(path):
    samples=array('f')
    samples.frombytes(ffmpeg('-i',path,'-ac','1','-ar','44100','-f','f32le','-'))
    return samples

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    WORK.mkdir(parents=True,exist_ok=True)
    records=[]
    for source,name,gain in SOURCES:
        original=WORK/source
        if not original.exists():
            original.write_bytes(urllib.request.urlopen(BASE+'sound/audio/'+source,timeout=30).read())
        dest=OUT/name
        ffmpeg('-i',original,'-ac','1','-ar','44100','-af',f'volume={gain}',
               '-map_metadata','-1','-c:a','libvorbis','-q:a','8',dest)
        before,after=decode(original),decode(dest)
        peak=max(map(abs,after))
        assert dest.read_bytes().startswith(b'OggS'), name+' is not Ogg'
        assert abs(len(after)-len(before))<=1024, name+' changed duration'
        assert peak<.98, name+' clips after encoding'
        n=min(len(before),len(after))
        dot=sum(before[i]*after[i] for i in range(n))
        correlation=dot/math.sqrt(sum(v*v for v in before[:n])*sum(v*v for v in after[:n]))
        assert correlation>.99, name+' changed the source timbre or timing'
        reference='obj_asriel_body' if source=='mus_create.ogg' else 'obj_hg_body'
        records.append({'file':name,'source':BASE+'sound/audio/'+source,'source_sha256':digest(original),
                        'output_sha256':digest(dest),'conversion':f'Mono Vorbis 44100 Hz quality 8; gain {gain} for encoding headroom; full recording, original speed and pitch.',
                        'playback_reference':BASE+'objects/'+reference+'.object.gmx',
                        'duration_seconds':len(after)/44100,'peak_dbfs':20*math.log10(peak),
                        'source_correlation':correlation})
        print(f'{name}: {len(after)/44100:.3f}s, peak {20*math.log10(peak):.2f} dBFS, source correlation {correlation:.6f}')
    metadata=OUT/'sources.json'
    data=json.loads(metadata.read_text())
    names={r['file'] for r in records}
    data['assets']=[r for r in data['assets'] if r['file'] not in names]+records
    metadata.write_text(json.dumps(data,indent=2)+'\n')
    (WORK/'validation.json').write_text(json.dumps(records,indent=2)+'\n')

if __name__=='__main__': main()

