package ru.reosfire.minestom.minesweper

sealed interface GameCell {

    data class Opened(val yz: Int, val xz: Int, val xy: Int) : GameCell

    data object Closed : GameCell
    data object Flag : GameCell
}