package com.abdulqadir.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

private val Mint = Color(0xFF72E0C0)
private val Ink = Color(0xFF10141B)
private val Card = Color(0xFF1A202A)
private val Muted = Color(0xFF9BA8BA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(16,20,27)
        window.navigationBarColor = android.graphics.Color.rgb(16,20,27)
        setContent { CalculatorApp() }
    }
}

@Composable
private fun CalculatorApp() {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("0") }
    var error by remember { mutableStateOf(false) }
    var degrees by remember { mutableStateOf(false) }
    var function by remember { mutableStateOf("x^2 - 4") }
    var point by remember { mutableStateOf("0") }
    var xMin by remember { mutableStateOf("-10") }
    var xMax by remember { mutableStateOf("10") }
    var labResult by remember { mutableStateOf("اكتب أي دالة بدلالة x ثم اختر العملية.") }
    var labTitle by remember { mutableStateOf("مختبر الدوال") }
    var plot by remember { mutableStateOf(false) }
    var samples by remember { mutableStateOf(emptyList<Pair<Double, Double>>()) }
    var selectedTab by remember { mutableIntStateOf(0) }

    MaterialTheme(colorScheme = darkColorScheme(primary = Mint, secondary = Color(0xFF9BB7FF), background = Ink, surface = Card, onBackground = Color.White, onSurface = Color.White)) {
        Surface(Modifier.fillMaxSize(), color = Ink) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("عبد القادر", fontSize = 27.sp, fontWeight = FontWeight.Black)
                        Text("آلة حاسبة علمية متقدمة", color = Muted, fontSize = 12.sp)
                    }
                    Text("PRO", color = Mint, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(16.dp)).padding(5.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    FilterChip(selected = selectedTab == 0, onClick = { selectedTab = 0 }, label = { Text("الحاسبة") }, modifier = Modifier.weight(1f))
                    FilterChip(selected = selectedTab == 1, onClick = { selectedTab = 1 }, label = { Text("مختبر الدوال") }, modifier = Modifier.weight(1f))
                }
                if (selectedTab == 0) {
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(24.dp)).padding(20.dp), horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("الوضع الزاوي", color = Muted, fontSize = 12.sp)
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { degrees = !degrees }) { Text(if (degrees) "DEG" else "RAD", color = Mint) }
                        }
                        Text(expression.ifBlank { "0" }, color = Muted, fontSize = 22.sp, textAlign = TextAlign.End)
                        Text(result, color = if (error) Color(0xFFFF8888) else Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    }
                    Spacer(Modifier.height(12.dp))
                    val keys = listOf(
                        listOf("sin", "cos", "tan", "⌫", "AC"),
                        listOf("ln", "log", "sqrt", "(", ")"),
                        listOf("7", "8", "9", "÷", "^"),
                        listOf("4", "5", "6", "×", "−"),
                        listOf("1", "2", "3", "+", "π"),
                        listOf("±", "0", ".", "e", "=")
                    )
                    keys.forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            row.forEach { key ->
                                val special = key in listOf("AC", "⌫", "=", "÷", "×", "−", "+", "^")
                                Button(onClick = {
                                    error = false
                                    when (key) {
                                        "AC" -> { expression = ""; result = "0" }
                                        "⌫" -> expression = expression.dropLast(1)
                                        "=" -> try {
                                            val normalized = expression.replace("−", "-").replace("×", "*").replace("÷", "/").replace("π", "pi")
                                            val v = CalculatorEngine.evaluate(normalized, degrees)
                                            result = if (v.isFinite()) String.format(java.util.Locale.US, "%.10g", v) else "غير معرّف"
                                        } catch (e: Exception) { result = e.message ?: "خطأ في التعبير"; error = true }
                                        "±" -> expression = if (expression.startsWith("-")) expression.drop(1) else "-($expression)"
                                        "sqrt" -> expression += "sqrt("
                                        "sin", "cos", "tan", "ln", "log" -> expression += "$key("
                                        "π" -> expression += "pi"
                                        "e" -> expression += "e"
                                        "×" -> expression += "*"
                                        "÷" -> expression += "/"
                                        "−" -> expression += "-"
                                        else -> expression += key
                                    }
                                }, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(13.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (key == "=") Mint else if (special) Color(0xFF293342) else Color(0xFF202B3A), contentColor = if (key == "=") Ink else Color.White),
                                    contentPadding = PaddingValues(1.dp)) {
                                    Text(key, fontSize = if (key.length > 3) 11.sp else 16.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Spacer(Modifier.height(7.dp))
                    }
                } else {
                    Spacer(Modifier.height(14.dp))
                    Column(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(22.dp)).padding(16.dp)) {
                        Text("مختبر الدوال", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("اكتب الدالة بحرية: كثيرات الحدود، الكسور، المثلثية واللوغاريتمية", color = Muted, fontSize = 12.sp)
                        Spacer(Modifier.height(14.dp))
                        Text("الدالة f(x)", color = Mint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = function, onValueChange = { function = it; plot = false }, modifier = Modifier.fillMaxWidth(), singleLine = false, minLines = 1, maxLines = 3, placeholder = { Text("مثال: (x^3 - 2x + 1)/(x^2 + 4)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii))
                        Spacer(Modifier.height(8.dp))
                        Text("أمثلة سريعة", color = Muted, fontSize = 12.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("x^2 - 4", "sin(x)", "ln(x)", "1/x").forEach { ex ->
                                SuggestionChip(onClick = { function = ex; plot = false }, label = { Text(ex, fontSize = 11.sp) })
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Button(onClick = {
                            runCatching { FunctionEngine.derivative(function) }
                                .onSuccess { labTitle = "المشتقة f′(x)"; labResult = it }
                                .onFailure { labTitle = "تعذر الاشتقاق"; labResult = it.message ?: "راجع صيغة الدالة" }
                            plot = false
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Mint, contentColor = Ink)) { Text("إيجاد المشتقة f′(x)", fontWeight = FontWeight.Bold) }
                        Spacer(Modifier.height(8.dp))
                        Text("حساب النهاية عندما x يقترب من:", color = Mint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(value = point, onValueChange = { point = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii), placeholder = { Text("0") })
                        Button(onClick = {
                            val at = when (point.trim().lowercase()) { "inf", "+inf", "∞", "+∞" -> Double.POSITIVE_INFINITY; "-inf", "-∞" -> Double.NEGATIVE_INFINITY; else -> point.toDoubleOrNull() }
                            if (at == null) { labTitle = "قيمة الاقتراب غير صحيحة"; labResult = "اكتب عددًا أو ∞ أو -∞"; return@Button }
                            runCatching { FunctionEngine.limit(function, at) }
                                .onSuccess { labTitle = "النهاية عند x → $point"; labResult = it }
                                .onFailure { labTitle = "تعذر حساب النهاية"; labResult = it.message ?: "راجع الدالة" }
                            plot = false
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF293342))) { Text("احسب النهاية") }
                        Spacer(Modifier.height(10.dp))
                        Text("مجال الرسم", color = Mint, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = xMin, onValueChange = { xMin = it }, modifier = Modifier.weight(1f), label = { Text("من x") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                            OutlinedTextField(value = xMax, onValueChange = { xMax = it }, modifier = Modifier.weight(1f), label = { Text("إلى x") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        }
                        Button(onClick = {
                            try {
                                val lo = xMin.toDouble(); val hi = xMax.toDouble()
                                require(lo.isFinite() && hi.isFinite() && hi > lo && hi - lo <= 1e6) { "تأكد أن الحد الأعلى أكبر من الأدنى." }
                                samples = (0..500).map { i ->
                                    val x = lo + (hi - lo) * i / 500.0
                                    x to runCatching { FunctionEngine.evaluate(function, x) }.getOrDefault(Double.NaN)
                                }
                                require(samples.any { it.second.isFinite() }) { "لا توجد قيم حقيقية في هذا المجال." }
                                plot = true; labTitle = "الرسم البياني"; labResult = "f(x) = $function"
                            } catch (e: Exception) { plot = false; labTitle = "تعذر الرسم"; labResult = e.message ?: "راجع مجال الرسم" }
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF293342))) { Text("رسم الدالة") }
                        Spacer(Modifier.height(12.dp))
                        Column(Modifier.fillMaxWidth().background(Ink, RoundedCornerShape(14.dp)).padding(14.dp)) {
                            Text(labTitle, color = Mint, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(labResult, color = Color.White, fontSize = 16.sp)
                        }
                        if (plot && samples.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            FunctionPlot(samples, Modifier.fillMaxWidth().height(260.dp))
                            Text("الرسم عددي تقريبي. النهايات محسوبة عدديًا وليست برهانًا رمزيًا؛ راجع النتائج قرب نقاط الانقطاع.", color = Muted, fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text("عبد القادر", modifier = Modifier.fillMaxWidth(), color = Color.White.copy(alpha = 0.12f), fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun FunctionPlot(points: List<Pair<Double, Double>>, modifier: Modifier = Modifier) {
    Canvas(modifier.background(Ink, RoundedCornerShape(12.dp)).padding(8.dp)) {
        if (points.size < 2) return@Canvas
        val finite = points.filter { it.second.isFinite() && abs(it.second) < 1e8 }
        if (finite.size < 2) return@Canvas
        var yMin = finite.minOf { it.second }; var yMax = finite.maxOf { it.second }
        if (yMax - yMin < 1e-9) { yMin -= 1.0; yMax += 1.0 }
        val pad = (yMax-yMin)*0.08; yMin -= pad; yMax += pad
        val xMin = points.first().first; val xMax = points.last().first
        val w = size.width; val h = size.height
        val grid = Color(0xFF293342); val axis = Color(0xFF72839A)
        for (i in 0..10) {
            val x = w*i/10f
            drawLine(grid, Offset(x,0f), Offset(x,h), 1f)
        }
        for (i in 0..8) {
            val y = h*i/8f
            drawLine(grid, Offset(0f,y), Offset(w,y), 1f)
        }
        if (xMin <= 0 && xMax >= 0) {
            val px = ((0-xMin)/(xMax-xMin)*w).toFloat()
            drawLine(axis,Offset(px,0f),Offset(px,h),1.5f)
        }
        if (yMin <= 0 && yMax >= 0) {
            val py = (h-(0-yMin)/(yMax-yMin)*h).toFloat()
            drawLine(axis,Offset(0f,py),Offset(w,py),1.5f)
        }
        val path = Path(); var active = false; var lastY = 0f
        points.forEach { (x,y) ->
            if (!y.isFinite() || abs(y)>1e8 || y < yMin-(yMax-yMin)*2 || y > yMax+(yMax-yMin)*2) { active=false }
            else {
                val px = ((x-xMin)/(xMax-xMin)*w).toFloat()
                val py = (h-(y-yMin)/(yMax-yMin)*h).toFloat()
                if (!active || abs(py-lastY)>h*0.45f) path.moveTo(px,py) else path.lineTo(px,py)
                active=true; lastY=py
            }
        }
        drawPath(path, Mint, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f))
    }
}
