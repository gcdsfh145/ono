import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

plugins {
    id("build-logic.android.application")
    alias(libs.plugins.protobuf)
    alias(libs.plugins.serialization)
    alias(libs.plugins.android.application)
    id("com.google.devtools.ksp") version "2.0.20-1.0.25"
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "moe.ono"
    compileSdk = 35

    val buildUUID = UUID.randomUUID()
    println("buildUUID: $buildUUID")

    defaultConfig {
        applicationId = "moe.ono"
        buildConfigField("String", "BUILD_UUID", "\"${buildUUID}\"")
        buildConfigField("String", "TAG", "\"[ono]\"")
        buildConfigField("long", "BUILD_TIMESTAMP", "${System.currentTimeMillis()}L")
        ndk {
            abiFilters.addAll(arrayOf("arm64-v8a"))
        }
    }


    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        // libxposed API uses META-INF/xposed
        resources.excludes.addAll(
            arrayOf(
                "kotlin/**",
                "**.bin",
                "kotlin-tooling-metadata.json"
            )
        )

        resources {
            merges += "META-INF/xposed/*"
            excludes += "**"
        }
    }


    android.applicationVariants.all {
        outputs.all {
            if (this is com.android.build.gradle.internal.api.ApkVariantOutputImpl) {
                val config = project.android.defaultConfig
                val versionName = config.versionName
                this.outputFileName = "ONO-RELEASE-${versionName}.apk"
            }
        }
    }


    androidResources {
        additionalParameters += arrayOf(
            "--allow-reserved-package-id",
            "--package-id", "0x54"
        )
    }

}



fun String.capitalizeUS(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
}


fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("MMddHHmm", Locale.getDefault())
    return sdf.format(Date())
}


fun getShortGitRevision(): String {
    val command = "git rev-parse --short HEAD"
    val processBuilder = ProcessBuilder(*command.split(" ").toTypedArray())
    val process = processBuilder.start()

    val output = process.inputStream.bufferedReader().use { it.readText() }
    val exitCode = process.waitFor()

    return if (exitCode == 0) {
        output.trim()
    } else {
        "no_commit"
    }
}

val adb: String = androidComponents.sdkComponents.adb.get().asFile.absolutePath
val packageName = "com.tencent.mobileqq"
val killQQ = tasks.register<Exec>("killQQ") {
    group = "ono"
    commandLine(adb, "shell", "am", "force-stop", packageName)
    isIgnoreExitValue = true
}


androidComponents.onVariants { variant ->
    val variantCapped = variant.name.capitalizeUS()
    task("install${variantCapped}AndRestartQQ") {
        group = "ono"
        dependsOn(":app:install$variantCapped")
        finalizedBy(killQQ)
    }
}

kotlin {
    sourceSets.configureEach {
        kotlin.srcDir("$buildDir/generated/ksp/$name/kotlin/")
    }
    sourceSets.main {
        kotlin.srcDir(File(rootDir, "libs/util/ezxhelper/src/main/java"))
    }
}

androidComponents.onVariants { variant ->
    val variantCapped = variant.name.capitalizeUS()
    val installAndRestartTask = tasks.named("install${variantCapped}AndRestartQQ")

    afterEvaluate {
        tasks.findByName("assemble${variantCapped}")?.let { assembleTask ->
            assembleTask.finalizedBy(installAndRestartTask)
        } ?: println("Task assemble${variantCapped} not found.")
    }
}

protobuf {
    protoc {
        artifact = libs.google.protobuf.protoc.get().toString()
    }
    plugins {
        generateProtoTasks {
            all().forEach {
                it.builtins {
                    create("java") {
                        option("lite")
                    }
                }
            }
        }
    }

}

configurations.configureEach {
    exclude(group = "androidx.appcompat", module = "appcompat")
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout) {
        exclude("androidx.appcompat", "appcompat")
    }

    implementation(libs.kotlinx.io.jvm)

    implementation(libs.dexkit)
    compileOnly(projects.libs.stub.qqStub)
    implementation(libs.hiddenapibypass)
    implementation(libs.gson)


    implementation(ktor("serialization", "kotlinx-json"))
    implementation(grpc("protobuf", "1.62.2"))

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.mmkv)

    implementation(projects.libs.util.libxposed.service)

    // Xposed API 89
    compileOnly(libs.xposed)

    // LSPosed API 100
    compileOnly(projects.libs.util.libxposed.api)

    implementation(libs.dexlib2)
    // ImmutableMethodImplementation
    implementation(libs.google.guava)

    implementation(libs.google.protobuf.java)
    implementation(libs.kotlinx.serialization.protobuf)

    implementation(libs.sealedEnum.runtime)
    ksp(libs.sealedEnum.ksp)

    ksp(projects.libs.util.annotationScanner)

    // Material Preference
    implementation(libs.material.preference)
    implementation(libs.dev.appcompat)
    implementation(libs.recyclerview)

    implementation(libs.material.dialogs.core)
    implementation(libs.material.dialogs.input)

    // Preference
    implementation(libs.preference)

    // fastjson2
    implementation(libs.fastjson2)

    // xView
    implementation(projects.libs.ui.xView)


    implementation(libs.glide)

    implementation(libs.byte.buddy)

    implementation(libs.dalvik.dx)

    implementation(libs.okhttp3.okhttp)

    implementation(libs.markdown.core)

    implementation(libs.blurview)
}