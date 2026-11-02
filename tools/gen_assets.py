"""生成 PiliX Timer 的提示音与启动图标资源。"""
import math
import os
import struct
import wave

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")

SR = 44100


def tone(freq, dur, amp=0.5, decay=8.5):
    n = int(SR * dur)
    out = []
    for i in range(n):
        t = i / SR
        env = math.exp(-t * decay)
        if t < 0.006:
            env *= t / 0.006
        s = (
            math.sin(2 * math.pi * freq * t)
            + 0.34 * math.sin(4 * math.pi * freq * t)
            + 0.10 * math.sin(6 * math.pi * freq * t)
        )
        out.append(amp * env * s / 1.44)
    return out


def make_chime(path):
    samples = []
    samples += tone(880.00, 0.26, 0.46)          # A5
    samples += tone(1318.51, 0.52, 0.52)         # E6
    # 末尾淡出，避免爆音
    fade = int(SR * 0.05)
    for i in range(fade):
        idx = len(samples) - fade + i
        samples[idx] *= (1 - i / fade)
    # 开头 3ms 静音作为间隔
    lead = [0.0] * int(SR * 0.003)
    samples = lead + samples

    frames = bytearray()
    for s in samples:
        v = int(max(-1.0, min(1.0, s)) * 32767)
        frames += struct.pack("<h", v)

    os.makedirs(os.path.dirname(path), exist_ok=True)
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(bytes(frames))
    print("chime ->", path, os.path.getsize(path), "bytes")


BLUE = (59, 130, 246)
TEAL = (16, 185, 129)
BG = (11, 16, 23)


def lerp(c1, c2, t):
    return tuple(int(round(c1[i] + (c2[i] - c1[i]) * t)) for i in range(3))


def draw_icon(size, round_icon):
    ss = 4  # 超采样
    s = size * ss
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # 背景
    if round_icon:
        d.ellipse([0, 0, s - 1, s - 1], fill=BG + (255,))
    else:
        d.rounded_rectangle([0, 0, s - 1, s - 1], radius=int(s * 0.22), fill=BG + (255,))

    # 环形进度：-90° 起顺时针 285°
    cx = cy = s / 2
    r = s * 0.33
    w = max(2, int(s * 0.095))
    start = -90.0
    sweep = 285.0
    steps = 360
    for i in range(steps):
        a0 = start + sweep * i / steps
        a1 = start + sweep * (i + 1) / steps + 0.6
        col = lerp(BLUE, TEAL, i / steps)
        d.arc(
            [cx - r, cy - r, cx + r, cy + r],
            a0,
            a1,
            fill=col + (255,),
            width=w,
        )

    # 圆头高亮
    ang = math.radians(start + sweep)
    hx = cx + r * math.cos(ang)
    hy = cy + r * math.sin(ang)
    d.ellipse([hx - w * 0.78, hy - w * 0.78, hx + w * 0.78, hy + w * 0.78], fill=TEAL + (110,))
    d.ellipse([hx - w * 0.30, hy - w * 0.30, hx + w * 0.30, hy + w * 0.30], fill=(255, 255, 255, 255))

    return img.resize((size, size), Image.LANCZOS)


def make_icons():
    densities = {
        "mdpi": 48,
        "hdpi": 72,
        "xhdpi": 96,
        "xxhdpi": 144,
        "xxxhdpi": 192,
    }
    for name, size in densities.items():
        folder = os.path.join(RES, "mipmap-" + name)
        os.makedirs(folder, exist_ok=True)
        draw_icon(size, False).save(os.path.join(folder, "ic_launcher.png"))
        draw_icon(size, True).save(os.path.join(folder, "ic_launcher_round.png"))
        draw_icon(size * 2, False).save(os.path.join(folder, "ic_launcher_foreground.png"))
    print("icons ->", os.path.join(RES, "mipmap-*"))


if __name__ == "__main__":
    make_chime(os.path.join(RES, "raw", "chime.wav"))
    make_icons()
