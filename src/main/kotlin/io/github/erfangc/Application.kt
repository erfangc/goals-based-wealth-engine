package io.github.erfangc

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class GoalsBasedWealthEngineApplication

fun main(args: Array<String>) {
	runApplication<GoalsBasedWealthEngineApplication>(*args)
}
