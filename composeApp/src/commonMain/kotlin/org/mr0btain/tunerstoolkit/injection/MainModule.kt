package org.mr0btain.tunerstoolkit.injection
import org.koin.dsl.module
import org.mr0btain.tunerstoolkit.MainViewModel
val mainModule = module {
    factory { MainViewModel() }
}