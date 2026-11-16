"""Re-master the local Shocker strike without changing its timing or runtime gain.

Uses only the cached original, Python's standard library and the bundled FFmpeg.
Backups and same-gain previews are kept under deliverables/asriel_audio.
"""
from array import array
from pathlib import Path
import hashlib
import json
import math
import shutil
import subprocess
import wave

ROOT = Path(__file__).resolve().parents[1]
FFMPEG = ROOT / "build/asriel-audio/deps/imageio_ffmpeg/binaries/ffmpeg-win-x86_64-v7.1.exe"
SOURCE = ROOT / "output/validation/shocker-audio/mus_sfx_a_lithit.ogg"
ASSETS = ROOT / "src/main/resources/assets/kazimod/sounds/asriel"
OUTPUT = ROOT / "deliverables/asriel_audio"
WORK = ROOT / "build/shocker-audio-master"
RATE = 44100
PITCH = 0.6


def ffmpeg(args, data=None):
    return subprocess.run([str(FFMPEG), "-hide_banner", "-loglevel", "error", "-y", *args],
                          input=data, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True).stdout


def decode(path, filters="", rate=RATE):
    args = ["-i", str(path)]
    if filters:
        args += ["-af", filters]
    raw = ffmpeg([*args, "-ar", str(rate), "-f", "f32le", "-"])
    result = array("f")
    result.frombytes(raw)
    return result


def rms(samples):
    return math.sqrt(sum(value * value for value in samples) / max(1, len(samples)))


def db(value):
    return 20 * math.log10(max(value, 1e-15))


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def wav(path, samples):
    pcm = array("h", (round(max(-1, min(1, value)) * 32767) for value in samples))
    with wave.open(str(path), "wb") as file:
        file.setnchannels(1)
        file.setsampwidth(2)
        file.setframerate(RATE)
        file.writeframes(pcm.tobytes())


def encode(samples, path):
    ffmpeg(["-f", "f32le", "-ar", str(RATE), "-ac", "1", "-i", "-", "-map_metadata", "-1",
            "-c:a", "libvorbis", "-q:a", "5", str(path)], samples.tobytes())


def metrics(path):
    original = decode(path)
    playback = decode(path, "asetrate=26460,aresample=44100")
    oversampled = decode(path, "asetrate=26460,aresample=176400", RATE * 4)
    bright = decode(path, "asetrate=26460,aresample=44100,highpass=f=1800,lowpass=f=12000")
    return {"sha256": digest(path), "channels": 1, "sample_rate": RATE,
            "source_frames": len(original), "source_seconds": len(original) / RATE,
            "source_rms_dbfs": db(rms(original)),
            "decoded_peak_dbfs": db(max(map(abs, original))),
            "playback_pitch": PITCH, "playback_seconds": len(playback) / RATE,
            "playback_rms_dbfs": db(rms(playback)),
            "playback_peak_dbfs": db(max(map(abs, playback))),
            "playback_4x_peak_dbfs": db(max(map(abs, oversampled))),
            "playback_high_band_rms_dbfs": db(rms(bright)),
            "playback_attack_100ms_rms_dbfs": db(rms(playback[:RATE // 10])),
            "playback_final_30ms_peak_dbfs": db(max(map(abs, playback[-RATE * 3 // 100:])))}


def master_limit(value, ceiling):
    knee = ceiling * .72
    magnitude = abs(value)
    if magnitude <= knee:
        return value
    limited = knee + (ceiling - knee) * math.tanh((magnitude - knee) / (ceiling - knee))
    return math.copysign(limited, value)


def main():
    if not SOURCE.is_file() or not FFMPEG.is_file():
        raise FileNotFoundError("The cached original strike and bundled FFmpeg must already exist.")
    OUTPUT.mkdir(parents=True, exist_ok=True)
    WORK.mkdir(parents=True, exist_ok=True)
    before = OUTPUT / "shocker_strike_before.ogg"
    before_alt = OUTPUT / "shocker_strike_alt_before.ogg"
    primary = ASSETS / "shocker_breaker_strike.ogg"
    alternate = ASSETS / "shocker_breaker_strike_alt.ogg"
    for source, backup in [(primary, before), (alternate, before_alt)]:
        if not backup.exists():
            shutil.copy2(source, backup)
    baseline = metrics(before)
    # Average explicitly: FFmpeg's automatic stereo downmix uses a different gain law.
    mono = "pan=mono|c0=0.5*c0+0.5*c1"
    dry = decode(SOURCE, mono)
    # The recognizable original is retained. Each extra layer is derived from that same recording.
    body_filter = (mono + ",highpass=f=110,lowpass=f=11000,"
                   "equalizer=f=1700:t=q:w=0.8:g=3.4,equalizer=f=4200:t=q:w=0.9:g=4.6,"
                   "asoftclip=type=tanh:threshold=0.72:output=1:oversample=4")
    crack_filter = (mono + ",highpass=f=2400,lowpass=f=12500,"
                    "asoftclip=type=tanh:threshold=0.20:output=1:oversample=4")
    arc_filter = (mono + ",highpass=f=650,lowpass=f=6500,"
                  "asoftclip=type=atan:threshold=0.30:output=1:oversample=4")
    body, crack, arc = [decode(SOURCE, recipe) for recipe in (body_filter, crack_filter, arc_filter)]
    length = len(dry)
    assert all(len(layer) == length for layer in (body, crack, arc)), "A filter changed strike duration."
    mixed = array("f")
    for i in range(length):
        t = i / RATE
        # Fast serrated attack, then gently modulated electrical mids; no delayed echoes or bass boom.
        transient = math.exp(-t / .050)
        electricity = (.72 + .28 * math.sin(t * math.tau * 83) ** 2) * (.55 + .45 * math.exp(-t / .4))
        value = .34 * dry[i] + .66 * body[i] + .68 * transient * crack[i] + .30 * electricity * arc[i]
        # Only smooth the last 45ms and first 0.35ms to avoid abrupt edges.
        ending = min(1.0, (length - 1 - i) / (RATE * .045))
        ending = ending * ending * (3 - 2 * ending)
        starting = min(1.0, i / (RATE * .00035))
        mixed.append(value * ending * starting)
    # Controlled peak shaping permits the old RMS with safer codec/inter-sample headroom.
    target = 10 ** (baseline["playback_rms_dbfs"] / 20)
    candidate = WORK / "shocker_strike_candidate.ogg"
    gain, ceiling = target / rms(mixed), .83
    for attempt in range(6):
        mastered = array("f", (master_limit(v * gain, ceiling) for v in mixed))
        encode(mastered, candidate)
        after = metrics(candidate)
        delta = after["playback_rms_dbfs"] - baseline["playback_rms_dbfs"]
        peak = max(after["decoded_peak_dbfs"], after["playback_peak_dbfs"], after["playback_4x_peak_dbfs"])
        if abs(delta) <= .12 and peak <= -.65:
            break
        gain *= 10 ** (-delta / 20)
        if peak > -.65:
            ceiling *= 10 ** ((-.80 - peak) / 20)
    assert abs(delta) <= .5, ("RMS mismatch", delta)
    assert peak <= -.5, ("Insufficient decoded or oversampled headroom", peak)
    assert after["source_frames"] == baseline["source_frames"], "The strike duration changed."
    # Both original alternates are identical. One master keeps their timbre consistent during overlap.
    shutil.copyfile(candidate, primary)
    shutil.copyfile(candidate, alternate)
    shutil.copyfile(candidate, OUTPUT / "shocker_strike_after.ogg")
    before_playback = decode(before, "asetrate=26460,aresample=44100")
    after_playback = decode(candidate, "asetrate=26460,aresample=44100")
    wav(OUTPUT / "shocker_before_at_game_pitch.wav", before_playback)
    wav(OUTPUT / "shocker_after_at_game_pitch.wav", after_playback)
    comparison = array("f", before_playback)
    comparison.extend([0.0] * (RATE * 3 // 4))
    comparison.extend(after_playback)
    wav(OUTPUT / "shocker_before_then_after.wav", comparison)
    report = {"source": str(SOURCE.relative_to(ROOT)), "source_sha256": digest(SOURCE),
              "baseline": baseline, "processed": after, "rms_delta_db": delta,
              "master_gain": gain, "master_soft_ceiling": ceiling,
              "layers": {"body": body_filter, "crack": crack_filter, "electrical": arc_filter},
              "processing": "Original averaged to mono; parallel dry/mid-forward saturated body, 50ms crack transient and 166Hz-modulated electrical midrange (99.6Hz at game pitch); 4x oversampled saturation; 45ms smooth tail; soft-knee mastering matched at runtime pitch 0.6; mono 44100 Hz Vorbis quality 5.",
              "preview": "Same playback gain; actual0.6 pitch; before, 750ms silence, after.",
              "primary_sha256": digest(primary), "alternate_sha256": digest(alternate)}
    (OUTPUT / "shocker_audio_metrics.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"before": baseline, "after": after, "rms_delta_db": delta,
                      "output": str(OUTPUT)}, indent=2))


if __name__ == "__main__":
    main()
