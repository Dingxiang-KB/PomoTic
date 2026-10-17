# Task Spec — PomoStudy Android 番茄钟 App

## Goal
在 `D:\Program\PomoStudy\Android` 现有 Compose 模板工程内，用 Kotlin + Jetpack Compose 实现完整 MVVM 番茄钟 App，最终可构建出 APK，并补齐 GitHub 所需文件（.gitignore/README/LICENSE）与项目说明文档。

## Scope (in)
- 计时（倒计时番茄 + 正计时自由）、前台 ForegroundService 后台计时、结束仅系统通知
- 标签管理（自定义颜色，删除置空）、Todo（优先级/重复规则/筛选）、月日历视图、统计（日/周/月 + 饼图 + 目标进度）、深浅色主题
- Room 三表 + DataStore 全局配置
- 依赖：Navigation Compose / Room / DataStore / Coroutines / compose-calendar / MPAndroidChart
- 产物：可构建 APK、README.md、LICENSE(MIT)、.gitignore、`reMD/项目说明.md`

## Non-goals
- CSV 导出、数据备份恢复（明确不需要）
- APP 内结束弹窗/震动
- 不重建工程、不改 namespace=`com.example.pomostudy`

## Constrained files (do not delete/overwrite except to add deps)
gradlew, gradlew.bat, gradle/wrapper/, local.properties, settings.gradle.kts(仅加仓库)

## Environment facts
- OS: Windows / PowerShell; date ~2026-10-17
- JDK: shell JAVA_HOME=JDK17 (D:\language\Java17); Android Studio JBR=JDK25 (D:\software\android-studio\jbr) ← use for Gradle via -Dorg.gradle.java.home
- Gradle 9.6.0 (extracted in wrapper dists); AGP 9.4.0; Kotlin 2.2.10; Compose BOM 2026.02.01
- compileSdk=37, minSdk=24, targetSdk=37; SDK platform android-37.0, build-tools 36.0.0
- Network: google/jitpack/aliyun OK; foojay NOT reachable (so no JDK auto-provision)
- Previous successful template build in daemon log (BUILD SUCCESSFUL 5m40s)

## Locked dependency versions
- KSP 2.2.10-2.0.2 (must match Kotlin 2.2.10)
- Room 2.8.5, Navigation-Compose 2.10.2, DataStore 1.2.1, Lifecycle 2.11.0, Activity-Compose 1.13.0, core-ktx 1.19.1
- compose-calendar: com.kizitonwose.calendar:compose:2.10.1
- MPAndroidChart: com.github.PhilJay:MPAndroidChart:v3.1.0 (jitpack; only depends androidx.annotation)
- kotlinx-coroutines-android 1.10.2

## Success criteria
1. `.\gradlew assembleDebug` 成功产出 app-debug.apk（用 JBR JDK25）
2. `.\gradlew assembleRelease` 可成功（release 关闭 R8 以规避混淆风险）
3. 代码满足功能需求 1–7，MVVM 分层清晰，注释规范
4. Manifest 声明 FGS 权限/service、POST_NOTIFICATIONS；Android13+ 运行时申请
5. .gitignore 忽略要求项且保留 gradlew/gradlew.bat/gradle-wrapper.jar；README.md + LICENSE(MIT)
6. 生成 D:\Program\PomoStudy\reMD\项目说明.md（含 7 节要求）
7. 仓库内不出现个人路径/密钥（local.properties 被忽略）

## Verification gates
- G1: gradle 配置可解析（dependencies 可下载）
- G2: Room+KSP 代码生成通过（compileDebugKotlin 绿）
- G3: assembleDebug 产出 APK（检查文件存在 + aapt 报告）
- G4: 静态审查 daos/service/manifest 与需求逐条对齐
- G5: 文档完整性自检
