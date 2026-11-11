# Clean Cleaner

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Kotlin-2.2.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![UI](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/minSdk-26-orange?style=flat-square)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/targetSdk-35-blue?style=flat-square)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-green?style=flat-square)](LICENSE)
[![Privacy](https://img.shields.io/badge/Telemetry-Zero%20/%20Ad--Free-brightgreen?style=flat-square)](#privacy--security-first)

**Clean Cleaner** is a modern, high-performance, ad-free Android storage analyzer and visual cache inspection utility built with Kotlin and Jetpack Compose. It provides real-time disk visualization, WinDirStat-style directory breakdown, granular dead-folder pruning, and background memory inspection without telemetry or network connectivity.

---

## Key Features

### 1. Visual Cache & Storage Map
* **Interactive Proportional Donut & Treemap:** Slices and blocks reflect real-time app cache footprints queried directly via the Android platform `StorageStatsManager`.
* **Zero Slice Overlap:** Contiguous arcs with flat edges (`StrokeCap.Butt`) and subtle 2dp dividers ensure crisp rendering without bubble distortion.
* **Smart Small-App Aggregation:** Apps consuming under 2% of the total cache are gracefully grouped into an `"Other Apps"` cluster to prevent UI crowding.
* **Center Cache Overview:** The Donut hub showcases total cumulative app cache when idle, and shifts dynamically to app metrics upon selecting any segment.
* **Direct Cache Deep-Link:** Deep-links directly to Android system package details (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`) allowing users to purge cache natively.

### 2. Global Storage Breakdown Bar
* Displays a multi-segment progress strip at the top of the Cache Map:
  `[ System / Other ]` | `[ App Code & Data ]` | `[ Total Apps Cache (Neon) ]` | `[ Free Space ]`
* Includes summary statistics: **Total Apps Cache** and **Total App Footprint**.

### 3. Multi-Drive & MicroSD Card Support
* Dynamically identifies external storage volumes using `StorageManager.storageVolumes` and `ContextCompat.getExternalFilesDirs`.
* Offers a segmented drive switch (`[ Internal Storage ]` | `[ SD Card ]`) that immediately refocuses both recursive file scanners and directory tree generators.

### 4. WinDirStat-Style Directory Treemap ("Disk Tree")
* Dedicated tab that models disk usage as an interactive, hierarchical treemap.
* Slices directories into color-coded blocks by category:
  * **Videos:** Rose / Coral (`#F43F5E`)
  * **Images:** Cyber Cyan (`#00E5FF`)
  * **Audio:** Emerald Green (`#00E676`)
  * **Archives:** Purple (`#A855F7`)
  * **APKs:** Amber (`#F59E0B`)
  * **Documents:** Sky Blue (`#38BDF8`)
* **Interactive Navigation & Zooming:** Tap any block to drill down, backed by a horizontal breadcrumb trail (`Root > Download > ...`) with back navigation.

### 5. Granular Empty Folder Pruner
* Recursively detects dead and empty directory trees left behind by removed applications.
* **No Truncation:** Full scrollable list of all detected folders with complete paths.
* **Granular Control:** Individual deletion per directory, multi-selection checkboxes, and one-tap **"Delete Selected"** or **"Delete All"** with safety confirmation dialogs.

### 6. Largest Files Finder
* Identifies space-consuming files with user-configurable volume tiers: **Top 20**, **Top 50**, or **Top 100**.
* Instant filtering across categories: `All`, `Videos`, `APKs`, `Archives`, `Audio`, and `Documents`.
* Quick actions for every file: single-tap clipboard path copying and `ACTION_VIEW` system launcher integration.

### 7. Background Running Apps Inspector
* Inspects running background processes via `ActivityManager` and recent usage windows via `UsageStatsManager`.
* Quantifies per-app RAM utilization (PSS) with one-click navigation to Android's Force Stop / App Info screen.

---

## Privacy & Security First

* **100% On-Device:** All disk traversals, cache queries, and process evaluations run strictly on the local device using background coroutines (`Dispatchers.IO`).
* **Zero Network Code:** Clean Cleaner contains no network permissions (`android.permission.INTERNET` is neither requested nor used).
* **Zero Analytics & Trackers:** Completely free of SDKs from advertising networks, third-party trackers, or crash analytics services.

---

## Architecture & Codebase Layout

Clean Cleaner adheres to clean Modern Android Architecture (MVVM, StateFlow, Coroutines, Unidirectional Data Flow):

```text
com.cleancleaner.app/
├── data/
│   ├── model/
│   │   ├── AppStorageItem.kt          # App cache, data, APK sizes, and color mapping
│   │   └── StorageModels.kt           # DirectoryNode, TopFileItem, RunningProcessItem, VolumeItem
│   ├── repository/
│   │   ├── CacheRepository.kt         # Queries StorageStatsManager on Dispatchers.IO
│   │   ├── FileScannerRepository.kt   # Recursive storage scanner & empty dir pruner
│   │   ├── DiskTreeRepository.kt      # WinDirStat-style hierarchical directory tree builder
│   │   └── ProcessInspectorRepository # ActivityManager & UsageStatsManager inspector
│   └── util/
│       ├── FormatUtils.kt             # Safe file size & byte formatting
│       ├── PermissionUtils.kt         # AppOpsManager & storage permission gates
│       └── StorageVolumeUtils.kt      # Internal vs MicroSD volume discovery
├── ui/
│   ├── analyzer/
│   │   ├── StorageAnalyzerScreen.kt   # Disk overview, empty folder cleaner, largest files
│   │   └── StorageAnalyzerViewModel.kt
│   ├── cachemap/
│   │   ├── CacheMapScreen.kt          # Donut / Treemap cache map and global storage bar
│   │   └── CacheMapViewModel.kt
│   ├── components/
│   │   ├── AppIconImage.kt            # Safe Compose Drawable-to-Bitmap icon renderer
│   │   ├── AppInspectionCard.kt       # Detailed app storage card with system settings button
│   │   ├── GlobalStorageBar.kt        # Multi-segment System/App/Cache/Free storage bar
│   │   ├── InteractiveDonutChart.kt   # Custom canvas-based touch-interactive donut chart
│   │   ├── InteractiveTreemapChart.kt # Proportional rectangular cache treemap
│   │   ├── PermissionGateCard.kt      # Onboarding permission card
│   │   └── RunningProcessesDialog.kt  # Modal RAM and running processes inspector
│   ├── disktree/
│   │   ├── DiskTreeScreen.kt          # WinDirStat view, breadcrumbs, folder details
│   │   ├── DiskTreeViewModel.kt
│   │   └── WinDirStatTreemap.kt       # Colored category blocks with zoom capabilities
│   ├── theme/
│   │   ├── Color.kt                   # Minimal OLED palette (#090B0F) and category colors
│   │   ├── Theme.kt                   # Material 3 dark color scheme
│   │   └── Type.kt
│   ├── MainAppScreen.kt               # Scaffold, CenterAlignedTopAppBar, and NavigationBar
│   └── MainActivity.kt                # Edge-to-edge entry point
```

---

## Permissions Disclosure

Clean Cleaner requests only the permissions necessary to inspect cache and local storage:

| Permission | Purpose |
|---|---|
| `android.permission.PACKAGE_USAGE_STATS` | Required by `StorageStatsManager` to read byte statistics for user-installed applications. |
| `android.permission.QUERY_ALL_PACKAGES` | Required on Android 11+ to enumerate installed user apps for cache visualization. |
| `android.permission.MANAGE_EXTERNAL_STORAGE` | Required on Android 11+ to scan public storage, identify empty folder trees, and locate large files across directories. |

---

## Prerequisites & Build Instructions

### Requirements
* **JDK:** Eclipse Temurin JDK 21 or OpenJDK 21
* **Android SDK:** Platform 35 / 36 with Build-Tools 35.0.0+
* **Gradle:** 9.1.0 (included via Gradle Wrapper)

### Building from Source

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/clean-cleaner.git
   cd clean-cleaner
   ```

2. **Run Unit Tests:**
   ```bash
   ./gradlew testDebugUnitTest
   ```

3. **Assemble Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```

   The compiled APK will be located at:
   `app/build/outputs/apk/debug/app-debug.apk`

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

