"""分析模拟器截图：统计各分段主色的像素占比，并给出全屏平均色。"""
import os
import sys
from collections import Counter

from PIL import Image

PALETTE = {
    "蓝 #3B82F6": (59, 130, 246),
    "琥珀 #F59E0B": (245, 158, 11),
    "翠绿 #10B981": (16, 185, 129),
    "紫 #8B5CF6": (139, 92, 246),
    "红 #EF4444": (239, 68, 68),
    "青 #06B6D4": (6, 182, 212),
    "粉 #F472B6": (244, 114, 182),
}


def classify(r, g, b):
    best, bestd = None, 1e9
    for name, (pr, pg, pb) in PALETTE.items():
        d = (r - pr) ** 2 + (g - pg) ** 2 + (b - pb) ** 2
        if d < bestd:
            best, bestd = name, d
    return best if bestd < 6000 else None


def main(folder):
    files = sorted(f for f in os.listdir(folder) if f.endswith(".png"))
    for f in files:
        path = os.path.join(folder, f)
        im = Image.open(path).convert("RGB")
        w, h = im.size
        small = im.resize((w // 4, h // 4))
        counts = Counter()
        total = 0
        rs = gs = bs = 0
        for r, g, b in small.getdata():
            total += 1
            rs += r
            gs += g
            bs += b
            c = classify(r, g, b)
            if c:
                counts[c] += 1
        top = counts.most_common(3)
        desc = ", ".join(f"{k}:{v*100//total}%" for k, v in top) or "无主色"
        print(f"{f}: 尺寸={w}x{h} 平均色=({rs//total},{gs//total},{bs//total}) {desc}")


if __name__ == "__main__":
    main(sys.argv[1])
