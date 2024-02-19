package ru.reosfire.minestom.utils

import java.util.concurrent.ThreadLocalRandom

class Array3d<T>(private val data: Array<Array<Array<T>>>) {

    val size: Size3d
        get() = Size3d(data.size, data.first().size, data.first().first().size)

    val indices: Iterable<Index3d> = object : Iterable<Index3d> {
        override fun iterator() = Index3dRegionIterator(size)
    }

    fun getSlice(layer: Int, plane: Plane) = Slice2d(data, layer, plane)

    fun get(x: Int, y: Int, z: Int): T {
        return data[x][y][z]
    }

    fun set(x: Int, y: Int, z: Int, value: T) {
        data[x][y][z] = value
    }

    operator fun get(at: Index3d): T {
        return data[at.x][at.y][at.z]
    }

    operator fun set(at: Index3d, value: T) {
        data[at.x][at.y][at.z] = value
    }
}

class Slice2d<T>(private val data: Array<Array<Array<T>>>, private val layer: Int, private val plane: Plane) {
    fun get(x: Int, y: Int): T {
        return when (plane) {
            Plane.YZ -> data[layer][x][y]
            Plane.XZ -> data[x][layer][y]
            Plane.XY -> data[x][y][layer]
        }
    }

    fun set(x: Int, y: Int, value: T) {
        when (plane) {
            Plane.YZ -> data[layer][x][y] = value
            Plane.XZ -> data[x][layer][y] = value
            Plane.XY -> data[x][y][layer] = value
        }
    }

    operator fun get(at: Index2d): T {
        return get(at.x, at.y)
    }

    operator fun set(at: Index2d, value: T) {
        set(at.x, at.y, value)
    }
}

enum class Plane {
    YZ,
    XZ,
    XY,
}

private class Index3dRegionIterator(private val size: Size3d) : Iterator<Index3d> {
    private val bound = size.volume - 1

    private var current: Int = -1

    override fun hasNext() = current < bound

    override fun next(): Index3d {
        current++
        return current.toIndex3d(size)
    }
}

inline fun <reified T> array3dOf(size: Size3d, init: () -> T) =
    Array3d(Array(size.width) { Array(size.height) { Array(size.depth) { init() } } })

data class Size3d(
    val width: Int,
    val height: Int,
    val depth: Int
) {
    val volume: Int
        get() = width * height * depth

    operator fun contains(index: Index3d): Boolean {
        return index.x in 0..<width && index.y in 0..<height && index.z in 0..<depth
    }

    fun projection(plane: Plane): Size2d {
        return when (plane) {
            Plane.YZ -> Size2d(height, depth)
            Plane.XZ -> Size2d(width, depth)
            Plane.XY -> Size2d(width, height)
        }
    }
}

data class Size2d(
    val width: Int,
    val height: Int
) {
    val area: Int
        get() = width * height

    operator fun contains(index: Index3d): Boolean {
        return index.x in 0..<width && index.y in 0..<height
    }
}

data class Index3d(
    val x: Int,
    val y: Int,
    val z: Int
) {

    fun projection(plane: Plane): Index2d {
        return when (plane) {
            Plane.YZ -> Index2d(y, z)
            Plane.XZ -> Index2d(x, z)
            Plane.XY -> Index2d(x, y)
        }
    }

    fun getIndependent(plane: Plane) = when(plane) {
        Plane.YZ -> x
        Plane.XZ -> y
        Plane.XY -> z
    }

    fun layInSamePlane(other: Index3d): Boolean {
        return x == other.x || y == other.y || z == other.z
    }

    companion object {
        fun random(bound: Size3d) = Index3d(
            ThreadLocalRandom.current().nextInt(bound.width),
            ThreadLocalRandom.current().nextInt(bound.height),
            ThreadLocalRandom.current().nextInt(bound.depth)
        )
    }
}

data class Index2d(
    val x: Int,
    val y: Int
) {
    fun mooreNeighboursIn(size: Size2d) = sequence {
        for (i in x - 1..x + 1) {
            if (i < 0 || i >= size.width) continue

            for (j in y - 1..y + 1) {
                if (j < 0 || j >= size.height) continue

                yield(Index2d(i, j))
            }
        }
    }

    fun to3d(plane: Plane, level: Int) = when(plane) {
        Plane.YZ -> Index3d(level, x, y)
        Plane.XZ -> Index3d(x, level, y)
        Plane.XY -> Index3d(x, y, level)
    }
}


fun Int.toIndex3d(size: Size3d) = Index3d(
    z = this / size.width / size.height,
    y = (this / size.width) % size.height,
    x = (this % size.width) % size.height,
)
