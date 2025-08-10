#!/usr/bin/bash
# ./makeapp.sh

echo "App Name?"
read APP_NAME

app_name_lc="${APP_NAME,,}"

echo "$app_name_lc"

# exit 1

COM_DOT_NAME="daveswaves"

mkdir -p "$APP_NAME/app/src/main/java/com/$COM_DOT_NAME/$app_name_lc"
mkdir -p "$APP_NAME/app/src/main/res/layout"
mkdir -p "$APP_NAME/app/src/main/res/mipmap-hdpi"
mkdir -p "$APP_NAME/app/src/main/res/mipmap-mdpi"
mkdir -p "$APP_NAME/app/src/main/res/mipmap-xhdpi"
mkdir -p "$APP_NAME/app/src/main/res/mipmap-xxhdpi"
mkdir -p "$APP_NAME/app/src/main/res/mipmap-xxxhdpi"

cat > "$APP_NAME/build.gradle.kts" <<EOL
plugins {
    id("com.android.application") version "8.4.2" apply false
    kotlin("android") version "1.9.24" apply false
}
EOL

cat > "$APP_NAME/gradle.properties" <<EOL
android.useAndroidX=true
android.enableJetifier=true
EOL

cat > "$APP_NAME/settings.gradle.kts" <<EOL
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "$APP_NAME"
include(":app")
EOL

# * * * * *

cat > "$APP_NAME/app/build.gradle.kts" <<EOL
plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.$COM_DOT_NAME.$app_name_lc"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.$COM_DOT_NAME.$app_name_lc"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
}
EOL

# * * * * *

cat > "$APP_NAME/app/src/main/AndroidManifest.xml" <<EOL
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:label="$APP_NAME"
        
        android:theme="@style/Theme.MaterialComponents.DayNight.NoActionBar">
        
        <activity android:name=".MainActivity" android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
            </intent-filter>
        </activity>
    </application>
</manifest>
<!--
# === Use if no PNG icons ===
android:icon="@android:drawable/ic_menu_search"
android:roundIcon="@android:drawable/ic_menu_search"
-->
EOL

# * * * * *

touch $APP_NAME/app/src/main/res/layout/activity_main.xml

touch $APP_NAME/app/src/main/java/com/$COM_DOT_NAME/$app_name_lc/MainActivity.kt

# * * * * *

touch $APP_NAME/app/src/main/res/mipmap-hdpi/ic_launcher-png
touch $APP_NAME/app/src/main/res/mipmap-hdpi/ic_launcher_round-png
touch $APP_NAME/app/src/main/res/mipmap-mdpi/ic_launcher-png
touch $APP_NAME/app/src/main/res/mipmap-mdpi/ic_launcher_round-png
touch $APP_NAME/app/src/main/res/mipmap-xhdpi/ic_launcher-png
touch $APP_NAME/app/src/main/res/mipmap-xhdpi/ic_launcher_round-png
touch $APP_NAME/app/src/main/res/mipmap-xxhdpi/ic_launcher-png
touch $APP_NAME/app/src/main/res/mipmap-xxhdpi/ic_launcher_round-png
touch $APP_NAME/app/src/main/res/mipmap-xxxhdpi/ic_launcher-png
touch $APP_NAME/app/src/main/res/mipmap-xxxhdpi/ic_launcher_round-png
