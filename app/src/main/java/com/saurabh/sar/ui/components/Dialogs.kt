package com.saurabh.sar.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.saurabh.sar.ui.theme.SarColors
import com.saurabh.sar.ui.theme.SarShapes
import com.saurabh.sar.ui.theme.SarType

@Composable
fun SarDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismiss: String? = "Cancel",
    danger: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SarColors.BgRaised,
        shape = SarShapes.CardLarge,
        title = { Text(title.uppercase(), style = SarType.CardTitle) },
        text = { Text(text, style = SarType.Body) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirm.uppercase(), style = SarType.Button, color = if (danger) SarColors.Danger else SarColors.Cyan)
            }
        },
        dismissButton = dismiss?.let {
            { TextButton(onClick = onDismiss) { Text(it.uppercase(), style = SarType.Button, color = SarColors.TextSecondary) } }
        },
    )
}
