package ru.reosfire.minestom.minesweper

import ru.reosfire.minestom.utils.Size3d

data class GameSettings(
    val size: Size3d,
    val minesCount: Int
) {
    companion object {
        val DEFAULT = GameSettings(Size3d(4, 5, 6), 30)
    }
}
