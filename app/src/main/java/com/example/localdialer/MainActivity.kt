package com.example.localdialer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject

private val ComponentActivity.store by preferencesDataStore(name = "local_dialer")

private data class DialTheme(
    val name: String = "Violet Night",
    val mode: String = "dark",
    val background: Long = 0xFF101114,
    val surface: Long = 0xFF202126,
    val button: Long = 0xFF303137,
    val text: Long = 0xFFFFFFFF,
    val secondary: Long = 0xFFB9BAC4,
    val accent: Long = 0xFF8B5CF6,
    val call: Long = 0xFF8B5CF6,
    val icon: Long = 0xFFFFFFFF,
    val divider: Long = 0xFF41424A
) {
    fun encode() = JSONObject().apply {
        put("name", name); put("mode", mode)
        put("background", background.toString()); put("surface", surface.toString())
        put("button", button.toString()); put("text", text.toString())
        put("secondary", secondary.toString()); put("accent", accent.toString())
        put("call", call.toString()); put("icon", icon.toString()); put("divider", divider.toString())
    }.toString()

    companion object {
        fun decode(raw: String): DialTheme = try {
            val j = JSONObject(raw)
            fun color(k: String, default: Long) = j.optString(k, default.toString()).toLongOrNull() ?: default
            DialTheme(
                name = j.optString("name", "Custom"),
                mode = j.optString("mode", "dark"),
                background = color("background", 0xFF101114),
                surface = color("surface", 0xFF202126),
                button = color("button", 0xFF303137),
                text = color("text", 0xFFFFFFFF),
                secondary = color("secondary", 0xFFB9BAC4),
                accent = color("accent", 0xFF8B5CF6),
                call = color("call", 0xFF8B5CF6),
                icon = color("icon", 0xFFFFFFFF),
                divider = color("divider", 0xFF41424A)
            )
        } catch (_: Exception) { DialTheme() }
    }
}

private fun Long.c() = Color(this)
private fun parseHex(s: String): Long? {
    val clean = s.trim().removePrefix("#")
    if (!clean.matches(Regex("[0-9a-fA-F]{6}"))) return null
    return try { 0xFF000000L or clean.toLong(16) } catch (_: Exception) { null }
}
private fun contrast(bg: Color): Color {
    val luminance = .2126 * bg.red + .7152 * bg.green + .0722 * bg.blue
    return if (luminance > .56) Color(0xFF111111) else Color.White
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val scope = rememberCoroutineScope()
            val pref = stringPreferencesKey("active_theme")
            var theme by remember { mutableStateOf(DialTheme()) }
            var loaded by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                val saved = store.data.first()[pref]
                if (saved != null) theme = DialTheme.decode(saved)
                loaded = true
            }
            fun update(next: DialTheme) {
                theme = next
                scope.launch { store.edit { it[pref] = next.encode() } }
            }
            MaterialTheme(colorScheme = darkColorScheme(
                background = theme.background.c(), surface = theme.surface.c(),
                primary = theme.accent.c(), onBackground = theme.text.c(), onSurface = theme.text.c()
            )) {
                if (loaded) DialerApp(
                    theme = theme,
                    onTheme = ::update,
                    onDial = { number ->
                        if (number.isNotBlank()) {
                            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}")))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun DialerApp(theme: DialTheme, onTheme: (DialTheme) -> Unit, onDial: (String) -> Unit) {
    var number by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf(false) }
    if (editing) {
        ThemeStudio(theme, onTheme, onBack = { editing = false }) { named ->
            onTheme(named)
        }
    } else {
        val bg = theme.background.c()
        Column(
            Modifier.fillMaxSize().background(bg).padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Phone", Modifier.weight(1f), color = theme.text.c(), fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = { editing = true }) {
                    Icon(Icons.Default.Palette, "Customize colors", tint = theme.icon.c())
                }
            }
            Spacer(Modifier.weight(1f))
            Text(number.ifBlank { "Enter a number" }, color = if (number.isBlank()) theme.secondary.c() else theme.text.c(), fontSize = 27.sp)
            Spacer(Modifier.height(24.dp))
            val keys = listOf(
                listOf("1" to "", "2" to "ABC", "3" to "DEF"),
                listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
                listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
                listOf("*" to "", "0" to "+", "#" to "")
            )
            keys.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    row.forEach { (digit, letters) ->
                        Column(
                            Modifier.weight(1f).padding(vertical = 5.dp).aspectRatio(1f)
                                .background(theme.button.c(), CircleShape)
                                .clickable { number += digit },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(digit, color = theme.text.c(), fontSize = 25.sp)
                            if (letters.isNotEmpty()) Text(letters, color = theme.secondary.c(), fontSize = 9.sp, letterSpacing = 1.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.size(54.dp))
                IconButton(
                    onClick = { onDial(number) },
                    modifier = Modifier.size(64.dp).background(theme.call.c(), CircleShape)
                ) { Icon(Icons.Default.Call, "Open phone call", tint = contrast(theme.call.c()), modifier = Modifier.size(28.dp)) }
                IconButton(onClick = { if (number.isNotEmpty()) number = number.dropLast(1) }, modifier = Modifier.size(54.dp)) {
                    Icon(Icons.Default.Backspace, "Delete digit", tint = theme.icon.c())
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Local-first • no account • no tracking", color = theme.secondary.c(), fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
        }
    }
}

private data class ColorField(val label: String, val get: (DialTheme) -> Long, val set: (DialTheme, Long) -> DialTheme)
private val colorFields = listOf(
    ColorField("Background", { it.background }, { t, v -> t.copy(background = v) }),
    ColorField("Surface", { it.surface }, { t, v -> t.copy(surface = v) }),
    ColorField("Keypad buttons", { it.button }, { t, v -> t.copy(button = v) }),
    ColorField("Primary text", { it.text }, { t, v -> t.copy(text = v) }),
    ColorField("Secondary text", { it.secondary }, { t, v -> t.copy(secondary = v) }),
    ColorField("Accent", { it.accent }, { t, v -> t.copy(accent = v) }),
    ColorField("Call button", { it.call }, { t, v -> t.copy(call = v) }),
    ColorField("Icons", { it.icon }, { t, v -> t.copy(icon = v) }),
    ColorField("Dividers", { it.divider }, { t, v -> t.copy(divider = v) })
)

@Composable
private fun ThemeStudio(theme: DialTheme, onChange: (DialTheme) -> Unit, onBack: () -> Unit, onSave: (DialTheme) -> Unit) {
    var selected by remember { mutableStateOf(colorFields.first()) }
    var hex by remember(theme, selected) { mutableStateOf("#%06X".format(selected.get(theme).toInt() and 0xFFFFFF)) }
    var name by remember(theme.name) { mutableStateOf(theme.name) }
    val fg = theme.text.c()
    val presets = listOf(
        DialTheme(name = "Violet Night"),
        DialTheme(name = "Ocean", accent = 0xFF38BDF8, call = 0xFF38BDF8),
        DialTheme(name = "Emerald", accent = 0xFF10B981, call = 0xFF10B981),
        DialTheme(name = "Rose", accent = 0xFFF472B6, call = 0xFFF472B6),
        DialTheme(name = "Light", mode = "light", background = 0xFFF5F5F7, surface = 0xFFFFFFFF, button = 0xFFE8E8EE, text = 0xFF17171B, secondary = 0xFF555560, accent = 0xFF7C3AED, call = 0xFF7C3AED, icon = 0xFF17171B, divider = 0xFFD1D1D8),
        DialTheme(name = "AMOLED", mode = "amoled", background = 0xFF000000, surface = 0xFF080808, button = 0xFF191919)
    )
    Column(Modifier.fillMaxSize().background(theme.background.c()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back") }
            Text("Theme studio", color = fg, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().background(theme.surface.c(), RoundedCornerShape(18.dp)).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Live preview", color = fg, fontWeight = FontWeight.SemiBold)
                Text("123 456 7890", color = fg, fontSize = 22.sp)
                Text("Your colors update instantly", color = theme.secondary.c(), fontSize = 12.sp)
            }
            Box(Modifier.size(50.dp).background(theme.call.c(), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Call, null, tint = contrast(theme.call.c()))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Presets", color = fg, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            presets.take(4).forEach { p ->
                AssistChip(onClick = { onChange(p) }, label = { Text(p.name) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            presets.drop(4).forEach { p -> AssistChip(onClick = { onChange(p) }, label = { Text(p.name) }) }
        }
        Spacer(Modifier.height(8.dp))
        Text("Component colors", color = fg, fontWeight = FontWeight.SemiBold)
        LazyColumn(Modifier.weight(1f)) {
            items(colorFields) { field ->
                Row(
                    Modifier.fillMaxWidth().clickable {
                        selected = field
                        hex = "#%06X".format(field.get(theme).toInt() and 0xFFFFFF)
                    }.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(30.dp).background(field.get(theme).c(), RoundedCornerShape(7.dp)).border(1.dp, theme.divider.c(), RoundedCornerShape(7.dp)))
                    Text(field.label, Modifier.weight(1f).padding(start = 10.dp), color = fg)
                    if (field == selected) Text("Selected", color = theme.accent.c(), fontSize = 12.sp)
                }
            }
        }
        Text("Selected: ${selected.label}", color = fg, fontWeight = FontWeight.Medium)
        OutlinedTextField(
            value = hex,
            onValueChange = { value ->
                hex = value
                parseHex(value)?.let { onChange(selected.set(theme, it).copy(name = "Custom")) }
            },
            label = { Text("Hex color (#RRGGBB)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Saved theme name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(
            onClick = {
                val named = theme.copy(name = name.ifBlank { "My theme" })
                onSave(named)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save theme locally") }
        Text("Text contrast on the call button is selected automatically. Theme preferences stay on this device.", color = theme.secondary.c(), fontSize = 11.sp)
    }
}
