# 嫦娥射月亮 🌕

Kotlin 原生 Android Canvas 小遊戲，無 WebView、無廣告、無網絡權限。

## 用 Android Studio 開啟

1. 解壓 ZIP，選擇 **Open**，開啟 `ChangEMoon` 資料夾（內含 `settings.gradle.kts`）。
2. Gradle JDK 選用 Android Studio 內置 JDK（17 或以上）。
3. 讓 Gradle Sync 完成；按提示安裝 Android SDK Platform 35 / Build Tools。首次同步需要網絡下載依賴。
4. 選擇 Android 6.0 / API 23 或以上模擬器或實機，按 **Run ▶**。

專案使用 Android Gradle Plugin 8.9.2、Gradle Wrapper 8.11.1、Kotlin 2.1.20。AGP 配對要求見 [Android 官方文件](https://developer.android.com/build/releases/agp-8-9-0-release-notes)。

命令列：Windows 用 `gradlew.bat assembleDebug`；macOS / Linux 用 `sh gradlew assembleDebug`。

## 玩法

- 按「開始追月」，在畫面拖曳瞄準；放手射箭。瞄準線由嫦娥位置指向手指方向。
- 40 秒內射中移動月亮 5 次就過關，獲得一個虛擬蓮蓉月餅。
- 月餅數量及下一關進度透過 SharedPreferences 儲存；關閉再開仍保留。
- 關卡越高，月亮移動越快，速度在第 12 關封頂。
- 右上角可暫停；切換其他 App 會自動暫停。時間用完可免費重試。
- 主頁「月餅收藏」可查看獎勵。月餅是遊戲內收藏，沒有實物兌換。

## 實作及驗證

`app/src/main/java/com/moon/change/MainActivity.kt` 包含原生 Activity、Canvas 繪圖、遊戲循環、觸控、移動目標碰撞與存檔。所有圖案由程式繪製，無外部素材依賴。

已用本機 Kotlin 編譯器及快取 Android API JAR 檢查 Kotlin 原始碼。環境無法讀取已安裝 Android SDK，因此未完成完整 Gradle APK 建置或模擬器實測；請在 Android Studio 同步並 Run。沒有附上未驗證的 APK。

建議實機驗收：首次開始 → 射中五次 → 月餅增加 → 下一關 → 暫停 / 回到 App → 失敗重試 → 重啟確認收藏保留。
