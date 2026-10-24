package com.theveloper.pixelplay.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.theveloper.pixelplay.R
import com.theveloper.pixelplay.data.model.Song
import com.theveloper.pixelplay.utils.formatDuration
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTrimDialog(
    song: Song,
    isTrimming: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (startMs: Long, endMs: Long, asRingtone: Boolean) -> Unit,
) {
    val totalDuration = song.duration.coerceAtLeast(1000L).toFloat()
    var sliderRange by remember(song.id, totalDuration) {
        val initialEnd = if (totalDuration > 30000f) 30000f else totalDuration
        mutableStateOf(0f..initialEnd)
    }
    var asRingtone by remember { mutableStateOf(false) }

    val startMs = sliderRange.start.toLong().coerceIn(0L, totalDuration.toLong())
    val endMs = sliderRange.endInclusive.toLong().coerceIn(startMs + 500L, totalDuration.toLong())
    val selectionDurationMs = (endMs - startMs).coerceAtLeast(0L)

    BasicAlertDialog(onDismissRequest = { if (!isTrimming) onDismiss() }) {
        Surface(
            shape = AbsoluteSmoothCornerShape(
                cornerRadiusTR = 32.dp,
                smoothnessAsPercentBR = 60,
                cornerRadiusBR = 32.dp,
                smoothnessAsPercentTL = 60,
                cornerRadiusTL = 32.dp,
                smoothnessAsPercentBL = 60,
                cornerRadiusBL = 32.dp,
                smoothnessAsPercentTR = 60,
            ),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCut,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.trim_audio_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Range Slider
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${stringResource(R.string.trim_audio_start)}: ${formatDuration(startMs)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${stringResource(R.string.trim_audio_end)}: ${formatDuration(endMs)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    RangeSlider(
                        value = sliderRange,
                        onValueChange = { newRange ->
                            val coercedStart = newRange.start.coerceIn(0f, totalDuration - 500f)
                            val coercedEnd = newRange.endInclusive.coerceIn(coercedStart + 500f, totalDuration)
                            sliderRange = coercedStart..coercedEnd
                        },
                        valueRange = 0f..totalDuration,
                        enabled = !isTrimming,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${stringResource(R.string.trim_audio_duration)}: ${formatDuration(selectionDurationMs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = formatDuration(totalDuration.toLong()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }

                    // Fine tuning buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Start fine tuning (50% width)
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                FilledTonalIconButton(
                                    onClick = {
                                        val newStart = (startMs - 1000L).coerceAtLeast(0L).toFloat()
                                        sliderRange = newStart..sliderRange.endInclusive
                                    },
                                    enabled = !isTrimming && startMs > 0L,
                                    modifier = Modifier.size(32.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    ),
                                ) {
                                    Icon(Icons.Rounded.Remove, null, modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = stringResource(R.string.trim_audio_start),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 2.dp),
                                )
                                FilledTonalIconButton(
                                    onClick = {
                                        val newStart = (startMs + 1000L).coerceAtMost(endMs - 500L).toFloat()
                                        sliderRange = newStart..sliderRange.endInclusive
                                    },
                                    enabled = !isTrimming && startMs < endMs - 500L,
                                    modifier = Modifier.size(32.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    ),
                                ) {
                                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        // End fine tuning (50% width)
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                FilledTonalIconButton(
                                    onClick = {
                                        val newEnd = (endMs - 1000L).coerceAtLeast(startMs + 500L).toFloat()
                                        sliderRange = sliderRange.start..newEnd
                                    },
                                    enabled = !isTrimming && endMs > startMs + 500L,
                                    modifier = Modifier.size(32.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    ),
                                ) {
                                    Icon(Icons.Rounded.Remove, null, modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = stringResource(R.string.trim_audio_end),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 2.dp),
                                )
                                FilledTonalIconButton(
                                    onClick = {
                                        val newEnd = (endMs + 1000L).coerceAtMost(totalDuration.toLong()).toFloat()
                                        sliderRange = sliderRange.start..newEnd
                                    },
                                    enabled = !isTrimming && endMs < totalDuration.toLong(),
                                    modifier = Modifier.size(32.dp),
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    ),
                                ) {
                                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Ringtone switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !isTrimming) { asRingtone = !asRingtone }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.trim_audio_set_ringtone),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Switch(
                        checked = asRingtone,
                        onCheckedChange = { asRingtone = it },
                        enabled = !isTrimming,
                    )
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isTrimming,
                    ) {
                        Text(
                            text = stringResource(R.string.common_cancel),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    FilledTonalButton(
                        onClick = {
                            onConfirm(startMs, endMs, asRingtone)
                        },
                        enabled = !isTrimming && endMs > startMs,
                    ) {
                        if (isTrimming) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = stringResource(R.string.trim_audio_save),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
