"""Import Chaos Beam's original recordings, preserving pitch and timing.

The native obj_afinal_body repeats spellcast six times during charging. obj_lastbeam
plays rainbowbeam_1 once and loops rainbowbeam_hold, then raises its pitch twice.
Only mono conversion and encoding headroom are applied to these source recordings.
"""
from array import array
import hashlib
import json
import math
from pathlib import Path
import subprocess
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
WORK = ROOT / 'build/chaos-beam-audio'
CACHE = ROOT / 'build/asriel-audio'
OUT = ROOT / 'src/main/resources/assets/kazimod/sounds/asriel'
FFMPEG = CACHE / 'deps/imageio_ffmpeg/binaries/ffmpeg-win-x86_64-v7.1.exe'
BASE = 'https://raw.githubusercontent.com/AspectFX/Undertale-Source-Code/master/'
ASSETS = [
    ('mus_sfx_spellcast.ogg', 'chaos_beam_charge.ogg', 'obj_afinal_body'),
    ('mus_sfx_rainbowbeam_1.ogg', 'chaos_beam_launch.ogg', 'obj_lastbeam'),
    ('mus_sfx_rainbowbeam_hold.ogg', 'chaos_beam_hold.ogg', 'obj_lastbeam'),
]

def ffmpeg(*args):
    return subprocess.run([str(FFMPEG), '-hide_banner', '-loglevel', 'error', '-y', *map(str,args)],
                          check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE).stdout

def decode(path):
    samples = array('f')
    samples.frombytes(ffmpeg('-i', path, '-ac', '1', '-ar', '44100', '-f', 'f32le', '-'))
    return samples

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    WORK.mkdir(parents=True, exist_ok=True)
    records=[]
    for source, name, reference in ASSETS:
        original = CACHE / source if (CACHE / source).exists() else WORK / source
        if not original.exists():
            original.write_bytes(urllib.request.urlopen(BASE+'sound/audio/'+source,timeout=30).read())
        before=decode(original)
        gain=min(1.0,.86/max(map(abs,before)))
        output=OUT/name
        ffmpeg('-i',original,'-ac','1','-ar','44100','-af',f'volume={gain:.9f}',
               '-map_metadata','-1','-c:a','libvorbis','-q:a','8',output)
        after=decode(output)
        count=min(len(before),len(after))
        dot=sum(before[i]*after[i] for i in range(count))
        correlation=dot/math.sqrt(sum(v*v for v in before[:count])*sum(v*v for v in after[:count]))
        peak=max(map(abs,after))
        assert output.read_bytes().startswith(b'OggS')
        assert abs(len(before)-len(after))<=1024, 'Duration changed: '+name
        assert peak<.98, 'Encoding clipped: '+name
        assert correlation>.99, 'Source timbre changed: '+name
        records.append({'file':name,'source':BASE+'sound/audio/'+source,
                        'source_sha256':sha(original),'output_sha256':sha(output),
                        'playback_reference':BASE+'objects/'+reference+'.object.gmx',
                        'conversion':f'Mono Vorbis 44100 Hz quality 8; gain {gain:.9f} for encoding headroom; full original recording, no pitch/time processing.',
                        'duration_seconds':len(after)/44100,'peak_dbfs':20*math.log10(peak),'source_correlation':correlation})
        print(f'{name}: {len(after)/44100:.3f}s, {20*math.log10(peak):.2f} dBFS, correlation {correlation:.6f}')
    metadata=OUT/'sources.json'
    data=json.loads(metadata.read_text(encoding='utf-8'))
    names={r['file'] for r in records}
    data['assets']=[r for r in data['assets'] if r['file'] not in names]+records
    metadata.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
    (WORK/'validation.json').write_text(json.dumps(records,indent=2)+'\n',encoding='utf-8')

if __name__=='__main__': main()
