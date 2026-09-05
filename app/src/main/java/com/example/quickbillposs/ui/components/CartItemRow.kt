package com.example.quickbillposs.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.quickbillposs.data.model.CartItem
import com.example.quickbillposs.ui.theme.*

@Composable
fun CartItemRow(
    item: CartItem,
    onRemove: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(150)) + slideInVertically(tween(150)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Item name & price subtitle
            Column(modifier = Modifier.weight(1f)) {
                val displayName = if (item.label.isNotBlank()) {
                    if (item.quantity > 1) "${item.label} (${formatAmount(item.amount)} x ${item.quantity})" else item.label
                } else {
                    "${formatAmount(item.amount)} x ${item.quantity}"
                }

                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = PosTextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = "₹${formatAmount(item.amount)} each",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = PosTextMuted
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Quantity stepper: [-] Qty [+]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SmallIconButton(
                    icon = Icons.Default.Remove,
                    onClick = { onQuantityChange(item.quantity - 1) },
                    contentDescription = "Decrease"
                )
                Text(
                    text = "${item.quantity}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = PosTextDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 18.dp)
                )
                SmallIconButton(
                    icon = Icons.Default.Add,
                    onClick = { onQuantityChange(item.quantity + 1) },
                    contentDescription = "Increase"
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Line Total
            Text(
                text = "₹${formatAmount(item.lineTotal)}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = PosTextDark
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Trash Icon
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = "Delete item",
                tint = PosTextMuted,
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onRemove() }
            )
        }
    }
}

@Composable
private fun SmallIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = PosBgMain,
        modifier = Modifier
            .size(26.dp)
            .border(1.dp, PosBorder, RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = PosTextDark,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

fun formatAmount(amount: Double): String {
    return if (amount % 1.0 == 0.0) {
        amount.toLong().toString()
    } else {
        String.format("%.2f", amount)
    }
}
