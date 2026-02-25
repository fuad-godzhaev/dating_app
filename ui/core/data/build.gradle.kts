plugins {
    kotlin("jvm") version "1.9.0"
}

group = "dating.app"
version = "unspecified"



dependencies {
    implementation("androidx.room:room-compiler:2.8.4")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}