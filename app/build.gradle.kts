import java.util.Properties

plugins {
    id("com.android.application")
}

val signingProperties = Properties()
val signingPropertiesFile = rootProject.file("keystore.properties")
if (signingPropertiesFile.exists()) {
    signingPropertiesFile.inputStream().use(signingProperties::load)
}

fun signingValue(name: String): String? =
    providers.environmentVariable(name).orNull ?: signingProperties.getProperty(name)

val configuredKeystoreFile = signingValue("VLSM_KEYSTORE_FILE")
val configuredKeystorePassword = signingValue("VLSM_KEYSTORE_PASSWORD")
val configuredKeyAlias = signingValue("VLSM_KEY_ALIAS")
val configuredKeyPassword = signingValue("VLSM_KEY_PASSWORD")

android {
    namespace = "com.example.vlsmcalculator"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nirmal.subnetcalculator"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(configuredKeystoreFile!!)
            storePassword = configuredKeystorePassword!!
            keyAlias = configuredKeyAlias!!
            keyPassword = configuredKeyPassword!!
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    //implementation ("androidx.appcompat:appcompat:1.6.1")
    //implementation ("androidx.constraintlayout:constraintlayout:2.1.4")
    //implementation ("com.google.android.material:material:1.9.0")
    //implementation ("androidx.recyclerview:recyclerview:1.3.1")
    //implementation ("androidx.fragment:fragment:1.6.1")
}



