# PiliX Timer

为 **EO Forum Update 环节** 设计的 Android 分段计时器。原生 Kotlin + Jetpack Compose 实现，界面为深色极简风格，无任何系统通知。

![技术栈](https://img.shields.io/badge/Kotlin-2.0.21-blue) ![Compose](https://img.shields.io/badge/Compose-BOM%202024.12-cyan) ![minSdk](https://img.shields.io/badge/minSdk-26%20(Android%208.0)-green)

---

## 1. 默认计时流程

| 阶段 | 时长 | 颜色 | 分段结束时的提醒 |
|---|---|---|---|
| 陈述 | 5 分钟 | 蓝 | 环形与数字 **3 秒高亮脉冲**（轻提醒，不出声） |
| 延长 | 1 分钟 | 琥珀 | **声音提示**分段结束，自动进入正计时 |
| 共鸣 | 2 分钟 | 翠绿 | **声音提示**（需手动点击开始，不操作则不计时） |

```
准备态 ──开始──▶ 陈述 5:00 ──(3s 高亮脉冲)──▶ 延长 1:00 ──(声音)──▶ 正计时(超时)
                                                                            │
                                                          点开始 ──────────┘
                                                                │
                                                                ▼
                                                            共鸣 2:00 ──(声音)──▶ 本轮完成
```

- **正计时（超时）**：延长段结束后自动进入，数字从 `00:00` 往上走，表示超出时间；本轮用时同时继续累加。
- **共鸣时间**不会自动开始 —— 主持人点「开始」后才倒计时；不点则一直停在准备态，不消耗时间。
- **本轮用时**：`共鸣` 结束后显示该发言人本轮共用的时间。

## 2. 三个控制按钮

| 按钮 | 行为 |
|---|---|
| ⟳ 刷新 | **任何时候**按下立即回到计时起始准备状态；总用时不清零 |
| ▶ / ⏸ | 计时中按下 → 暂停（按钮变为开始）；非计时状态按下 → 开始计时 |
| 🔊 | 声音提醒开关。关闭后，所有「声音提示」改为 **5 秒急促醒目的屏幕变化**代替 |

## 3. 总用时

底部的小字「总用时」是一个**独立的连续累加器**：

- 从第一次开始计时起算，**中间永不暂停**（暂停、切换分段、刷新都不影响）
- 点击它即可**归零并重新计时**
- 刷新按钮不会清零总用时

## 4. 方案管理

点顶部方案名打开方案列表，可切换 / 编辑。每个分段可调整：

- **名称**
- **时长**（以分钟为单位，1–120）
- **颜色**（点色块循环切换 7 色）
- **结束提醒**：轻提醒 / 声音 / 无
- **开始方式**：自动接续 / 手动开始

方案可**另存为**新方案，内置的「标准 Update」为**默认方案**（可编辑、不可删除）。

## 5. 提醒与勿扰模式

- 提示音使用系统 **闹钟音轨**（`USAGE_ALARM`），**不受勿扰模式静音影响**
- 震动按**闹钟用途**登记（API 33+ 使用 `VibrationAttributes.USAGE_ALARM`），勿扰模式下仍会震动
- 应用**不申请 `POST_NOTIFICATIONS`**，不发送任何系统通知，也没有前台服务
- 若听不到声音，请在「设置 → 提醒不被静音 → 系统音量」中确认**闹钟音量**不为 0

## 6. 系统行为

- **屏幕常亮**：计时期间屏幕不熄灭（可在设置中关闭）
- **沉浸式全屏**：隐藏状态栏与导航栏
- **切后台不暂停**：时间基准为 `SystemClock.elapsedRealtime()`，所有状态按时间戳推算，切回前台立即校正，不会漂移
- **进程被杀可恢复**：计时快照每 2 秒落盘，重启后自动恢复到中断时的状态

## 7. 界面

- 醒目的环形剩余时间，中央大号数字（等宽数字 `tnum`，不跳动）
- 分段色带（按时长比例分配宽度）显示当前处在哪个分段
- 分段切换时强调色平滑过渡（500ms），按钮按压带回弹动画
- 环形进度用 Canvas 直接绘制，50Hz 状态刷新 + 实时时间推算，动画丝滑无跳变
- 深色 / 浅色 / 跟随系统三种主题

---

## 构建

### 环境要求

本机已配置**用户级共享 Android 开发环境**（`%USERPROFILE%\.android-dev`），本项目直接复用，无需单独安装：

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | Temurin 21 | `JAVA_HOME` |
| Android SDK | platform 35 + 36、build-tools 34/35/36 | `ANDROID_HOME` |
| Gradle | Wrapper 8.11.1（另有独立 8.14.3） | `GRADLE_USER_HOME` 指向共享依赖缓存 |
| 模拟器 | AVD `GTool_API36`（Pixel 9 参数，1080×2424，Android 16） | 共享 AVD，可用于安装与验证 |

环境变量已在系统用户级生效，新开终端即可用；也可手动加载：

```powershell
. "$env:USERPROFILE\.android-dev\bin\android-env.ps1"
```

### 命令

```powershell
# 一键构建 Release APK（自动探测共享工具链）
powershell -ExecutionPolicy Bypass -File tools\build.ps1

# 或直接用 Gradle
./gradlew assembleRelease
```

产物：

```
app/build/outputs/apk/release/app-release.apk
```

已用仓库内的 `keystore/pilix-release.jks` 签名（口令 `pilix2026`），可直接安装与覆盖升级。
**正式发布前请换成你自己的密钥库。**

> **OneDrive / 网盘目录注意**：构建目录可能被同步进程加锁导致 `AccessDeniedException`。
> 此时把构建目录挪到本地磁盘即可：
> ```powershell
> powershell -File tools\build.ps1 -BuildDir "$env:LOCALAPPDATA\PiliXTimer\build"
> ```

### 安装到手机

```powershell
adb install -r app\build\outputs\apk\release\app-release.apk
```

或直接安装仓库根目录下的 `PiliXTimer-v1.0.0.apk`。

### 重新生成图标 / 提示音

```powershell
python tools\gen_assets.py
```

---

## 网页版（PWA）—— iPhone 不上架 App Store 的方案

`web/` 目录下是一份**完全等价**的网页版实现：同一套状态机、同一套配色与环形动画，
用 iPhone Safari 打开 → 分享 → **添加到主屏幕**即可像原生 App 一样全屏使用。

```powershell
# 启动本地服务器（会打印局域网地址，iPhone 同 Wi-Fi 直接访问）
powershell -ExecutionPolicy Bypass -File tools\serve_pwa.ps1
```

### 部署到 GitHub Pages（免费、https、可离线）

```powershell
cd PiliXtimer
git init
git add web/
git commit -m "PWA 版 PiliX Timer"
git branch -M main
git remote add origin https://github.com/<你的用户名>/PiliXtimer.git
git push -u origin main
```

然后在 GitHub 仓库 **Settings → Pages → Source 选 `main` 分支 / `web` 目录**，
几分钟后 `https://<你的用户名>.github.io/PiliXtimer/` 即可访问。
（建议把仓库设为 Public，Pages 才有免费额度。）

### 与 Android 版的差异

| 能力 | Android APK | 网页版 / iPhone |
|---|---|---|
| 环形倒计时、分段配色、动画 | ✅ | ✅ 完全一致 |
| 方案编辑、多方案保存、默认方案 | ✅ | ✅ |
| 声音提示 | ✅ 闹钟流穿透勿扰 | ⚠️ 网页无法穿透静音键，请关闭静音模式 |
| 震动提醒 | ✅ | ⚠️ **iOS Safari 不支持网页震动**，仅 Android 浏览器可用 |
| 屏幕常亮 | ✅ 原生 | ✅ Wake Lock（iOS 16.4+） |
| 切后台不暂停 | ✅ | ✅ 时间戳推算，锁屏后依然准确 |
| 5 秒屏幕变化（静音模式） | ✅ | ✅ |
| 总用时独立连续累计 | ✅ | ✅ |
| 离线可用 | ✅ | ✅（https 部署后 Service Worker 生效） |

> 网页版在 iOS 上的声音需要**先点一下屏幕**才会解锁（Safari 的自动播放限制），
> 点「开始」正好完成解锁，之后各时段的提示音都会正常响。

---

## 已验证项（Android 14 模拟器 + 全新安装）

| 项目 | 结果 |
|---|---|
| 全新安装首次运行 | 默认方案「标准 Update」，显示 `05:00 / 陈述 / 准备开始` |
| 开始 / 暂停 / 继续 | 按钮在 ▶ ⏸ 间正确切换，暂停时剩余时间冻结 |
| **总用时暂停期间仍走** | 暂停 3 秒后 `已用` 不变，总用时 00:09 → 00:14 ✓ |
| **刷新不清零总用时** | 刷新后回到 `05:00 / 准备开始`，总用时继续累计 ✓ |
| 分段自动接续 | 陈述结束自动进入延长（蓝 → 琥珀） |
| 延长结束 | 声音提示 + 震动，随后进入正计时「超出时间」 |
| 共鸣需手动开始 | 停在准备态不计时，点开始后才倒计时 |
| 本轮完成 | 显示本轮用时 + 「点击刷新开始下一位」 |
| 分段配色 | 截图逐帧统计确认 蓝 `#3B82F6` → 琥珀 `#F59E0B` → 翠绿 `#10B981` |
| **静音时 5 秒屏幕变化** | 逐帧确认整屏饱和色闪烁约 4.8 秒 ✓ |
| 提示音音轨 | `AudioAttributes: usage=USAGE_ALARM` → `STREAM_ALARM`（勿扰不静音） |
| 震动用途 | `VibrationAttributes: Usage=ALARM`（勿扰下仍震动） |
| **切后台不暂停** | 后台 20 秒后剩余时间正确减少 26 秒 |
| **进程被杀后恢复** | 强杀进程 8 秒后重开，分段与剩余时间完整恢复 |
| 无系统通知 | 未申请 `POST_NOTIFICATIONS`，`dumpsys notification` 无该应用记录 |
| 深色 / 浅色主题 | 均正常渲染 |

---

---

## 项目结构

```
app/src/main/java/com/pilix/timer/
├── MainActivity.kt          入口、窗口标志（常亮 / 沉浸式）、页面导航
├── TimerViewModel.kt        计时引擎：状态机、时间推算、提醒触发、快照持久化
├── data/
│   ├── Models.kt            Segment / Plan / Palette / 默认方案
│   ├── TimerState.kt        Phase / SessionSnapshot / AlertKind
│   └── Repo.kt              SharedPreferences + JSON 持久化
├── notify/
│   └── Alerts.kt            提示音（闹钟流）与震动（闹钟用途）
└── ui/
    ├── TimerScreen.kt       主计时界面、环形 Canvas 绘制、动画
    ├── PlanEditorScreen.kt  方案编辑
    ├── SettingsScreen.kt    设置
    └── Theme.kt             配色
```
