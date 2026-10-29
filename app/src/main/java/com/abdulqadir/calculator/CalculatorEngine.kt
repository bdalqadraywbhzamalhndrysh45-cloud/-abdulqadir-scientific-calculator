package com.abdulqadir.calculator

import kotlin.math.*

/** A small recursive-descent expression evaluator with selectable angle units. */
object CalculatorEngine {
    fun evaluate(expression: String, degrees: Boolean = false): Double {
        val parser = Parser(expression.replace("×", "*").replace("÷", "/").replace("π", "pi"), degrees)
        val result = parser.parseExpression()
        parser.skipSpaces()
        require(parser.atEnd()) { "Unexpected input" }
        require(result.isFinite()) { "Result is not finite" }
        return result
    }

    private class Parser(private val source: String, private val degrees: Boolean) {
        private var index = 0
        fun atEnd() = index >= source.length
        fun skipSpaces() { while (!atEnd() && source[index].isWhitespace()) index++ }
        private fun consume(c: Char): Boolean {
            skipSpaces()
            if (!atEnd() && source[index] == c) { index++; return true }
            return false
        }
        fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                value = when {
                    consume('+') -> value + parseTerm()
                    consume('-') -> value - parseTerm()
                    else -> return value
                }
            }
        }
        private fun parseTerm(): Double {
            var value = parsePower()
            while (true) {
                value = when {
                    consume('*') -> value * parsePower()
                    consume('/') -> value / parsePower()
                    else -> return value
                }
            }
        }
        private fun parsePower(): Double {
            val base = parseUnary()
            return if (consume('^')) base.pow(parsePower()) else base
        }
        private fun parseUnary(): Double = when {
            consume('+') -> parseUnary()
            consume('-') -> -parseUnary()
            else -> parsePrimary()
        }
        private fun parsePrimary(): Double {
            skipSpaces()
            if (consume('(')) {
                val value = parseExpression()
                require(consume(')')) { "Missing closing parenthesis" }
                return value
            }
            if (!atEnd() && (source[index].isLetter())) {
                val start = index
                while (!atEnd() && source[index].isLetter()) index++
                val name = source.substring(start, index).lowercase()
                if (name == "pi") return Math.PI
                if (name == "e") return Math.E
                require(consume('(')) { "Use $name(...)" }
                val x = parseExpression()
                require(consume(')')) { "Missing closing parenthesis" }
                return when (name) {
                    "sin" -> sin(if (degrees) Math.toRadians(x) else x)
                    "cos" -> cos(if (degrees) Math.toRadians(x) else x)
                    "tan" -> tan(if (degrees) Math.toRadians(x) else x)
                    "asin" -> if (degrees) Math.toDegrees(asin(x)) else asin(x)
                    "acos" -> if (degrees) Math.toDegrees(acos(x)) else acos(x)
                    "atan" -> if (degrees) Math.toDegrees(atan(x)) else atan(x)
                    "sqrt" -> sqrt(x); "ln" -> ln(x); "log" -> log10(x)
                    "abs" -> abs(x); "exp" -> exp(x)
                    else -> error("Unknown function: $name")
                }
            }
            val start = index
            var seenDot = false
            while (!atEnd()) {
                val c = source[index]
                if (c.isDigit()) index++
                else if (c == '.' && !seenDot) { seenDot = true; index++ }
                else break
            }
            require(index > start) { "Expected a number" }
            return source.substring(start, index).toDouble()
        }
    }
}
