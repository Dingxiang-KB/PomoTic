# PomoTic · 番茄钟学习 App

> 一款基于 **Kotlin + Jetpack Compose** 的安卓番茄钟 / 学习记录应用，采用 **MVVM** 架构，
> 支持番茄倒计时与自由正计时、待办清单、月日历视图、学习时长统计与深浅色主题，数据全部本地离线存储。

## ✨ 功能列表

| 模块 | 说明 |
| --- | --- |
| ⏱ 计时 | 倒计时番茄模式（工作时长/休息时长可自定义）、正计时自由学习模式；开始 / 暂停 / 继续 / 重置 / 手动结束 |
| 📌 后台计时 | 前台 `ForegroundService` + 常驻通知，APP 切后台、锁屏时计时不中断；计时结束仅发送系统通知 |
| 🔗 待办绑定 | 每条学习记录可绑定一条 Todo，统计该待办上花费的学习时长；可选“计时结束后自动标记待办完成” |
| 🏷 标签 | 新增 / 编辑 / 删除标签，支持自定义颜色；删除标签后关联记录与待办的标签字段自动置空 |
| ✅ 待办 | 标题、备注、截止日期、标签、优先级（高/中/低）、完成状态、重复规则（不重复/每日/每月）；支持筛选 |
| 📅 日历 | 月网格日历，周一到周日排布；格子内展示当天待办文本并标识待办 vs 已完成番茄记录；长按查看、点击弹出当日详情 |
| 📊 统计 | 按日 / 周 / 月聚合各标签学习总时长；扇形饼图可视化 + 刷新；每日学习目标完成进度 |
| 🎨 主题 | 一键切换深色 / 浅色模式，基于 Material3，全局自动适配 |
| 💾 存储 | Room（SQLite）三表 + DataStore 全局配置，离线本地存储 |

## 🧱 技术栈

- **语言**：Kotlin
- **UI**：Jetpack Compose + Material 3
- **架构**：MVVM（View / ViewModel / Repository），手动依赖注入（`AppContainer`）
- **导航**：Navigation Compose
- **数据库**：Room（KSP 代码生成，底层 SQLite）
- **配置存储**：DataStore（Preferences）
- **异步**：Kotlin Coroutines + Flow
- **日历**：compose-calendar（`com.kizitonwose.calendar`）
- **图表**：MPAndroidChart
- **构建**：Gradle（Kotlin DSL）、Android Gradle Plugin

## 📁 目录结构

```
Android/
├── app/
│   ├── build.gradle.kts              # 模块构建脚本（依赖配置）
│   └── src/main/
│       ├── AndroidManifest.xml       # 权限、前台服务声明
│       ├── java/com/example/pomostudy/
│       │   ├── MainActivity.kt        # 入口 Activity
│       │   ├── PomoStudyApp.kt        # Application，初始化依赖容器
│       │   ├── di/AppContainer.kt     # 手动依赖注入容器
│       │   ├── domain/model/          # 领域枚举（Priority/RepeatRule/ThemeMode/Timer*）
│       │   ├── data/
│       │   │   ├── local/             # Room：实体 / DAO / 数据库 / 类型转换
│       │   │   └── repository/        # 数据仓库（Tag/Todo/Study/Settings）
│       │   ├── service/               # 计时控制器 + 前台服务 + 通知
│       │   ├── ui/
│       │   │   ├── MainScreen.kt      # 底部导航 + 顶栏 + NavHost
│       │   │   ├── theme/             # Material3 主题、颜色
│       │   │   ├── components/        # 通用组件
│       │   │   ├── timer/             # 计时页
│       │   │   ├── todo/              # 待办页
│       │   │   ├── calendar/          # 日历页
│       │   │   ├── stats/             # 统计页
│       │   │   ├── tags/              # 标签管理页
│       │   │   └── settings/          # 设置页
│       │   └── util/TimeUtils.kt      # 时间工具
│       └── res/                       # 资源（主题、图标、字符串）
├── gradle/
│   ├── libs.versions.toml            # 版本目录（依赖版本集中管理）
│   └── wrapper/                      # Gradle Wrapper
├── build.gradle.kts                  # 根构建脚本
├── settings.gradle.kts               # 仓库配置（含国内镜像）
├── gradle.properties
├── gradlew / gradlew.bat
├── .gitignore
├── LICENSE
└── README.md
```

## 🚀 如何编译 / 运行 / 打包

### 环境要求
- **Android Studio**（建议最新稳定版，内置 JDK）
- **JDK 17 及以上**（AGP 9.x 建议使用 JDK 17+，本项目在 JDK 25 下构建通过）
- **Android SDK**：`compileSdk 37`，需安装对应 Platform 与 Build-Tools

### 1. 克隆
```bash
git clone <你的仓库地址>
cd Android
```

### 2. 配置 SDK 路径
复制/新建根目录 `local.properties`（该文件已被 `.gitignore` 忽略，不会提交）：
```properties
sdk.dir=/your/path/to/Android/Sdk
```
Windows 示例：`sdk.dir=C\:\\Users\\<你的用户名>\\AppData\\Local\\Android\\Sdk`

### 3. 用 Android Studio 打开
`File → Open` 选择本仓库根目录，等待 Gradle Sync 完成即可运行。

### 4. 命令行编译 / 打包
```bash
# 调试包（可直接安装）
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk

# 发布包（默认关闭 R8，可正常产出）
./gradlew assembleRelease
# 产物：app/build/outputs/apk/release/app-release-unsigned.apk
```
> Windows 使用 `gradlew.bat`。若命令行 JDK 版本与 Gradle 期望不一致，
> 可用 `-Dorg.gradle.java.home="<Android Studio 的 jbr 路径>"` 指定构建 JDK。

发布包正式上架需配置签名（`keystore`）后再 `assembleRelease`。

## 📲 最低支持的 Android 版本

- **最低支持：Android 7.0（API 24）**，`minSdk = 24`
- 目标版本：`targetSdk = 37`
- 通知权限（`POST_NOTIFICATIONS`）在 **Android 13（API 33）及以上** 首次进入时申请

## 📝 说明

- 计时结束只发送 **系统通知**，不弹应用内弹窗、不震动。
- 使用 `/app/src/main/java/com/example/pomostudy` 作为包名前缀，可按需替换为自有域名。
- 数据仅保存在本机，卸载即清除。

## 📄 License

本项目基于 [MIT License](LICENSE) 开源。
