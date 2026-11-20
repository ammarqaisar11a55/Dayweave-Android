import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val appVersion = providers.gradleProperty("dayweave.version").get()

/** 1.2.3 becomes 10203, so every release installs over the one before it. */
fun versionCodeOf(version: String): Int {
    val (major, minor, patch) = version.substringBefore('-').split('.').map(String::toInt)
    return major * 10_000 + minor * 100 + patch
}

// Release signing comes from an untracked keystore.properties file, or from the environment in CI.
// Updates only install over a build signed with the same key, so this key must never change.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}
fun signingValue(key: String, variable: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv(variable)

android {
    namespace = "io.github.ammarqaisar11a55.dayweave"
    compileSdk = 35
    ndkVersion = "26.1.10909125"

    defaultConfig {
        applicationId = "io.github.ammarqaisar11a55.dayweave"
        minSdk = 26
        targetSdk = 35
        versionCode = versionCodeOf(appVersion)
        versionName = appVersion
        // Where updates come from. -PupdateReleases points a test build at a local copy of the layout.
        val releases = providers.gradleProperty("updateReleases")
            .getOrElse("https://github.com/ammarqaisar11a55/Dayweave-Android/releases")
        buildConfigField("String", "UPDATE_RELEASES", "\"$releases\"")
    }

    signingConfigs {
        val storeFile = signingValue("storeFile", "DAYWEAVE_KEYSTORE")
        if (storeFile != null) {
            create("release") {
                this.storeFile = rootProject.file(storeFile)
                storePassword = signingValue("storePassword", "DAYWEAVE_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "DAYWEAVE_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "DAYWEAVE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
    lint {
        abortOnError = true
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
    }
}

// The shared Rust core, built for each Android architecture by the NDK. Pass -PrustAbis=x86_64 to
// build only for the emulator while developing.
val rustTargets = mapOf("arm64-v8a" to "aarch64-linux-android", "x86_64" to "x86_64-linux-android")
val selectedAbis = providers.gradleProperty("rustAbis").map { it.split(',') }.getOrElse(rustTargets.keys.toList())
val rustLibraries = layout.buildDirectory.dir("rustJniLibs")
val rustTargetDir = rootProject.layout.buildDirectory.dir("rust")

val buildRust by tasks.registering {
    description = "Builds the Rust core for Android"
    inputs.dir(rootProject.file("native/src"))
    inputs.file(rootProject.file("native/Cargo.toml"))
    inputs.dir(rootProject.file("core/crates/dayweave-core/src"))
    inputs.property("abis", selectedAbis)
    outputs.dir(rustLibraries)
    doLast {
        val bin = android.ndkDirectory.resolve("toolchains/llvm/prebuilt/linux-x86_64/bin")
        selectedAbis.forEach { abi ->
            val triple = rustTargets.getValue(abi)
            val variable = triple.uppercase().replace('-', '_')
            val clang = bin.resolve("$triple${android.defaultConfig.minSdk}-clang").path
            exec {
                workingDir = rootProject.file("native")
                commandLine("cargo", "build", "--release", "--target", triple)
                environment("CARGO_TARGET_DIR", rustTargetDir.get().asFile.path)
                environment("CARGO_TARGET_${variable}_LINKER", clang)
                environment("CC_${triple.replace('-', '_')}", clang)
                environment("AR_${triple.replace('-', '_')}", bin.resolve("llvm-ar").path)
            }
            copy {
                from(rustTargetDir.get().file("$triple/release/libdayweave.so"))
                into(rustLibraries.get().dir(abi))
            }
        }
    }
}

android.sourceSets["main"].jniLibs.srcDir(rustLibraries)
tasks.named("preBuild") { dependsOn(buildRust) }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
