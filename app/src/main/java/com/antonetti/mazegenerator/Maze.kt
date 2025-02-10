package com.antonetti.mazegenerator

import android.util.Rational
import com.antonetti.util.RationalExtensions.plus
import com.antonetti.util.RationalExtensions.minus
import com.antonetti.util.RationalExtensions.times
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.random.Random
import kotlin.math.atan2
import kotlin.math.floor

class Maze (val x : Int, val y : Int) {

    data class Square(val x: Int, val y: Int)

    val UP : Int = 2;
    val DOWN : Int = 1;
    val UNDETERMINED : Int = 0;
    val ENT_EXIT : Int = 3;

    val rand = Random(1);
    var occlusionCulling : Boolean = false;
    val squares: Array<IntArray> = Array(x) { row ->
        IntArray(y) { col -> row * y + col + 1 }
    }
    val vertical: Array<IntArray> = Array(x + 1) { IntArray(y) { UNDETERMINED } }
    val horizontal: Array<IntArray> = Array(x) { IntArray(y + 1) { UNDETERMINED } }

    var playerX : Int = 0
    var playerY : Int = 0;

    var exitX : Int = 0
    var exitY : Int = 0;

    fun resolveSquares(startingSquare : Square, from: Int, to: Int) {
        val queue = LinkedList<Square>();
        queue.add(startingSquare)
        while (!queue.isEmpty()) {
            val s = queue.remove()
            squares[s.x][s.y] = to
            if (s.x + 1 < squares.size && squares[s.x + 1][s.y] == from) {
                queue.add(Square(s.x + 1, s.y))
                squares[s.x + 1][s.y] = to
            }
            if (s.x > 0 && squares[s.x - 1][s.y] == from) {
                queue.add(Square(s.x - 1, s.y))
                squares[s.x - 1][s.y] = to
            }
            if (s.y + 1 < squares[0].size && squares[s.x][s.y + 1] == from) {
                queue.add(Square(s.x, s.y + 1))
                squares[s.x][s.y + 1] = to
            }
            if (s.y > 0 && squares[s.x][s.y - 1] == from) {
                queue.add(Square(s.x, s.y - 1))
                squares[s.x][s.y - 1] = to
            }
        }
    }

    fun toggleCulling() {
        occlusionCulling = !occlusionCulling
    }

    fun initLines() : MutableList<MazeLine> {
        val lines: MutableList<MazeLine> = ArrayList()

        for (i in 1 until vertical.size - 1) {
            for (j in vertical[i].indices) {
                if (squares[i - 1][j] != squares[i][j]) {
                    lines.add(MazeLine(i, j, false))
                } else if (vertical[i][j] == UNDETERMINED){
                    vertical[i][j] = UP
                }
            }
        }
        for (i in horizontal.indices) {
            for (j in 1 until horizontal[i].size - 1) {
                if (squares[i][j - 1] != squares[i][j]) {
                    lines.add(MazeLine(i, j, true))
                } else if (horizontal[i][j] == UNDETERMINED) {
                    horizontal[i][j] = UP
                }
            }
        }
        return lines;
    }

    fun generate() : Maze {
        for (j in vertical[0].indices) {
            vertical[0][j] = UP
            vertical[vertical.size - 1][j] = UP
        }
        for (i in horizontal.indices) {
            horizontal[i][0] = UP
            horizontal[i][horizontal[i].size-1] = UP
        }

        val size = (vertical[0].size + horizontal.size - 1) / 2
        val start = rand.nextInt(size);
        val end = rand.nextInt(size);
        val halfVert = vertical[0].size / 2
        if (start < halfVert) {
            vertical[0][start] = ENT_EXIT
            playerY = start
            playerX = 0
        } else {
            horizontal[start - halfVert][0] = ENT_EXIT
            playerY = 0
            playerX = start - halfVert
        }

        if (end < halfVert) {
            val endIndex = vertical[vertical.size - 1].size - end - 1
            vertical[vertical.size - 1][endIndex] = ENT_EXIT
            exitX = squares.size - 1
            exitY = endIndex
        } else {
            val exitIndex = horizontal.size - 1 - (end - halfVert);
            horizontal[exitIndex][horizontal[exitIndex].size - 1] = ENT_EXIT
            exitX = exitIndex
            exitY = squares[exitIndex].size - 1
        }

        var lines = initLines()
        while (lines.isNotEmpty()) {
            var index = rand.nextInt(lines.size)
            var l : MazeLine = lines.get(index)
            lines.removeAt(index);
            if (l.horizontal) {
                if (squares[l.x][l.y - 1] != squares[l.x][l.y]) {
                    horizontal[l.x][l.y] = DOWN
                    resolveSquares(Square(l.x, l.y - 1), squares[l.x][l.y - 1], squares[l.x][l.y])
                } else {
                    lines = initLines()
                }
            } else {
                if (squares[l.x - 1][l.y] != squares[l.x][l.y]) {
                    vertical[l.x][l.y] = DOWN
                    resolveSquares(Square(l.x - 1, l.y), squares[l.x - 1][l.y], squares[l.x][l.y])
                } else {
                    lines = initLines()
                }
            }
        }

        for (i in squares.indices) {
            for (j in squares[i].indices) {
                squares[i][j]=Int.MAX_VALUE
            }
        }
        squares[exitX][exitY]=0
        val queue : Queue<Square> = LinkedList()
        queue.add(Square(exitX, exitY))
        while(!queue.isEmpty()) {
            val square = queue.remove()
            if (horizontal[square.x][square.y] == DOWN && square.y > 0
                && squares[square.x][square.y - 1] == Int.MAX_VALUE) {
                squares[square.x][square.y - 1] = squares[square.x][square.y] + 1
                queue.add(Square(square.x, square.y - 1))
            }
            if (vertical[square.x][square.y] == DOWN && square.x > 0
                && squares[square.x - 1][square.y] == Int.MAX_VALUE) {
                squares[square.x - 1][square.y] = squares[square.x][square.y] + 1
                queue.add(Square(square.x - 1, square.y))
            }
            if (horizontal[square.x][square.y + 1] == DOWN && square.y < squares[0].size
                && squares[square.x][square.y + 1] == Int.MAX_VALUE) {
                squares[square.x][square.y + 1] = squares[square.x][square.y] + 1
                queue.add(Square(square.x, square.y + 1))
            }
            if (vertical[square.x + 1][square.y] == DOWN && square.x < squares.size
                && squares[square.x + 1][square.y] == Int.MAX_VALUE) {
                squares[square.x + 1][square.y] = squares[square.x][square.y] + 1
                queue.add(Square(square.x + 1, square.y))
            }
        }
        return this
    }

    fun calculatePlayerSlope(x : Int, y : Int) : Rational {
        return Rational(x * 2 - playerX * 2 + 1, y - playerY * 2 + 1)
    }

    data class Point(val x : Float, val y : Float, val horizontal : Boolean, val isUp : Boolean)
    var pointsList : MutableList<Square> = ArrayList();

    fun getPointPlayerDistance(pt : Point) : Float {
        val xDist = playerX - pt.x
        val yDist = playerY - pt.y
        return xDist * xDist + yDist * yDist
    }

    fun getPointPlayerDistance(pt : Square) : Int {
        val xDist = playerX - pt.x
        val yDist = playerY - pt.y
        return xDist * xDist + yDist * yDist
    }

    fun calculateFullAngle(x2: Double, y2: Double): Double {
        val deltaY = y2 - playerY
        val deltaX = x2 - playerX
        var angle = atan2(deltaY, deltaX)

        if (angle < 0) {
            angle += Math.PI * 2
        }

        return angle
    }

    fun ccwVertex(pt : Point) : Square {
        if (pt.horizontal) {
            if (calculateFullAngle(floor(pt.x.toDouble()), floor(pt.y.toDouble())) >
                calculateFullAngle(floor((pt.x + 1).toDouble()), floor(pt.y.toDouble()))) {
                return Square(pt.x.toInt(), pt.y.toInt())
            } else {
                return Square((pt.x + 1).toInt(), pt.y.toInt())
            }
        } else {
            if (calculateFullAngle(floor(pt.x.toDouble()), floor(pt.y.toDouble())) >
                calculateFullAngle(floor((pt.x).toDouble()), floor((pt.y+ 1).toDouble()))) {
                return Square(pt.x.toInt(), pt.y.toInt())
            } else {
                return Square(pt.x.toInt(), (pt.y + 1).toInt())
            }
        }
    }

    fun crossProduct (s1 : Square, s2 : Square) : Double {
        val playerXPos = Rational(playerX * 2 + 1, 2)
        val playerYPos = Rational(playerY * 2 + 1, 2)
        return (s1.x - playerXPos.toDouble()) * (s2.y - playerXPos.toDouble()) -
                (s2.x - playerXPos.toDouble()) * (s1.y - playerXPos.toDouble())
    }

    fun findNearestCWIntercept(initalSquare : Square) : Square {
        val playerXPos = Rational(playerX * 2 + 1, 2)
        val playerYPos = Rational(playerY * 2 + 1, 2)

        val squareVert : Square = Square(Int.MAX_VALUE, Int.MAX_VALUE);
        val squareVHoriz : Square = Square(Int.MAX_VALUE, Int.MAX_VALUE);
        var slope = calculatePlayerSlope(initalSquare.x, initalSquare.y)
        for (i in playerX + 1 until vertical.size) {
            val targetY = (Rational(i, 1) - playerXPos) * slope
            val crossProductLower = crossProduct(initalSquare, Square(i, targetY.toInt()))
            val crossProductUpper = crossProduct(initalSquare, Square(i, ceil(targetY.toDouble()).toInt()))
        }
        return squareVert
    }

    fun resolveCulling() {
        pointsList = ArrayList();

        var slopeCCW = Square(1, Integer.MAX_VALUE);
        while (true) {
            val square = findNearestCWIntercept(slopeCCW)
            pointsList.add(square)
            //slopeCCW = calculatePlayerSlope(square.x, square.y)
            print(slopeCCW)
        }
    }
}