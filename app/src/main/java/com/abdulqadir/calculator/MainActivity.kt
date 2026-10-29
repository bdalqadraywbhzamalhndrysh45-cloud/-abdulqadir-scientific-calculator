package com.abdulqadir.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CalculatorApp() }
    }
}

@Composable
private fun CalculatorApp() {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("0") }
    var error by remember { mutableStateOf(false) }
    var degrees by remember { mutableStateOf(false) }
    val keys = listOf(
        listOf("sin", "cos", "tan", "⌫", "AC"),
        listOf("ln", "log", "√", "(", ")"),
        listOf("7", "8", "9", "÷", "^"),
        listOf("4", "5", "6", "×", "−"),
        listOf("1", "2", "3", "+", "π"),
        listOf("±", "0", ".", "e", "=")
    )
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Color(0xFF72E0C0), secondary = Color(0xFF9BB7FF),
        background = Color(0xFF10141B), surface = Color(0xFF1A202A),
        onBackground = Color(0xFFF4F6FA), onSurface = Color(0xFFF4F6FA)
    )) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("SCIENTIFIC CALCULATOR", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                        Text("عبد القادر", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { degrees = !degrees }) { Text(if (degrees) "DEG" else "RAD") }
                }
                Spacer(Modifier.height(20.dp))
                Box(Modifier.fillMaxWidth().weight(1f).background(Color(0xFF151B24), RoundedCornerShape(28.dp)).padding(20.dp)) {
                    Column(Modifier.fillMaxWidth().align(Alignment.BottomEnd), horizontalAlignment = Alignment.End) {
                        Text(expression.ifBlank { "0" }, color = Color(0xFFAFB9C8), fontSize = 24.sp, textAlign = TextAlign.End, maxLines = 3)
                        Spacer(Modifier.height(12.dp))
                        Text(result, color = if (error) Color(0xFFFF8A8A) else Color.White, fontSize = 38.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, maxLines = 2)
                    }
                    Text("عبد القادر", Modifier.align(Alignment.Center).alpha(0.055f), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(16.dp))
                keys.forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { key ->
                            val special = key in listOf("AC", "⌫", "=", "÷", "×", "−", "+", "^")
                            Button(
                                onClick = {
                                    error = false
                                    when (key) {
                                        "AC" -> { expression = ""; result = "0" }
                                        "⌫" -> expression = expression.dropLast(1)
                                        "=" -> try {
                                            val exp = expression.replace("−", "-")
                                            val value = CalculatorEngine.evaluate(exp)
                                            result = if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
                                        } catch (e: Exception) { result = "تحقق من العملية"; error = true }
                                        "±" -> expression = if (expression.startsWith("-")) expression.drop(1) else "-($expression)"
                                        "√" -> expression += "sqrt("
                                        "sin", "cos", "tan", "ln", "log" -> expression += "$key("
                                        "π" -> expression += "pi"
                                        "e" -> expression += "e"
                                        "×" -> expression += "×"
                                        "÷" -> expression += "÷"
                                        "−" -> expression += "−"
                                        else -> expression += key
                                    }
                                },
                                modifier = Modifier.weight(1f).height(54.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        key == "=" -> Color(0xFF72E0C0)
                                        special -> Color(0xFF293342)
                                        key in listOf("sin", "cos", "tan", "ln", "log", "√") -> Color(0xFF202B3A)
                                        else -> Color(0xFF1A202A)
                                    },
                                    contentColor = if (key == "=") Color(0xFF10201C) else Color.White
                                ), contentPadding = PaddingValues(2.dp)
                            ) { Text(key, fontSize = if (key.length > 2) 12.sp else 17.sp, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
