# adb-bridge

Bridge your adb connection to wherever you want.

> show "normal connection vs bridge connection" image

## Run

You have to restart your Android device's adbd on TCP mode to extend it

```
$ adb tcpip <PORT>
```

To Install app,

```
$ ./gradlew assembleDebug
$ adb install app/build/output/apk/app-debug.apk
```
