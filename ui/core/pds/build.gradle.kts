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

    packagingOptions {
        // Exclude unnecessary files from node_modules to reduce APK size and memory usage
        resources.excludes.addAll(listOf(
            // Exclude documentation and markdown files
            "**/node_modules/**/*.md",
            "**/node_modules/**/*.markdown",
            "**/node_modules/**/README",
            "**/node_modules/**/CHANGELOG",
            "**/node_modules/**/LICENSE",
            "**/node_modules/**/CONTRIBUTING",
            // Exclude source maps
            "**/node_modules/**/*.map",
            // Exclude TypeScript definitions
            "**/node_modules/**/*.d.ts",
            "**/node_modules/**/*.d.ts.map",
            // Exclude test files
            "**/node_modules/**/test/**",
            "**/node_modules/**/tests/**",
            "**/node_modules/**/__tests__/**",
            "**/node_modules/**/*.test.js",
            "**/node_modules/**/*.spec.js",
            // Exclude build artifacts for other platforms
            "**/node_modules/**/build/Debug/**",
            "**/node_modules/**/build/win32-**/**",
            "**/node_modules/**/build/linux-**/**",
            "**/node_modules/**/build/darwin-**/**",
            // Exclude documentation directories
            "**/node_modules/**/docs/**",
            "**/node_modules/**/doc/**",
            "**/node_modules/**/examples/**",
            "**/node_modules/**/example/**",
            // Exclude config files
            "**/node_modules/**/.npmignore",
            "**/node_modules/**/.gitignore",
            "**/node_modules/**/.editorconfig",
            "**/node_modules/**/.eslintrc*",
            "**/node_modules/**/.prettierrc*",
            "**/node_modules/**/tsconfig.json"
        ))
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

// Task to copy PDS source code (without node_modules)
tasks.register<Copy>("copyPdsSourceToAssets") {
    description = "Copies PDS source code to Android assets (without node_modules)"

    // Source: the PDS service directory
    from("${project.rootDir}/../pds/pds-main/service") {
        include("index.js", "package.json")
        // NOTE: node_modules with prebuilt ARM64 binaries should already exist
        // in assets/nodejs-project/ (downloaded from GitHub Actions artifact)
        // This task only updates the source files, not node_modules
        exclude("node_modules/**")
    }

    // Copy .env file from pds-main
    from("${project.rootDir}/../pds/pds-main") {
        include(".env")
    }

    // Destination: assets/nodejs-project
    into("${projectDir}/src/main/assets/nodejs-project")

    doLast {
        println("PDS source copied to assets/nodejs-project")

        // Check if prebuilt node_modules exist
        val nodeModulesDir = file("${projectDir}/src/main/assets/nodejs-project/node_modules")
        if (nodeModulesDir.exists()) {
            println("✓ Prebuilt node_modules found (includes ARM64 binaries)")
        } else {
            println("⚠ WARNING: node_modules not found!")
            println("  You need to download prebuilt modules from GitHub Actions")
            println("  See CI_BUILD_GUIDE.md for instructions")
        }
    }
}

// Task to install Node.js dependencies (pure JS modules only, skip native builds)
tasks.register<Exec>("installNodeDependencies") {
    description = "Installs Node.js dependencies (pure JS only, skipping native builds)"
    dependsOn("copyPdsSourceToAssets")

    workingDir("${projectDir}/src/main/assets/nodejs-project")

    val npmCommand = if (System.getProperty("os.name").lowercase().contains("windows")) {
        "npm.cmd"
    } else {
        "npm"
    }

    // Install with --ignore-scripts to skip native module builds
    commandLine(npmCommand, "install", "--omit=dev", "--ignore-scripts")

    doFirst {
        println("========================================")
        println("Installing Node.js dependencies (pure JS only)")
        println("========================================")
    }

    doLast {
        println("✓ Pure JavaScript dependencies installed")
        println("  Native modules will be built separately using prebuild-for-nodejs-mobile")
        println("========================================")
    }
}

// Task to build native modules for Android using prebuild-for-nodejs-mobile
tasks.register<Exec>("buildNativeModulesForAndroid") {
    description = "Build native modules for Android using prebuild-for-nodejs-mobile"
    dependsOn("installNodeDependencies")

    workingDir("${projectDir}/src/main/assets/nodejs-project")

    val prebuildCommand = if (System.getProperty("os.name").lowercase().contains("windows")) {
        "prebuild-for-nodejs-mobile.cmd"
    } else {
        "prebuild-for-nodejs-mobile"
    }

    // Build for arm64-v8a (most common Android ABI)
    // You can build for other ABIs by running this command multiple times
    val targetAbi = "android-arm64"

    doFirst {
        println("========================================")
        println("Building native modules for Android ($targetAbi)")
        println("========================================")
        println("NOTE: This uses Docker for cross-compilation")
        println("First build may take several minutes to download Docker images...")
        println("")

        // Check if Docker is running
        try {
            val dockerCheck = ProcessBuilder("docker", "ps")
                .redirectErrorStream(true)
                .start()
            dockerCheck.waitFor()
            if (dockerCheck.exitValue() != 0) {
                throw GradleException("Docker is not running. Please start Docker Desktop and try again.")
            }
            println("✓ Docker is running")
        } catch (e: Exception) {
            throw GradleException("Docker is required for prebuild-for-nodejs-mobile.\nPlease install and start Docker Desktop, then try again.")
        }
    }

    // Build native modules for the target ABI
    // Point directly to better-sqlite3 in the flat node_modules structure
    commandLine(prebuildCommand, targetAbi, "node_modules/better-sqlite3")

    // Don't fail the build immediately, let's see what happens
    isIgnoreExitValue = true

    doLast {
        println("========================================")

        // Check if native module was built successfully
        val sqliteModule = file("${workingDir}/node_modules/better-sqlite3/build/Release/better_sqlite3.node")

        if (sqliteModule.exists()) {
            println("✓ Native modules built successfully for $targetAbi!")
            println("✓ better-sqlite3 binary found")
            println("  Size: ${sqliteModule.length()} bytes")
            println("  Location: ${sqliteModule.absolutePath}")
            println("========================================")
        } else {
            println("⚠ Native module not found at expected location")
            println("  Expected: ${sqliteModule.absolutePath}")
            println("")
            println("Checking for better-sqlite3 anywhere in node_modules...")

            val nodeModulesDir = file("${workingDir}/node_modules")
            val foundModules = mutableListOf<File>()
            nodeModulesDir.walk().forEach { file ->
                if (file.name == "better_sqlite3.node") {
                    foundModules.add(file)
                }
            }

            if (foundModules.isNotEmpty()) {
                println("✓ Found better-sqlite3 modules at:")
                foundModules.forEach { println("  - ${it.absolutePath}") }
            } else {
                println("❌ No better-sqlite3 native module found")
                println("")
                println("Troubleshooting:")
                println("1. Check logs above for errors from prebuild-for-nodejs-mobile")
                println("2. Verify Docker is running: docker ps")
                println("3. Try building manually:")
                println("   cd ${workingDir}")
                println("   prebuild-for-nodejs-mobile $targetAbi node_modules/@atproto/pds")
            }
            println("========================================")
        }
    }
}

// Make preBuild depend on our tasks
// NOTE: Native modules are now prebuilt using GitHub Actions CI
// The copyPdsSourceToAssets task copies the entire nodejs-project folder
// (including prebuilt node_modules with ARM64 binaries) from PDS source to assets
// See CI_BUILD_GUIDE.md for instructions on building native modules
tasks.named("preBuild") {
    dependsOn("copyPdsSourceToAssets")
}

// Clean task to remove generated assets
tasks.named("clean") {
    doLast {
        delete("${projectDir}/src/main/assets/nodejs-project")
    }
}
