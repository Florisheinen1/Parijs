package client

import game.*
import java.awt.Color
import java.awt.Dialog
import java.awt.Graphics
import java.awt.GridLayout
import java.awt.event.*
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.SwingUtilities;
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.SwingConstants
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.BadLocationException
import kotlin.concurrent.thread
import kotlin.math.abs

var SELECTED_SCREEN_BUILDING: ScreenBuilding? = null;
var SELECTED_BLOCK: TopBlock? = null;
var HOVERS_ON_BOARD: Boolean = false;

var ALLOW_TURN_1: Boolean = false;

class Gui : JFrame() {

    val drawingCanvas = DrawingCanvas();
    var isRunning = false;

    fun run() {
        isRunning = true;

        // Now the rest
        this.add(this.drawingCanvas);
        this.pack();

        this.setSize(300,300);
        this.setTitle("Parijs");
        this.isVisible = true;
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(900, 700);
        this.addWindowListener(object : WindowListener {
            override fun windowOpened(e: WindowEvent?) {}
            override fun windowIconified(e: WindowEvent?) {}
            override fun windowDeiconified(e: WindowEvent?) {}
            override fun windowActivated(e: WindowEvent?) {}
            override fun windowDeactivated(e: WindowEvent?) {}
            override fun windowClosing(e: WindowEvent?) {
                isRunning = false;
            }
            override fun windowClosed(e: WindowEvent?) {
                isRunning = false;
            }
        });

        println("Starting to render");
        while (this.isRunning) {
//            this.dispatchEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSING));
            Thread.sleep(100);
            this.repaint();
        }
        println("Closed gui thread");
    }

    fun updateBoard(board: Board) {
        this.drawingCanvas.updateBoard(board);
        println("Board updated in GUI");
    }

    fun allowTurnPart1() {
        ALLOW_TURN_1 = true;
    }
}

abstract class ParisScreenElement : MouseAdapter(), MouseMotionListener {
    abstract fun draw(g: Graphics, screenSize: Vec2);
}

class UnpickedScreenBuilding(building: Placable, val board: Board) : ScreenBuilding(building) {
    override fun mouseClicked(e: MouseEvent?) {
        if (!ALLOW_TURN_1) {
            return;
        }
        if (e != null) {
            if (super.contains(e.x, e.y) && SELECTED_BLOCK == null) {
                println("Me got clicked: %s".format(building.name.name));
                board.pickBuilding(building.name, PlayerColor.PLAYER_BLUE);
                ALLOW_TURN_1 = false;
            } else if (SELECTED_SCREEN_BUILDING == this) {
//                SELECTED_SCREEN_BUILDING = null;
            }
        }
    }
}
class PickedScreenBuilding(building: Placable, val board: Board) : ScreenBuilding(building) {
    override fun mouseClicked(e: MouseEvent?) {
        if (e != null) {
            if (super.contains(e.x, e.y)) {
                println("Picked me got clicked: %s".format(building.name.name));
            } else if (SELECTED_SCREEN_BUILDING == this) {
//                SELECTED_SCREEN_BUILDING = null;
            }
        }
    }
}

abstract class ScreenBuilding(val building: Placable) : MouseAdapter(), MouseMotionListener {
    var isHovered = false;
    val borderColor = Color.WHITE;
    val borderColorHover = Color.BLACK;
    val borderColorSelected = Color.RED;
    val innerColor = Color.GREEN;

    var origin = Vec2(0, 0);
    var unitSize = 10;

    fun draw(g: Graphics) {
        var border_color = if (isHovered) borderColorHover else borderColor;
        if (SELECTED_SCREEN_BUILDING == this) {
            border_color = borderColorSelected;
        }

        val BORDER_WIDTH = 5; // pixels

        for (pos in building.parts) {
            val outerX = origin.x + unitSize * pos.x;
            val outerY = origin.y + unitSize * pos.y;
            val outerEndX = outerX + unitSize;
            val outerEndY = outerY + unitSize;
            g.color = border_color;
            g.fillRect(outerX, outerY, outerEndX - outerX, outerEndY - outerY);

            val neighbours = getNeighborDirections(pos, building.parts);

            val coloredX = outerX + if (neighbours.contains(Direction.WEST)) 0 else BORDER_WIDTH;
            val coloredY = outerY + if (neighbours.contains(Direction.NORTH)) 0 else BORDER_WIDTH;
            val coloredEndX = outerEndX - if (neighbours.contains(Direction.EAST)) 0 else BORDER_WIDTH;
            val coloredEndY = outerEndY - if (neighbours.contains(Direction.SOUTH)) 0 else BORDER_WIDTH;
            g.color = innerColor;
            g.fillRect(coloredX, coloredY, coloredEndX - coloredX, coloredEndY - coloredY);
        }
    }

    override fun mouseMoved(e: MouseEvent?) {
        if (e != null) {
            this.isHovered = this.contains(e.x, e.y);
        }
    }

    fun contains(x: Int, y: Int): Boolean {
        for (rectOrigin in building.parts) {
            val left = origin.x + rectOrigin.x * unitSize;
            val top = origin.y + rectOrigin.y * unitSize;
            val right = left + unitSize;
            val bottom = top + unitSize;
            if (x in left..right && y in top..bottom) {
                return true;
            }
        }
        return false;
    }

    private fun getNeighborDirections(pos: Vec2, others: Vector<Vec2>): Vector<Direction> {
        val neighbors = Vector<Direction>();
        for (other in others) {
            val diffX = other.x - pos.x;
            val diffY = other.y - pos.y;
            if (abs(diffX) + abs(diffY) == 1) {
                // This position is a neighbor!
                if (diffX == -1) {
                    neighbors.addElement(Direction.WEST);
                } else if (diffX == 1) {
                    neighbors.addElement(Direction.EAST);
                }
                if (diffY == 1) {
                    neighbors.addElement(Direction.SOUTH);
                } else if (diffY == -1) {
                    neighbors.addElement(Direction.NORTH);
                }
            }
        }
        return neighbors;
    }
}

class ScreenPickedBuildingArea(board: Board, val playerColor: PlayerColor) : ParisScreenElement() {
    private val AREA_SIZE = 420;
    private val UNIT_SIZE = AREA_SIZE / 7;
    private val yOffset = 30;
    private val xOffset = if (playerColor == PlayerColor.PLAYER_BLUE) -600 else 600;

    private val pickedScreenBuildings = Vector<PickedScreenBuilding>();

    init {
        updateBoard(board);
    }

    override fun draw(g: Graphics, screenSize: Vec2) {
        val origin = Vec2(screenSize.x / 2 - AREA_SIZE / 2 - xOffset, yOffset);

        g.color = Color(100, 20, 20);
        g.fillRect(origin.x, origin.y, AREA_SIZE, AREA_SIZE + UNIT_SIZE)

        for (pickedBuilding in this.pickedScreenBuildings) {
            pickedBuilding.unitSize = UNIT_SIZE;
            pickedBuilding.origin = getBuildingOrigin(origin, UNIT_SIZE, pickedBuilding.building.name);
            pickedBuilding.draw(g);
        }
    }

    override fun mouseMoved(e: MouseEvent?) {
        for (pickedScreenBuilding in pickedScreenBuildings) {
            pickedScreenBuilding.mouseMoved(e);
        }
    }
    override fun mouseClicked(e: MouseEvent?) {
        for (pickedScreenBuilding in pickedScreenBuildings) {
            pickedScreenBuilding.mouseClicked(e);
        }
    }

    fun getBuildingOrigin(origin: Vec2, unitSize: Int, name: PlacableName): Vec2 {
        val relativeUnits = when (name) {
            PlacableName.SMILE -> Vec2(0, 0)
            PlacableName.BIG_L -> Vec2(4, 0)
            PlacableName.SMALL_L -> Vec2(5, 1)
            PlacableName.BIG_T -> Vec2(4, 2)
            PlacableName.SMALL_T -> Vec2(2, 1)
            PlacableName.CORNER -> Vec2(0, 1)
            PlacableName.SQUARE -> Vec2(0, 3)
            PlacableName.STAIRS -> Vec2(1, 3)
            PlacableName.ZIGZAG -> Vec2(0, 5)
            PlacableName.CROSS -> Vec2(2, 5)
            PlacableName.LINE -> Vec2(4, 5)
            PlacableName.CHONK -> Vec2(4, 6)
            else -> throw Exception("Failed to draw non-building");
        };
        return Vec2(
                origin.x + relativeUnits.x * unitSize,
                origin.y + relativeUnits.y * unitSize
        );
    }

    fun updateBoard(board: Board) {
        pickedScreenBuildings.clear();

        val targetList = if (playerColor == PlayerColor.PLAYER_BLUE) board.blueInventoryBuildings else board.orangeInventoryBuildings;

        for (pickedBuilding in targetList) {
            val screenBuilding = PickedScreenBuilding(pickedBuilding.deepClone(), board);
            pickedScreenBuildings.addElement(screenBuilding);
        }
    }
}

class ScreenUnpickedBuildingArea(board: Board) : ParisScreenElement() {
    private val AREA_SIZE = 420;
    private val UNIT_SIZE = AREA_SIZE / 7;
    private val yOffset = 650;

    private val unpickedScreenBuildings = Vector<UnpickedScreenBuilding>();

    init {
        updateBoard(board);
    }

    override fun draw(g: Graphics, screenSize: Vec2) {

        val origin = Vec2(screenSize.x / 2 - AREA_SIZE / 2, yOffset);

        g.color = Color(100, 20, 20);
        g.fillRect(origin.x, origin.y, AREA_SIZE, AREA_SIZE + UNIT_SIZE)

        for (unpickedBuilding in this.unpickedScreenBuildings) {
            unpickedBuilding.unitSize = UNIT_SIZE;
            unpickedBuilding.origin = getBuildingOrigin(origin, UNIT_SIZE, unpickedBuilding.building.name);
            unpickedBuilding.draw(g);
        }
    }

    override fun mouseMoved(e: MouseEvent?) {
        for (unpickedScreenBuilding in unpickedScreenBuildings) {
            unpickedScreenBuilding.mouseMoved(e);
        }
    }

    override fun mouseClicked(e: MouseEvent?) {
        for (unpickedScreenBuilding in unpickedScreenBuildings) {
            unpickedScreenBuilding.mouseClicked(e);
        }
    }

    fun updateBoard(board: Board) {
        unpickedScreenBuildings.clear();

        for (unpickedBuilding in board.unpickedBuildings) {
            val screenBuilding = UnpickedScreenBuilding(unpickedBuilding.deepClone(), board);
            unpickedScreenBuildings.addElement(screenBuilding);
        }
    }

    fun getBuildingOrigin(origin: Vec2, unitSize: Int, name: PlacableName): Vec2 {
        val relativeUnits = when (name) {
            PlacableName.SMILE -> Vec2(0, 0)
            PlacableName.BIG_L -> Vec2(4, 0)
            PlacableName.SMALL_L -> Vec2(5, 1)
            PlacableName.BIG_T -> Vec2(4, 2)
            PlacableName.SMALL_T -> Vec2(2, 1)
            PlacableName.CORNER -> Vec2(0, 1)
            PlacableName.SQUARE -> Vec2(0, 3)
            PlacableName.STAIRS -> Vec2(1, 3)
            PlacableName.ZIGZAG -> Vec2(0, 5)
            PlacableName.CROSS -> Vec2(2, 5)
            PlacableName.LINE -> Vec2(4, 5)
            PlacableName.CHONK -> Vec2(4, 6)
            else -> throw Exception("Failed to draw non-building");
        };
        return Vec2(
            origin.x + relativeUnits.x * unitSize,
            origin.y + relativeUnits.y * unitSize
        );
    }
}

class ScreenBoard(var board: Board) : ParisScreenElement() {
    val BOARD_SIZE_PIXELS = 600;
    val yOffset = 20;
    val unitSize = BOARD_SIZE_PIXELS / 8;

    var hoveredCoord: Vec2? = null;
    private var lastScreenSize = Vec2(0, 0);

    override fun draw(g: Graphics, screenSize: Vec2) {
        lastScreenSize = screenSize;

        val boardOrigin = Vec2((screenSize.x / 2) - (BOARD_SIZE_PIXELS / 2), yOffset);

        g.color = Color.GREEN;
        g.fillRect(boardOrigin.x, boardOrigin.y, BOARD_SIZE_PIXELS, BOARD_SIZE_PIXELS);

        for (x in 0..<this.board.SIZE) {
            for (y in 0..<this.board.SIZE) {
                val tileX = boardOrigin.x + x * unitSize;
                val tileY = boardOrigin.y + y * unitSize;
                g.color = Color.WHITE;
                g.fillRect(tileX, tileY, unitSize, unitSize);

                drawTile(this.board.getTile(x, y), g, Vec2(tileX, tileY), unitSize);

//                g.color = when (this.board.getTile(x, y)) {
//                    Tile.BLUE -> Color.BLUE
//                    Tile.ORANGE -> Color.ORANGE
//                    Tile.LANTERN -> Color.YELLOW
//                    Tile.BRICKS -> Color.GRAY
//                    Tile.SHARED -> Color.PINK
//                }
//
//                g.fillRect(tileX+1, tileY+1, unitSize-2, unitSize-2);
            }
        }
        drawHoverStuff(g, screenSize);
    }

    fun drawHoverStuff(g: Graphics, screenSize: Vec2) {
        if (hoveredCoord != null) {
            if (SELECTED_BLOCK != null) {
                val clippedCoord = clipBoardCoord(hoveredCoord!!);
                val origin = boardCoordToScreen(clippedCoord.x, clippedCoord.y, screenSize);

                g.color = Color.GREEN;
                g.fillRect(origin.x, origin.y, unitSize*2, unitSize*2);

                SELECTED_BLOCK!!.block?.let { drawTile(it.topLeft, g, origin, unitSize) };
                SELECTED_BLOCK!!.block?.let { drawTile(it.topRight, g, Vec2(origin.x+unitSize, origin.y), unitSize) };
                SELECTED_BLOCK!!.block?.let { drawTile(it.bottomLeft, g, Vec2(origin.x, origin.y+unitSize), unitSize) };
                SELECTED_BLOCK!!.block?.let { drawTile(it.bottomRight, g, Vec2(origin.x+unitSize, origin.y+unitSize), unitSize) };
            }
        }
    }

    fun clipBoardCoord(pos: Vec2): Vec2 {
        return Vec2(
            if (pos.x % 2 == 0) pos.x else pos.x -1,
            if (pos.y % 2 == 0) pos.y else pos.y -1,
        );
    }

    fun boardCoordToScreen(x: Int, y: Int, screenSize: Vec2): Vec2 {
        val boardOrigin = Vec2((screenSize.x / 2) - (BOARD_SIZE_PIXELS / 2), yOffset);
        val tileX = boardOrigin.x + x * unitSize;
        val tileY = boardOrigin.y + y * unitSize;
        return Vec2(tileX, tileY);
    }

    fun updateBoard(board: Board) {
        this.board = board;
    }

    override fun mouseMoved(e: MouseEvent?) {
        if (e != null) {
            val matchingCoord = getBoardCoord(e.x, e.y);
            HOVERS_ON_BOARD = matchingCoord != null;
            hoveredCoord = matchingCoord;
        }
    }

    override fun mouseClicked(e: MouseEvent?) {
        if (e != null) {
            if (SELECTED_BLOCK != null && hoveredCoord != null) {
                val place = clipBoardCoord(hoveredCoord!!)
                SELECTED_BLOCK!!.block?.let { board.placeBlock(it, place) };
                SELECTED_BLOCK = null;
            }
        }
    }

    private fun getBoardCoord(x: Int, y: Int): Vec2? {
        var matchingCoord: Vec2? = null;

        for (column in 0..7) {
            for (row in 0..7){
                val tileOrigin = boardCoordToScreen(column, row, lastScreenSize);
                val left = tileOrigin.x;
                val right = tileOrigin.x + unitSize;
                val top = tileOrigin.y;
                val bottom = tileOrigin.y + unitSize;
                if (x in left..right && y in top..bottom) {
                    matchingCoord = Vec2(column, row);
                    break;
                }
            }
            if (matchingCoord != null) {
                break;
            }
        }
        return matchingCoord;
    }
}

class ScreenCard(val card: Cards, val unitSize: Int) : MouseAdapter(), MouseMotionListener {
    var isHovered = false;

    var origin = Vec2(0, 0);

    fun draw(g: Graphics) {
        g.color = if (isHovered) Color.BLACK else Color.WHITE;
        g.fillRect(origin.x, origin.y, unitSize*2, unitSize);

        val border = 5;
        g.color = Color.DARK_GRAY;
        g.fillRect(origin.x + border, origin.y + border, (unitSize-border)*2, unitSize-border*2);
    }

    fun contains(x: Int, y: Int): Boolean {
        val left = origin.x
        val right = left + 2 * unitSize;
        val top = origin.y;
        val bottom = top + unitSize;
        return x in left..right && y in top..bottom;
    }

    override fun mouseMoved(e: MouseEvent?) {
        if (e != null) {
            isHovered = contains(e.x, e.y);
        }
    }
}

class CardsArea(var board: Board) : ParisScreenElement() {
    val AREA_SIZE = 500;
    var origin = Vec2(0, 0);

    val screenCards = Vector<ScreenCard>();

    init {
        updateBoard(board);
    }

    override fun draw(g: Graphics, screenSize: Vec2) {
        this.origin = Vec2(
                (screenSize.x / 2) - (AREA_SIZE / 2) - 600,
                650
        );

        g.color = Color.PINK;
        g.fillRect(origin.x, origin.y, AREA_SIZE, AREA_SIZE);

        val cardUnitSize = AREA_SIZE / 4;
        var row = 0;
        var col = 0;
        for (screenCard in this.screenCards) {
            screenCard.origin = Vec2(
                    this.origin.x + col * cardUnitSize * 2,
                    this.origin.y + row * cardUnitSize
            );
            if (col == 0) {
                col = 1
            } else {
                row++;
                col = 0;
            }


            screenCard.draw(g);
        }
    }

    override fun mouseMoved(e: MouseEvent?) {
        for (screenCard in screenCards) {
            screenCard.mouseMoved(e);
        }
    }

    fun updateBoard(newBoard: Board) {
        this.board = newBoard;

        this.screenCards.clear();

        val cardUnitSize = AREA_SIZE / 4;

        if (board.selectedCardsForGame.size == 0) {return}

        for (card in board.selectedCardsForGame) {
            screenCards.add(ScreenCard(card, cardUnitSize));
        }
    }
}

fun drawTile(tile: Tile, g: Graphics, origin: Vec2, size: Int) {
    when (tile) {
        Tile.BLUE -> {
            g.color = Color(61, 85, 173);
            g.fillRect(origin.x, origin.y, size, size);
        }
        Tile.ORANGE -> {
            g.color = Color(227, 121, 18);
            g.fillRect(origin.x, origin.y, size, size);
        }
        Tile.SHARED -> {
            g.color = Color(138, 0, 138);
            g.fillRect(origin.x, origin.y, size, size);
        }
        Tile.LANTERN -> {
            g.color = Color.GRAY;
            g.fillRect(origin.x, origin.y, size, size);

            val unit = size / 4;
            g.color = Color.YELLOW;
            g.fillRect(origin.x + unit, origin.y + unit, size - 2*unit, size-2*unit);
        }
        Tile.BRICKS -> {
            g.color = Color.GRAY;
            g.fillRect(origin.x, origin.y, size, size);
        }
    }
}

class TopBlock : ParisScreenElement() {
    val SIZE = 100;
    val xOffset = 400;
    val yOffset = 700;
    var block: Block? = null;
    var origin = Vec2(0, 0);
    var isHovering = false;
    var borderSize = 5;
    var isSelected = false;

    override fun draw(g: Graphics, screenSize: Vec2) {
        this.origin = Vec2(
            screenSize.x / 2 - SIZE / 2 + xOffset,
                yOffset
        );

        if (block == null) {
            g.color = Color(100, 50, 50);
            g.fillRect(origin.x, origin.y, SIZE, SIZE);
        } else {
            if (isHovering) {
                g.color = Color.WHITE;
                g.fillRect(origin.x - borderSize, origin.y - borderSize, SIZE+2*borderSize, SIZE+2*borderSize);
            }
            if (isSelected) {
                g.color = Color.RED;
                g.fillRect(origin.x - borderSize, origin.y - borderSize, SIZE+2*borderSize, SIZE+2*borderSize);
            }

            val unit = SIZE/2;
            drawTile(block!!.topLeft, g, origin, unit);

            drawTile(block!!.topRight, g, Vec2(origin.x+unit, origin.y), unit);

            drawTile(block!!.bottomLeft, g, Vec2(origin.x, origin.y+unit), unit);
            drawTile(block!!.bottomRight, g, Vec2(origin.x+unit, origin.y+unit), unit);
        }
    }

    private fun contains(x: Int, y: Int): Boolean {
        val left = origin.x;
        val right = origin.x + SIZE;
        val top = origin.y;
        val bottom = origin.y + SIZE;
        return x in left..right && y in top..bottom;
    }

    override fun mouseMoved(e: MouseEvent?) {
        if (e != null) {
            isHovering = contains(e.x, e.y);
        }
    }

    override fun mouseClicked(e: MouseEvent?) {
        if (!ALLOW_TURN_1) {
            return;
        }
        if (e != null) {
            if (contains(e.x, e.y)) {
                SELECTED_BLOCK = this;
                this.isSelected = true;
            } else if (SELECTED_BLOCK == this && !HOVERS_ON_BOARD) {
                SELECTED_BLOCK = null;
                this.isSelected = false;
            }
        }
    }

    fun updateBoard(newBoard: Board) {
        block = newBoard.topBlueBlock;
    }
}

class DrawingCanvas : JPanel() {

    var screenElements = Vector<ParisScreenElement>();

    var screenBoard = ScreenBoard(Board());
    var unpickedBuildings = ScreenUnpickedBuildingArea(Board());
    var bluePickedBuildings = ScreenPickedBuildingArea(Board(), PlayerColor.PLAYER_BLUE);
    var orangePickedBuildings = ScreenPickedBuildingArea(Board(), PlayerColor.PLAYER_ORANGE);
    var cardsArea = CardsArea(Board());
    var topBlock = TopBlock();


    init {
//        println("Board width: %d, %d".format(BOARD_ORIGIN.x, BOARD_ORIGIN.y));

//        super.setBorder(BorderFactory.createLineBorder(Color.BLACK));

//        for (unpickedBuilding in screenBoard.board.unpickedBuildings) {
//            val sb = ScreenBuilding(UNPICKED_BUILDINGS_ORIGIN, UNPICKED_UNIT_SIZE, unpickedBuilding);
//            screenElements.addElement(sb);
//            this.addMouseMotionListener(sb);
//            this.addMouseListener(sb);
//        }
        screenElements.addElement(this.screenBoard);
        screenElements.addElement(this.unpickedBuildings);
        screenElements.addElement(this.cardsArea);
        screenElements.addElement(this.bluePickedBuildings);
        screenElements.addElement(this.orangePickedBuildings);
        screenElements.addElement(this.topBlock);

        this.addMouseMotionListener(unpickedBuildings);
        this.addMouseMotionListener(screenBoard);
        this.addMouseMotionListener(cardsArea);
        this.addMouseMotionListener(bluePickedBuildings);
        this.addMouseMotionListener(orangePickedBuildings);
        this.addMouseMotionListener(topBlock);
        this.addMouseListener(unpickedBuildings);
        this.addMouseListener(screenBoard);
        this.addMouseListener(cardsArea);
        this.addMouseListener(bluePickedBuildings);
        this.addMouseListener(orangePickedBuildings);
        this.addMouseListener(topBlock);
    }

    override fun paintComponent(g: Graphics) {
        g.color = Color.GRAY;
        g.fillRect(0, 0, this.width, this.height);

        for (screenBoi in this.screenElements) {
            screenBoi.draw(g, Vec2(width, height));
        }
    }

    fun updateBoard(board: Board) {
        screenBoard.updateBoard(board);
        unpickedBuildings.updateBoard(board);
        cardsArea.updateBoard(board);
        bluePickedBuildings.updateBoard(board);
        orangePickedBuildings.updateBoard(board);
        topBlock.updateBoard(board);
    }


//    private fun drawUnpickedBuildings(g: Graphics) {
//        val unitSize = UNPICKED_BUILDINGS_AREA_SIZE / 7;
//        val areaX = (this.width / 2) - (UNPICKED_BUILDINGS_AREA_SIZE / 2);
//        val areaY = this.BOARD_SIZE_PIXELS + 20*2;
//
//        g.color = Color.PINK;
//        g.fillRect(areaX-5, areaY-5, UNPICKED_BUILDINGS_AREA_SIZE+10, UNPICKED_BUILDINGS_AREA_SIZE+unitSize+10);
//
//        for (unpickedBuilding in this.board.unpickedBuildings) {
////            if (unpickedBuilding.name != PlacableName.BIG_T) {
////                continue;
////            }
//            val unpickedBuildingOrigin = getUnpickedBuildingOrigin(unpickedBuilding);
//            var screenOriginX = areaX + unpickedBuildingOrigin.x * unitSize;
//            var screenOriginY = areaY + unpickedBuildingOrigin.y * unitSize;
//
//            this.drawBuilding(g, unpickedBuilding, unitSize, Vec2(screenOriginX, screenOriginY), Color.GREEN, Color.WHITE);
//        }
//    }
}

class StartDialog : JDialog() {
    var DEFAULT_ADDRESS = "127.0.0.1";
    var DEFAULT_PORT = "39939";

    var serverAddress: InetAddress? = null;
    var serverPort: Int? = null;
    var submitted = false;

    fun run() {
        this.title = "Start Parijs";
        this.setSize(400, 150);
        this.setLocationRelativeTo(null);
        this.defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE;
        this.layout = GridLayout(5, 1);

        val submit = JButton("Join server");
        submit.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent?) {
                if (serverAddress == null) serverAddress = InetAddress.getByName(DEFAULT_ADDRESS);
                if (serverPort == null) serverPort = DEFAULT_PORT.toInt();

                submitted = true;
            }
        })

        val addressLabel = JLabel("Server Address:");
        val addressInput = JTextField(DEFAULT_ADDRESS);
        addressInput.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) { updateValue(e); }
            override fun removeUpdate(e: DocumentEvent) { updateValue(e); }
            override fun changedUpdate(e: DocumentEvent) { updateValue(e); }

            fun updateValue(e: DocumentEvent) {
                try {
                    val newAddr = InetAddress.getByName(e.document.getText(0, e.document.length));
                    serverAddress = newAddr;
                } catch (e: BadLocationException) {
                    serverAddress = null;
                } catch (e: UnknownHostException) {
                    serverAddress = null;
                }

                if (serverAddress == null) {
                    addressInput.background = Color(255, 200, 200);
                } else {
                    addressInput.background = Color(200, 255, 200);
                }
                submit.isEnabled = hasValidData();
            }
        })

        val portLabel = JLabel("Port:");
        val portInput = JTextField(DEFAULT_PORT);
        portInput.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) { updateValue(e) }
            override fun removeUpdate(e: DocumentEvent) { updateValue(e) }
            override fun changedUpdate(e: DocumentEvent) { updateValue(e) }

            fun updateValue(e: DocumentEvent) {
                try {
                    val newPort = e.document.getText(0, e.document.length).toInt();
                    serverPort = newPort;
                } catch (e: NumberFormatException) {
                    serverPort = null;
                }
                if (serverPort != null) {
                    portInput.background = Color(200, 255, 200);
                } else {
                    portInput.background = Color(255, 200, 200);
                }
                submit.isEnabled = hasValidData();
            }
        })

        this.add(addressLabel);
        this.add(addressInput);
        this.add(portLabel);
        this.add(portInput);
        this.add(submit);



        this.isVisible = true;
    }

    fun hasValidData(): Boolean {
        return serverAddress != null && serverPort != null;
    }
}