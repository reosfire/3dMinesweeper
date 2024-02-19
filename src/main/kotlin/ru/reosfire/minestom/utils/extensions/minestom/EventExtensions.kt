package ru.reosfire.minestom.utils.extensions.minestom

import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import java.util.function.Consumer

inline fun <reified T : Event> EventNode<Event>.addListener(action: Consumer<T>) =
    addListener(T::class.java, action)