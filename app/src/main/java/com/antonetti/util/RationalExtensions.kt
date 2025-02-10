package com.antonetti.util

import android.util.Rational

object RationalExtensions {
    // Addition
    operator fun Rational.plus(other: Rational): Rational {
        return Rational(
            this.numerator * other.denominator + other.numerator * this.denominator,
            this.denominator * other.denominator
        )
    }

    // Subtraction
    operator fun Rational.minus(other: Rational): Rational {
        return Rational(
            this.numerator * other.denominator - other.numerator * this.denominator,
            this.denominator * other.denominator
        )
    }

    // Multiplication
    operator fun Rational.times(other: Rational): Rational {
        return Rational(
            this.numerator * other.numerator,
            this.denominator * other.denominator
        )
    }

    // Division
    operator fun Rational.div(other: Rational): Rational {
        require(other.numerator != 0) { "Cannot divide by zero" }
        return Rational(
            this.numerator * other.denominator,
            this.denominator * other.numerator
        )
    }
}