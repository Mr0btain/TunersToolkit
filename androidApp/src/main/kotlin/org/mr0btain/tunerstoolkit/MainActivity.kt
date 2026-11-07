package org.mr0btain.tunerstoolkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ProjectFilesManager reads its folders from this, and App() loads the settings through it straight away
        appContext = applicationContext

        setContent {
            App()
        }
    }
}
