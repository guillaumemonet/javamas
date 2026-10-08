plugins {
    `java-library`
    `maven-publish`
}

group = "fr.eloane"
version = "2.0.0-SNAPSHOT"
description = "Multi agents framework"

repositories {
    mavenCentral()
}

dependencies {
    // JSON codec
    implementation("tools.jackson.core:jackson-databind:3.2.3")

    testImplementation(platform("org.junit:junit-bom:5.13.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.compileJava {
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-serial", "-Xlint:-this-escape"))
}

val examples: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
}

// ./gradlew runExample -Pexample=network.TcpPingPong [--args="..."]
tasks.register<JavaExec>("runExample") {
    group = "application"
    description = "Runs an example, e.g. -Pexample=simple.HelloWorld"
    classpath = examples.runtimeClasspath
    mainClass = providers.gradleProperty("example").map { "fr.eloane.javamas.examples.$it" }
        .orElse("fr.eloane.javamas.examples.simple.HelloWorld")
    standardInput = System.`in`
}

tasks.check {
    dependsOn(tasks.named(examples.classesTaskName))
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name = "JavaMAS"
                description = project.description
                url = "https://github.com/guillaumemonet/javamas"
                licenses {
                    license {
                        name = "MIT"
                        url = "https://opensource.org/licenses/MIT"
                        distribution = "repo"
                    }
                }
                scm {
                    connection = "scm:git:git@github.com:guillaumemonet/javamas.git"
                    url = "https://github.com/guillaumemonet/javamas"
                }
            }
        }
    }
}
