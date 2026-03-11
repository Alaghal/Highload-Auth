package com.example.authservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AuthenticationApplication

fun main(args: Array<String>) {
	runApplication<AuthenticationApplication>(*args)
}
