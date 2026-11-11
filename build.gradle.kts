subprojects {
    apply(plugin = "java")

    val apiVersion: String by project
    val lombokVersion: String by project

    repositories {
        mavenLocal()
        maven {
            url = uri("https://repo.runelite.net")
            content {
                includeGroupByRegex("net\\.runelite.*")
            }
        }
        mavenCentral()
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    dependencies {
        "compileOnly"("net.runelite:client:$apiVersion")
        "compileOnly"("com.tonic:base-api:$apiVersion")
        "compileOnly"("com.tonic:api:$apiVersion")
        "compileOnly"("org.projectlombok:lombok:$lombokVersion")
        "annotationProcessor"("org.projectlombok:lombok:$lombokVersion")
    }
}
