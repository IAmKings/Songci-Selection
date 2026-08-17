import WidgetKit
import SwiftUI
import SQLite3

// MARK: - App Group 共享 db(与 Compose 应用同步,版本标记判新)

enum SharedDb {
    static let groupId = "group.com.songci.selection"
    static let dbName = "songci.db"

    /// App Group 容器内的共享 db 路径(应用侧负责同步副本)
    static var url: URL {
        FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: groupId)!
            .appendingPathComponent(dbName)
    }

    /// 乱码检测:编码损坏词句(日文假名/半角片假名/全角拉丁数字/带圈/拉丁扩展/注音符号混入)。
    /// 区间与 commonMain GarbledText.kt 保持一致(2026-08-16 全库扫描 23 行命中,0 误伤)。
    private static func isGarbled(_ s: String) -> Bool {
        let ranges: [ClosedRange<UInt32>] = [
            0x3040...0x30FF, 0xFF66...0xFF9F, 0xFF21...0xFF5A, 0xFF10...0xFF19,
            0x2460...0x24FF, 0x0080...0x024F, 0x3100...0x312F,
        ]
        for scalar in s.unicodeScalars {
            let v = scalar.value
            if ranges.contains(where: { $0.contains(v) }) { return true }
        }
        return false
    }

    /// 随机一首词:乱码词排除(重试至多 3 次;全库仅 23 行乱码,命中概率极低)。
    static func randomPoem() -> (id: Int64, rhythmic: String, author: String, firstLine: String)? {
        for _ in 0..<3 {
            if let p = tryOnce(), !isGarbled(p.rhythmic + p.firstLine) { return p }
        }
        return nil
    }

    private static func tryOnce() -> (id: Int64, rhythmic: String, author: String, firstLine: String)? {
        var db: OpaquePointer?
        guard sqlite3_open(url.path, &db) == SQLITE_OK else { return nil }
        defer { sqlite3_close(db) }
        var stmt: OpaquePointer?
        let sql = """
        SELECT p.id, p.rhythmic, a.name, p.content FROM poems p
        JOIN authors a ON a.id = p.author_id
        WHERE p.rhythmic NOT LIKE '%⿰%'
          AND instr(p.content, '⿰') = 0
          AND instr(p.content, '𠴇') = 0
          AND instr(p.content, '𫍙') = 0
          AND length(p.rhythmic) <= 12
          AND instr(p.content, char(10)) > 0
        ORDER BY RANDOM() LIMIT 1
        """
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return nil }
        defer { sqlite3_finalize(stmt) }
        guard sqlite3_step(stmt) == SQLITE_ROW else { return nil }
        let id = sqlite3_column_int64(stmt, 0)
        let rhythmic = String(cString: sqlite3_column_text(stmt, 1))
        let author = String(cString: sqlite3_column_text(stmt, 2))
        let content = String(cString: sqlite3_column_text(stmt, 3))
        let firstLine = content.split(separator: "\n").first.map(String.init) ?? content
        return (id, rhythmic, author, firstLine)
    }
}

// MARK: - Widget

struct SongciEntry: TimelineEntry {
    let date: Date
    let poemId: Int64
    let rhythmic: String
    let author: String
    let firstLine: String
}

struct SongciProvider: TimelineProvider {
    func placeholder(in context: Context) -> SongciEntry {
        SongciEntry(date: Date(), poemId: 0, rhythmic: "水调歌头", author: "苏轼", firstLine: "明月几时有")
    }

    func getSnapshot(in context: Context, completion: @escaping (SongciEntry) -> Void) {
        completion(makeEntry())
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<SongciEntry>) -> Void) {
        completion(Timeline(entries: [makeEntry()], policy: .after(nextMidnight())))
    }

    /// 下一个本地 0 点:凌晨自动刷新(无需 app 运行,系统到点调 getTimeline)。
    private func nextMidnight() -> Date {
        Calendar.current.nextDate(
            after: Date(),
            matching: DateComponents(hour: 0, minute: 0),
            matchingPolicy: .nextTime
        ) ?? Date().addingTimeInterval(86400)
    }

    private func makeEntry() -> SongciEntry {
        let poem = SharedDb.randomPoem()
        return SongciEntry(date: Date(),
                           poemId: poem?.id ?? 0,
                           rhythmic: poem?.rhythmic ?? "宋词",
                           author: poem?.author ?? "",
                           firstLine: poem?.firstLine ?? "词库未同步")
    }
}

struct SongciWidgetView: View {
    @Environment(\.widgetFamily) var family
    let entry: SongciEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("\(entry.rhythmic)\(entry.author.isEmpty ? "" : " · \(entry.author)")")
                .font(.system(size: 13, weight: .bold))
                .foregroundColor(Color(red: 0, green: 0.13, blue: 0.27))
            Text(entry.firstLine)
                .font(.system(size: 11))
                .foregroundColor(Color(red: 0.38, green: 0.37, blue: 0.35))
                .lineLimit(family == .systemSmall ? 2 : 3)
        }
        .padding(12)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        // 整卡点击直达对应词详情(与 Android/iOS 深链一致);id 无效时不挂载
        .widgetURL(entry.poemId > 0 ? URL(string: "songci://poem/\(entry.poemId)") : nil)
        // 背景双层:containerBackground 是 macOS 26 的强制要求(否则占位提示),但实测 SDK 26 编译的
        // 扩展在 macOS 15 上该 API 不生效(深色模式下回退系统默认黑色背景)→ 叠加普通 .background 兜底。
        // 两处固定米白(与 app 主题一致,不随外观变化)。
        .background(Color(red: 0.96, green: 0.96, blue: 0.93))
        .containerBackground(for: .widget) { Color(red: 0.96, green: 0.96, blue: 0.93) }
    }
}

struct SongciWidget: Widget {
    let kind = "SongciWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: SongciProvider()) { entry in
            SongciWidgetView(entry: entry)
        }
        .configurationDisplayName("宋词选粹")
        .description("随机展示一首宋词")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge, .systemExtraLarge])
    }
}
