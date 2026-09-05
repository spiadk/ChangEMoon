plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.moon.change"
    compileSdk = 35
    defaultConfig { applicationId = "com.moon.change"; minSdk = 23; targetSdk = 35; versionCode = 1; versionName = "1.0" }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
