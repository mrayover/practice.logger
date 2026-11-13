import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}
android {
    namespace = "com.mrayover.practicelogger"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.mrayover.practicelogger.app"
        minSdk = 26
        targetSdk = 36
        versionCode = providers.gradleProperty("versionCodeOverride").orNull?.toInt() ?: 117
        versionName = "0.44.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes { release { isMinifyEnabled = false } }
    buildFeatures { compose = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    testOptions { unitTests.isIncludeAndroidResources = true }
    sourceSets.getByName("test").resources.srcDir("schemas")
    sourceSets.getByName("test").java.srcDir("src/sharedTest/java")
    sourceSets.getByName("androidTest").java.srcDir("src/sharedTest/java")
    sourceSets.getByName("androidTest").assets.srcDirs("schemas", "src/test/resources")
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
// Both variants export the same Room schema. Serialize first generation to avoid a partial JSON read.
tasks.matching { it.name == "kspReleaseKotlin" }.configureEach { mustRunAfter("kspDebugKotlin") }
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.12.01"))
    implementation("androidx.activity:activity-compose:1.12.2")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.exifinterface:exifinterface:1.4.2")
    ksp("androidx.room:room-compiler:2.8.5")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("androidx.test:core:1.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2025.12.01"))
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.4.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// Use Maven Central's canonical endpoint for Robolectric's runtime downloads.
tasks.withType<Test>().configureEach {
    systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
}

// Scan source and textual resources without rejecting legitimate Unicode or opening binary assets.
val verifySourceEncoding by tasks.registering {
    val sources = fileTree("src") {
        include("**/*.kt", "**/*.java", "**/*.xml", "**/*.json", "**/*.txt", "**/*.html", "**/*.properties")
    }
    inputs.files(sources)
    doLast {
        val continuation = "[\u0080-\u00BF€‚ƒ„…†‡ˆ‰Š‹ŒŽ‘’“”•–—˜™š›œžŸ]"
        val mojibake = Regex("[\uFFFD\u0080-\u009F]|\u00C2$continuation|\u00C3$continuation|" +
            "\u00E2$continuation$continuation|\u00F0$continuation$continuation$continuation|\u00EF\u00BB\u00BF")
        val invalid = sources.files.sorted().flatMap { file ->
            val path = file.relativeTo(projectDir)
            try {
                val text = Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(file.readBytes()))
                text.lineSequence().mapIndexedNotNull { index, line ->
                    if (mojibake.containsMatchIn(line)) "$path:${index + 1}: suspected mojibake" else null
                }.toList()
            } catch (_: CharacterCodingException) {
                listOf("$path: invalid UTF-8")
            }
        }
        check(invalid.isEmpty()) { invalid.joinToString("\n") }
    }
}
tasks.named("preBuild") { dependsOn(verifySourceEncoding) }
