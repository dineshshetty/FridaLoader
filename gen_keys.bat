@echo off
keytool -genkey -v -keystore FridaLoader/debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000
keytool -genkey -v -keystore FridaLoader/release.keystore -storepass android -alias androidreleasekey -keypass android -keyalg RSA -keysize 2048 -validity 10000