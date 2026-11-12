# NEXUS WIN12 DESKTOP

**Windows 12 概念风格 · Android TV 桌面启动器**

深色科技风 · 毛玻璃任务栏 · 内置遥控器虚拟鼠标 · 完整 TV Launcher

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%20TV-green.svg)]()
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26-orange.svg)]()
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-purple.svg)]()

---

## 项目简介

NEXUS WIN12 DESKTOP 是一款面向 **Android TV / Google TV** 的自定义桌面启动器，视觉风格参考 Windows 12 概念设计。

核心亮点是**内置遥控器虚拟鼠标系统**——使用普通电视遥控器方向键即可控制桌面指针，无需外接鼠标或 Root。

> **重要说明**：虚拟鼠标默认仅在本桌面应用界面内生效（真实可运行实现）。如需尝试系统级控制，需用户手动开启辅助功能服务，且仍受 Android 安全机制限制。

---

## 功能特性

### 外观
- 深色科技风 + 毛玻璃效果任务栏
- 开始菜单、桌面图标网格
- 系统时间 / 日期实时显示
- 动态壁纸轻微缩放动画
- 横屏 16:9 完美适配 Android TV

### 核心：遥控器虚拟鼠标
- 方向键控制鼠标指针（短按 / 长按持续移动 + 加速度）
- OK / 确认键 = 左键单击
- 长按 OK = 右键
- 返回键可配置为右键
- 指针始终可见，带点击动画反馈
- 速度可调节、开关可控制
- 支持在桌面和启动器内完整使用

### 桌面功能
- 按 Home 键返回桌面
- 开机自动启动
- 拦截 Back 键防止误退出
- 可设为默认 Launcher
- 应用列表、打开、卸载、应用信息
- 完整遥控器操作支持

### 技术规格
| 项目 | 说明 |
|------|------|
| 语言 | Kotlin |
| 最低系统 | Android 8.0 (API 26) |
| 目标平台 | Android TV / Google TV |
| 构建工具 | Gradle + Android Studio |
| Root | 不需要 |
| 外接鼠标 | 不需要 |

---

## 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/yxing7249-cmd/Vc.git
cd Vc
```

### 2. 使用 Android Studio 打开

1. 打开 Android Studio
2. File → Open → 选择本项目根目录
3. 等待 Gradle Sync 完成
4. 连接 Android TV 设备或启动 TV 模拟器
5. 点击 Run

### 3. 命令行构建 APK

```bash
chmod +x gradlew
./gradlew assembleDebug
```

生成的 APK 位置：
```
app/build/outputs/apk/debug/app-debug.apk
```

### 4. 安装到电视

```bash
adb connect <电视IP>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

安装后请到系统设置中将本应用设为**默认主屏幕**。

---

## 虚拟鼠标使用说明

1. 进入应用设置，确保「启用遥控器虚拟鼠标」已打开（默认开启）
2. 使用方向键移动白色指针
3. 短按 **OK** = 左键点击
4. 长按 **OK** = 右键
5. **返回键** 默认作为右键（可在设置中关闭）

---

## 项目结构

```
NEXUS_WIN12_DESKTOP/
├── app/
│   ├── src/main/
│   │   ├── java/com/nexus/win12desktop/
│   │   │   ├── ui/                 # DesktopActivity, SettingsActivity
│   │   │   ├── mouse/              # VirtualMouseManager（核心鼠标逻辑）
│   │   │   ├── launcher/           # BootReceiver
│   │   │   ├── service/            # AccessibilityService + Overlay
│   │   │   └── utils/
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── .github/workflows/build.yml     # 自动构建 APK
├── README.md
└── LICENSE
```

---

## 设置为默认桌面

安装后进入应用内设置页面，点击「设置为默认桌面启动器」，或在系统设置 → 应用 → 默认应用 → 主屏幕 中选择本应用。

---

## 免责声明

- 本项目为开源概念实现，非微软官方产品
- Windows 12 仅为视觉风格参考
- 辅助功能服务仅在用户明确授权后生效
- 请遵守当地法律法规，仅用于学习与个人使用

---

## License

[MIT License](LICENSE)

---

## 仓库建议

当前仓库名为 `Vc`，建议在 GitHub 仓库设置中将名称修改为更清晰的名字，例如：

- `NEXUS-WIN12-DESKTOP`
- `NexusWin12Desktop`
- `android-tv-win12-launcher`
