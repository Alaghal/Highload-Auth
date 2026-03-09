package alag.example.auth

import org.springframework.boot.fromApplication
import org.springframework.boot.with


fun main(args: Array<String>) {
	fromApplication<AuthenticationApplication>().with(TestcontainersConfiguration::class).run(*args)
}
