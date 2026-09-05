package com.example.quickbillposs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quickbillposs.ui.theme.PosBgKeypadKey
import com.example.quickbillposs.ui.theme.PosSteelBlue
import com.example.quickbillposs.ui.theme.PosTextDark
import com.example.quickbillposs.ui.theme.PosTextWhite

@Composable
fun NumericKeypad(
    modifier: Modifier = Modifier,
    isMultiplyActive: Boolean = false,
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                isBackspace = true,
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
            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                isActive = isMultiplyActive,
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
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left 3 columns: 1,2,3 on top row; 0,00,. on bottom row
            Column(
                modifier = Modifier.weight(3f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
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
            .background(
                if (isPressed) PosBgKeypadKey.copy(alpha = 0.7f)
                else PosBgKeypadKey
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
                fontSize = 26.sp
            ),
            color = PosTextDark,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SpecialKey(
    modifier: Modifier = Modifier,
    label: String = "",
    isBackspace: Boolean = false,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isPressed) PosBgKeypadKey.copy(alpha = 0.7f)
                else PosBgKeypadKey
            )
            .then(
                if (isActive) Modifier.border(1.5.dp, PosTextDark, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple()
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isBackspace) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace",
                tint = PosTextDark,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Normal,
                    fontSize = 24.sp
                ),
                color = PosTextDark,
                textAlign = TextAlign.Center
            )
        }
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
                if (isPressed) PosSteelBlue.copy(alpha = 0.85f)
                else PosSteelBlue
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
                fontSize = 16.sp,
                lineHeight = 20.sp
            ),
            color = PosTextWhite,
            textAlign = TextAlign.Center
        )
    }
}
