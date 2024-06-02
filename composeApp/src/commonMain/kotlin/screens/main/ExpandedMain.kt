package screens.main

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
import components.hextexteditor.HexTextEditor
import components.hextexteditor.HexTextEditor2
import org.jetbrains.compose.resources.Font
import tunerstoolkit.composeapp.generated.resources.Res
import tunerstoolkit.composeapp.generated.resources.jbmono_reg

@Composable
fun ExpandedMain(
    modifier: Modifier = Modifier
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

        /*
        HexTextEditor(
            modifier = Modifier,
            input = "AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvXxYyZz00112233445566778899",
            bits = 2,
            digitsPerLine = 12,
            textStyle = TextStyle(fontSize = 20.sp)
        )
         */

        Spacer(modifier = Modifier.padding(4.dp))

        HexTextEditor2(
            input = "010101010101010202020202020202030303030303030304040404040404040505050505050060606060606060606007070707070707" +
                    "0707008080808080808080808080808009090909090909090909001010101010101010101010202020202020202020303030303030303" +
                    "04040404040405050505050505006060606060606070707070707070708080808080800909090090909090909090",
            textStyle = TextStyle.Default
        )
    }
}