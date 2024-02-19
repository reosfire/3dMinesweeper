package ru.reosfire.minestom.utils

import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.Metadata
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.network.packet.server.ServerPacket
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket
import net.minestom.server.network.packet.server.play.EntityMetaDataPacket
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min

private const val initialEntityId = 10000
private const val initialBlockData = 1000

class NumberRenderer {

    private val itemsCache: Array<Map<Int, Metadata.Entry<*>>>

    private var currentEntityId = AtomicInteger(initialEntityId)

    init {
        itemsCache = Array(9) { number ->
            val item = ItemStack.builder(Material.SHULKER_SHELL).meta {
                it.customModelData(initialBlockData + number)
            }.build()

            createDataValues(item)
        }
    }

     fun renderNumber(location: Pos, facing: BlockFace, number: Int): List<ServerPacket> {
         if (number !in 0..8) throw IllegalArgumentException("number must be in [0..8]")

         val entityId = currentEntityId.getAndIncrement()
         val result = listOf(
             createSpawnFramePacket(entityId, location, facing),
             EntityMetaDataPacket(entityId, itemsCache[number])
         )

         return result
    }

    fun clearNumbers(): List<ServerPacket> {
        val range = initialEntityId..<currentEntityId.get()
        return createDestroyEntitiesPackets(range.toList())
    }

    private fun createDataValues(item: ItemStack): Map<Int, Metadata.Entry<*>> {
        return mapOf(
            0 to Metadata.Byte(IS_INVISIBLE),
            8 to Metadata.Slot(item)
        )
    }
}

private fun createDestroyEntitiesPackets(
    ids: List<Int>,
    batching: Int = 256,
): List<ServerPacket> {
    val result = mutableListOf<ServerPacket>()

    var start = 0
    while (start < ids.size) {
        val packet = DestroyEntitiesPacket(ids.subList(start, min(start + batching, ids.size)))

        result.add(packet)

        start += batching
    }

    return result
}

private fun createSpawnFramePacket(entityId: Int, position: Pos, facing: BlockFace) = spawnEntityPacket(
    entityId = entityId,
    type = EntityType.ITEM_FRAME.id(),
    position = position,
    data = facing.frameDirection,
)

private val BlockFace.frameDirection: Int
    get() = when(this) {
        BlockFace.BOTTOM -> 0
        BlockFace.TOP -> 1
        BlockFace.NORTH -> 2
        BlockFace.SOUTH -> 3
        BlockFace.WEST -> 4
        BlockFace.EAST -> 5
        else -> throw UnsupportedOperationException()
    }


private const val IS_INVISIBLE: Byte = 0x20
