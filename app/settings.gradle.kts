rootProject.name = "songci"

// CI(GitHub Actions)访问阿里云镜像不稳定(502),直接用官方仓库;
// 本地网络对 dl.google.com TLS 握手不稳定,保留阿里云镜像优先。
// 注意:pluginManagement 是早期求值块,不能引用顶层局部变量,须在各块内用 System.getenv 判断。
pluginManagement {
    repositories {
        if (System.getenv("GITHUB_ACTIONS") != "true") {
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/central")
        }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        if (System.getenv("GITHUB_ACTIONS") != "true") {
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/central")
        }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

include(":composeApp")
