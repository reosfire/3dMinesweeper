package ru.reosfire.minestom.utils

import net.minestom.server.coordinate.Pos
import net.minestom.server.network.packet.server.play.SpawnEntityPacket
import java.util.*

fun spawnEntityPacket(
    entityId: Int,
    uuid: UUID = UUID.randomUUID(),
    type: Int,
    position: Pos,
    headRotation: Float = 0f,
    data: Int = 0,
    velocityX: Short = 0,
    velocityY: Short = 0,
    velocityZ: Short = 0
) = SpawnEntityPacket(entityId, uuid, type, position, headRotation, data, velocityX, velocityY, velocityZ)