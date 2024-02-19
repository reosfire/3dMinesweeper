package ru.reosfire.minestom.utils

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import net.kyori.adventure.resource.ResourcePackInfo
import net.minestom.server.MinecraftServer
import net.minestom.server.event.EventListener
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import ru.reosfire.minestom.IP
import java.io.InputStream
import java.net.URI
import java.security.MessageDigest
import java.util.*

fun applyResourcePackPrompting(
    endpointRoute: String = "resource_pack.zip",
    resourcePath: String = "/resource_pack.zip",
) {
    val port = 25500

    createWebApp(
        ip = IP,
        port = port,
        endpointRoute = endpointRoute,
        resourcePath = resourcePath
    )

    RPPromptEventsHandler(
        rpUri = "http://$IP:$port/$endpointRoute",
        resourcePath = resourcePath,
    )


    MinecraftServer.getGlobalEventHandler().addChild(
        EventNode
            .all("rp prompting node")
            .addListener(
                RPPromptEventsHandler(
                    rpUri = "http://$IP:$port/$endpointRoute",
                    resourcePath = resourcePath,
                )
            )
    )
}


private class RPPromptEventsHandler(
    private val rpUri: String,
    private val resourcePath: String
): EventListener<AsyncPlayerConfigurationEvent> {
    val hash = calculateHash()

    private fun calculateHash(): ByteArray {
        val digest = MessageDigest.getInstance("SHA-1")
        javaClass.getResourceAsStream(resourcePath)!!.use {
            digest.update(javaClass.getResourceAsStream(resourcePath)!!)
        }
        return digest.digest()
    }

    override fun eventType(): Class<AsyncPlayerConfigurationEvent> = AsyncPlayerConfigurationEvent::class.java

    @OptIn(ExperimentalStdlibApi::class)
    override fun run(event: AsyncPlayerConfigurationEvent): EventListener.Result {
        event.player.sendResourcePacks(
            ResourcePackInfo.resourcePackInfo(UUID.randomUUID(), URI.create(rpUri), hash.toHexString()))

        return EventListener.Result.SUCCESS
    }
}

private fun MessageDigest.update(stream: InputStream, bufferSize: Int = 1024) {
    val buffer = ByteArray(bufferSize)
    do {
        val read = stream.read(buffer)
        update(buffer, 0, read)
    } while (read == bufferSize)
}

//Web server

private fun createWebApp(
    port: Int,
    ip: String,
    endpointRoute: String,
    resourcePath: String,
): ApplicationEngine {
    return embeddedServer(
        factory = Netty,
        port = port,
        host = ip
    ) {
        configureRouting(
            endpointRoute = endpointRoute,
            resourcePath = resourcePath,
        )
    }.start(wait = false)
}

private fun Application.configureRouting(
    endpointRoute: String,
    resourcePath: String,
) {
    routing {
        get("/$endpointRoute") {
            call.respondOutputStream {
                javaClass.getResourceAsStream(resourcePath)?.copyTo(this)
            }
        }
    }
}
