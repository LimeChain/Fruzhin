plugins {
    id("java")
    id("war")
    id("org.teavm") version "0.10.0"
    id("io.freefair.lombok") version "8.7.1"
}

group = "com.limechain"
version = "0.1.0"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://artifacts.consensys.net/public/maven/maven/")
}

dependencies {
    implementation("org.rocksdb:rocksdbjni:9.4.0")
    compileOnly("org.projectlombok:lombok:1.18.34")
    implementation("org.projectlombok:lombok:1.18.34")

    implementation("org.teavm:teavm-jso-apis:0.10.0")
    implementation("org.teavm:teavm-core:0.10.0")
}

teavm.js {
    addedToWebApp = true
    mainClass = "com.limechain.Main"
    targetFileName = "fruzhin.js"
}


//TODO: Debug only. Remove when doing release build
teavm {
    js {
        sourceMap.set(true)
        debugInformation.set(true)
    }
}