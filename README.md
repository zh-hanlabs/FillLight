# 补光灯 FillLight

仿 iOS 热门「屏幕补光灯」应用的安卓版：把手机屏幕变成一块可调色的大灯板，适合自拍补光、视频通话打光、夜灯、氛围灯，也支持频闪和 SOS 求救灯。

## 功能

- **全屏补光**：整块屏幕变灯板，屏幕常亮，App 内亮度可调（不影响系统亮度设置）
- **色温调节**：1500K（烛光暖黄）~ 9000K（冷白）连续可调，带渐变滑条
- **彩色模式**：HSV 取色轮任意取色（中心白、边缘全色相）
- **氛围色预设**：樱花粉、日落橙、蜜桃、薄荷绿、海盐蓝、薰衣草、青柠、玫瑰金
- **色温预设**：烛光 / 暖白 / 自然白 / 冷白 / 正午
- **四种灯光模式**：
  - 常亮
  - 频闪（0.5 ~ 10 Hz 频率可调）
  - 呼吸（明暗渐变）
  - SOS（摩尔斯电码求救灯）
- **快速设置磁贴**：下拉通知栏添加「补光灯」磁贴，一键进入
- **桌面小组件**：1x1 灯泡组件，点击直达
- 点按屏幕任意位置显示/收起控制面板，返回键先收面板再退出

## 技术栈

- Kotlin + Jetpack Compose (Material 3)
- Gradle 8.13 / AGP 8.13 / Kotlin 2.1 / Compose BOM 2025.01
- minSdk 26 / targetSdk 35 / compileSdk 36
- 无任何权限、无网络、无第三方 SDK

## 构建运行

命令行（本机已配好 SDK 与镜像）：

```bash
./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
adb install app/build/outputs/apk/debug/app-debug.apk
```

或直接用 Android Studio 打开本目录，点 Run。

## 结构速览

```
app/src/main/java/com/glow/filllight/
  MainActivity.kt          入口，屏幕常亮 + 暗色主题
  FillLightScreen.kt       全屏灯面 + 控制面板 + 频闪/呼吸/SOS 动效
  ColorWheel.kt            HSV 取色轮（Canvas 自绘）
  LightModel.kt            模式枚举、预设、色温转 RGB 算法、SOS 节奏
  FillLightTileService.kt  快速设置磁贴
  FillLightWidgetProvider.kt 桌面小组件
```
