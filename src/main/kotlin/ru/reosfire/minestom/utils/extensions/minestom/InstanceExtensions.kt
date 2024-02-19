package ru.reosfire.minestom.utils.extensions.minestom

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Chunk
import net.minestom.server.instance.Explosion
import net.minestom.server.instance.Instance
import net.minestom.server.instance.LightingChunk
import net.minestom.server.network.packet.server.play.data.LightData
import net.minestom.server.utils.chunk.ChunkSupplier
import java.util.*

fun Instance.explode(point: Point, force: Float) {
    explode(point.x().toFloat(), point.y().toFloat(), point.z().toFloat(), force)
}

fun Instance.useConstantLighting(light: Byte = 0xFF.toByte()) {
    chunkSupplier = ConstantLightChunkSupplier(ByteArray(2048) { light })
}

fun Instance.useHarmlessExplosions() {
    val emptyExplodedBlocks = mutableListOf<Point>()

    setExplosionSupplier { x, y, z, st, _ ->
        object : Explosion(x, y, z, st) {
            override fun prepare(p0: Instance?) = emptyExplodedBlocks
        }
    }
}

private class ConstantLightChunkSupplier(private val sectionLighting: ByteArray): ChunkSupplier {
    override fun createChunk(instance: Instance, x: Int, z: Int): Chunk =
        ConstantLightChunk(sectionLighting, instance, x, z)
}

private class ConstantLightChunk(
    private val sectionLighting: ByteArray,
    instance: Instance,
    x: Int,
    z: Int
) : LightingChunk(instance, x, z) {
    override fun createLightData(): LightData {
        val skyMask = BitSet()
        val blockMask = BitSet()
        val emptySkyMask = BitSet()
        val emptyBlockMask = BitSet()
        val skyLights: MutableList<ByteArray> = ArrayList<ByteArray>()
        val blockLights: MutableList<ByteArray> = ArrayList<ByteArray>()

        for (i in 1..sections.size) {
            skyMask.set(i)
            emptyBlockMask.set(i)
            skyLights.add(sectionLighting)
        }

        return LightData(skyMask, blockMask, emptySkyMask, emptyBlockMask, skyLights, blockLights)
    }
}
