# 日常打卡

纯本地、免登录的安卓打卡工具，采用 Kotlin、Compose 和 Room。

## 当前状态

开发中，尚非完整交付版本。已实现主要页面、多次起飞记录、补卡统计、JSON/ZIP 备份恢复及提醒基础能力。15 个单元测试通过，调试 APK 构建通过。

尚待完成：桌面小组件、部分个性化、普通/周期连续统计细节、设备实测、恢复故障测试、正式签名和最终审阅。

## 构建

需要 JDK 17 或 21、Android SDK 35。配置 ANDROID_HOME 或 local.properties 后执行：

```sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Windows 使用 gradlew.bat。build-local.ps1 是原开发环境辅助脚本，其他电脑请使用 Gradle Wrapper。

不含用户数据、SDK、缓存、签名密钥及本机配置。
