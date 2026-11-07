plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
}
android {
  namespace = "com.musicvibe.app"
  compileSdk = 36
  defaultConfig {
    applicationId = "com.saunevibe.fresh"
    minSdk = 24
    targetSdk = 35
    versionCode = 7
    versionName = "1.1.1"
  }
  buildFeatures { compose = true }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions { jvmTarget = "17" }
}
dependencies {
  val bom = platform("androidx.compose:compose-bom:2025.04.01")
  implementation(bom)
  implementation("androidx.activity:activity-compose:1.10.1")
  implementation("androidx.core:core-ktx:1.16.0")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.foundation:foundation")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.material:material-icons-extended")
  implementation("io.coil-kt:coil-compose:2.7.0")
  implementation("androidx.media3:media3-exoplayer:1.11.1")
  implementation("androidx.media3:media3-session:1.11.1")
}
