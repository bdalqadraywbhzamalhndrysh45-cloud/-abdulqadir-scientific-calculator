'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
global.window = global;
require('../math-engine.js');
const engine = global.AQMath;

test('evaluates arithmetic and implicit multiplication', () => {
  assert.equal(engine.evaluate('2x + 1', {x:3}), 7);
  assert.equal(engine.evaluate('(x + 1)(x - 1)', {x:4}), 15);
});

test('evaluates common functions and constants', () => {
  assert.ok(Math.abs(engine.evaluate('sin(pi / 2)') - 1) < 1e-10);
  assert.ok(Math.abs(engine.evaluate('ln(e)') - 1) < 1e-10);
  assert.equal(engine.evaluate('sqrt(81)'), 9);
});

test('produces a derivative that evaluates correctly', () => {
  const derivative = engine.derivative('x^3 - 2x + 1');
  assert.ok(Math.abs(engine.evaluate(derivative, {x:2}) - 10) < 1e-9);
});

test('estimates a removable limit numerically', () => {
  const result = engine.limit('sin(x) / x', 0);
  assert.match(result, /1/);
  assert.match(result, /تقريب عددي/);
});

test('detects a two-sided limit that does not exist', () => {
  assert.match(engine.limit('1/x', 0), /غير موجودة/);
});

test('rejects unsupported symbols instead of evaluating arbitrary code', () => {
  assert.throws(() => engine.evaluate('x + alert(1)', {x:1}));
});