plugins {
    java
}

group = "com.tonic.vitawintertodt"
version = "1.0-SNAPSHOT"

base {
    archivesName.set("VitaWinterTodt")
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
