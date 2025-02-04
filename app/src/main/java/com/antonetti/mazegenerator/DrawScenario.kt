package com.antonetti.mazegenerator

import android.graphics.Paint

data class DrawScenario(val zoomLevel : Float, val xTrans : Float, val yTrans : Float,
                        val paintMapping : Map<ColorMapping, Paint>)
