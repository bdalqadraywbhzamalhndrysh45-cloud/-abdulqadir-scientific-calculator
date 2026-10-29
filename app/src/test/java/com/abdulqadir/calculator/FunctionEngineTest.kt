package com.abdulqadir.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FunctionEngineTest {
    @Test fun acceptsFreeFormPolynomialAndImplicitMultiplication() {
        assertEquals(14.0, FunctionEngine.evaluate("2x + x^2", 3.0), 1e-9)
    }

    @Test fun differentiatesPolynomialSymbolically() {
        val derivative = FunctionEngine.derivative("x^2")
        assertEquals(6.0, FunctionEngine.evaluate(derivative, 3.0), 1e-8)
    }

    @Test fun differentiatesSineWithChainRule() {
        val derivative = FunctionEngine.derivative("sin(x)")
        assertEquals(kotlin.math.cos(0.4), FunctionEngine.evaluate(derivative, 0.4), 1e-8)
    }

    @Test fun estimatesRemovableLimit() {
        val result = FunctionEngine.limit("sin(x)/x", 0.0)
        assertTrue(result.contains("1.000") || result.contains("~ 1"))
    }
}
