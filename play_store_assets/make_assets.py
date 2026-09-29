from PIL import Image, ImageDraw, ImageFont, ImageFilter
from pathlib import Path

out = Path(__file__).resolve().parent
src = Image.open(out.parent / "app/src/main/res/drawable/app_icon.png").convert("RGBA")

# --- 512x512 Play icon ---
icon = src.resize((512, 512), Image.Resampling.LANCZOS)
bg = Image.new("RGBA", (512, 512), (243, 240, 228, 255))
bg.alpha_composite(icon)
icon512 = bg.convert("RGB")
icon_path = out / "play_icon_512.png"
icon512.save(icon_path, "PNG", optimize=True)
print("icon", icon_path, icon512.size, icon_path.stat().st_size)

# --- Feature graphic 1024x500 ---
W, H = 1024, 500
feat = Image.new("RGBA", (W, H), (243, 240, 228, 255))
draw = ImageDraw.Draw(feat)

for y in range(H):
    t = y / (H - 1)
    r = int(243 - 20 * t)
    g = min(255, int(240 - 10 * t + 18 * t))
    b = int(228 - 40 * t)
    draw.line([(0, y), (W, y)], fill=(r, g, b, 255))

overlay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
od = ImageDraw.Draw(overlay)
od.ellipse([620, -80, 1100, 360], fill=(82, 183, 136, 38))
od.ellipse([-80, 220, 420, 620], fill=(45, 106, 79, 28))
od.ellipse([380, 300, 780, 620], fill=(149, 213, 178, 35))
feat = Image.alpha_composite(feat, overlay)

icon_big = src.resize((360, 360), Image.Resampling.LANCZOS)
shadow = Image.new("RGBA", (360, 360), (0, 0, 0, 0))
sd = ImageDraw.Draw(shadow)
sd.rounded_rectangle([18, 24, 342, 348], radius=72, fill=(27, 67, 50, 55))
shadow = shadow.filter(ImageFilter.GaussianBlur(18))
feat.alpha_composite(shadow, (54, 78))
feat.alpha_composite(icon_big, (48, 70))

draw = ImageDraw.Draw(feat)
try:
    font_title = ImageFont.truetype(r"C:\Windows\Fonts\georgiai.ttf", 96)
except Exception:
    try:
        font_title = ImageFont.truetype(r"C:\Windows\Fonts\georgia.ttf", 96)
    except Exception:
        font_title = ImageFont.load_default()
try:
    font_sub = ImageFont.truetype(r"C:\Windows\Fonts\segoeui.ttf", 30)
except Exception:
    font_sub = ImageFont.load_default()

title = "Verdly"
subtitle = "Build habits with friends"
tx, ty = 450, 165
draw.text((tx + 2, ty + 3), title, font=font_title, fill=(27, 67, 50, 40))
draw.text((tx, ty), title, font=font_title, fill=(29, 74, 55, 255))
draw.text((tx, ty + 120), subtitle, font=font_sub, fill=(45, 90, 70, 220))
draw.rounded_rectangle([tx, ty + 108, tx + 120, ty + 114], radius=3, fill=(82, 183, 136, 255))

feat_path = out / "feature_graphic_1024x500.png"
feat.convert("RGB").save(feat_path, "PNG", optimize=True)
print("feature", feat_path, feat.size, feat_path.stat().st_size)
