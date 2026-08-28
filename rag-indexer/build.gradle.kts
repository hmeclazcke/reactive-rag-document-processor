plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
}

dependencies {
    implementation(platform("org.springframework.ai:spring-ai-bom:2.0.1"))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb-reactive")
    implementation("org.springframework.ai:spring-ai-starter-model-transformers")
    implementation("org.springframework.ai:spring-ai-starter-vector-store-qdrant")
    implementation("io.projectreactor:reactor-core")

    testImplementation("io.projectreactor:reactor-test")
}
