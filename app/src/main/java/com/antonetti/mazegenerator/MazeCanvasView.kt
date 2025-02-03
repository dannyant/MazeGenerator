package com.antonetti.mazegenerator

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import java.util.HashMap


class MazeCanvasView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    // I need to initialize this, but height / width is not saved yet
    var maze : Maze = newMaze(2)
    var startup = true

    private var lastX = 0f
    private var lastY = 0f
    private var newDown = false
    private var hint = true;


    private val black : Paint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    private val blue = Paint().apply {
        color = Color.BLUE
        strokeWidth = 10f
        style = Paint.Style.FILL
    }

    private val red = Paint().apply {
        color = Color.RED
        strokeWidth = 20f
        style = Paint.Style.FILL
    }

    private val gray = Paint().apply {
        color = Color.GRAY
        strokeWidth = 1f
        style = Paint.Style.FILL
    }

    val matrix0   = Matrix().apply { postRotate(0f) }
    val matrix90  = Matrix().apply { postRotate(90f) }
    val matrix180 = Matrix().apply { postRotate(180f) }
    val matrix270 = Matrix().apply { postRotate(270f) }

    val bitmapUp : Bitmap;
    val bitmapDown : Bitmap;
    val bitmapLeft : Bitmap;
    val bitmapRight : Bitmap;

    init {
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.larger)
        bitmapUp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.height, bitmap.width, matrix0, true)
        bitmapDown = Bitmap.createBitmap(bitmap, 0, 0, bitmap.height, bitmap.width, matrix180, true)
        bitmapLeft = Bitmap.createBitmap(bitmap, 0, 0, bitmap.height, bitmap.width, matrix270, true)
        bitmapRight = Bitmap.createBitmap(bitmap, 0, 0, bitmap.height, bitmap.width, matrix90, true)
    }

    val walls: MutableMap<Int, Paint> = HashMap(3)

    init {
        walls[maze.UP] = black
        walls[maze.ENT_EXIT] = red
    }
    init {
        // Make sure the view can receive key events
        isFocusable = true
        isFocusableInTouchMode = true
    }

    fun hintOn() {
        hint = true;
    }

    fun larger() {
        newMaze(1 + (maze.y * 1.1).toInt())
    }

    fun smaller() {
        newMaze(Math.max(10, (maze.y * 0.95f).toInt()))
    }

    fun up() {
        if (maze.horizontal[maze.playerX][maze.playerY] == maze.DOWN) {
            if (maze.playerY > 0) {
                maze.playerY--
                hint = false;
            }
        }
    }

    fun down() {
        val wallValue = maze.horizontal[maze.playerX][maze.playerY+1];
        if (wallValue == maze.DOWN) {
            if (maze.playerY < maze.squares[0].size - 1) {
                maze.playerY++
                hint = false;
            }
        } else if (wallValue == maze.ENT_EXIT) {
            larger()
        }
    }

    fun left() {
        if (maze.vertical[maze.playerX][maze.playerY] == maze.DOWN) {
            if (maze.playerX > 0) {
                maze.playerX--
                hint = false;
            }
        }
    }

    fun right() {
        val wallValue = maze.vertical[maze.playerX+1][maze.playerY]
        if (wallValue == maze.DOWN) {
            if (maze.playerX < maze.squares.size - 1) {
                maze.playerX++
                hint = false;
            }
        } else if (wallValue == maze.ENT_EXIT) {
            larger()
        }
    }

    fun newMaze(size : Int?) : Maze{
        hint = false;
        var sizeVar : Int = if (size != null) size else maze.y
        if (sizeVar < 5) {
            sizeVar = 5
        }
        val ratio: Double = if ((height == 0)) 1.0 else (1.0 * width / height)
        maze = Maze((sizeVar * ratio).toInt(), sizeVar)
        return maze.generate();
    }

    fun calculateXLoc(x : Float) : Float {
        return 1 + 1f * (width - 1) * (1.0f * x / maze.squares.size);
    }

    fun calculateYloc(y : Float) : Float {
        return 1 + 1f * (height - 1) * (1.0f * y / maze.squares[0].size)
    }

    override fun onDraw(canvas: Canvas) {

        super.onDraw(canvas)
        if (startup) {
            newMaze(10)
            startup = false
        }

        for (i in maze.vertical.indices) {
            for (j in maze.vertical[i].indices) {
                val paint = walls[maze.vertical[i][j]];
                if (paint != null) {
                    val x1 : Float = 1 + 1f * (canvas.width - 1) * (1.0f * i / maze.squares.size);
                    val y1 : Float = 1 + 1f * (canvas.height - 1) * (1.0f * j / maze.squares[0].size);
                    val y2 : Float = 1 + 1f * (canvas.height - 1) * (1.0f * (j + 1f) / maze.squares[0].size);
                    canvas.drawLine(x1, y1, x1, y2, paint)
                }
            }
        }

        for (i in maze.horizontal.indices) {
            for (j in maze.horizontal[i].indices) {
                val paint = walls[maze.horizontal[i][j]];
                if (paint != null) {
                    val x1 : Float = 1 + 1f * (canvas.width - 1) * (1.0f * i / maze.squares.size);
                    val x2 : Float = 1 + 1f * (canvas.width - 1) * (1.0f * (i + 1f) / maze.squares.size);
                    val y1 : Float = 1 + 1f * (canvas.height - 1) * (1.0f * j / (maze.squares[0].size));
                    canvas.drawLine(x1, y1, x2, y1, paint)
                }
            }
        }
        val pX : Float = 1 + 1f * (canvas.width - 1) * ((maze.playerX + 0.5f) / maze.squares.size);
        val pY : Float = 1 + 1f * (canvas.height - 1) * ((maze.playerY + 0.5f) / (maze.squares[0].size));
        val rX : Float = 1 + 1f * (canvas.width - 1) * ((0.25f) / maze.squares.size);
        val rY : Float = 1 + 1f * (canvas.height - 1) * ((0.25f) / (maze.squares[0].size));
        canvas.drawCircle(pX, pY, Math.min(rX, rY), blue)

        if (hint) {
            var distance = maze.squares[maze.playerX][maze.playerY]
            var hintX = -1.0f;
            var hintY = -1.0f;
            var matrix = matrix270
            if (maze.playerX > 0 && maze.squares[maze.playerX - 1][maze.playerY] < distance
                && maze.vertical[maze.playerX][maze.playerY] == maze.DOWN) {
                hintX = maze.playerX - 0.5f;
                hintY = maze.playerY.toFloat();
            }

            if (maze.playerX < maze.squares.size - 1
                && maze.squares[maze.playerX + 1][maze.playerY] < distance
                && maze.vertical[maze.playerX + 1][maze.playerY] == maze.DOWN) {
                hintX = maze.playerX + 0.5f;
                hintY = maze.playerY.toFloat();
                matrix = matrix90
            }

            if (maze.playerY > 0 && maze.squares[maze.playerX][maze.playerY - 1] < distance
                && maze.horizontal[maze.playerX][maze.playerY] == maze.DOWN) {
                hintX = maze.playerX.toFloat();
                hintY = maze.playerY - 0.5f;
                matrix = matrix0
            }

            if (maze.playerY < maze.squares[0].size - 1
                && maze.squares[maze.playerX][maze.playerY + 1] < distance
                && maze.horizontal[maze.playerX][maze.playerY + 1] == maze.DOWN) {
                hintX = maze.playerX.toFloat();
                hintY = maze.playerY + 0.5f;
                matrix = matrix180
            }


            val x1 : Float = 1 + 1f * (canvas.width - 1) * (1.0f * (hintX + 0.25f) / maze.squares.size);
            val y1 : Float = 1 + 1f * (canvas.height - 1) * (1.0f * (hintY + 0.25f) / maze.squares[0].size);

            val width : Float = 1 + 1f * (canvas.width - 1) * (.5f / maze.squares.size);
            val height : Float = 1 + 1f * (canvas.height - 1) * (.5f / maze.squares[0].size);

            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.larger)
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.height, bitmap.width, matrix, true)
            val resizedBitmap = Bitmap.createScaledBitmap(rotatedBitmap, width.toInt(), height.toInt(), true)
            canvas.drawBitmap(resizedBitmap, x1, y1, gray)
        }

    }

    // Override onTouchEvent to track dragging
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Store the starting position of the drag
                lastX = event.x
                lastY = event.y
                newDown = true
            }
            MotionEvent.ACTION_MOVE -> {
                // Update the position of the dragged object as the user moves their finger
                val dx = event.x - lastX
                val dy = event.y - lastY

                if ((Math.abs(dx) > 50 || Math.abs(dy) > 50) && newDown) {
                    if (Math.abs(dx) > Math.abs(dy)) {
                        if (dx < 0) {
                            left()
                        } else if (dx > 0) {
                            right()
                        }
                    } else if (Math.abs(dx) < Math.abs(dy)) {
                        if (dy < 0) {
                            up()
                        } else if (dy > 0) {
                            down()
                        }
                    }
                    invalidate()
                    newDown = false
                }
            }
            MotionEvent.ACTION_UP -> {
                newDown = false
            }
        }
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W -> {
                up()
            }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S -> {
                down()
            }
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> {
                left()
            }
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> {
                right()
            }
        }
        invalidate()
        return true
    }

}