package com.saurabh.sar.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.saurabh.sar.R
import com.saurabh.sar.SarApp
import com.saurabh.sar.ui.theme.SarColors
import com.saurabh.sar.ui.theme.SarShapes
import com.saurabh.sar.ui.theme.SarType
import androidx.compose.ui.platform.LocalContext

/** Static glow: a coloured shadow drawn once per frame the element is drawn. Never animated. */
fun Modifier.glow(color: Color = SarColors.Cyan, radius: Dp = 18.dp, shape: Shape = SarShapes.Button): Modifier =
    this.shadow(radius, shape, clip = false, ambientColor = color, spotColor = color)

/** Screen background with one big pre-computed accent blob in the top-right corner (DESIGN: effects). */
@Composable
fun SarBackground(accent: Color = SarColors.Cyan, content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(SarColors.BgBase)
            .drawBehind {
                val r = size.width * 0.75f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.22f), accent.copy(alpha = 0.06f), Color.Transparent),
                        center = Offset(size.width * 0.95f, size.height * 0.02f), radius = r,
                    ),
                    radius = r, center = Offset(size.width * 0.95f, size.height * 0.02f),
                )
            },
        content = content,
    )
}

@Composable
fun SarIcon(@DrawableRes id: Int, tint: Color = SarColors.TextPrimary, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    Icon(painterResource(id), contentDescription = null, tint = tint, modifier = modifier.size(size))
}

enum class ButtonKind { PRIMARY, GHOST, DANGER }

@Composable
fun SarButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: ButtonKind = ButtonKind.PRIMARY,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
) {
    val (bg, fg, border) = when (kind) {
        ButtonKind.PRIMARY -> Triple(SarColors.Cyan, SarColors.OnAccent, Color.Transparent)
        ButtonKind.GHOST -> Triple(SarColors.Cyan.copy(alpha = 0.06f), SarColors.Cyan, SarColors.Cyan.copy(alpha = 0.6f))
        ButtonKind.DANGER -> Triple(SarColors.Danger.copy(alpha = 0.08f), SarColors.Danger, SarColors.Danger.copy(alpha = 0.6f))
    }
    Row(
        modifier
            .height(52.dp)
            .then(if (kind == ButtonKind.PRIMARY && enabled) Modifier.glow() else Modifier)
            .clip(SarShapes.Button)
            .background(bg)
            .border(1.dp, border, SarShapes.Button)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.45f)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { SarIcon(icon, fg, 18.dp); Spacer(Modifier.width(8.dp)) }
        Text(text.uppercase(), style = SarType.Button, color = fg, maxLines = 1)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 6.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = SarType.Section, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun SarCard(
    modifier: Modifier = Modifier,
    tint: Color? = null,
    borderColor: Color = SarColors.LineSubtle,
    shape: Shape = SarShapes.Card,
    glowColor: Color? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    @OptIn(ExperimentalFoundationApi::class)
    val click = if (onClick != null || onLongClick != null) {
        Modifier.combinedClickable(onClick = { onClick?.invoke() }, onLongClick = onLongClick)
    } else Modifier
    Column(
        modifier
            .then(if (glowColor != null) Modifier.glow(glowColor, 20.dp, shape) else Modifier)
            .clip(shape)
            .background(SarColors.BgSurface)
            .then(if (tint != null) Modifier.background(tint) else Modifier)
            .border(1.dp, borderColor, shape)
            .then(click)
            .padding(padding),
        content = content,
    )
}

@Composable
fun Tag(text: String, color: Color, modifier: Modifier = Modifier, filled: Boolean = false, @DrawableRes icon: Int? = null) {
    Row(
        modifier
            .clip(SarShapes.Chip)
            .background(if (filled) color else color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.6f), SarShapes.Chip)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { SarIcon(icon, if (filled) SarColors.OnAccent else color, 11.dp); Spacer(Modifier.width(4.dp)) }
        Text(text.uppercase(), style = SarType.Chip, color = if (filled) SarColors.OnAccent else color, maxLines = 1)
    }
}

/** Selectable chip (duration chips, warning presets). */
@Composable
fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(40.dp)
            .then(if (selected) Modifier.glow(SarColors.Cyan, 10.dp, SarShapes.Chip) else Modifier)
            .clip(SarShapes.Chip)
            .background(if (selected) SarColors.Cyan else SarColors.BgSurface)
            .border(1.dp, if (selected) SarColors.Cyan else SarColors.LineSubtle, SarShapes.Chip)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text.uppercase(), style = SarType.Chip.copy(fontSize = SarType.Chip.fontSize * 1.15f),
            color = if (selected) SarColors.OnAccent else SarColors.TextPrimary)
    }
}

/** Two- or three-way segmented control (TYPE, BLOCK / LIMIT, TODAY / 7 DAYS). */
@Composable
fun Segmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<Int?> = emptyList(),
    enabled: Boolean = true,
) {
    Row(
        modifier
            .clip(SarShapes.Input)
            .background(SarColors.BgSurface)
            .border(1.dp, SarColors.LineSubtle, SarShapes.Input)
            .padding(4.dp)
            .alpha(if (enabled) 1f else 0.5f),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Row(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (on) SarColors.Cyan.copy(alpha = 0.12f) else Color.Transparent)
                    .border(1.dp, if (on) SarColors.Cyan.copy(alpha = 0.7f) else Color.Transparent, RoundedCornerShape(10.dp))
                    .clickable(enabled = enabled) { onSelect(i) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                icons.getOrNull(i)?.let { SarIcon(it, if (on) SarColors.Cyan else SarColors.TextSecondary, 14.dp); Spacer(Modifier.width(6.dp)) }
                Text(label.uppercase(), style = SarType.Chip.copy(fontSize = SarType.Chip.fontSize * 1.1f),
                    color = if (on) SarColors.Cyan else SarColors.TextSecondary)
            }
        }
    }
}

@Composable
fun SarToggle(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(
        modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(CircleShape)
            .background(if (checked) SarColors.Cyan else SarColors.LineSubtle)
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.size(22.dp).clip(CircleShape).background(if (checked) SarColors.OnAccent else SarColors.TextSecondary))
    }
}

@Composable
fun SarCheckbox(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(24.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (checked) SarColors.Cyan else Color.Transparent)
            .border(1.5.dp, if (checked) SarColors.Cyan else SarColors.TextSecondary, RoundedCornerShape(7.dp))
            .clickable { onChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) SarIcon(R.drawable.ic_check, SarColors.OnAccent, 16.dp)
    }
}

/** − value + stepper. */
@Composable
fun Stepper(text: String, onMinus: () -> Unit, onPlus: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(SarShapes.Input)
            .background(SarColors.BgBase)
            .border(1.dp, SarColors.LineSubtle, SarShapes.Input),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clickable(onClick = onMinus), contentAlignment = Alignment.Center) { SarIcon(R.drawable.ic_minus, SarColors.TextPrimary, 18.dp) }
        Text(text.uppercase(), style = SarType.Button, color = SarColors.Cyan, modifier = Modifier.width(84.dp), textAlign = TextAlign.Center)
        Box(Modifier.size(44.dp).clickable(onClick = onPlus), contentAlignment = Alignment.Center) { SarIcon(R.drawable.ic_plus, SarColors.Cyan, 18.dp) }
    }
}

/** Thin progress bar (App Row). */
@Composable
fun ProgressBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(SarColors.LineSubtle)) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}

/** Static progress ring with a soft glow arc (Home: time left). */
@Composable
fun ProgressRing(fraction: Float, size: Dp, modifier: Modifier = Modifier, color: Color = SarColors.Cyan, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = this.size.minDimension * 0.10f
            val inset = stroke / 2 + 6f
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(SarColors.LineSubtle, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            val sweep = 360f * fraction.coerceIn(0f, 1f)
            drawArc(color.copy(alpha = 0.25f), -90f, sweep, false, topLeft, arcSize, style = Stroke(stroke * 2.2f, cap = StrokeCap.Round))
            drawArc(color, -90f, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        content()
    }
}

/** Real installed app icon, or a violet letter tile like in Figma when the icon is missing. */
@Composable
fun AppIcon(pkg: String, label: String, size: Dp = 36.dp, tint: Color = SarColors.Violet) {
    val app = LocalContext.current.applicationContext as SarApp
    val bmp = remember(pkg) { app.installedApps.icon(pkg) }
    if (bmp != null) {
        Image(bmp, contentDescription = null, modifier = Modifier.size(size).clip(RoundedCornerShape(10.dp)))
    } else {
        Box(
            Modifier.size(size).clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) { Text(label.take(1).uppercase(), style = SarType.CardTitle, color = tint) }
    }
}

enum class AppRowState { LIMIT, WARNING, BLOCKED }

/** App Row (Figma Components): icon, name, "14 / 20 MIN" or "BLOCKED", 6 dp bar. */
@Composable
fun AppRow(pkg: String, label: String, valueText: String, fraction: Float, state: AppRowState, modifier: Modifier = Modifier) {
    val color = when (state) {
        AppRowState.LIMIT -> SarColors.Cyan
        AppRowState.WARNING -> SarColors.Warn
        AppRowState.BLOCKED -> SarColors.Danger
    }
    SarCard(
        modifier.fillMaxWidth(),
        borderColor = if (state == AppRowState.BLOCKED) SarColors.Danger.copy(alpha = 0.5f) else SarColors.LineSubtle,
        padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(pkg, label)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = SarType.RowTitle, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(valueText.uppercase(), style = SarType.Chip.copy(fontSize = SarType.Chip.fontSize * 1.1f), color = if (state == AppRowState.LIMIT) SarColors.TextPrimary else color)
                }
                Spacer(Modifier.height(8.dp))
                ProgressBar(fraction, color)
            }
        }
    }
}

fun modeIcon(key: String): Int = when (key) {
    "book" -> R.drawable.ic_book_open
    "activity" -> R.drawable.ic_activity
    "dumbbell" -> R.drawable.ic_dumbbell
    "wind" -> R.drawable.ic_wind
    "briefcase" -> R.drawable.ic_briefcase
    "sun" -> R.drawable.ic_sun
    "moon" -> R.drawable.ic_moon
    "clock" -> R.drawable.ic_clock
    "zap" -> R.drawable.ic_zap
    "eye" -> R.drawable.ic_eye
    "timer" -> R.drawable.ic_timer
    else -> R.drawable.ic_layers
}

val MODE_ICON_KEYS = listOf("book", "activity", "dumbbell", "wind", "briefcase", "sun", "moon", "clock", "zap", "eye", "timer", "layers")

enum class ModeCardState { IDLE, RUNNING, SCHEDULED }

/** Mode Card (Figma Components): icon tile 48, name, meta line, tag chip. */
@Composable
fun ModeCard(name: String, meta: String, icon: String, state: ModeCardState, tag: String, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    val running = state == ModeCardState.RUNNING
    SarCard(
        modifier.fillMaxWidth(),
        tint = if (running) SarColors.Cyan.copy(alpha = 0.07f) else null,
        borderColor = if (running) SarColors.Cyan.copy(alpha = 0.6f) else SarColors.LineSubtle,
        glowColor = if (running) SarColors.Cyan else null,
        onClick = onClick, onLongClick = onLongClick,
        padding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(SarColors.BgBase).border(1.dp, SarColors.LineSubtle, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) { SarIcon(modeIcon(icon), if (running) SarColors.Cyan else SarColors.TextPrimary, 22.dp) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(name.uppercase(), style = SarType.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Text(meta, style = SarType.Meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            when (state) {
                ModeCardState.RUNNING -> Tag(tag, SarColors.Cyan)
                ModeCardState.SCHEDULED -> Tag(tag, SarColors.Violet)
                ModeCardState.IDLE -> Tag(tag, SarColors.TextSecondary)
            }
        }
    }
}

/** Top bar with back button, title and optional subtitle / action. */
@Composable
fun TopBar(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, action: (@Composable RowScope.() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(SarColors.BgSurface)
                    .border(1.dp, SarColors.LineSubtle, RoundedCornerShape(12.dp)).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { SarIcon(R.drawable.ic_chevron_left, SarColors.TextPrimary, 20.dp) }
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title.uppercase(), style = if (onBack != null) SarType.ScreenTitle.copy(fontSize = SarType.ScreenTitle.fontSize * 0.75f) else SarType.ScreenTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = SarType.Meta, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        action?.invoke(this)
    }
}

/** A tappable settings-style row with icon, title, value and chevron. */
@Composable
fun NavRow(
    title: String,
    value: String? = null,
    @DrawableRes icon: Int? = null,
    subtitle: String? = null,
    color: Color = SarColors.TextPrimary,
    valueColor: Color = SarColors.TextSecondary,
    iconTint: Color = SarColors.TextSecondary,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).alpha(if (enabled) 1f else 0.5f)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { SarIcon(icon, iconTint, 18.dp); Spacer(Modifier.width(14.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = SarType.RowTitle.copy(fontSize = SarType.RowTitle.fontSize * 0.95f), color = color)
            if (subtitle != null) Text(subtitle, style = SarType.Meta)
        }
        if (value != null) { Text(value, style = SarType.Meta, color = valueColor); Spacer(Modifier.width(8.dp)) }
        SarIcon(R.drawable.ic_chevron_right, SarColors.TextSecondary, 18.dp)
    }
}

@Composable
fun Divider() { Box(Modifier.fillMaxWidth().height(1.dp).background(SarColors.LineSubtle)) }

/** Red banner (permissions revoked, blocking off). */
@Composable
fun Banner(text: String, color: Color, action: String? = null, onAction: () -> Unit = {}, modifier: Modifier = Modifier) {
    SarCard(modifier.fillMaxWidth(), tint = color.copy(alpha = 0.10f), borderColor = color.copy(alpha = 0.6f), padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SarIcon(R.drawable.ic_triangle_alert, color, 18.dp)
            Spacer(Modifier.width(10.dp))
            Text(text, style = SarType.Body, modifier = Modifier.weight(1f))
            if (action != null) {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.clip(SarShapes.Chip).background(color).clickable(onClick = onAction).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(action.uppercase(), style = SarType.Chip, color = SarColors.OnAccent)
                }
            }
        }
    }
}

@Composable
fun SarTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    maxLength: Int = 120,
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.take(maxLength)) },
        singleLine = singleLine,
        textStyle = SarType.Body.copy(fontSize = SarType.Body.fontSize * 1.05f),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(SarColors.Cyan),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box(
                Modifier.fillMaxWidth().clip(SarShapes.Input).background(SarColors.BgSurface)
                    .border(1.dp, SarColors.LineSubtle, SarShapes.Input).padding(horizontal = 16.dp, vertical = 15.dp),
            ) {
                if (value.isEmpty()) Text(placeholder, style = SarType.Body, color = SarColors.TextSecondary)
                inner()
            }
        },
    )
}
