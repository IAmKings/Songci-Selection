#!/bin/bash
# 一步部署 macOS 完整版:compose 打包 → xcodebuild 小组件 → 嵌入/重签 → 部署 /Applications。
# 用法:./scripts/macos-widget-deploy.sh(无需任何前置构建)
# 签名:设置 CERT_IDENTITY 或 ~/.songci-signing.env 用证书签名;缺省 ad-hoc(本地开发可用,发布需证书)。
set -euo pipefail
cd "$(dirname "$0")/.."

APP=app/composeApp/build/compose/binaries/main/app/SongciSelection.app
APPEX=app/iosApp/build/dd/Build/Products/Release/SongciWidgetExtension.appex
DEST=/Applications/SongciSelection.app

echo "==> 1/4 compose 打包"
(cd app && ./gradlew :composeApp:createDistributable --quiet)

echo "==> 2/4 xcodebuild 小组件 extension"
(cd app/iosApp && xcodebuild -project MacWidgetExtension.xcodeproj -scheme SongciWidgetExtension \
    -configuration Release -derivedDataPath build/dd build -quiet)

# 签名身份:证书优先,缺省 ad-hoc
CERT="${CERT_IDENTITY:-}"
if [ -z "$CERT" ] && [ -f "$HOME/.songci-signing.env" ]; then
    # shellcheck disable=SC1090
    . "$HOME/.songci-signing.env"
    CERT="${CERT_IDENTITY:-}"
fi
if [ -z "$CERT" ]; then
    echo "==> 3/4 无证书,ad-hoc 签名(本地开发)"
    SIGN_ID="-"
else
    echo "==> 3/4 证书签名: $CERT"
    SIGN_ID="$CERT"
fi

[ -d "$APPEX" ] || { echo "缺少 $APPEX" >&2; exit 1; }
[ -d "$APP" ] || { echo "缺少 $APP" >&2; exit 1; }

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
codesign --force --sign "$SIGN_ID" --entitlements "$ENT_APP" "$APP"

rm -rf "$DEST"
cp -R "$APP" "$DEST"
codesign --verify --deep --strict --verbose=2 "$DEST"
echo "==> 4/4 部署完成,启动中..."
open "$DEST"
