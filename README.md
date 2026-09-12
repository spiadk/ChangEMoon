# 嫦娥射月亮 🌕

Kotlin 原生 Android Canvas 小遊戲，無 WebView、無廣告；遊戲可離線玩，Firebase Analytics 會在有網絡時傳送使用事件。

## 用 Android Studio 開啟

1. 解壓 ZIP，選擇 **Open**，開啟 `ChangEMoon` 資料夾（內含 `settings.gradle.kts`）。
2. Gradle JDK 選用 Android Studio 內置 JDK（17 或以上）。
3. 讓 Gradle Sync 完成；按提示安裝 Android SDK Platform 36 / Build Tools。首次同步需要網絡下載依賴。
4. 選擇 Android 6.0 / API 23 或以上模擬器或實機，按 **Run ▶**。

專案使用 Android Gradle Plugin 9.4.0、Gradle Wrapper 9.7.1、Kotlin 2.2.10。AGP 配對要求見 [Android 官方文件](https://developer.android.com/build/releases/agp-8-9-0-release-notes)。

命令列：Windows 用 `gradlew.bat assembleDebug`；macOS / Linux 用 `sh gradlew assembleDebug`。

## 玩法

- 按「開始追月」，在畫面拖曳瞄準；放手射箭。瞄準線由嫦娥位置指向手指方向。
- 40 秒內射中移動月亮 5 次就過關，獲得一個虛擬蓮蓉月餅。
- 月餅數量及下一關進度透過 SharedPreferences 儲存；關閉再開仍保留。
- 關卡越高，月亮移動越快，速度在第 12 關封頂。
- 右上角可暫停；切換其他 App 會自動暫停。時間用完可免費重試。
- 主頁「月餅收藏」可查看獎勵。月餅是遊戲內收藏，沒有實物兌換。

## 實作及驗證

`app/src/main/java/com/hkaiSolveAcademy/vibeCoding/ChangEMoon/MainActivity.kt` 包含原生 Activity、Canvas 繪圖、遊戲循環、觸控、移動目標碰撞與存檔。所有圖案由程式繪製，無外部素材依賴。

已用本機 Kotlin 編譯器及快取 Android API JAR 檢查 Kotlin 原始碼。環境無法讀取已安裝 Android SDK，因此未完成完整 Gradle APK 建置或模擬器實測；請在 Android Studio 同步並 Run。沒有附上未驗證的 APK。

建議實機驗收：首次開始 → 射中五次 → 月餅增加 → 下一關 → 暫停 / 回到 App → 失敗重試 → 重啟確認收藏保留。

## Firebase Analytics

Application ID：`com.hkaiSolveAcademy.vibeCoding.changEMoon`，配合現有 Google Play App。Namespace / Kotlin package 保留 `com.hkaiSolveAcademy.vibeCoding.ChangEMoon`。Firebase 已新增細寫 Android App，設定檔已按控制台確認的 Firebase App ID `1:519315450676:android:ca1a27a50faa68afcf1361` 更新，與 application ID 完全一致；沿用同一專案的 API key。

已加入 `app/google-services.json`，連接 Firebase 專案 `spiadk-f15ca`，及 Google services plugin / Firebase Analytics SDK。
事件：`level_start`（開始關卡）、`level_end`（關卡及成功與否、命中數）、`earn_virtual_currency`（每次勝利獲得 1 個 mooncake）。SDK 亦會收集預設使用事件。

Firebase Console → Project settings → Integrations → Google Analytics：確認連結到你想使用的 Analytics 資源。本機設定不能證明數字 ID `553862037` 的後台連結狀態。

驗證：Gradle Sync 後 Run，在連接裝置執行：

```text
adb shell setprop debug.firebase.analytics.app com.hkaiSolveAcademy.vibeCoding.changEMoon
```

重新開啟遊戲、開始並完成關卡，在 Firebase Analytics DebugView 檢查上述事件。完成後關閉 debug：

```text
adb shell setprop debug.firebase.analytics.app .none.
```

這次修改尚未完成 APK 建置及 Firebase DebugView 端到端驗證。更改 application ID 後會被 Android 視作另一個 App，舊 App 收藏不會自動遷移。

## 最新建置驗證（2026-09-12）

Gradle bundleRelease 成功，包含 release lint 及 Firebase 設定處理；已確認產出 manifest 的 application ID 為 com.hkaiSolveAcademy.vibeCoding.changEMoon，targetSdk 35。先前 SDK 存取限制已不再阻止本次建置。

目前專案未設定 release signingConfig，產生的 AAB 未簽署。上傳 Google Play 前須使用 Android Studio 的 Generate Signed Bundle / APK，選取現有 upload keystore 簽署。不要使用另一把新金鑰取代既有上傳金鑰。Analytics 實際事件接收仍未驗證。


API 36 更新：compileSdk / targetSdk 已升至 36，minSdk 維持 23；bundleRelease 及 release lint 通過，產出 manifest 確認 targetSdkVersion=36。尚未完成 API 36 裝置實測。
