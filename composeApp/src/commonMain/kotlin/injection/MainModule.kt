package injection

import org.koin.dsl.module
import MainViewModel

val mainModule = module {
    factory { MainViewModel() }
}