plugins {
    java
}

group = "com.tonic.vitaarceuuslibrary"
version = "1.0-SNAPSHOT"

base {
    archivesName.set("VitaArceuusLibrary")
}

val apiVersion: String by project

dependencies {
    testImplementation("net.runelite:client:$apiVersion")
    testImplementation("com.tonic:base-api:$apiVersion")
    testImplementation("com.tonic:api:$apiVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.build {
    finalizedBy("deployToRuneLite")
}

tasks.register<Copy>("deployToRuneLite") {
    description = "Copy built JAR to RuneLite sideloaded-plugins directory"

    dependsOn(tasks.jar)

    from(tasks.jar.get().archiveFile)
    into("${System.getProperty("user.home")}/.runelite/sideloaded-plugins")

    doLast {
        println("Deployed ${tasks.jar.get().archiveFileName.get()} to RuneLite sideloaded-plugins")
    }
}
