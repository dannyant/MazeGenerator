package com.antonetti.mazegenerator

import android.util.Rational
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.random.Random

class Maze (val x : Int, val y : Int) {

    data class Square(val x: Int, val y: Int)

    val UP : Int = 2;
    val DOWN : Int = 1;
    val UNDETERMINED : Int = 0;
    val ENT_EXIT : Int = 3;
    val CULLING_EPSILON = 0.001

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

    fun resolveSquares(from: Int, to: Int) {
        squares.forEachIndexed { i, row ->
            row.forEachIndexed { j, value ->
                if (value == from) squares[i][j] = to
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
                    resolveSquares(squares[l.x][l.y - 1], squares[l.x][l.y])
                } else {
                    lines = initLines()
                }
            } else {
                if (squares[l.x - 1][l.y] != squares[l.x][l.y]) {
                    vertical[l.x][l.y] = DOWN
                    resolveSquares(squares[l.x - 1][l.y], squares[l.x][l.y])
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

    data class Point(val x : Float, val y : Float);
    var pointsList : MutableList<Point> = ArrayList();

    fun findNearestCWIntercept(slopeCW : Rational) : Rational {
        var x = playerX + 0.5
        var y = playerY + 0.5
        val slope = slopeCW.toDouble()

        // Determine step direction
        val xStep = if (slopeCW.numerator >= 0) 1 else -1
        val yStep = if (slopeCW.numerator > 0) 1 else -1

        var count = 10
        while(count > 0) {
            count--
            // Find the next vertical and horizontal intercepts
            val nextVerticalX = if (xStep > 0) kotlin.math.ceil(x) else kotlin.math.floor(x) - 1
            val nextVerticalY = y + slope * (nextVerticalX - x)

            val nextHorizontalY = if (yStep > 0) kotlin.math.ceil(y) else kotlin.math.floor(y) - 1
            val nextHorizontalX = x + (nextHorizontalY - y) / slope

            // Determine which intercept is closer
            val distToVertical = abs(nextVerticalX - x)
            val distToHorizontal = abs(nextHorizontalY - y)

            var findClosest : MutableList<Point> = ArrayList();
            if (nextVerticalX.toInt() < vertical.size &&
                nextVerticalY.toInt() < vertical[0].size &&
                vertical[nextVerticalX.toInt()][nextVerticalY.toInt()] == UP) {
                findClosest.add(Point(nextVerticalX.toFloat(), nextVerticalY.toFloat()))
            }

            if (nextHorizontalX.toInt() < horizontal.size &&
                nextHorizontalY.toInt() < horizontal[0].size &&
                horizontal[nextHorizontalX.toInt()][nextHorizontalY.toInt()] == UP) {
                findClosest.add(Point(nextHorizontalX.toFloat(), nextHorizontalY.toFloat()))
            }

            if (distToVertical < distToHorizontal) {
                if (vertical[nextVerticalX.toInt()][nextVerticalY.toInt()] == UP) {
                    pointsList.add(Point(nextVerticalX.toFloat(), nextVerticalY.toFloat()))
                    return calculatePlayerSlope(nextVerticalX.toInt(), ceil(nextVerticalY).toInt());
                }
                x = nextVerticalX
                y = nextVerticalY
            } else if (distToVertical > distToHorizontal || (distToVertical == distToHorizontal && distToVertical != 0.0)) {
                if (horizontal[nextHorizontalX.toInt()][nextHorizontalY.toInt()] == UP) {
                    pointsList.add(Point(nextHorizontalX.toFloat(), nextHorizontalY.toFloat()))
                    return calculatePlayerSlope(ceil(nextVerticalX).toInt(), nextVerticalY.toInt());
                }
                x = nextHorizontalX
                y = nextHorizontalY
            } else {

                x += CULLING_EPSILON
                y += CULLING_EPSILON * slope
            }
        }
        return slopeCW
    }

    fun resolveCulling() {
        pointsList = ArrayList();

        var slopeCW : Rational = Rational(-1, -1);
        var slopeCCW : Rational = Rational(-1, -1);
        var initalLine = playerY
        while (initalLine >= 0) {
            if (horizontal[playerX][initalLine] == UP) {
                slopeCCW = calculatePlayerSlope(playerX + 1, initalLine)
                slopeCW = calculatePlayerSlope(playerX, initalLine)
                pointsList.add(Point(playerX.toFloat() + 1, initalLine.toFloat()))
                pointsList.add(Point(playerX.toFloat(), initalLine.toFloat()))
                initalLine = -1
            }
            initalLine--
        }

        var slopeCWEnd : Rational = Rational(-1, -1);
        var slopeCCWEnd : Rational = Rational(-1, -1);
        initalLine = playerY + 1
        while (initalLine < horizontal[playerX].size) {
            if (horizontal[playerX][initalLine] == UP) {
                slopeCCWEnd = calculatePlayerSlope(playerX + 1, initalLine)
                slopeCWEnd = calculatePlayerSlope(playerX, initalLine)
                pointsList.add(Point(playerX.toFloat() + 1, initalLine.toFloat()))
                pointsList.add(Point(playerX.toFloat(), initalLine.toFloat()))
                initalLine = horizontal[playerX].size
            }
            initalLine++
        }

        slopeCW = findNearestCWIntercept(slopeCW)
        slopeCW = findNearestCWIntercept(slopeCW)
        slopeCW = findNearestCWIntercept(slopeCW)
        slopeCW = findNearestCWIntercept(slopeCW)


        //var count = 0;
        //while (slopeCW > slopeCCWEnd && count < 10) {
        //    slopeCW = findNearestCWIntercept(slopeCW)
        //    count++
        //}

    }
}