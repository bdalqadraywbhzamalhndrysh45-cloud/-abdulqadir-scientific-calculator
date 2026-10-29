package com.abdulqadir.calculator

import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatorEngineTest {
    @Test fun evaluatesArithmeticWithPrecedence() {
        assertEquals(14.0, CalculatorEngine.evaluate("2+3*4"), 1e-9)
    }
    @Test fun evaluatesParenthesesAndPower() {
        assertEquals(64.0, CalculatorEngine.evaluate("(2+2)^3"), 1e-9)
    }
    @Test fun evaluatesNaturalAndBaseTenLogs() {
        assertEquals(1.0, CalculatorEngine.evaluate("ln(e)"), 1e-9)
        assertEquals(2.0, CalculatorEngine.evaluate("log(100)"), 1e-9)
    }
    @Test fun evaluatesTrigonometryInRadians() {
        assertEquals(1.0, CalculatorEngine.evaluate("sin(90)", degrees = true), 1e-9)
        assertEquals(90.0, CalculatorEngine.evaluate("asin(1)", degrees = true), 1e-9)
    }

    @Test fun evaluatesTrigonometryInRadiansRegression() {
        assertEquals(1.0, CalculatorEngine.evaluate("sin(1.5707963267948966)"), 1e-9)
    }
}
