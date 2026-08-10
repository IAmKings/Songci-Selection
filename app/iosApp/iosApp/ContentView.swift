import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    /// 深链词 id(widget 点击 songci://poem/{id});由外部 .id() 变化触发重建
    var initialPoemId: Int64?
    /// 深链事件序号:同词重复点击也强制重建重导航
    var deepLinkToken: Int

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(initialPoemId: initialPoemId, deepLinkToken: deepLinkToken)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @State private var poemId: Int64?
    @State private var deepLinkToken: Int = 0

    var body: some View {
        ComposeView(initialPoemId: poemId, deepLinkToken: deepLinkToken)
            .id("\(poemId ?? 0)-\(deepLinkToken)") // 词 id 或事件序号变化 → 重建 Compose 层
            .ignoresSafeArea(.keyboard) // Compose 自行处理键盘
            .onOpenURL { url in
                // 小组件点击: songci://poem/{id}
                guard url.scheme == "songci", url.host == "poem" else { return }
                if let id = Int64(url.lastPathComponent) {
                    poemId = id
                    deepLinkToken += 1
                }
            }
    }
}
