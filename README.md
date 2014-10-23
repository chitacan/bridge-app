# bridge-app

Android client for [bridge](https://github.com/chitacan/bridge).

> show "normal connection vs bridge connection" image

## Install

To Install app,

```
$ ./gradlew assembleDebug
$ adb install app/build/output/apk/app-debug.apk
```

## Run

You have to restart your Android device's adbd on TCP mode to extend it

```
$ adb tcpip <PORT>
```
