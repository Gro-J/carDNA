package com.example.cardna.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cardna.CarDnaUiState
import com.example.cardna.data.VehicleSourceType
import com.example.cardna.ui.theme.CarDnaTheme
import com.example.cardna.usage.ReplacementStatus
import com.example.cardna.usage.UsageUiState
import kotlin.math.min

@Composable
fun MainScreen(
    uiState: CarDnaUiState,
    onToggleSource: (VehicleSourceType) -> Unit,
    onReplacementConfirmed: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WiperUsageGauge(usage = uiState.usage, modifier = Modifier.size(280.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = statusLabel(uiState.usage.status),
                    color = statusColor(uiState.usage.status),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (uiState.isSimulated) {
                    SimulationBadge()
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("데이터 소스", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (uiState.isSimulated) "시뮬레이션" else "Pleos Vehicle SDK",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                        Switch(
                            checked = uiState.isSimulated,
                            onCheckedChange = { checked ->
                                onToggleSource(
                                    if (checked) VehicleSourceType.SIMULATED else VehicleSourceType.PLEOS_SDK
                                )
                            }
                        )
                    }
                }

                Button(
                    onClick = onReplacementConfirmed,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("교체 완료", fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun SimulationBadge() {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "시뮬레이션",
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun WiperUsageGauge(usage: UsageUiState, modifier: Modifier = Modifier) {
    val color = statusColor(usage.status)
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val clampedFraction = (min(usage.usagePercent, 100.0) / 100.0).toFloat()
    val startAngle = 150f
    val sweepAngle = 240f

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.1f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = androidx.compose.ui.geometry.Offset(strokeWidth / 2f, strokeWidth / 2f)

            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle * clampedFraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${usage.usagePercent.toInt()}%",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "와이퍼 블레이드 사용률",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
    }
}

private fun statusLabel(status: ReplacementStatus): String = when (status) {
    ReplacementStatus.NORMAL -> "정상"
    ReplacementStatus.NEXT_SERVICE -> "다음 정비 때 함께"
    ReplacementStatus.CHECK_NOW -> "지금 확인"
}

@Composable
private fun statusColor(status: ReplacementStatus): Color = when (status) {
    ReplacementStatus.NORMAL -> Color(0xFF4CAF50)
    ReplacementStatus.NEXT_SERVICE -> Color(0xFFFFC107)
    ReplacementStatus.CHECK_NOW -> Color(0xFFF44336)
}

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun MainScreenPreview() {
    CarDnaTheme {
        MainScreen(
            uiState = CarDnaUiState(
                sourceType = VehicleSourceType.SIMULATED,
                usage = UsageUiState(usagePercent = 76.0, status = ReplacementStatus.NEXT_SERVICE)
            ),
            onToggleSource = {},
            onReplacementConfirmed = {}
        )
    }
}
