package com.example.quickbillposs.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
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
import java.util.Locale

@Composable
fun CartItemRow(
    item: CartItem,
    onRemove: () -> Unit,
    onQuantityChange: (Double) -> Unit,
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
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Item name & price subtitle (.item-info)
            Column(modifier = Modifier.weight(1f)) {
                val formattedQty = formatQuantity(item.quantity)
                val displayName = if (item.label.isNotBlank()) {
                    if (item.quantity != 1.0) "${item.label} (${formatAmount(item.amount)} x $formattedQty)" else item.label
                } else {
                    "${formatAmount(item.amount)} x $formattedQty"
                }

                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = PosTextMain,
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

            // Quantity stepper (.qty-controls): [.qty-btn] .qty-val [.qty-btn]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                SmallIconButton(
                    icon = Icons.Default.Remove,
                    onClick = {
                        val newQty = (item.quantity - 1.0).coerceAtLeast(0.0)
                        onQuantityChange(newQty)
                    },
                    contentDescription = "Decrease"
                )
                Text(
                    text = formatQuantity(item.quantity),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    ),
                    color = PosTextMain,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 36.dp)
                )
                SmallIconButton(
                    icon = Icons.Default.Add,
                    onClick = { onQuantityChange(item.quantity + 1.0) },
                    contentDescription = "Increase"
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Line Total (.item-total-price)
            Text(
                text = "₹${formatAmount(item.lineTotal)}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = PosTextMain,
                textAlign = TextAlign.End,
                modifier = Modifier.width(76.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Trash Icon (.trash-btn)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onRemove() }
                    .padding(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Remove item",
                    tint = PosTextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
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
        shape = RoundedCornerShape(8.dp),
        color = PosBorder,
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = PosTextMain,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

fun formatAmount(amount: Double): String {
    return if (amount % 1.0 == 0.0) {
        amount.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", amount)
    }
}

fun formatQuantity(qty: Double): String {
    return if (qty % 1.0 == 0.0) {
        qty.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", qty).trimEnd('0').trimEnd('.')
    }
}
