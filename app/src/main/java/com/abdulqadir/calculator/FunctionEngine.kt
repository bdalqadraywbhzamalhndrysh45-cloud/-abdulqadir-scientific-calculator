package com.abdulqadir.calculator

import kotlin.math.*

/** Safe parser and calculus helpers for user-entered expressions. */
object FunctionEngine {
    private sealed interface N {
        data class Num(val v: Double) : N
        data object X : N
        data class Unary(val op: Char, val a: N) : N
        data class Bin(val op: Char, val a: N, val b: N) : N
        data class Fn(val name: String, val a: N) : N
    }

    fun evaluate(source: String, x: Double): Double = eval(parse(source), x)
    fun derivative(source: String): String = print(diff(parse(source)))

    fun limit(source: String, at: Double): String {
        val n = parse(source)
        if (at.isInfinite()) {
            val vals = listOf(1e3, 1e4, 1e5, 1e6).map { if (at > 0) it else -it }
                .mapNotNull { runCatching { eval(n, it) }.getOrNull()?.takeIf(Double::isFinite) }
            if (vals.size < 3) return "غير محددة عدديًا"
            val a = vals.takeLast(2)
            return if (abs(a[1] - a[0]) < 1e-3 * max(1.0, abs(a[1]))) fmt(a[1]) else "قد تكون غير منتهية أو غير موجودة"
        }
        val scales = listOf(1e-2, 1e-3, 1e-4, 1e-5, 1e-6, 1e-7)
        val left = scales.mapNotNull { h -> runCatching { eval(n, at - h) }.getOrNull()?.takeIf(Double::isFinite) }
        val right = scales.mapNotNull { h -> runCatching { eval(n, at + h) }.getOrNull()?.takeIf(Double::isFinite) }
        if (left.isEmpty() || right.isEmpty()) return "النهاية غير محسومة عدديًا"
        val l = left.last(); val r = right.last()
        val tolerance = 2e-3 * max(1.0, max(abs(l), abs(r)))
        if (abs(l - r) > tolerance) return "النهاية الثنائية غير موجودة (اليسار " + fmt(l) + "، اليمين " + fmt(r) + ")"
        return "~ " + fmt((l + r) / 2) + " (تقريب عددي)"
    }

    private fun parse(source: String): N {
        require(source.isNotBlank()) { "اكتب الدالة أولًا" }
        require(source.length <= 300) { "الدالة طويلة جدًا" }
        val p = Parser(source.replace("×", "*").replace("÷", "/").replace("−", "-").replace("π", "pi"))
        val n = p.expression()
        require(p.done()) { "صيغة غير مكتملة قرب: " + p.remaining() }
        return n
    }

    private class Parser(private val s: String) {
        private var i = 0
        fun done(): Boolean { spaces(); return i >= s.length }
        fun remaining(): String = s.substring(i.coerceIn(0, s.length))
        private fun spaces() { while (i < s.length && s[i].isWhitespace()) i++ }
        private fun eat(c: Char): Boolean { spaces(); if (i < s.length && s[i] == c) { i++; return true }; return false }
        fun expression(): N {
            var n = term()
            while (true) n = when { eat('+') -> N.Bin('+', n, term()); eat('-') -> N.Bin('-', n, term()); else -> return n }
        }
        private fun term(): N {
            var n = unary()
            while (true) n = when {
                eat('*') -> N.Bin('*', n, unary())
                eat('/') -> N.Bin('/', n, unary())
                startsPrimary() -> N.Bin('*', n, unary())
                else -> return n
            }
        }
        private fun startsPrimary(): Boolean {
            spaces()
            return i < s.length && (s[i].isLetter() || s[i].isDigit() || s[i] == '.' || s[i] == '(')
        }
        private fun power(): N {
            val base = primary()
            return if (eat('^')) N.Bin('^', base, unary()) else base
        }
        private fun unary(): N = when { eat('+') -> unary(); eat('-') -> N.Unary('-', unary()); else -> power() }
        private fun primary(): N {
            spaces()
            require(i < s.length) { "تعبير غير مكتمل" }
            if (eat('(')) { val n = expression(); require(eat(')')) { "قوس إغلاق مفقود" }; return n }
            if (s[i].isDigit() || s[i] == '.') {
                val start = i
                while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                if (i < s.length && s[i] in "eE") {
                    val save=i; i++; if(i<s.length&&s[i] in "+-")i++; val d=i
                    while(i<s.length&&s[i].isDigit())i++; if(d==i)i=save
                }
                return N.Num(s.substring(start, i).toDouble())
            }
            require(s[i].isLetter()) { "رمز غير مدعوم: " + s[i] }
            val start = i; while (i < s.length && s[i].isLetter()) i++
            val name = s.substring(start, i).lowercase()
            if (name == "x") return N.X
            if (name == "pi") return N.Num(Math.PI)
            if (name == "e") return N.Num(Math.E)
            require(name in setOf("sin","cos","tan","asin","acos","atan","sinh","cosh","tanh","sqrt","ln","log","abs","exp")) { "دالة غير معروفة: " + name }
            val arg = if (eat('(')) { val a=expression(); require(eat(')')) { "قوس إغلاق مفقود" }; a } else unary()
            return N.Fn(name, arg)
        }
    }

    private fun eval(n: N, x: Double): Double = when (n) {
        is N.Num -> n.v
        N.X -> x
        is N.Unary -> -eval(n.a, x)
        is N.Bin -> { val a=eval(n.a,x); val b=eval(n.b,x); when(n.op) {'+'->a+b;'-'->a-b;'*'->a*b;'/'->a/b;'^'->a.pow(b);else->Double.NaN} }
        is N.Fn -> { val a=eval(n.a,x); when(n.name) {
            "sin"->sin(a); "cos"->cos(a); "tan"->tan(a); "asin"->asin(a); "acos"->acos(a); "atan"->atan(a)
            "sinh"->sinh(a); "cosh"->cosh(a); "tanh"->tanh(a); "sqrt"->sqrt(a); "ln"->ln(a); "log"->log10(a)
            "abs"->abs(a); "exp"->exp(a); else->Double.NaN
        } }
    }

    private fun diff(n: N): N = when (n) {
        is N.Num -> N.Num(0.0)
        N.X -> N.Num(1.0)
        is N.Unary -> N.Unary('-', diff(n.a))
        is N.Bin -> {
            val a=n.a; val b=n.b; val da=diff(a); val db=diff(b)
            when(n.op) {
                '+' -> N.Bin('+',da,db)
                '-' -> N.Bin('-',da,db)
                '*' -> N.Bin('+',N.Bin('*',da,b),N.Bin('*',a,db))
                '/' -> N.Bin('/',N.Bin('-',N.Bin('*',da,b),N.Bin('*',a,db)),N.Bin('^',b,N.Num(2.0)))
                '^' -> if (!containsX(b)) N.Bin('*',N.Bin('*',b,N.Bin('^',a,N.Bin('-',b,N.Num(1.0)))),da)
                    else N.Bin('*',N.Bin('^',a,b),N.Bin('+',N.Bin('*',db,N.Fn("ln",a)),N.Bin('*',b,N.Bin('/',da,a))))
                else -> N.Num(Double.NaN)
            }
        }
        is N.Fn -> {
            val a=n.a; val da=diff(a)
            val outer = when(n.name) {
                "sin" -> N.Fn("cos",a)
                "cos" -> N.Unary('-',N.Fn("sin",a))
                "tan" -> N.Bin('/',N.Num(1.0),N.Bin('^',N.Fn("cos",a),N.Num(2.0)))
                "ln" -> N.Bin('/',N.Num(1.0),a)
                "log" -> N.Bin('/',N.Num(1.0),N.Bin('*',a,N.Num(ln(10.0))))
                "exp" -> N.Fn("exp",a)
                "sqrt" -> N.Bin('/',N.Num(1.0),N.Bin('*',N.Num(2.0),N.Fn("sqrt",a)))
                "asin" -> N.Bin('/',N.Num(1.0),N.Fn("sqrt",N.Bin('-',N.Num(1.0),N.Bin('^',a,N.Num(2.0)))))
                "acos" -> N.Unary('-',N.Bin('/',N.Num(1.0),N.Fn("sqrt",N.Bin('-',N.Num(1.0),N.Bin('^',a,N.Num(2.0))))))
                "atan" -> N.Bin('/',N.Num(1.0),N.Bin('+',N.Num(1.0),N.Bin('^',a,N.Num(2.0))))
                "sinh" -> N.Fn("cosh",a); "cosh" -> N.Fn("sinh",a)
                "tanh" -> N.Bin('/',N.Num(1.0),N.Bin('^',N.Fn("cosh",a),N.Num(2.0)))
                "abs" -> N.Bin('*',N.Bin('/',a,N.Fn("abs",a)),da)
                else -> error("لا يدعم الاشتقاق الرمزي الدالة " + n.name + " حاليًا")
            }
            N.Bin('*',outer,da)
        }
    }
    private fun containsX(n:N):Boolean=when(n){N.X->true;is N.Num->false;is N.Unary->containsX(n.a);is N.Bin->containsX(n.a)||containsX(n.b);is N.Fn->containsX(n.a)}
    private fun fmt(v:Double):String=if(!v.isFinite()) v.toString() else if(abs(v)<1e-10) "0" else String.format(java.util.Locale.US,"%.8g",v)
    private fun print(n:N,parent:Int=0):String = when(n){
        is N.Num -> fmt(n.v)
        N.X -> "x"
        is N.Unary -> "-"+print(n.a,4)
        is N.Fn -> n.name+"("+print(n.a)+")"
        is N.Bin -> {
            val prec=when(n.op){'+','-'->1;'*','/'->2;'^'->3;else->0}
            val rightPrec=prec+if(n.op=='-'||n.op=='/')1 else 0
            val s=when(n.op){'+','-','*','/'->print(n.a,prec)+" "+n.op+" "+print(n.b,rightPrec);'^'->print(n.a,prec)+"^"+print(n.b,prec);else->""}
            if(prec<parent) "("+s+")" else s
        }
    }
}
