#!/usr/bin/env python3
"""将 db/songci.db 复制为应用资源,并设置 user_version=1 以匹配 SQLDelight schema 版本。

SQLDelight 驱动(Android/iOS)在 user_version 与 schema.version 一致时跳过建表;
预建库必须带 user_version=1 才能被原样打开(否则驱动会尝试 DROP 重建,造成数据丢失)。
"""
import shutil
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]                      # app/
SRC = ROOT / ".." / "db" / "songci.db"
DST = ROOT / "composeApp" / "src" / "commonMain" / "composeResources" / "files" / "songci.db"

DST.parent.mkdir(parents=True, exist_ok=True)
shutil.copy2(SRC, DST)
con = sqlite3.connect(DST)
con.execute("PRAGMA user_version = 1")
con.commit()
print(f"copied {SRC.name} -> {DST} ({DST.stat().st_size/1024/1024:.1f} MB, user_version=1)")
