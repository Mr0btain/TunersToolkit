package injection

import org.koin.dsl.module
import screens.editor.components.MainViewModel

val mainModule = module {
    factory { MainViewModel() }
}