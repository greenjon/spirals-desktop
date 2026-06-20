package llm.slop.spirals.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import llm.slop.spirals.ui.theme.AppAccent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info

@Composable
fun HdmiStatusOverlay(
    modifier: Modifier = Modifier,
    isConnected: Boolean
) {
    if (isConnected) {
        Row(
            modifier = modifier
                .padding(8.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "HDMI",
                tint = AppAccent,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "HDMI ACTIVE",
                color = AppAccent,
                fontSize = 10.sp,
                letterSpacing = 1.sp
            )
        }
    }
}
