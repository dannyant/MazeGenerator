package com.antonetti.mazegenerator

import java.util.*
import kotlin.collections.ArrayList
import kotlin.random.Random

class Maze (val x : Int, val y : Int) {

    data class Square(val x: Int, val y: Int)

    val UP : Int = 2;
    val DOWN : Int = 1;
    val UNDETERMINED : Int = 0;
    val ENT_EXIT : Int = 3;


    val rand = Random(System.nanoTime());
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
}