# MP3 Player - Native Android 9 (API 28)

A native Android MP3 player built using Java and XML. This project features a 3-page swipeable UI (Queue, Player, Lyrics), dynamic Day/Night theming, and a mock LAN QR sharing dialog.

## 🛠 Prerequisites

Before building the project, ensure your Android Studio environment meets the following requirements:

*   **Android Studio:** Arctic Fox (2020.3.1) or newer recommended.
*   **Minimum SDK:** API 28 (Android 9.0 Pie)
*   **Target SDK:** API 34 (or your current stable target)
*   **Language:** Java

## 📦 Gradle Dependencies

This project relies on standard AndroidX and Google Material components to handle the UI and backward compatibility. Add the following dependencies to your app-level `build.gradle` (usually `app/build.gradle`) inside the `dependencies` block.

```gradle
dependencies {
    // Core AndroidX libraries for backward compatibility and basic components
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

    // Google Material Design Components (Required for Dark Theme & Tab Indicators)
    implementation 'com.google.android.material:material:1.11.0'

    // ViewPager2 (Required for the swipeable 3-page carousel)
    implementation 'androidx.viewpager2:viewpager2:1.0.0'

    // Fragments (Required for managing the 3 different screens)
    implementation 'androidx.fragment:fragment:1.6.2'
    
    // Testing libraries (Default)
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}
```

### Dependency Breakdown

| Library | Purpose in this App |
| :--- | :--- |
| **`appcompat`** | Handles the `AppCompatDelegate` logic for the Light/Dark/System theme switcher dialog. |
| **`material`** | Provides the `TabLayout` for the indicator dots at the bottom of the screen, as well as the modern Material alert dialogs. |
| **`viewpager2`** | Powers the core horizontal swiping mechanics between the Playlist, Player, and Lyrics screens. |
| **`fragment`** | Required by the `FragmentStateAdapter` to load the distinct screens efficiently into memory as the user swipes. |

## 🚀 Setup Instructions

1. Clone or download this repository.
2. Open **Android Studio** and select **Open an existing project**.
3. Navigate to the downloaded folder and select it.
4. Allow Gradle to sync the dependencies listed above.
5. Hit **Run** (`Shift + F10`) to launch the app on an emulator or physical device running Android 9 (API 28) or higher.