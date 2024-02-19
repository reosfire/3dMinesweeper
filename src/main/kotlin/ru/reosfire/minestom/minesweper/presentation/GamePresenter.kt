package ru.reosfire.minestom.minesweper.presentation

import kotlinx.coroutines.flow.Flow
import ru.reosfire.minestom.minesweper.GameCell
import ru.reosfire.minestom.utils.Index3d

interface GamePresenter {
    val openEventFlow: Flow<Index3d>
    val flagEventFlow: Flow<Index3d>

    suspend fun renderCell(cell: GameCell, at: Index3d)
    suspend fun clear()

    suspend fun showWin(at: Index3d)
    suspend fun showLoose(at: Index3d)

    fun showMinesFlagsCount(mines: Int, flags: Int)
    //fun showCount(count: Int)
}
