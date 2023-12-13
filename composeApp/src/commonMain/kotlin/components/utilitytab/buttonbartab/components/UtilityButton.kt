@file:Suppress("FunctionName")

package components.utilitytab.buttonbartab.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Not sure if this is even going to be used
@Composable
fun UtilityButton(
    fontFamily: FontFamily,
    label: String,
    icon: Painter,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp)),
        shadowElevation = 8.dp,
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .wrapContentSize()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary)
                .clickable(
                    onClick = onClick,
                ),
        ) {
            Icon(
                icon,
                "",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(start = 8.dp, top = 6.dp, bottom = 6.dp)
                    .height(20.dp)
            )

            Spacer(
                Modifier.size(
                    ButtonDefaults.IconSpacing
                )
            )

            Text(
                text = label,
                textAlign = TextAlign.Center,
                fontFamily = fontFamily,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(top = 6.dp, bottom = 6.dp, end = 20.dp),
            )
        }
    }
}