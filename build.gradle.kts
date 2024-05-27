plugins {
    id("java")
    id("io.freefair.lombok") version "8.6"
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.5"
    id("application")
}

application {
    mainClass.set("com.limechain.Main")
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
    implementation("net.openhft:zero-allocation-hashing:0.16")
    implementation("org.rocksdb:rocksdbjni:9.1.1")
    compileOnly("org.projectlombok:lombok:1.18.32")
    implementation("org.projectlombok:lombok:1.18.32")
    implementation("org.web3j:crypto:4.11.3")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
    testImplementation("org.mockito:mockito-core:5.12.0")

    // CLI
    implementation("commons-cli:commons-cli:1.7.0")

    // TODO: Publish imported packages to mvnrepository and import them
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // Websockets
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    implementation("org.java-websocket:Java-WebSocket:1.5.6")
    implementation("org.javatuples:javatuples:1.2")

    implementation("com.github.luben:zstd-jni:1.5.6-3")

    // Prometheus
    implementation("io.prometheus:prometheus-metrics-core:1.3.0")
    implementation("io.prometheus:prometheus-metrics-instrumentation-jvm:1.3.0")
    implementation("io.prometheus:prometheus-metrics-exporter-httpserver:1.3.1")

    // NOTE:
    //  We implicitly rely on Nabu's transitive dependency on Netty's public interfaces.
    //  We could explicitly include Netty ourselves, but we prefer to make sure we use the same version as Nabu.

    // NOTE:
    //  For jitpack's syntax for GitHub version resolution, refer to: https://docs.jitpack.io/

    // Nabu
//    implementation("com.github.LimeChain:nabu:master-SNAPSHOT") // Uncomment for "most-recent on the master branch"
    implementation("com.github.LimeChain:nabu:32f159f413")

    //JSON-RPC
    implementation("com.github.LimeChain:jsonrpc4j:aefaade0c5")

    // Guava
    implementation("com.google.guava:guava:33.2.0-jre")

    // Apache commons
    implementation("commons-io:commons-io:2.16.1")

}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
    jvmArgs("-Dnet.bytebuddy.experimental=true")
}