package org.mr0btain.tunerstoolkit.screens.main
import org.mr0btain.tunerstoolkit.MainViewModel
import androidx.compose.foundation.layout.*
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
import org.mr0btain.tunerstoolkit.components.hextexteditor.HexEditor
import org.mr0btain.tunerstoolkit.components.testing.randomHexString
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
        modifier = modifier.fillMaxWidth().wrapContentHeight()
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
            input = randomHexString(80000),
            viewModel = viewModel
        )
    }
}