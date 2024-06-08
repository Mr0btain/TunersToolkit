package screens.main

import MainViewModel
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import components.hextexteditor.HexEditor
import components.testing.randomHexString
import org.jetbrains.compose.resources.Font
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.jbmono_reg

@Composable
fun ExpandedMain(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    val textFont = FontFamily(Font(Res.font.jbmono_reg))

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Text(
            text = "Main Screen Expanded",
            style = TextStyle(
                fontSize = 40.sp,
                fontFamily = textFont,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.padding(4.dp))

        HexEditor(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            input = randomHexString(400),
            viewModel = viewModel
        )
    }
}