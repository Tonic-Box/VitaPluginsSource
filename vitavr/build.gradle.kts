import java.nio.file.Paths

plugins {
    java
}

group = "com.tonic.vitavr"
version = "1.0-SNAPSHOT"

base {
    archivesName.set("VitaVR")
}

val apiVersion: String by project
val lwjglVersion = "3.3.2"
val sideloadedPluginsDir: File = Paths.get(System.getProperty("user.home"), ".runelite", "sideloaded-plugins").toFile()

val shade: Configuration by configurations.creating {
    isTransitive = false
}

dependencies {
    compileOnly("org.lwjgl:lwjgl:$lwjglVersion")
    compileOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-openxr:$lwjglVersion")
    shade("org.lwjgl:lwjgl-openxr:$lwjglVersion")
    shade("org.lwjgl:lwjgl-openxr:$lwjglVersion:natives-windows")

    testImplementation("net.runelite:client:$apiVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<Jar>("buildPlugin") {
    group = "build"
    description = "Builds the plugin JAR with OpenXR bundled and moves it to sideloaded-plugins for easy refreshing"
    dependsOn(tasks.named("classes"))

    from(sourceSets.main.get().output)
    from({ shade.map { zipTree(it) } }) {
        exclude("META-INF/MANIFEST.MF", "META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA", "META-INF/versions/**", "**/module-info.class")
    }

    archiveBaseName.set("VitaVR")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    destinationDirectory.set(file(sideloadedPluginsDir))
    doFirst {
        sideloadedPluginsDir.mkdirs()
    }
}
