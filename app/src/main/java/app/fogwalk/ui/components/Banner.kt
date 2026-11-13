package app.fogwalk.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.fogwalk.ui.theme.U

/**
 * A painted banner across the top of a tab: the scene cropped to a strip, darkened from the left so the words laid
 * over it read, with a gilt edge like the cards.
 */
@Composable
fun Banner(@DrawableRes image: Int, modifier: Modifier = Modifier, height: Dp = 150.dp, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.fillMaxWidth().height(height).clip(U.RLg).border(1.5.dp, U.c.gilt, U.RLg)) {
        Image(painterResource(image), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(0f to Color(0xD92B1D10), 0.55f to Color(0x662B1D10), 1f to Color(0x002B1D10)),
            ),
        )
        content()
    }
}
