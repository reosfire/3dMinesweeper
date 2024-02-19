package ru.reosfire.minestom.minesweper

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.reosfire.minestom.minesweper.presentation.GamePresenter
import ru.reosfire.minestom.utils.*

class Game(
    private val presenter: GamePresenter,
    private val settings: GameSettings = GameSettings.DEFAULT,
) {
    private val size: Size3d
        get() = settings.size

    private var mines: Array3d<Boolean>? = null
    private var field: Array3d<GameCell> = createEmptyField()

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        scope.launch {
            launch {
                presenter.flagEventFlow.collect {
                    flag(it)
                }
            }
            launch {
                presenter.openEventFlow.collect {
                    open(it)
                }
            }
            renderField()
        }
    }

    private suspend fun open(at: Index3d) {
        if (field[at] !is GameCell.Closed) return

        if (mines == null) mines = generateMines(at)

        if (mines!![at]) {
            presenter.showLoose(at)
            regenerate()
            return
        }

        openRecursively(at)

        recount(at)
    }
    private suspend fun flag(at: Index3d) {
        when (field[at]) {
            is GameCell.Flag -> field[at] = GameCell.Closed
            is GameCell.Closed -> field[at] = GameCell.Flag
            else -> return
        }

        presenter.renderCell(field[at], at)

        recount(at)
    }

    private fun generateMines(initialPoint: Index3d): Array3d<Boolean> {
        val result = array3dOf(size) { false }

        var generated = 0
        while (generated < settings.minesCount) {
            val randomPoint3d = Index3d.random(size)

            if (result[randomPoint3d] || randomPoint3d == initialPoint) continue

            result[randomPoint3d] = true
            generated++
        }

        return result
    }

    private suspend fun openRecursively(
        current: Index3d,
        initialPoint: Index3d = current,
        awaitRender: Boolean = true
    ): List<Job> {
        if (field[current] is GameCell.Opened) return emptyList()

        val yz = getCountAround(current, Plane.YZ)
        val xz = getCountAround(current, Plane.XZ)
        val xy = getCountAround(current, Plane.XY)

        val cell = GameCell.Opened(yz, xz, xy)

        field[current] = cell

        val renderJobsMutex = Mutex()
        val renderJobs = mutableListOf(scope.launch {
            presenter.renderCell(cell, current)
        })

        val recursiveJobs = mutableListOf<Job>()

        fun travers(plane: Plane) {
            for (neighbour in current.projection(plane).mooreNeighboursIn(size.projection(plane))) {
                val next = neighbour.to3d(plane, current.getIndependent(plane))

                if (next.layInSamePlane(initialPoint)) {
                    recursiveJobs.add(scope.launch {
                        val renderJobsToAdd = openRecursively(next, initialPoint, false)
                        renderJobsMutex.withLock {
                            renderJobs.addAll(renderJobsToAdd)
                        }
                    })
                }
            }
        }

        if (yz == 0) travers(Plane.YZ)
        if (xz == 0) travers(Plane.XZ)
        if (xy == 0) travers(Plane.XY)

        recursiveJobs.joinAll()
        return if (awaitRender) {
            renderJobs.joinAll()
            emptyList()
        } else renderJobs
    }

    private fun getCountAround(point: Index3d, plane: Plane): Int {
        val slice = mines!!.getSlice(point.getIndependent(plane), plane)

        val neighbours = point.projection(plane).mooreNeighboursIn(settings.size.projection(plane))
        return neighbours.count { slice[it] }
    }

    private suspend fun recount(at: Index3d) {
        var closed = 0
        for (i in field.indices) {
            if (field[i] !is GameCell.Opened) closed++
        }

        if (closed == settings.minesCount) {
            presenter.showWin(at)

            mines = null

            regenerate()
        }

        var flags = 0
        for (i in field.indices) {
            if (field[i] is GameCell.Flag) flags++
        }

        presenter.showMinesFlagsCount(settings.minesCount, flags)
        println("flags $flags/${settings.minesCount}      $closed")
    }

    private suspend fun regenerate() {
        mines = null
        field = createEmptyField()
        presenter.clear()
        renderField()
    }

    private suspend fun renderField() {
        field.indices.map {
            scope.launch {
                presenter.renderCell(field[it], it)
            }
        }.joinAll()
    }

    private fun createEmptyField(): Array3d<GameCell> = array3dOf(settings.size) { GameCell.Closed }
}
