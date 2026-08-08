#!/usr/bin/env python3
"""将 db/songci.db 复制为应用资源,并设置 user_version=1 以匹配 SQLDelight schema 版本。

SQLDelight 驱动(Android/iOS)在 user_version 与 schema.version 一致时跳过建表;
预建库必须带 user_version=1 才能被原样打开(否则驱动会尝试 DROP 重建,造成数据丢失)。

同时生成 db_version.txt(源 db 内容哈希前 8 位)——驱动缓存版本标记:
缓存 db 旁版本文件与资源版本不一致时重新复制(比大小对比更稳)。
"""
import hashlib
import shutil
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]                      # app/
SRC = ROOT / ".." / "db" / "songci.db"
DST = ROOT / "composeApp" / "src" / "commonMain" / "composeResources" / "files" / "songci.db"
VER = ROOT / "composeApp" / "src" / "commonMain" / "composeResources" / "files" / "db_version.txt"

DST.parent.mkdir(parents=True, exist_ok=True)
shutil.copy2(SRC, DST)
con = sqlite3.connect(DST)
con.execute("PRAGMA user_version = 1")
con.commit()
version = hashlib.md5(SRC.read_bytes()).hexdigest()[:8]
VER.write_text(version, encoding="utf-8")
print(f"copied {SRC.name} -> {DST} ({DST.stat().st_size/1024/1024:.1f} MB, user_version=1, version={version})")
