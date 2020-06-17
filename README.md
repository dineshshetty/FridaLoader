# FridaLoader
A quick and dirty Android app to download and launch the latest version of Frida server on Genymotion Emulator and Rooted Android Physical Devices. 

Auto-downloads the Frida server based off the architecture of the device/emulator.

Note:
- Make sure your emulator/device is connected to the Internet when using FridaLoader. Disable any intermediate MITM proxies.
- Make sure grant root access to FridaLoader and adb shell during first launch
- Does not support default Google Android emulator out of the box. Requires the Android emulator to be rooted.
- Currently does not support Magisk

# Demo
![FridaLoader Demo](screenshots/demo.gif)


# ToDo
- Add support for Magisk
