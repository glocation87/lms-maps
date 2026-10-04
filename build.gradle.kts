plugins {
    java
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "studio.chef"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.129-stable")

    testImplementation("io.papermc.paper:paper-api:26.2.build.129-stable")
    // a fake server, so block names in themes can be checked against the real registry
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.2:4.116.1")
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 25
    }

    test {
        useJUnitPlatform()
        // MockBukkit's ByteBuddy still uses sun.misc.Unsafe
        jvmArgs("--sun-misc-unsafe-memory-access=allow")
    }

    runServer {
        minecraftVersion("26.2")
        jvmArgs("-Dcom.mojang.eula.agree=true")
        // 25565 test-server, 25566 Nature7, 25567 Combat7
        args("--port", "25568")
    }

    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
