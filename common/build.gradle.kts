plugins {
	id("ore-conventions")
	id("java-library")
}

dependencies {
	implementation(platform(libs.spring.boot.dependencies))
	implementation("org.springframework.boot:spring-boot")
	implementation("org.springframework.boot:spring-boot-autoconfigure")
	api(platform("software.amazon.awssdk:bom:2.51.3"))
	api("software.amazon.awssdk:sns")
	testImplementation(libs.junit.jupiter)
}
