#!/usr/bin/env python3
"""Regenerate app icons for all platforms from design/app-icon/screen.png (1024x1024).

Single source of truth; rerun after any icon change. Deterministic output (git diff clean on rerun).
Requires macOS (iconutil) + Pillow.

Outputs:
  iOS    iosApp/iosApp/Preview Content/Assets.xcassets/AppIcon.appiconset/AppIcon.png
  Android composeApp/src/androidMain/res/mipmap-*/ic_launcher.png (legacy, all densities)
         composeApp/src/androidMain/res/mipmap-anydpi-v26/ic_launcher.xml (adaptive)
         composeApp/src/androidMain/res/mipmap-xxxhdpi/ic_launcher_foreground.png
         composeApp/src/androidMain/res/values/colors.xml (adaptive background color)
  Desktop composeApp/src/desktopMain/resources/icons/icon.icns | icon.ico | icon.png
"""
import shutil
import subprocess
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[3]  # <root>/app/data/tools/gen_app_icons.py
SRC = ROOT / "design" / "app-icon" / "screen.png"

# Android adaptive icon: 108dp canvas, content safe zone 66dp (Google spec)
FULL_CANVAS = 432          # xxxhdpi (144dpi * 3 = 432px)
SAFE_CONTENT = round(FULL_CANVAS * 66 / 108)  # 264px content within canvas

LEGACY_MIPMAPS = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}

# Apple icon grid: 1024 canvas, artwork in centered 824px safe zone, rounded corners
ICON_SIZE = 1024
ICON_CONTENT = 824            # margin 100px each side
ICON_RADIUS = 185             # Apple-spec corner radius on the content square


def rounded_icon() -> Image.Image:
    """macOS/iOS style icon: artwork as a rounded card on transparent canvas,
    so the OS squircle mask never clips the square corners of the artwork."""
    canvas = Image.new("RGBA", (ICON_SIZE, ICON_SIZE), (0, 0, 0, 0))
    art = Image.open(SRC).convert("RGBA").resize((ICON_CONTENT, ICON_CONTENT), Image.LANCZOS)
    mask = Image.new("L", (ICON_CONTENT, ICON_CONTENT), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        (0, 0, ICON_CONTENT, ICON_CONTENT), radius=ICON_RADIUS, fill=255)
    canvas.paste(art, ((ICON_SIZE - ICON_CONTENT) // 2,) * 2, mask)
    return canvas


def android_icon():
    res = ROOT / "app" / "composeApp" / "src" / "androidMain" / "res"
    (res / "mipmap-anydpi-v26").mkdir(parents=True, exist_ok=True)
    (res / "values").mkdir(parents=True, exist_ok=True)

    img = Image.open(SRC).convert("RGBA")
    for dpi, size in LEGACY_MIPMAPS.items():
        out = res / f"mipmap-{dpi}" / "ic_launcher.png"
        out.parent.mkdir(parents=True, exist_ok=True)
        img.resize((size, size), Image.LANCZOS).save(out)

    # Adaptive foreground: source scaled into the safe zone on transparent canvas
    fg = Image.new("RGBA", (FULL_CANVAS, FULL_CANVAS), (0, 0, 0, 0))
    fg.paste(img.resize((SAFE_CONTENT, SAFE_CONTENT), Image.LANCZOS),
             ((FULL_CANVAS - SAFE_CONTENT) // 2,) * 2)
    fg.save(res / "mipmap-xxxhdpi" / "ic_launcher_foreground.png")

    # Background: sample a corner pixel of the source art (paper/ink background)
    bg = "#{:02x}{:02x}{:02x}".format(*img.getpixel((4, 4))[:3])
    (res / "values" / "colors.xml").write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<resources>\n'
        f'    <color name="ic_launcher_background">{bg}</color>\n'
        '</resources>\n')
    (res / "mipmap-anydpi-v26" / "ic_launcher.xml").write_text(
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
        '    <background android:drawable="@color/ic_launcher_background"/>\n'
        '    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>\n'
        '</adaptive-icon>\n')


def ios_icon():
    # iOS: full-bleed opaque RGB (App Store rejects alpha); the OS squircle mask
    # supplies the rounded shape, so no baked rounded card here (unlike macOS).
    dest = ROOT / "app" / "iosApp" / "iosApp" / "Preview Content" / "Assets.xcassets" \
        / "AppIcon.appiconset" / "AppIcon.png"
    Image.open(SRC).convert("RGB").save(dest)


def desktop_icons(img):
    icons = ROOT / "app" / "composeApp" / "src" / "desktopMain" / "resources" / "icons"
    icons.mkdir(parents=True, exist_ok=True)

    img.resize((512, 512), Image.LANCZOS).save(icons / "icon.png")
    img.save(icons / "icon.ico", format="ICO", sizes=[(16, 16), (32, 32), (48, 48), (256, 256)])

    # icns via native iconutil (deterministic)
    iconset = icons / "icon.iconset"
    iconset.mkdir(exist_ok=True)
    for size in (16, 32, 128, 256, 512):
        img.resize((size, size), Image.LANCZOS).save(iconset / f"icon_{size}x{size}.png")
        img.resize((size * 2, size * 2), Image.LANCZOS).save(iconset / f"icon_{size}x{size}@2x.png")
    subprocess.run(["iconutil", "-c", "icns", str(iconset), "-o", str(icons / "icon.icns")],
                   check=True)
    shutil.rmtree(iconset)


if __name__ == "__main__":
    if not SRC.exists():
        raise SystemExit(f"source icon not found: {SRC}")
    android_icon()
    ios_icon()
    desktop_icons(rounded_icon())
    print("ok: android + ios + desktop icons regenerated")
