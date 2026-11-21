# Charging Live Meter

Android live wallpaper project for Samsung Galaxy A23 / Android 14.

## Import
1. Extract the ZIP.
2. Open the `ChargingLiveMeter` folder in KlencodIDE as an existing Gradle project.
3. Allow Gradle sync and install missing Android SDK/build tools if prompted.
4. Build and install the debug APK.
5. Open the app and tap **معاينة وتعيين الخلفية الحية**.
6. Choose whether to apply it to the Home screen (or both if the launcher offers that option).

## Measurement note
The displayed W value is an estimate of battery-side electrical power calculated from the battery current and voltage exposed by Android. It is NOT a guaranteed measurement of USB input power or the charger's advertised maximum wattage. Some Samsung firmware versions may not expose a usable current reading; in that case the wallpaper displays `-- W`.

## Build status
This is a source project ZIP. It has not been built or tested on a physical phone in this environment.
