package com.chesko.stream_pro.core.utils

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CastRequest(val url: String, val name: String)

class LocalCastServer(private val onPlayRequested: (url: String, name: String) -> Unit) {
    private var server: NettyApplicationEngine? = null

    fun start(port: Int = 8080) {
        server = embeddedServer(Netty, port = port) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
            routing {
                post("/play") {
                    val request = call.receive<CastRequest>()
                    onPlayRequested(request.url, request.name)
                    call.respond(mapOf("status" to "ok"))
                }
                get("/ping") {
                    call.respond(mapOf("status" to "pong"))
                }
            }
        }.start(wait = false)
    }

    fun stop() {
        server?.stop(1000, 2000)

        server = null
    }
}
