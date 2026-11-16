"""Import the original obj_FinalWriter voice without pitch or timing processing.

The public mirror's .ogg file actually contains stereo PCM WAV. Minecraft needs
real Vorbis, so retain the whole recording and convert its container/encoding.
Playback reproduces the original stop/restart, volume 0.9, pitch 1.15 in Java.
"""
from array import array
import hashlib
import json
import math
from pathlib import Path
import subprocess
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / 'build/asriel-audio'
WORK = ROOT / 'output/validation/asriel-text-audio'
OUTPUT = ROOT / 'src/main/resources/assets/kazimod/sounds/asriel/asriel_text.ogg'
FFMPEG = CACHE / 'deps/imageio_ffmpeg/binaries/ffmpeg-win-x86_64-v7.1.exe'
SOURCE = 'https://raw.githubusercontent.com/AspectFX/Undertale-Source-Code/master/sound/audio/mus_sfx_a_gigatalk.ogg'
REFERENCE = 'https://github.com/AspectFX/Undertale-Source-Code/blob/master/objects/obj_FinalWriter.object.gmx'


def ffmpeg(*args):
    return subprocess.run([str(FFMPEG), '-hide_banner', '-loglevel', 'error', '-y', *map(str, args)],
                          check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE).stdout


def decode(path):
    samples = array('f')
    samples.frombytes(ffmpeg('-i', path, '-ac', '1', '-ar', '44100', '-f', 'f32le', '-'))
    return samples


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    CACHE.mkdir(parents=True, exist_ok=True)
    WORK.mkdir(parents=True, exist_ok=True)
    source = CACHE / 'mus_sfx_a_gigatalk.ogg'
    if not source.exists():
        source.write_bytes(urllib.request.urlopen(SOURCE, timeout=30).read())
    before = decode(source)
    gain = min(1.0, .86 / max(map(abs, before)))
    ffmpeg('-i', source, '-ac', '1', '-ar', '44100', '-af', f'volume={gain:.9f}',
           '-map_metadata', '-1', '-c:a', 'libvorbis', '-q:a', '8', OUTPUT)
    after = decode(OUTPUT)
    count = min(len(before), len(after))
    dot = sum(before[i] * after[i] for i in range(count))
    correlation = dot / math.sqrt(sum(v*v for v in before[:count]) * sum(v*v for v in after[:count]))
    peak = max(map(abs, after))
    assert OUTPUT.read_bytes().startswith(b'OggS'), 'Minecraft asset must contain real Ogg Vorbis'
    assert abs(len(before) - len(after)) <= 1024, 'Original recording duration changed'
    assert correlation > .99, 'Original voice timbre changed'
    assert peak < .98, 'Encoded voice clips'
    record = {
        'file': OUTPUT.name,
        'source': SOURCE,
        'source_sha256': sha(source),
        'output_sha256': sha(OUTPUT),
        'playback_reference': REFERENCE,
        'conversion': f'Source .ogg is stereo PCM WAV; converted to mono Vorbis 44100 Hz quality 8, gain {gain:.9f}. Full recording, no pitch or time processing.',
        'original_playback': 'Stop previous voice, then replay per non-space letter at volume 0.9 and pitch 1.15.',
        'duration_seconds': len(after) / 44100,
        'peak_dbfs': 20 * math.log10(peak),
        'source_correlation': correlation
    }
    manifest = OUTPUT.parent / 'sources.json'
    metadata = json.loads(manifest.read_text(encoding='utf-8'))
    metadata['assets'] = [item for item in metadata['assets'] if item['file'] != OUTPUT.name] + [record]
    manifest.write_text(json.dumps(metadata, indent=2) + '\n', encoding='utf-8')
    (WORK / 'validation.json').write_text(json.dumps(record, indent=2) + '\n', encoding='utf-8')
    print(f'Asriel text: {len(after)/44100:.6f}s, peak {20*math.log10(peak):.2f}dBFS, correlation {correlation:.8f}, gain {gain:.6f}')


if __name__ == '__main__':
    main()
