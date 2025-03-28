plugins {
      id("com.android.application")
      id("org.jetbrains.kotlin.android")
    }

    android {
      namespace = "com.example.osmmapapp"
      compileSdk = 34

      defaultConfig {
        applicationId = "com.example.osmmapapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
      }
      kotlinOptions {
        jvmTarget = "17"
      }
      buildFeatures {
        viewBinding = true
      }
    }

    dependencies {

      implementation("androidx.core:core-ktx:1.12.0")
      implementation("androidx.appcompat:appcompat:1.6.1")
      implementation("com.google.android.material:material:1.11.0")
      implementation("androidx.constraintlayout:constraintlayout:2.1.4")
      testImplementation("junit:junit:4.13.2")
      androidTestImplementation("androidx.test.ext:junit:1.1.5")
      androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

      // OSMdroid
      implementation("org.osmdroid:osmdroid-android:6.1.16")

      // KML/KMZ parsing (consider alternatives if this doesn't work well)
      implementation("org.mapsforge:mapsforge-kml:0.17.0")

      // SHP parsing (consider alternatives if this doesn't work well)
      implementation("com.github.geoxol:shp-reader:1.0.0")

      // Permissions
      implementation("androidx.activity:activity-ktx:1.8.2")
      implementation("androidx.fragment:fragment-ktx:1.6.2")
    }
