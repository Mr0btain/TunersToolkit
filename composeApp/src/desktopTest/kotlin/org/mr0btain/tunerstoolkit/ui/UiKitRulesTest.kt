package org.mr0btain.tunerstoolkit.ui

import org.mr0btain.uikit.rules.KitRules
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Keeps the screens on the UI kit: colors, sizes, shapes and controls come from Tm-UI-Kit (or this app's own kit
 * components in ui/kit), so changing one there changes it everywhere. If this fails, use or add a kit component
 * instead of styling the screen directly
 */
class UiKitRulesTest {
    private val root = File("src/commonMain/kotlin/org/mr0btain/tunerstoolkit")

    private val screenFiles: List<File> =
        (root.resolve("ui").walkTopDown().filter { it.extension == "kt" && !it.path.contains("/ui/kit/") } +
            root.resolve("App.kt")).toList()

    @Test
    fun screensOnlyStyleThroughTheKit() {
        assertTrue(screenFiles.size >= 5, "Expected to find the screens under ${root.absolutePath}")
        val problems = KitRules.checkFiles(root, screenFiles)
        assertTrue(problems.isEmpty(), "Screens styling things outside the UI kit:\n" + problems.joinToString("\n"))
    }
}
