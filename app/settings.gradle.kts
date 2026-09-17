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
        // kheti(io.github.iamkings)阿里云 central 镜像未同步(2026-09-17),按 group 定向官方 central;
        // 限定 content 不影响其余依赖的镜像策略。CI(GitHub Actions)官方源稳定,同样命中。
        maven("https://repo1.maven.org/maven2") {
            content { includeGroup("io.github.iamkings") }
        }
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
