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

    /// 随机一首词(id+词牌+作者+首句):SQLite 直查,零依赖
    static func randomPoem() -> (id: Int64, rhythmic: String, author: String, firstLine: String)? {
        var db: OpaquePointer?
        guard sqlite3_open(url.path, &db) == SQLITE_OK else { return nil }
        defer { sqlite3_close(db) }
        var stmt: OpaquePointer?
        let sql = """
        SELECT p.id, p.rhythmic, a.name, p.content FROM poems p
        JOIN authors a ON a.id = p.author_id
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
        completion(Timeline(entries: [makeEntry()], policy: .after(Date().addingTimeInterval(3600))))
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
        // 整卡点击直达对应词详情(与 Android/macOS 深链一致);id 无效时不挂载
        .widgetURL(entry.poemId > 0 ? URL(string: "songci://poem/\(entry.poemId)") : nil)
        .background(Color(red: 0.96, green: 0.96, blue: 0.93))
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
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}
