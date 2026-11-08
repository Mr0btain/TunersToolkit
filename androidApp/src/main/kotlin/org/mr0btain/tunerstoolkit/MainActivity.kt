package org.mr0btain.tunerstoolkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The project store and the settings find their folders from this, and App() loads the settings straight away
        appContext = applicationContext

        setContent {
            App()
        }
    }
}
