plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.pushplatform.sdk"
    compileSdk = 34

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("proguard-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")

    // EncryptedSharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // HTTP Client
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Firebase Messaging
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("androidx.test:core:1.5.0")
}

val documentationJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(rootProject.file("docs/API_REFERENCE.md"))
    from(rootProject.file("README.md"))
    from(rootProject.file("LICENSE"))
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifact(documentationJar)

                groupId = "com.pushplatform"
                artifactId = "sdk-android"
                version = "1.0.0"

                pom {
                    name.set("PushPilot Android SDK")
                    description.set("Standalone Android SDK for PushPilot push notifications")
                    url.set("https://github.com/pushplatform/pushpilot-sdk-android")

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            id.set("pushplatform")
                            name.set("Push Platform Team")
                            url.set("https://github.com/pushplatform")
                        }
                    }
                    scm {
                        url.set("https://github.com/pushplatform/pushpilot-sdk-android")
                        connection.set("scm:git:https://github.com/pushplatform/pushpilot-sdk-android.git")
                        developerConnection.set("scm:git:ssh://git@github.com/pushplatform/pushpilot-sdk-android.git")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "releaseStaging"
                url = rootProject.layout.buildDirectory.dir("maven-repository").get().asFile.toURI()
            }
        }
    }
}

