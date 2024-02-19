package ru.reosfire.minestom.minesweper.presentation

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Player
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerBlockBreakEvent
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.network.packet.server.ServerPacket
import ru.reosfire.minestom.minesweper.GameCell
import ru.reosfire.minestom.utils.Index3d
import ru.reosfire.minestom.utils.NumberRenderer
import ru.reosfire.minestom.utils.Size3d
import ru.reosfire.minestom.utils.extensions.minestom.*
import java.time.Duration

const val cellSize = 3
const val yShift = 0

class InstanceGamePresenter(
    private val instance: Instance,
    private val players: List<Player>,
    private val size: Size3d,
) : GamePresenter {
    override val openEventFlow: Flow<Index3d>
        get() = _openEventFlow
    override val flagEventFlow: Flow<Index3d>
        get() = _flagEventFlow

    private val _openEventFlow = MutableSharedFlow<Index3d>(extraBufferCapacity = 10)
    private val _flagEventFlow = MutableSharedFlow<Index3d>(extraBufferCapacity = 10)

    private val numbersRenderer = NumberRenderer()

    private val playersIds = players.map { it.uuid }.toSet()

    override suspend fun renderCell(cell: GameCell, at: Index3d) {
        val location = at.toPoint()

        when (cell) {
            GameCell.Closed -> location.setStone()
            GameCell.Flag -> location.setRedstone()
            is GameCell.Opened -> {
                if (cell.yz == 0 && cell.xy == 0 && cell.xz == 0) {
                    location.setAir()
                } else {
                    location.setWool()

                    numbersRenderer.renderNumber(location addX 1, BlockFace.EAST, cell.yz).send()
                    numbersRenderer.renderNumber(location addX -1, BlockFace.WEST, cell.yz).send()

                    numbersRenderer.renderNumber(location addZ 1, BlockFace.SOUTH, cell.xy).send()
                    numbersRenderer.renderNumber(location addZ -1, BlockFace.NORTH, cell.xy).send()

                    numbersRenderer.renderNumber(location addY 1, BlockFace.TOP, cell.xz).send()
                    numbersRenderer.renderNumber(location addY -1, BlockFace.BOTTOM, cell.xz).send()
                }
            }
        }
    }

    override suspend fun clear() {
        val clearNumbersPackets = numbersRenderer.clearNumbers()
        clearNumbersPackets.send()
    }

    override suspend fun showWin(at: Index3d) {
        for (player in players) {
            player.showTitle(
                Title.title(
                    Component.text("Win!!"),
                    Component.text("Not bruh"),
                    Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1))
                )
            )
        }
    }

    override suspend fun showLoose(at: Index3d) {
        for (player in players) {
            player.showTitle(
                Title.title(
                    Component.text("Lose!!"),
                    Component.text("Bruh"),
                    Title.Times.times(Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1))
                )
            )
        }

        instance.explode(at.toPoint(), 10f)
    }

    override fun showMinesFlagsCount(mines: Int, flags: Int) {
        Audience.audience(players).sendActionBar(Component.text("flags/mines   $flags/$mines"))
    }

    val eventNode = EventNode.all("presenter events")
        .addListener<PlayerBlockBreakEvent> {
            println("block break")
            if (it.player.uuid !in playersIds) return@addListener

            val clickedPoint = it.blockPosition.toIndex3d()
            if (clickedPoint !in size) return@addListener

            runBlocking {
                _openEventFlow.emit(clickedPoint)
            }

            it.isCancelled = true
        }
        .addListener<PlayerBlockInteractEvent> {
            println("interact")
            if (it.player.uuid !in playersIds) return@addListener

            val clickedPoint = it.blockPosition.toIndex3d()
            if (clickedPoint !in size) return@addListener

            if (System.currentTimeMillis() - lastRClick < 100) return@addListener

            runBlocking {
                _flagEventFlow.emit(clickedPoint)
            }

            lastRClick = System.currentTimeMillis()

            it.isCancelled = true
        }

    private var lastRClick = 0L

    private fun List<ServerPacket>.send() {
        for (packetContainer in this@send) {
            packetContainer.send()
        }
    }

    private fun ServerPacket.send() {
        for (player in players) {
            player.playerConnection.sendPacket(this)
        }
    }

    private fun Point.setAir() {
        set(Block.AIR)
    }

    private fun Point.setStone() {
        set(Block.STONE)
    }

    private fun Point.setWool() {
        set(Block.WHITE_WOOL)
    }

    private fun Point.setRedstone() {
        set(Block.REDSTONE_BLOCK)
    }

    private fun Point.set(block: Block) {
        instance.setBlock(this, block)
    }

    private fun Point.toIndex3d() = Index3d(
        (x() / cellSize).toInt(),
        ((y() - yShift) / cellSize).toInt(),
        (z() / cellSize).toInt(),
    )

    private fun Index3d.toPoint() = Pos(
        x * cellSize.toDouble(),
        y * cellSize.toDouble() + yShift,
        z * cellSize.toDouble(),
    )
}
