plugins {
    id("java")
}

group = "org.limechain"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")

    implementation("com.github.briandilley.jsonrpc4j:jsonrpc4j:1.6")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.1")
    // https://mvnrepository.com/artifact/commons-cli/commons-cli
    implementation("commons-cli:commons-cli:1.3.1")


}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}