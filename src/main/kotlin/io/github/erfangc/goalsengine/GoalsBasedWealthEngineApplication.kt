package io.github.erfangc.goalsengine

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GoalsBasedWealthEngineApplication

fun main(args: Array<String>) {
	runApplication<GoalsBasedWealthEngineApplication>(*args)
}
