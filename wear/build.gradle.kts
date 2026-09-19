plugins { alias(libs.plugins.android.application) }
android {
    namespace = "com.lucas.financeflow.watch"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.lucas.financeflow"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    sourceSets["main"].java.srcDir("../shared/src/main/java")
    testOptions { unitTests.isIncludeAndroidResources = true }
}
dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation("com.google.android.gms:play-services-wearable:20.0.1")
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.14.1")
}
