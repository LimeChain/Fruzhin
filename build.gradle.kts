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

    // TODO: Publish imported packages to mvnrepository and import them
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))

    // JSON-RPC dependencies
    // Needed for strange error when starting up json rpc server
    // See: https://github.com/briandilley/jsonrpc4j/issues/280
    implementation("javax.jws:javax.jws-api:1.1")

    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // Websockets
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    implementation("org.java-websocket:Java-WebSocket:1.5.3")

}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}