"""生成 PWA 图标（与 Android 端同一套视觉：深色底 + 蓝绿渐变环 + 白色圆头）。"""
import os

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "web", "icons")

BLUE = (59, 130, 246)
TEAL = (16, 185, 129)
BG = (11, 16, 23)


def lerp(c1, c2, t):
    return tuple(int(round(c1[i] + (c2[i] - c1[i]) * t)) for i in range(3))


def draw_icon(size, maskable=False):
    ss = 4
    s = size * ss
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    if maskable:
        # maskable：整幅铺满背景，图案缩到安全区内
        d.rectangle([0, 0, s - 1, s - 1], fill=BG + (255,))
        scale = 0.62
    else:
        d.rounded_rectangle([0, 0, s - 1, s - 1], radius=int(s * 0.22), fill=BG + (255,))
        scale = 1.0

    cx = cy = s / 2
    r = s * 0.33 * scale
    w = max(2, int(s * 0.095 * scale))
    start, sweep = -90.0, 285.0
    steps = 360
    for i in range(steps):
        a0 = start + sweep * i / steps
        a1 = start + sweep * (i + 1) / steps + 0.6
        col = lerp(BLUE, TEAL, i / steps)
        d.arc([cx - r, cy - r, cx + r, cy + r], a0, a1, fill=col + (255,), width=w)

    import math

    ang = math.radians(start + sweep)
    hx = cx + r * math.cos(ang)
    hy = cy + r * math.sin(ang)
    d.ellipse([hx - w * 0.78, hy - w * 0.78, hx + w * 0.78, hy + w * 0.78], fill=TEAL + (110,))
    d.ellipse([hx - w * 0.30, hy - w * 0.30, hx + w * 0.30, hy + w * 0.30], fill=(255, 255, 255, 255))
    return img.resize((size, size), Image.LANCZOS)


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for n in (192, 512):
        draw_icon(n).save(os.path.join(OUT, f"icon-{n}.png"))
    draw_icon(512, maskable=True).save(os.path.join(OUT, "icon-maskable-512.png"))
    draw_icon(180).save(os.path.join(OUT, "apple-touch-icon.png"))
    print("icons ->", OUT)
