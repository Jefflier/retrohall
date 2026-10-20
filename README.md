# 复古游戏厅 RetroHall

一款开源的 Android 多平台复古游戏模拟器，参考小鸡模拟器、悟饭游戏厅的使用方式：一个 App 管理 FC、SFC、GB/GBC、GBA、NDS、N64、MD、PS、街机等平台的游戏，自动识别平台、下载封面，按需下载模拟器核心，打开即玩。

模拟部分基于 [LibretroDroid](https://github.com/Swordfish90/LibretroDroid) 和 libretro 生态的成熟核心（与 Lemuroid 同源），界面使用 Jetpack Compose 编写。

> 本应用不包含、不提供任何游戏 ROM 或 BIOS，请使用你合法拥有的游戏备份。

## 功能

**游戏库**
- 添加游戏文件夹后自动扫描子文件夹，或直接导入单个 ROM 文件
- 按扩展名、文件头（区分 MD / PS 的 `.bin`、PS / PSP 的 `.iso`）和文件夹名识别平台
- 支持 `.zip` 压缩的卡带游戏（启动时自动解压）、街机 zip、`.cue` 多轨与 `.m3u` 多光盘游戏
- 根据 ROM 名自动匹配 [libretro-thumbnails](https://github.com/libretro-thumbnails/libretro-thumbnails) 封面，也可自选图片
- 平台分类、最近游玩、我的收藏、搜索（支持输入平台名如 `GBA`）、按拼音 / 最近 / 时长排序
- 记录游玩次数与时长

**游戏中**
- 每个平台单独设计的虚拟按键：FC 连发键（TA/TB）、N64 摇杆 + C 键、PS 四肩键、街机 6 键 + 投币等
- 多点触控，手指可在按键间滑动；可调透明度、大小、触感反馈
- 实体手柄（蓝牙 / USB，最多 4 人）和键盘，检测到手柄时自动隐藏虚拟按键
- 即时存档：6 个手动栏位 + 自动存档，带截图预览；退出时自动保存、下次启动继续
- 电池存档（SRAM）自动保存
- 快进（2×/3×/4×/6×）、金手指（手动输入或导入 RetroArch `.cht`）、画面滤镜（原始 / 锐利 / CRT / LCD / 高清平滑）
- 多光盘游戏换盘、NDS 触屏、横竖屏自适应
- 游戏运行在独立进程中，核心崩溃不会影响游戏库

**核心与 BIOS**
- 首次打开某平台游戏时自动下载对应核心，也可在「核心」页一键下载常用核心
- 两个下载源：GitHub（LemuroidCores，推荐）和 libretro 官方构建服务器；可设置 GitHub 镜像前缀加速
- 支持从本地导入核心 `.so` 文件
- BIOS 导入后按文件名自动放到核心需要的位置

## 支持的平台

| 平台 | 核心 | 文件格式 | BIOS |
| --- | --- | --- | --- |
| FC / 红白机 | FCEUmm | `.nes` `.fds` `.unf` `.unif` | FDS 游戏需要 `disksys.rom` |
| SFC / 超级任天堂 | Snes9x | `.sfc` `.smc` `.swc` `.fig` | — |
| Game Boy | Gambatte | `.gb` | — |
| Game Boy Color | Gambatte | `.gbc` | — |
| Game Boy Advance | mGBA | `.gba` | 可选 `gba_bios.bin` |
| 任天堂 DS | melonDS | `.nds` | 可选 `bios7.bin` `bios9.bin` `firmware.bin` |
| 任天堂 64 | Mupen64Plus-Next | `.n64` `.z64` `.v64` | — |
| 世嘉五代 MD | Genesis Plus GX | `.md` `.gen` `.smd` `.bin` | — |
| 世嘉 Master System | Genesis Plus GX | `.sms` | — |
| 世嘉 Game Gear | Genesis Plus GX | `.gg` | — |
| PlayStation | PCSX ReARMed | `.cue` `.chd` `.pbp` `.m3u` `.iso` `.bin` | 可选 `scph5501.bin` 等 |
| PC Engine | Beetle PCE Fast | `.pce` | — |
| 街机 | FinalBurn Neo | `.zip` | Neo Geo 游戏需要 `neogeo.zip` |
| Neo Geo Pocket | Beetle NeoPop | `.ngp` `.ngc` | — |
| WonderSwan | Beetle Cygne | `.ws` `.wsc` | — |
| 雅达利 2600 | Stella | `.a26` | — |

以上卡带格式也都可以放在 `.zip` 里。

## 安装

需要 Android 8.0 及以上，推荐 64 位 ARM 设备。

- 正式版：在 [Releases](../../releases) 页面下载 APK（推送 `v*` 标签时由 CI 自动发布）
- 开发版：在 [Actions](../../actions) 中打开最近一次成功的构建，下载 `RetroHall-apk` 产物

## 使用说明

1. **添加游戏**：游戏库页右上角「+」→「添加游戏文件夹」。建议按平台分文件夹存放，例如：
   ```
   ROMs/
   ├── FC/        魂斗罗.nes
   ├── GBA/       Pokemon - Emerald Version (USA).gba
   ├── MD/        Sonic.bin        ← .bin 需要放在 MD / Genesis 文件夹里或带有效文件头
   ├── PS/        Final Fantasy VII (Disc 1).cue + .bin
   └── 街机/       kof98.zip
   ```
   文件夹名（`FC`、`GBA`、`PS`、`街机`、`arcade`、`mame` 等）用于识别扩展名不唯一的文件。ROM 使用 No-Intro 标准命名时封面匹配率最高。
2. **下载核心**：点开游戏后点「开始游戏」，缺少核心时会自动下载。国内访问 GitHub 慢时，可在「设置 → GitHub 加速镜像」填写镜像前缀，或切换到 libretro 官方下载源。
3. **导入 BIOS**（可选）：「核心」页 →「导入 BIOS」，可一次多选。街机的 `neogeo.zip` 也在这里导入，而不是放在游戏文件夹中。
4. **游戏中**：按返回键或点虚拟按键中间的菜单按钮打开游戏菜单，可存档 / 读档、快进、金手指、切换滤镜、换盘、重启和退出。实体手柄可按 Mode / 返回键打开菜单。

**键盘默认映射**：方向键 = 十字键，Z = B，X = A，A = Y，S = X，Q/W = L/R，E/R = L2/R2，回车 = START，空格 = SELECT。

## 从源码构建

需要 JDK 17 和 Android SDK（compileSdk 35）。

```bash
git clone <本仓库地址>
cd RetroHall
./gradlew :core:test      # 平台识别、金手指解析等单元测试
./gradlew assembleDebug   # 输出 app/build/outputs/apk/debug/
```

CI（`.github/workflows/android.yml`）会在每次推送时运行测试并构建 debug / release APK。如需用自己的密钥签名 release 包，在仓库 Secrets 中添加 `RELEASE_KEYSTORE_BASE64`、`RELEASE_KEYSTORE_PASSWORD`、`RELEASE_KEY_ALIAS`、`RELEASE_KEY_PASSWORD`；未配置时使用 debug 密钥签名。

## 项目结构

```
core/                       纯 Kotlin 模块，不依赖 Android，带单元测试
  Platform.kt               平台目录：核心、扩展名、文件夹提示、封面目录、BIOS、核心参数
  EmulatorCore.kt           libretro 核心列表
  GamepadLayout.kt          各平台虚拟按键布局及 RetroPad 映射
  RomIdentifier.kt          平台识别（扩展名 / 文件头 / 压缩包内容 / 文件夹名）
  ZipDirectory.kt           只读取 zip 中央目录，快速列出压缩包内容
  GameTitles.kt             标题清理与封面地址
  CheatParser.kt            金手指解析（.cht / 文本）
  CoreDownloads.kt          核心下载地址
  DiscFiles.kt              .cue / .m3u 引用文件解析
app/src/main/java/com/retrohall/app/
  data/                     SQLite 游戏库、文件夹扫描、ROM 准备、核心下载、BIOS、存档、设置、封面加载
  ui/                       Compose 界面：游戏库、平台页、搜索、详情、核心与 BIOS、设置
  game/                     GameActivity（独立进程）、模拟会话、虚拟手柄、游戏菜单、输入映射
```

## 路线图

- PSP（PPSSPP 核心需要额外的资源文件）
- 局域网 / 在线联机（libretro netplay）
- 自定义虚拟按键位置、实体手柄按键重映射
- 街机游戏中文名与封面数据库
- 金手指在线库、倒带、截图分享
- 云存档同步
- 多语言界面

## 致谢

- [LibretroDroid](https://github.com/Swordfish90/LibretroDroid) 与 [Lemuroid](https://github.com/Swordfish90/Lemuroid)（Filippo Scognamiglio），预编译核心来自 [LemuroidCores](https://github.com/Swordfish90/LemuroidCores)
- [libretro](https://www.libretro.com/) 及各核心的开发者
- [libretro-thumbnails](https://github.com/libretro-thumbnails/libretro-thumbnails) 提供封面

## 许可证

[GNU GPL v3](LICENSE)。各模拟器核心遵循其各自的开源协议。
