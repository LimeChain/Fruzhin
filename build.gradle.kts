plugins {
    id("java")
    id("io.freefair.lombok") version "8.6"
    id("war")
    id("org.teavm") version "0.9.2"
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

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation("org.javatuples:javatuples:1.2")

    implementation("com.github.LimeChain:nabu:0.7.8")

    implementation("org.teavm:teavm-jso-apis:0.9.2")

}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
    jvmArgs("-Dnet.bytebuddy.experimental=true")
}

teavm.js {
    addedToWebApp = true
    mainClass = "com.limechain.Main"
    targetFileName = "fruzhin.js"
}