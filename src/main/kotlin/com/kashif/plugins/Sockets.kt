package com.kashif.plugins

import com.kashif.Connection
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Duration
import java.util.*

@Serializable
data class Message(
    val sender: String = "",
    val message: String = ""
)


fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }
    routing {
        val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())
        webSocket("/chat/{name}") {
            val name = call.parameters["name"]
            if (name != null) {
                val thisConnection = Connection(this, call.parameters["name"]!!)
                connections += thisConnection
                val msg = Message(sender = "Server", message = "Welcome to the channel $name")
                val encodedMessage = json.encodeToString(Message.serializer(), msg)
                thisConnection.session.send(encodedMessage)
                try {
                    for (frame in incoming) {
                        frame as? Frame.Text ?: continue
                        val receivedText = frame.readText()
                        val decode = json.decodeFromString<Message>(receivedText)
                        if(decode.message.isNotEmpty() && decode.sender.isNotEmpty()) {
                            connections.forEach {
                                it.session.send(receivedText)
                            }
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

