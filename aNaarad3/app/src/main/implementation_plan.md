# Implementation Plan - Fix Project Build Errors

Fix manifest package declaration and resolve all Kotlin compilation errors across the project to achieve a clean `:app:assembleDebug` build, and save a copy of this plan in `app/src/main/`.

## Proposed Changes

### Documentation

#### [NEW] [implementation_plan.md](file:///home/sanjay/Projects/GitHub/aNaarad/aNaarad3/app/src/main/implementation_plan.md)
- Save a copy of this implementation plan in the `app/src/main/` directory as requested.

---

### App Module

#### [MODIFY] [AndroidManifest.xml](file:///home/sanjay/Projects/GitHub/aNaarad/aNaarad3/app/src/main/AndroidManifest.xml)
- Remove `package="naarad.client.tabhost"` from the root `<manifest>` tag to comply with modern AGP namespace handling (the namespace `com.sanbee.anaarad` is already set in `app/build.gradle.kts`).

#### [MODIFY] [NaaradControlFragment.kt](file:///home/sanjay/Projects/GitHub/aNaarad/aNaarad3/app/src/main/java/naarad/client/tabhost/NaaradControlFragment.kt)
- Add missing import `android.view.View.MeasureSpec`.
- Fix nullable safe calls (`?.` / `!!`) on `selected_item`, `lamp0`, `lamp1`, `lamp2`, `currentToggleButton`, and `getActivity()`.
- Fix smart cast errors on mutable property `selected_item` by binding to a local `val view = gDnDParams?.selected_item`.
- Fix argument type mismatches where nullable views (`ToggleButton?`, `ImageView?`, `TextView?`) are passed to functions expecting `View`.

#### [MODIFY] [NaaradDnDOnTouchListener.kt](file:///home/sanjay/Projects/GitHub/aNaarad/aNaarad3/app/src/main/java/naarad/client/tabhost/NaaradDnDOnTouchListener.kt)
- Remove self-assignment `gDnDParams = gDnDParams` in `init` block.
- Change re-assigned `val` variables (`oX`, `oY`, `x`, `y`, `dx`, `dy`) to `var`.

#### [MODIFY] [NaaradPlotter.kt](file:///home/sanjay/Projects/GitHub/aNaarad/aNaarad3/app/src/main/java/naarad/client/tabhost/NaaradPlotter.kt)
- Add missing imports: `DisplayMetrics`, `LinearLayout`, `PointStyle`, `GraphicalView`, `XYMultipleSeriesDataset`, `XYSeries`, `XYMultipleSeriesRenderer`, `XYSeriesRenderer`, `R`.

#### [MODIFY] [NaaradSensorDataSource.kt](file:///home/sanjay/Projects/GitHub/aNaarad/aNaarad3/app/src/main/java/naarad/client/tabhost/NaaradSensorDataSource.kt)
- Add missing imports: `Gravity`, `GraphicalView`, `XYMultipleSeriesDataset`, `XYSeries`, `XYMultipleSeriesRenderer`, `MathHelper`, `JSONException`.

#### [MODIFY] [NaaradPlotFragment.kt](file:///home/sanjay/Projects/GitHub/aNaarad/aNaarad3/app/src/main/java/naarad/client/tabhost/NaaradPlotFragment.kt)
- Add missing imports: `BroadcastReceiver`, `AsyncTask`, `DisplayMetrics`, `Gravity`, `LayoutInflater`, `EditText`, `LinearLayout`, `PointStyle`, `ChartFactory`, `GraphicalView`, `XYMultipleSeriesDataset`, `XYSeries`, `XYMultipleSeriesRenderer`, `XYSeriesRenderer`, `MathHelper`, `JSONException`, `R`.
- Fix constructor call for inner class `SensorDataSource()`.
- Safe-null handling for `values[0]` in `onProgressUpdate`.

#### [MODIFY] [NaaradSettingFragment.kt](file:///home/sanjay/Projects/GitHub/aNaarad/aNaarad3/app/src/main/java/naarad/client/tabhost/NaaradSettingFragment.kt)
- Add missing imports: `Gravity`, `LayoutInflater`, `CheckBox`, `EditText`, `Toast`, `R`.
- Fix safe calls on nullable UI references (`serverNameField`, `serverPortField`, `wifiControl`, `wakeButton`, `setButton`).

## Verification Plan

### Automated Tests
- Run Gradle task `:app:assembleDebug` via `gradle_build` tool to confirm successful compilation and build output.
