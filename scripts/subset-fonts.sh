#!/bin/bash
# 字体子集化:按词库字符集(5275 字实测)+ UI 白名单裁剪内嵌字体。
# 前置:fonttools(pip3 install fonttools);db 在 App Group 容器(或 ~/.songci)。
# 用法:./scripts/subset-fonts.sh
set -euo pipefail
cd "$(dirname "$0")/.."

FONT_DIR=app/composeApp/src/commonMain/composeResources/font
CHARSET_FILE=scripts/font-charset.txt
DB=${SONGCI_DB:-$HOME/Library/Group\ Containers/group.com.songci.selection/songci.db}

# 1. 提取词库字符集(词/词牌/作者全量)
python3 - "$DB" "$CHARSET_FILE" <<'PY'
import sqlite3, sys
db_path, out = sys.argv[1], sys.argv[2]
con = sqlite3.connect(db_path)
rows = con.execute("SELECT content FROM poems UNION ALL SELECT rhythmic FROM poems UNION ALL SELECT name FROM authors").fetchall()
chars = set()
for (row,) in rows:
    for ch in row:
        if ch.isprintable() and not ch.isspace():
            chars.add(ch)
con.close()
# 2. UI 文本白名单(设置页/按钮等文案字符,缺失会豆腐块)
ui_whitelist = "字体风格楷宋体选择设置仅应用内生效取消确定保存恢复默认大中小收藏索引首页搜索词牌作者朝代格律异名平仄中韵脚阅读全文随机一词词库未同步正在加载点击左侧词作目录目录索引暂无词作该暂无收录作者与朝代年份证据词作详情刷新收藏已收藏书签翻阅"
chars.update(ui_whitelist)
# 3. 追加常用标点/数字
chars.update("，。、；：？！“”‘’（）《》【】·—…年月日一二三四五六七八九十百千万上下左右前后中大小多少")
with open(out, "w") as f:
    f.write("".join(sorted(chars)))
print(f"字符集 {len(chars)} 字符 → {out}")
PY

# 3. 子集化中文字体(Inter 拉丁界面字体保留原样,体积小无需裁)
for f in "$FONT_DIR"/lxgw_*.ttf; do
    [ -f "$f" ] || continue
    base=$(basename "$f")
    tmp="${f}.subset"
    echo "子集化: $base"
    python3 -m fontTools.subset "$f" \
        --unicodes="$(tr -d '\n' < "$CHARSET_FILE" | python3 -c "import sys; print(','.join(f'{ord(c):X}' for c in sys.stdin.read()))")" \
        --no-hinting --layout-features='*' \
        --output-file="$tmp"
    mv "$tmp" "$f"
done
ls -la "$FONT_DIR"/*.ttf
