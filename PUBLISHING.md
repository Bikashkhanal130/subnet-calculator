# Publishing

## One-time setup

Install Android Studio or the Android command-line tools with a JDK 17, then set:

```powershell
$env:JAVA_HOME = "C:\Path\To\Android\Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Install the Android SDK platform and build tools required by `app/build.gradle.kts`.
Update `applicationId` to a unique Play Console package name before the first upload.

Create an upload key and keep the generated file and passwords private:

```powershell
keytool -genkeypair -v -keystore release-upload.jks -alias vlsm-upload -keyalg RSA -keysize 2048 -validity 10000
Copy-Item keystore.properties.example keystore.properties
```

Replace the values in `keystore.properties`. The release build reads those values, or
the equivalent `VLSM_*` environment variables, and the file is ignored by Git.

## Build the Play bundle

```powershell
.\gradlew.bat clean bundleRelease
```

The signed bundle is created at:

```text
app\build\outputs\bundle\release\app-release.aab
```

If signing values are not present, Gradle still creates an unsigned release bundle for
local inspection, but Google Play will reject it. Upload the signed bundle to Play
Console and use the same upload key for future updates.