package com.kashif.plugins

import com.kashif.Connection
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import java.util.*

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())
        webSocket("/chat/{name}") {
            val name = call.parameters["name"]
            if(name != null) {
                val thisConnection = Connection(this, call.parameters["name"]!!)
                connections += thisConnection
                try {
                    for (frame in incoming) {
                        frame as? Frame.Text ?: continue
                        val receivedText = frame.readText()
                        val textWithUsername = "[${thisConnection.name}]: $receivedText"
                        connections.forEach {
                            it.session.send(textWithUsername)
                        }
                    }
                } catch (e: Exception) {
                    println(e.localizedMessage)
                } finally {
                    println("Removing $thisConnection!")
                    connections -= thisConnection
                }
            }
        }
    }
}

