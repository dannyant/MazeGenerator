package com.antonetti.mazegenerator

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.graphics.*
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.core.graphics.ColorUtils
import java.util.HashMap


class MazeCanvasView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    // I need to initialize this, but height / width is not saved yet
    lateinit var maze : Maze
    var startup = true

    private var lastX = 0f
    private var lastY = 0f
    private var newDown = false
    private var hint = true;

    private val transVal : Int = 32;

    private val black : Paint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    private val blackTrans : Paint = Paint().apply {
        color = ColorUtils.setAlphaComponent(Color.BLACK, transVal)
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    private val blue = Paint().apply {
        color = Color.BLUE
        strokeWidth = 10f
        style = Paint.Style.FILL
    }

    private val blueTrans = Paint().apply {
        color = ColorUtils.setAlphaComponent(Color.BLUE, transVal)
        strokeWidth = 10f
        style = Paint.Style.FILL
    }

    private val red = Paint().apply {
        color = Color.RED
        strokeWidth = 20f
        style = Paint.Style.FILL
    }

    private val redTrans = Paint().apply {
        color = ColorUtils.setAlphaComponent(Color.RED, transVal)
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

    var zoomLevel : Float = 1f
    var zoomX : Float = 0f
    var zoomY : Float = 0f

    init {
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.larger)
        bitmapUp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.height, bitmap.width, matrix0, true)
        bitmapDown = Bitmap.createBitmap(bitmap, 0, 0, bitmap.height, bitmap.width, matrix180, true)
        bitmapLeft = Bitmap.createBitmap(bitmap, 0, 0, bitmap.height, bitmap.width, matrix270, true)
        bitmapRight = Bitmap.createBitmap(bitmap, 0, 0, bitmap.height, bitmap.width, matrix90, true)

        scaleGestureDetector = ScaleGestureDetector(this.context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                var tempFactor = detector.scaleFactor
                tempFactor = tempFactor.coerceIn(0.5f, 3.0f)
                zoomLevel *= tempFactor
                recenterZoomXY()

                //imageView.scaleX = scaleFactor
                //imageView.scaleY = scaleFactor
                invalidate()
                return true
            }
        })
    }

    val walls: MutableMap<Int, Paint> = HashMap(3)

    init {
        newMaze(null)
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

    fun moveRest() {
        hint = false;
        recenterZoomXY()
    }



    fun recenterZoomXY() {
        val paintMapping = mapOf(ColorMapping.wallUp to blackTrans)
        val zoomScenario = DrawScenario(zoomLevel, 0f, 0f, paintMapping)
        var playerZoomX : Float  = - calculateYLoc(maze.playerX - 0.5f, zoomScenario) + width / 2
        var playerZoomY : Float  = - calculateYLoc(maze.playerY - 0.5f, zoomScenario) + height / 2

        var originZoomX : Float = - calculateXLoc(- 0.5f, zoomScenario)
        var originZoomY : Float = - calculateYLoc(- 0.5f, zoomScenario)

        zoomX = Math.min(playerZoomX, originZoomX)
        zoomY = Math.min(playerZoomY, originZoomY)

    }

    fun recenterZoom() {
        zoomLevel = 1.25f;
        recenterZoomXY()
    }


        fun resetZoom() {
        zoomLevel = 1f
        zoomX = 0f
        zoomY = 0f
    }

    fun toggleZoom() {
        if (zoomOn()) {
            resetZoom()
        } else {
            recenterZoom()
        }
    }

    fun zoomOn() : Boolean {
        return zoomLevel != 1f
    }

    fun up() {
        if (maze.horizontal[maze.playerX][maze.playerY] == maze.DOWN) {
            if (maze.playerY > 0) {
                maze.playerY--
                moveRest()
            }
        }
    }

    fun down() {
        val wallValue = maze.horizontal[maze.playerX][maze.playerY+1];
        if (wallValue == maze.DOWN) {
            if (maze.playerY < maze.squares[0].size - 1) {
                maze.playerY++
                moveRest()
            }
        } else if (wallValue == maze.ENT_EXIT) {
            larger()
        }
    }

    fun left() {
        if (maze.vertical[maze.playerX][maze.playerY] == maze.DOWN) {
            if (maze.playerX > 0) {
                maze.playerX--
                moveRest()
            }
        }
    }

    fun right() {
        val wallValue = maze.vertical[maze.playerX+1][maze.playerY]
        if (wallValue == maze.DOWN) {
            if (maze.playerX < maze.squares.size - 1) {
                maze.playerX++
                moveRest()
            }
        } else if (wallValue == maze.ENT_EXIT) {
            larger()
        }
    }

    fun newMaze(size : Int?) : Maze{
        var sizeVar : Int = if (size != null) size else getMazeSize()

        val ratio: Double = if ((height == 0)) 1.0 else (1.0 * width / height)
        saveMazeSize(sizeVar)
        maze = Maze((sizeVar * ratio).toInt(), sizeVar)
        maze.generate();
        moveRest()
        resetZoom()
        return maze
    }

    fun calculateXLoc(x : Number, scenario : DrawScenario, trans: Boolean = true) : Float {
        var xLoc = 1 + 1f * (width - 1) * (1.0f * x.toFloat() / maze.squares.size);
        val xZoom = xLoc * scenario.zoomLevel
        if (trans) {
            return xZoom + scenario.xTrans
        } else {
            return xZoom
        }
    }

    fun calculateYLoc(y : Number, scenario : DrawScenario, trans: Boolean = true) : Float {
        var yLoc = 1 + 1f * (height - 1) * (1.0f * y.toFloat() / maze.squares[0].size)
        val yZoom = yLoc * scenario.zoomLevel
        if (trans) {
            return yZoom + scenario.yTrans
        } else {
            return yZoom
        }
    }

    fun getDefaultScenario() : DrawScenario {
        val paintMapping : Map<ColorMapping, Paint>;
        if (zoomOn()) {
            paintMapping = mapOf(
                ColorMapping.wallUp to blackTrans,
                ColorMapping.exitWall to redTrans,
                ColorMapping.playerCircle to blueTrans)
        } else {
            paintMapping = mapOf(
                ColorMapping.wallUp to black,
                ColorMapping.exitWall to red,
                ColorMapping.playerCircle to blue)
        }
        return DrawScenario(1f, 0f, 0f ,paintMapping)
    }

    fun getZoomScenario() : DrawScenario {
        val paintMapping : Map<ColorMapping, Paint> = mapOf(
            ColorMapping.wallUp to black,
            ColorMapping.exitWall to red,
            ColorMapping.playerCircle to blue)
        return DrawScenario(zoomLevel, zoomX, zoomY, paintMapping)
    }

    fun drawWalls(canvas: Canvas, scenario : DrawScenario) {
        for (i in maze.vertical.indices) {
            for (j in maze.vertical[i].indices) {
                var paint : Paint? = null
                if (maze.vertical[i][j] == maze.UP) {
                    paint = scenario.paintMapping.get(ColorMapping.wallUp)
                } else if (maze.vertical[i][j] == maze.ENT_EXIT) {
                    paint = scenario.paintMapping.get(ColorMapping.exitWall)
                }
                if (paint != null) {
                    val x1 = calculateXLoc(i, scenario)
                    val y1 = calculateYLoc(j, scenario)
                    val y2 = calculateYLoc(j + 1, scenario)
                    canvas.drawLine(x1, y1, x1, y2, paint)
                }
            }
        }

        for (i in maze.horizontal.indices) {
            for (j in maze.horizontal[i].indices) {
                var paint : Paint? = null
                if (maze.horizontal[i][j] == maze.UP) {
                    paint = scenario.paintMapping.get(ColorMapping.wallUp)
                } else if (maze.horizontal[i][j] == maze.ENT_EXIT) {
                    paint = scenario.paintMapping.get(ColorMapping.exitWall)
                }

                if (paint != null) {
                    val x1 : Float = calculateXLoc(i, scenario);
                    val x2 : Float = calculateXLoc(i + 1, scenario)
                    val y1 : Float = calculateYLoc(j, scenario)
                    canvas.drawLine(x1, y1, x2, y1, paint)
                }
            }
        }
    }

    fun drawPlayerCircle(canvas: Canvas, scenario : DrawScenario) {
        val pX : Float = calculateXLoc(maze.playerX + 0.5f, scenario)
        val pY : Float = calculateYLoc(maze.playerY + 0.5f, scenario)
        val rX : Float = calculateXLoc(0.25f, scenario, false)
        val rY : Float = calculateYLoc(0.25f, scenario, false)
        val playerColor : Paint? = scenario.paintMapping.get(ColorMapping.playerCircle)
        if (playerColor != null) {
            canvas.drawCircle(pX, pY, Math.min(rX, rY), playerColor)
        }
    }

    fun drawHint(canvas: Canvas, scenario : DrawScenario) {
        if (hint) {
            val distance = maze.squares[maze.playerX][maze.playerY]
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


            val x1 : Float = calculateXLoc(hintX + 0.25f, scenario)
            val y1 : Float = calculateYLoc(hintY + 0.25f, scenario)
            val width : Float = calculateXLoc(.5f, scenario, false);
            val height : Float = calculateXLoc(.5f, scenario, false);

            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.larger)
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.height, bitmap.width, matrix, true)
            val resizedBitmap = Bitmap.createScaledBitmap(rotatedBitmap, width.toInt(), height.toInt(), true)
            canvas.drawBitmap(resizedBitmap, x1, y1, gray)
        }
    }

    fun getMazeSize() : Int {
        val sharedPreferences = context.getSharedPreferences("MazeSize", MODE_PRIVATE)
        return sharedPreferences.getInt("MazeSize", 10)
    }

    fun saveMazeSize(size : Int) {
        val sharedPreferences = context.getSharedPreferences("MazeSize", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("MazeSize", size)  // Save integer
        editor.apply()
    }


    override fun onDraw(canvas: Canvas) {

        super.onDraw(canvas)
        if (startup) {
            newMaze(getMazeSize())
            startup = false
        }

        val defaultScenario = getDefaultScenario()
        drawWalls(canvas, defaultScenario)
        drawPlayerCircle(canvas, defaultScenario)
        drawHint(canvas, defaultScenario)

        if (zoomOn()) {
            val zoomScenario = getZoomScenario()
            drawWalls(canvas, zoomScenario)
            drawPlayerCircle(canvas, zoomScenario)
            drawHint(canvas, zoomScenario)
        }

        /**
        maze.resolveCulling()
        val pX : Float = calculateXLoc(maze.playerX + 0.5f, defaultScenario)
        val pY : Float = calculateYLoc(maze.playerY + 0.5f, defaultScenario)
        for (pt in maze.pointsList) {
            val px : Float = calculateXLoc(pt.x, defaultScenario)
            val py : Float = calculateYLoc(pt.y, defaultScenario)
            canvas.drawLine(pX, pY, px, py, blue)
        }
        */
    }

    // Override onTouchEvent to track dragging
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
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