#!/usr/bin/env python3
"""将 db/songci.db 复制为应用资源,并设置 user_version=1 以匹配 SQLDelight schema 版本。

SQLDelight 驱动(Android/iOS)在 user_version 与 schema.version 一致时跳过建表;
预建库必须带 user_version=1 才能被原样打开(否则驱动会尝试 DROP 重建,造成数据丢失)。

同时生成 db_version.txt(产物 db 内容哈希前 8 位)——驱动缓存版本标记:
缓存 db 旁版本文件与资源版本不一致时重新复制(比大小对比更稳)。

产物净化:复制后清空用户表(favorites/recommendation_pool/recent_views)——随包库
必须无用户数据(否则开发期数据会发给所有新装用户)。升级时用户数据由驱动层
mergeUserData(ATTACH 旧库合并)保留,与产物无关。
"""
import hashlib
import shutil
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]                      # app/
SRC = ROOT / ".." / "db" / "songci.db"
DST = ROOT / "composeApp" / "src" / "commonMain" / "composeResources" / "files" / "songci.db"
VER = ROOT / "composeApp" / "src" / "commonMain" / "composeResources" / "files" / "db_version.txt"

# 与驱动层 USER_TABLES 保持一致(commonMain/UserDataMigration.kt)
USER_TABLES = ("favorites", "recommendation_pool", "recent_views")

DST.parent.mkdir(parents=True, exist_ok=True)
shutil.copy2(SRC, DST)
con = sqlite3.connect(DST)
for t in USER_TABLES:
    con.execute(f"DELETE FROM {t}")   # 产物用户表清空(幂等:表必存在于产物 schema)
con.execute("PRAGMA user_version = 1")
con.commit()
# 源库为 WAL 模式:不强制落盘的话哈希读到的是未含本次写入的主文件(与最终字节不符,
# 版本标记失去"内容变更检测"语义)。checkpoint + close 后哈希 = 随包字节的哈希。
con.execute("PRAGMA wal_checkpoint(TRUNCATE)")
con.close()
version = hashlib.md5(DST.read_bytes()).hexdigest()[:8]
VER.write_text(version, encoding="utf-8")
print(f"copied {SRC.name} -> {DST} ({DST.stat().st_size/1024/1024:.1f} MB, user_version=1, version={version}, user tables emptied)")
