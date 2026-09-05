package com.example.quickbillposs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class KeypadButton(
    val label: String,
    val icon: ImageVector? = null,
    val isSpecial: Boolean = false,   // backspace, clear
    val isAction: Boolean = false,    // "Add Item" — large accent button
    val isMultiply: Boolean = false,  // the × button
)

@Composable
fun NumericKeypad(
    modifier: Modifier = Modifier,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    onMultiply: () -> Unit,
    onAddItem: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    // 4-column grid:
    // Row 1: 7  8  9  ⌫
    // Row 2: 4  5  6  ×
    // Row 3: 1  2  3  [Add Item - spans rows 3+4]
    // Row 4: 0 00  .  [Add Item continued]

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Row 1
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("7", "8", "9").forEach { digit ->
                RegularKey(
                    label = digit,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDigit(digit)
                    }
                )
            }
            SpecialKey(
                label = "⌫",
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onBackspace()
                }
            )
        }

        // Row 2
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("4", "5", "6").forEach { digit ->
                RegularKey(
                    label = digit,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDigit(digit)
                    }
                )
            }
            SpecialKey(
                label = "×",
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onMultiply()
                }
            )
        }

        // Rows 3 & 4 with "Add Item" spanning right column
        Row(
            modifier = Modifier.weight(2f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Left 3 columns: 1,2,3 on top row; 0,00,. on bottom row
            Column(
                modifier = Modifier.weight(3f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("1", "2", "3").forEach { digit ->
                        RegularKey(
                            label = digit,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDigit(digit)
                            }
                        )
                    }
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("0", "00", ".").forEach { digit ->
                        RegularKey(
                            label = digit,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDigit(digit)
                            }
                        )
                    }
                }
            }

            // Right column: big "Add Item" button
            AddItemKey(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAddItem()
                }
            )
        }
    }
}

@Composable
private fun RegularKey(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .shadow(if (isPressed) 0.dp else 2.dp, RoundedCornerShape(12.dp))
            .background(
                if (isPressed) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surface
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple()
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Normal,
                fontSize = 24.sp
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SpecialKey(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isPressed) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple()
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Medium,
                fontSize = if (label == "×") 28.sp else 22.sp
            ),
            color = if (label == "⌫") MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AddItemKey(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isPressed) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.primary
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple()
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Add\nItem",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            ),
            color = MaterialTheme.colorScheme.onPrimary,
            textAlign = TextAlign.Center
        )
    }
}
