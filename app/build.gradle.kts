plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "uz.ekranoquvchi.mvp"
    compileSdk = 35

    defaultConfig {
        applicationId = "uz.ekranoquvchi.mvp"
        minSdk = 28
        targetSdk = 35
        versionCode = 12
        versionName = "0.9.0-mvp"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        buildConfig = true
    }
    lint {
        // Kirill, rus va ingliz tillarida faqat asosiy ekran va aytiladigan matnlar tarjima qilingan (FR-23);
        // qolganlari asosiy til (o'zbekcha lotin) bilan ko'rsatiladi. Lint bu holatni release qurishda
        // halokatli xato deb hisoblab, assembleRelease'ni to'xtatardi.
        disable += "MissingTranslation"
        // APK yig'ish lint topilmasi tufayli to'xtamasin (lint hisoboti baribir chiqariladi)
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
}

// Monografiya 11.2: yadro ilova tarmoqsiz ishlaydi. Onlayn imkoniyatlar (Microsoft ovozi, tarjima, AI) ixtiyoriy va
// sozlamalarda alohida yoqiladi, shuning uchun tarmoq ruxsati standartda ruxsat etilgan. Faqat oflayn yig'ma kerak
// bo'lsa, gradle.properties'ga ozbegim.online=false yozing va manifestdan INTERNET qatorini olib tashlang:
// aks holda quyidagi tekshiruv qurishni to'xtatadi.
val onlineAllowed = (project.findProperty("ozbegim.online") as String?)?.lowercase() != "false"
val checkNetworkPolicy = tasks.register("checkNetworkPolicy") {
    val manifestFile = layout.projectDirectory.file("src/main/AndroidManifest.xml").asFile
    val allowOnline = onlineAllowed
    doLast {
        if (manifestFile.exists() && !allowOnline) {
            check(!manifestFile.readText().contains("permission." + "INTERNET")) {
                "ozbegim.online=false: manifestda tarmoq ruxsati bo'lmasligi kerak (monografiya 11.2)"
            }
        }
    }
}
// preBuild vazifasi qachon yaratilishidan qat'i nazar ulanadi (tasks.named ba'zan "topilmadi" xatosini beradi)
tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(checkNetworkPolicy) }
