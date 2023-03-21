plugins {
    id("java")
    id("io.freefair.lombok") version "6.6.1"
    // Can't use newest 3.x version of Spring Boot as it fails to build with jsonprc4j
    // See: https://stackoverflow.com/questions/74760350/application-fails-on-run-in-a-new-release-of-springboot-3-0-0
    id("org.springframework.boot") version "2.7.3"
    id("io.spring.dependency-management") version "1.1.0"
    id("application")
    id("checkstyle")
}

application {
    mainClass.set("com.limechain.Main")
}

checkstyle {
    config = resources.text.fromFile("checkstyle.xml")
}

group = "com.limechain"
version = "1.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
    maven { setUrl("https://jcenter.bintray.com/") }
}

dependencies {
    implementation("org.rocksdb:rocksdbjni:7.8.3")
    compileOnly("org.projectlombok:lombok:1.18.8")
    implementation("org.projectlombok:lombok:1.18.22")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation("org.mockito:mockito-core:5.1.1")

    // Jackson
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.2")

    // CLI
    implementation("commons-cli:commons-cli:1.3.1")

    // JSON-RPC dependencies
    // TODO: Publish imported packages to mvnrepository and import them
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // Needed for strange error when starting up json rpc server
    // See: https://github.com/briandilley/jsonrpc4j/issues/280
    implementation("javax.jws:javax.jws-api:1.1")

    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // Websockets
    implementation("org.springframework.boot:spring-boot-starter-websocket")

//    implementation("com.github.peergos:nabu:-SNAPSHOT")
    implementation("com.github.multiformats:java-multiaddr:v1.4.11")
    implementation("com.github.peergos:jvm-libp2p:0.9.7")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
    implementation("org.apache.commons:commons-lang3:3.6")
    implementation("org.apache.commons:commons-collections4:4.1")
    implementation("com.github.dnsjava:dnsjava:v3.5.2")
    implementation("com.offbynull.portmapper:portmapper:2.0.6")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.21")
    implementation("com.h2database:h2:2.1.214")


}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}