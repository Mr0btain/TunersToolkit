package components.helpers

import androidx.compose.ui.unit.IntSize

@Suppress("UNCHECKED_CAST")
inline fun <reified T : Any> List<*>.checkItemsAre() =
    if (all { it is T }) this as List<T>
    else listOf()

@Suppress("UNCHECKED_CAST")
inline fun <reified K : Any, reified V : Any> Map<*, *>.checkItemsAre() =
    if (all { it.key is K && it.value is V }) this as Map<K, V>
    else mapOf()

inline fun <reified T : Any> Any.checkItemsAre() =
    if (this is T )
        this
    else null

fun IntSize.isSmallerThan(size: IntSize): Boolean {
    return when {
        this.width < size.width -> true
        this.height < size.height -> true
        else -> false
    }
}