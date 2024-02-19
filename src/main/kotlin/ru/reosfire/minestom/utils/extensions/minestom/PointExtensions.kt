package ru.reosfire.minestom.utils.extensions.minestom

import net.minestom.server.coordinate.Pos

infix fun Pos.addX(addition: Int) = Pos(x + addition, y, z)
infix fun Pos.addY(addition: Int) = Pos(x, y + addition, z)
infix fun Pos.addZ(addition: Int) = Pos(x, y, z + addition)
