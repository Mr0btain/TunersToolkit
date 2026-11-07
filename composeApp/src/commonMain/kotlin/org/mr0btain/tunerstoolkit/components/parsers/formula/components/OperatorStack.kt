package org.mr0btain.tunerstoolkit.components.parsers.formula.components
/**
 * Stack as type alias of ArrayDeque
 */
typealias Stack<T> = ArrayDeque<T>

/**
 * Pushes item to [Stack]
 * @param element Item to be pushed
 */
fun <T> Stack<T>.push(element: T) = addLast(element)

/**
 * Pops (removes and return) last item from [Stack]
 * @return item Last item if [Stack] is not empty, null otherwise
 */
fun <T> Stack<T>.pop() = removeLastOrNull()

/**
 * Peeks (return) last item from [Stack]
 * @return item Last item if [Stack] is not empty, null otherwise
 */
fun <T> Stack<T>.peek(): T? = if (isNotEmpty()) last() else null

fun factorialRecursive(n: Int): Int {
    require(n >= 0) { "n must be positive" }
    return if (n <= 1) {
        1
    } else {
        n * factorialRecursive(n - 1)
    }
}