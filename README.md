# SimpleCalculator

A clean Android calculator app built with Kotlin, XML, and ViewBinding.  
It includes a splash screen, dark/light theme support, and a calculator UI inspired by the modern phone calculator style.

## Features

- Splash screen with app logo and footer
- Dark and light theme toggle
- Calculator operations: `+`, `-`, `×`, `÷`
- Extra actions: `AC`, `%`, `00`, backspace, decimal, equals
- Live preview result below the main expression
- Custom app icon and theme toggle icon
- Number system conversion screen with binary input support
- Currency converter screen with live exchange-rate fetching
- Age calculator with improved next-birthday calculation
- Transparent private vault with passcode protection, encrypted imports, folders, move/copy, and trash restore

## Screenshots

### Splash Screen

<img src="docs/screenshots/splash.png" alt="Splash Screen" width="200" height="400" />

### Calculator - Light Theme

<img src="docs/screenshots/calculator-light.png" alt="Calculator Light Theme" width="200" height="400" />

### Calculator - Dark Theme

<img src="docs/screenshots/calculator-dark.png" alt="Calculator Dark Theme" width="200" height="400" />

## Recent Updates

- Added a dedicated number system converter screen that accepts only valid binary input and shows the decimal result live.
- Added a currency converter screen that fetches live exchange rates and supports swapping currencies.
- Fixed the age calculator next-birthday calculation so it uses the correct upcoming birthday date.
- Improved the main calculator top bar icon so it stays centered inside the custom button background.
- Added a transparent private vault screen for securely importing and managing files inside the app's private storage.

## Tech Stack

- Kotlin
- XML layouts
- ViewBinding
- Material Components
- AndroidX

## Requirements

- Android Studio
- JDK 11 or higher
- Android SDK 24+

## Project Setup

1. Clone the repository.
2. Open it in Android Studio.
3. Let Gradle sync complete.
4. Run the app on an emulator or physical device.

## How It Works

- `SplashActivity` is the launcher activity.
- After a short delay, it opens `MainActivity`.
- `MainActivity` handles the calculator input state and updates the result preview in real time.
- Theme preference is stored locally and restored on app launch.

## Project Structure

```text
app/src/main/java/com/example/simplecalculator/
├── AppTheme.kt
├── MainActivity.kt
└── SplashActivity.kt

app/src/main/res/
├── drawable/
├── layout/
├── mipmap-*/
├── values/
└── values-night/
```

## Notes

- The app uses ViewBinding instead of `findViewById`.
- The launcher icon and splash artwork use the provided calculator image.
- The footer text on the splash screen shows `Made By Ritesh ❤️`.
- The newer converter screens are designed to match the same rounded-card style as the main calculator.

## License

No license has been added yet. Add one if you plan to publish or share the project publicly.
