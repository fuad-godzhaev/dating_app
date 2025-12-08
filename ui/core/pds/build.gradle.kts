plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.apiguave.pds"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        targetSdk = 34

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        ndk {
            // Node.js supports these ABIs
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86_64"))
        }

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                arguments += listOf(
                    "-DANDROID_STL=c++_shared"
                )
            }
        }
    }

    buildTypes {
        create("mock") {
            initWith(getByName("debug"))
        }
        release {
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

    kotlinOptions {
        jvmTarget = "1.8"
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.18.1"
        }
    }

    sourceSets {
        getByName("main") {
            // JNI libraries location
            jniLibs.srcDirs("src/main/jniLibs")
            // Assets with Node.js project
            assets.srcDirs("src/main/assets")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// Task to copy PDS source code to assets
tasks.register<Copy>("copyPdsToAssets") {
    description = "Copies PDS source code to Android assets"

    // Source: the PDS service directory
    from("${project.rootDir}/../pds/pds-main/service") {
        include("index.js", "package.json")
    }

    // Copy .env file from pds-main
    from("${project.rootDir}/../pds/pds-main") {
        include(".env")
    }

    // Destination: assets/nodejs-project
    into("${projectDir}/src/main/assets/nodejs-project")

    doLast {
        println("PDS source copied to assets/nodejs-project")
    }
}

// Task to install Node.js dependencies
tasks.register<Exec>("installNodeDependencies") {
    description = "Installs Node.js dependencies for PDS"
    dependsOn("copyPdsToAssets")

    workingDir("${projectDir}/src/main/assets/nodejs-project")

    // Detect npm command based on OS
    val npmCommand = if (System.getProperty("os.name").lowercase().contains("windows")) {
        "npm.cmd"
    } else {
        "npm"
    }

    commandLine(npmCommand, "install", "--omit=dev", "--production")

    // Make task fail gracefully if npm is not available
    isIgnoreExitValue = false

    doFirst {
        val nodeProjectDir = file("${projectDir}/src/main/assets/nodejs-project")
        if (!nodeProjectDir.exists()) {
            throw GradleException("nodejs-project directory not found. Run copyPdsToAssets first.")
        }

        // Check if npm is available
        try {
            val npmCheck = ProcessBuilder(npmCommand, "--version")
                .redirectErrorStream(true)
                .start()
            npmCheck.waitFor()
            if (npmCheck.exitValue() != 0) {
                throw GradleException("npm is not installed or not in PATH. Please install Node.js from https://nodejs.org/")
            }
        } catch (e: Exception) {
            throw GradleException("npm is not installed or not in PATH. Please install Node.js from https://nodejs.org/\nError: ${e.message}")
        }
    }

    doLast {
        println("Node.js dependencies installed in assets/nodejs-project")
    }
}

// Make preBuild depend on our tasks
tasks.named("preBuild") {
    // Temporarily disabled - uncomment when Node.js is installed
    // dependsOn("installNodeDependencies")
    dependsOn("copyPdsToAssets")
}

// Clean task to remove generated assets
tasks.named("clean") {
    doLast {
        delete("${projectDir}/src/main/assets/nodejs-project")
    }
}
