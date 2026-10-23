"""Prepare the CC0 elephant recording for positional Minecraft playback."""
from pathlib import Path
import subprocess
import sys
import urllib.request

root = Path(__file__).resolve().parents[1]
source = root / 'build/elephant_trumpet_source.ogg'
source.parent.mkdir(parents=True, exist_ok=True)
if not source.exists():
    urllib.request.urlretrieve(
        'https://upload.wikimedia.org/wikipedia/commons/4/40/Elephant_voice_-_trumpeting.ogg', source)
output = root / 'src/main/resources/assets/kazimod/sounds/mammoth_roar.ogg'
subprocess.run([
    sys.argv[1], '-hide_banner', '-loglevel', 'error', '-y', '-i', str(source),
    '-ac', '1', '-ar', '48000', '-af',
    'volume=8dB,afade=t=in:d=0.015,afade=t=out:st=1.34:d=0.1',
    '-c:a', 'libvorbis', '-q:a', '5', str(output)
], check=True)
assert output.read_bytes()[:4] == b'OggS'
print(f'Prepared CC0 elephant trumpet recording: {output.stat().st_size} bytes.')
