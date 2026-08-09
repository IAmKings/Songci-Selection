import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    /// 深链词 id(widget 点击 songci://poem/{id});由外部 .id() 变化触发重建
    var initialPoemId: Int64?

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(initialPoemId: initialPoemId)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @State private var poemId: Int64?

    var body: some View {
        ComposeView(initialPoemId: poemId)
            .id(poemId) // poemId 变化 → 重建 Compose 层并携带新深链
            .ignoresSafeArea(.keyboard) // Compose 自行处理键盘
            .onOpenURL { url in
                // 小组件点击: songci://poem/{id}
                guard url.scheme == "songci", url.host == "poem" else { return }
                if let id = Int64(url.lastPathComponent) {
                    poemId = id
                }
            }
    }
}
