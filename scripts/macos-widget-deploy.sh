#!/bin/bash
# 嵌入小组件扩展(.appex)→ 重签 host app → 部署 /Applications。
# 前置:app/iosApp 下 xcodebuild 已产出 .appex,compose 已产出 .app(见 README 或 prd.md)。
set -euo pipefail
cd "$(dirname "$0")/.."

APP=app/composeApp/build/compose/binaries/main/app/SongciSelection.app
APPEX=app/iosApp/build/dd/Build/Products/Debug/SongciWidgetExtension.appex
DEST=/Applications/SongciSelection.app
# 签名身份从环境变量读取(避免账号信息入库);本地可放 ~/.songci-signing.env 自动加载
CERT="${CERT_IDENTITY:-}"
if [ -z "$CERT" ] && [ -f "$HOME/.songci-signing.env" ]; then
    # shellcheck disable=SC1090
    . "$HOME/.songci-signing.env"
    CERT="${CERT_IDENTITY:-}"   # source 后再取一次(env 文件设置的是 CERT_IDENTITY)
fi
[ -n "$CERT" ] || { echo "缺少签名证书:设置 CERT_IDENTITY 环境变量或 ~/.songci-signing.env" >&2; exit 1; }

[ -d "$APPEX" ] || { echo "缺少 $APPEX —— 先跑 xcodebuild" >&2; exit 1; }
[ -d "$APP" ] || { echo "缺少 $APP —— 先跑 compose 打包" >&2; exit 1; }

# host app 需要 application-groups entitlement(与扩展共享 App Group 容器)
ENT_APP=$(mktemp /tmp/songci-app-ent.XXXXXX.plist)
trap 'rm -f "$ENT_APP"' EXIT
cat > "$ENT_APP" <<'PLIST'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
  <key>com.apple.security.application-groups</key>
  <array><string>group.com.songci.selection</string></array>
</dict></plist>
PLIST

pkill -f "/Applications/SongciSelection.app" 2>/dev/null || true

# 注册 songci:// URL scheme(小组件 widgetURL 点击跳转)
/usr/libexec/PlistBuddy -c "Delete :CFBundleURLTypes" "$APP/Contents/Info.plist" 2>/dev/null || true
/usr/libexec/PlistBuddy -c "Add :CFBundleURLTypes array" "$APP/Contents/Info.plist"
/usr/libexec/PlistBuddy -c "Add :CFBundleURLTypes:0 dict" "$APP/Contents/Info.plist"
/usr/libexec/PlistBuddy -c "Add :CFBundleURLTypes:0:CFBundleURLName string com.songci.app" "$APP/Contents/Info.plist"
/usr/libexec/PlistBuddy -c "Add :CFBundleURLTypes:0:CFBundleURLSchemes array" "$APP/Contents/Info.plist"
/usr/libexec/PlistBuddy -c "Add :CFBundleURLTypes:0:CFBundleURLSchemes:0 string songci" "$APP/Contents/Info.plist"

# 扩展已由 xcodebuild 签名(含 sandbox+app-groups);只重签 host(嵌套签名先签子)
mkdir -p "$APP/Contents/Extensions"
rm -rf "$APP/Contents/Extensions/SongciWidgetExtension.appex"
cp -R "$APPEX" "$APP/Contents/Extensions/"
codesign --force --sign "$CERT" --entitlements "$ENT_APP" "$APP"

rm -rf "$DEST"
cp -R "$APP" "$DEST"
codesign --verify --deep --strict --verbose=2 "$DEST"
echo "部署完成,启动中..."
open "$DEST"
