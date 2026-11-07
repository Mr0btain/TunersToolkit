@file:Suppress("FunctionName")

package org.mr0btain.tunerstoolkit.components.utilitytab.buttonbartab.components
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp

@Composable
fun UtilityIconButton(
    modifier: Modifier = Modifier,
    icon: Painter,
    onClick: () -> Unit = {},
) {
    IconButton(
        onClick = onClick,
        modifier = modifier

    ) {
        Icon(
            icon,
            "",
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .fillMaxSize()
                .shadow(
                    elevation = 4.dp,
                    ambientColor = MaterialTheme.colorScheme.scrim
                )
                .background(MaterialTheme.colorScheme.primary)
                .padding(4.dp),
            tint = MaterialTheme.colorScheme.onPrimary
        )
    }
}