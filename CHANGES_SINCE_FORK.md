# Changes Since Fork

All changes relative to upstream `4325633` (moved images to correct location).

## Build Toolchain Modernisation

- Gradle wrapper updated to 8.7
- Android Gradle Plugin updated to 8.5.2
- Kotlin Gradle Plugin updated to 1.9.24
- `compileSdk` and `targetSdk` raised to 34
- `minSdk` raised from 21 to 29 (Android 10)
- Java source/target compatibility set to 17
- `jcenter()` removed (defunct); repositories are now `google()`, `mavenCentral()`, `jitpack.io`
- `buildFeatures.aidl` and `buildConfig` explicitly enabled (AGP 8 defaults)
- `flavorDimensions` updated for AGP 8 syntax

## Library Upgrades

- AndroidX appcompat 1.7.0, recyclerview 1.3.2, material 1.12.0, preference 1.2.1, mediarouter 1.7.0, constraintlayout 2.1.4
- AndroidX Room 2.6.1, Lifecycle 2.7.0, Paging 3.3.6
- OkHttp upgraded from 3.12.13 to 4.12.0
- ExoPlayer 2.18.2 migrated to AndroidX Media3 1.4.1
- Gson 2.11.0, Picasso 2.8, picasso-transformations 2.4.0
- MaterialPopupMenu 4.1.0 (JitPack), SearchPreference v2.3.0
- Google Play Services Cast 21.5.0
- `play-services-safetynet` removed (unused dependency)
- `lifecycle-extensions` removed (deprecated, replaced with specific lifecycle artifacts)
- `ViewModelProviders.of(this)` migrated to `new ViewModelProvider(this)`

## AGP 8 Compatibility

- All `switch/case` on `R.id.*` and `R.string.*` converted to `if/else` chains
- Transitive R class references fixed to use library-owned R classes directly:
  - `androidx.appcompat.R.attr.colorAccent` in `Utils.java`
  - `com.google.android.material.R.attr.boxBackgroundColor` in `ItemAdapterStation.java` and `ItemAdapterIconOnlyStation.java`
  - `com.github.zawadz88.materialpopupmenu.R.style` in `StationPopupMenu.kt`
- `android.nonTransitiveRClass=false` and `android.nonFinalResIds=false` shims removed from `gradle.properties`
- `IPlayerService` AIDL build feature enabled

## Media3 Migration Details

- All `com.google.android.exoplayer2` imports replaced with `androidx.media3` equivalents
- `@OptIn(markerClass = UnstableApi.class)` annotations added where required
- `DefaultBandwidthMeter` no-arg constructor replaced with `getSingletonInstance(context)`
- `HlsMediaSource` package corrected from `.source.hls` to `.hls`
- `InvalidResponseCodeException` constructor updated to 6-argument Media3 signature

## Bug Fixes

- Unchecked cast warning in `StationsFilter.publishResults` suppressed with annotation
- Consecutive `if` blocks in `ActivityMain.onOptionsItemSelected` fixed to `else if` (leftover from switch conversion)

## Dead Code Removal (minSdk 29)

- Removed pre-LOLLIPOP `BroadcastReceiver` fallback in `ConnectivityChecker` (NetworkCallback always available)
- Removed pre-JELLY_BEAN `MediaPlayerWrapper` fallback in `RadioPlayer` (ExoPlayer always available)
- Removed `HandlerThread` and unused `playerThread` field from `RadioPlayer`
- Removed `enableTls12OnPreLollipop` TLS 1.2 compat shim in `Utils` (TLS 1.2 native from API 20)
- Removed pre-KITKAT `ACTION_MEDIA_MOUNTED` broadcast in `StationSaveManager`
- Removed pre-LOLLIPOP URI permission fallbacks in `RecordingsAdapter`
- Removed `Build.VERSION.SDK_INT` checks for notification channels in `PlayerService` and `AlarmReceiver`
- Removed `PendingIntent.FLAG_IMMUTABLE` conditional in `PlayerService` and `RadioAlarmManager`
- Removed `WIFI_MODE_FULL` fallback in `PlayerService` and `AlarmReceiver`
- Removed pre-JELLY_BEAN_MR1 `setCompoundDrawables` fallback in `ItemAdapterStation` and `FragmentPlayerFull`
- Removed `>= 25` shortcut checks in `DataRadioStation` and `FavouriteManager`
- Removed `>= O_MR1` shortcut menu item check in `StationPopupMenu`
- Removed `>= 26` TV channel check in `TvChannelManager`
- Removed pre-JELLY_BEAN settings visibility check in `FragmentSettings`
- Removed `>= M` battery optimization checks in `FragmentSettings`
- Removed all now-unused `android.os.Build` imports across affected files
