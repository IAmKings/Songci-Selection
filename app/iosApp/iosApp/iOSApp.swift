import SwiftUI
import UserNotifications

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.all)
        }
    }
}

/// 每日一词通知点击:didReceive → songci://poem/{id}(复用 ContentView 的 onOpenURL 深链路径)。
/// 冷启动时系统也会经此回调(需在 didFinishLaunching 尽早注册 delegate)。
final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        if let poemId = (response.notification.request.content.userInfo["poemId"] as? NSNumber)?.int64Value,
           let url = URL(string: "songci://poem/\(poemId)") {
            UIApplication.shared.open(url)
        }
        completionHandler()
    }
}
