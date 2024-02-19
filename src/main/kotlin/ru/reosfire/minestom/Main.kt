package ru.reosfire.minestom

import net.minestom.server.MinecraftServer
import net.minestom.server.entity.GameMode
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.instance.Instance
import net.minestom.server.world.DimensionType
import ru.reosfire.minestom.utils.applyResourcePackPrompting
import ru.reosfire.minestom.minesweper.Game
import ru.reosfire.minestom.minesweper.GameSettings
import ru.reosfire.minestom.minesweper.presentation.InstanceGamePresenter
import ru.reosfire.minestom.utils.extensions.minestom.addListener
import ru.reosfire.minestom.utils.extensions.minestom.useConstantLighting
import ru.reosfire.minestom.utils.extensions.minestom.useHarmlessExplosions

const val IP = "127.0.0.1"

private object Router {
    private val instanceManager = MinecraftServer.getInstanceManager()

    private fun createGameInstance(): Instance {
        return instanceManager.createInstanceContainer(DimensionType.OVERWORLD).apply {
            useConstantLighting()
            useHarmlessExplosions()
        }
    }

    val PLAYERS_ROUTER_NODE = EventNode.all("routing node")
        .addListener<AsyncPlayerConfigurationEvent> { event ->
            val instance = createGameInstance()
            event.spawningInstance = instance

            val presenter = InstanceGamePresenter(
                instance = instance,
                players = listOf(event.player),
                size = GameSettings.DEFAULT.size,
            )

            MinecraftServer.getGlobalEventHandler().addChild(presenter.eventNode)

            Game(presenter)
        }
        .addListener<PlayerSpawnEvent> { event ->
            with(event.player) {
                gameMode = GameMode.CREATIVE
                permissionLevel = 4
                isFlying = true
            }
        }
}

fun main() {
    val minecraftServer = MinecraftServer.init()

    MinecraftServer.getGlobalEventHandler().addChild(Router.PLAYERS_ROUTER_NODE)

    applyResourcePackPrompting()

    minecraftServer.start(IP, 25565)
}
