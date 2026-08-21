package com.example.todoapp.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.todoapp.R
import com.example.todoapp.mascot.MascotAppearance
import kotlin.math.roundToInt

@Composable
fun MascotScreen(
    overlayAllowed: Boolean,
    mascotVisible: Boolean,
    sizePercent: Int,
    opacityPercent: Int,
    movementEnabled: Boolean,
    onRequestOverlayAccess: () -> Unit,
    onShowMascot: () -> Unit,
    onHideMascot: () -> Unit,
    onSizeChange: (Int) -> Unit,
    onOpacityChange: (Int) -> Unit,
    onMovementEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Image(
                painter = painterResource(R.drawable.mascot_reference),
                contentDescription = "採用したキャラクターの三面図",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .padding(12.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
        Icon(
            imageVector = if (mascotVisible) Icons.Outlined.CheckCircle else Icons.Outlined.Layers,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = if (mascotVisible) {
                MaterialTheme.colorScheme.primary
            } else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = when {
                mascotVisible -> "キャラクターを表示しています"
                overlayAllowed -> "表示する準備ができています"
                else -> "画面上への表示を許可してください"
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = when {
                mascotVisible && movementEnabled -> "歩行アニメーションで移動中です。停止時には表情が変わります。"
                mascotVisible -> "自動移動は停止中です。表示設定から再開できます。"
                overlayAllowed -> "ボタンを押すと、ほかのアプリを開いている間もキャラクターが表示されます。"
                else -> "Androidの設定画面で、ToDoに「他のアプリの上に重ねて表示」を許可します。"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "表示設定",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(12.dp))
                AppearanceSlider(
                    label = "サイズ",
                    value = sizePercent,
                    minimum = MascotAppearance.MIN_SIZE_PERCENT,
                    maximum = MascotAppearance.MAX_SIZE_PERCENT,
                    supportingText = "100%が標準サイズです",
                    onValueChange = onSizeChange,
                )
                Spacer(Modifier.height(8.dp))
                AppearanceSlider(
                    label = "不透明度",
                    value = opacityPercent,
                    minimum = MascotAppearance.MIN_OPACITY_PERCENT,
                    maximum = MascotAppearance.MAX_OPACITY_PERCENT,
                    supportingText = "数値を下げるほど透明になります",
                    onValueChange = onOpacityChange,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "自動移動",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "画面内のランダムな位置へ移動します",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = movementEnabled,
                        onCheckedChange = onMovementEnabledChange,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        when {
            mascotVisible -> OutlinedButton(
                onClick = onHideMascot,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("キャラクターを非表示")
            }

            overlayAllowed -> Button(
                onClick = onShowMascot,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("キャラクターを表示")
            }

            else -> Button(
                onClick = onRequestOverlayAccess,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("表示権限の設定を開く")
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AppearanceSlider(
    label: String,
    value: Int,
    minimum: Int,
    maximum: Int,
    supportingText: String,
    onValueChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "$value%",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
    Slider(
        value = value.toFloat(),
        onValueChange = { sliderValue ->
            onValueChange((sliderValue / SLIDER_INCREMENT).roundToInt() * SLIDER_INCREMENT)
        },
        valueRange = minimum.toFloat()..maximum.toFloat(),
        steps = ((maximum - minimum) / SLIDER_INCREMENT) - 1,
    )
    Text(
        text = supportingText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private const val SLIDER_INCREMENT = 5
