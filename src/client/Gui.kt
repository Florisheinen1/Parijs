package client

import game.*
import java.awt.*
import java.awt.event.*
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JLayeredPane
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.BadLocationException
import javax.swing.text.Position
import kotlin.contracts.contract
import kotlin.math.abs

val INITIAL_GUI_PHASE = GuiPhase.Connecting;

class Gui : UI() {
    val actionStarter = object : UserActionStarter() {
        override fun fire(action: UserAction) {
            onUserAction(action);
        }
    }

    val window = GameWindow(actionStarter);
    private var guiPhase: GuiPhase = INITIAL_GUI_PHASE;

    override fun getServerAddress(): Pair<InetAddress, Int> {
        val startDialog = ConnectDialog();
        startDialog.run();
        while (!startDialog.submitted) Thread.sleep(100);
        startDialog.dispose();
        return Pair(startDialog.serverAddress!!, startDialog.serverPort!!);
    }

    override fun updatePhase(newPhase: GuiPhase) {
        if (newPhase != this.guiPhase) this.window.onPhaseChange(newPhase);

        when (newPhase) {
            is GuiPhase.Connecting -> TODO()
            is GuiPhase.InLobby -> {
                if (this.guiPhase is GuiPhase.Connecting) this.openGameWindow();
            }
            is GuiPhase.GamePart1 -> {
                println("Part 1 with turn: %s".format(newPhase.hasTurn));
            }
            is GuiPhase.GamePart2 -> TODO()
            is GuiPhase.GameEnd -> TODO()
        }

        this.guiPhase = newPhase;
    }

    override fun updateGameState(board: Board) {
        val a = { i: Int -> i + 1 }
    }

    private fun openGameWindow() {
        println("Opening window");
        this.window.isVisible = true;
    }
}

class GameWindow(private val actionStarter: UserActionStarter) : JFrame() {

    private val screenBoard = ScreenBoard();
    private val unpicked = BuildingCollection();
    var uiPhase = INITIAL_GUI_PHASE;

    init {
        this.setTitle("Parijs");
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setSize(900, 700);
        this.layout = BorderLayout(50, 50);

        this.add(screenBoard, BorderLayout.CENTER);

        unpicked.updateBuildings(BuildingName.entries);
        this.add(unpicked, BorderLayout.SOUTH);

        this.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                super.windowClosing(e)
                actionStarter.fire(UserAction.CloseWindow);
            }
        })

        this.pack();
    }

    fun updateBoard(newBoard: Board) {
        this.unpicked.updateBuildings(newBoard.unpickedBuildings);
        this.screenBoard.updateBoard(newBoard);
    }

    fun onPhaseChange(newPhase: GuiPhase) {
        this.screenBoard.onPhaseChange(newPhase);
    }
}

class ScreenTile(type: Tile = Tile.BRICKS) : JComponent() {
    var tileType = type;
    var isHovered = false;

    init {
        this.tileType = Tile.entries.random();
        this.cursor = Cursor(Cursor.HAND_CURSOR);

        this.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                isHovered = true;
                repaint();
            }

            override fun mouseExited(e: MouseEvent?) {
                isHovered = false;
                repaint();
            }
        })
    }

    fun updateTileType(newTile: Tile) {
        this.tileType = newTile;
        this.repaint();
    }

    override fun paintComponent(g: Graphics?) {
        if (g == null) {
            println("G was null!");
            return;
        }
        super.paintComponent(g);

        if (this.isHovered) {
            g.color = Color.GREEN;
            g.fillRect(0, 0, width, height);
        } else {
            this.drawTile(this.tileType, g);
        }
    }

    private fun drawTile(tile: Tile, g: Graphics) {
        when (tile) {
            Tile.BLUE -> {
                g.color = Color(61, 85, 173);
                g.fillRect(0, 0, width, height);
            }
            Tile.ORANGE -> {
                g.color = Color(227, 121, 18);
                g.fillRect(0, 0, width, height);
            }
            Tile.SHARED -> {
                g.color = Color(138, 0, 138);
                g.fillRect(0, 0, width, height);
            }
            Tile.LANTERN -> {
                g.color = Color(128, 128, 128);
                g.fillRect(0, 0, width, height);

                val xUnit = width / 4;
                val yUnit = height / 4;
                g.color = Color(255, 255 ,100);
                g.fillRect(0 + xUnit, y + yUnit, width - 2*xUnit, height - 2*yUnit);
            }
            Tile.BRICKS -> {
                g.color = Color(128, 128, 128);
                g.fillRect(0, 0, width, height);
            }
        }
    }
}

class ScreenTileBlock() : JPanel() {
    private val topLeftTile = ScreenTile();
    private val topRightTile = ScreenTile();
    private val bottomLeftTile = ScreenTile();
    private val bottomRightTile = ScreenTile();
    private var isHovered = false;

    init {
        this.layout = GridLayout(2, 2, 3, 3);
        this.add(topLeftTile);
        this.add(topRightTile);
        this.add(bottomLeftTile);
        this.add(bottomRightTile);

        val childMouseListener = object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                if (e == null) return;
                handleChildMouseEnterEvent(e);
            }
            override fun mouseExited(e: MouseEvent?) {
                if (e == null) return;
                handleChildMouseExitEvent(e);
            }
        }
        this.topLeftTile.addMouseListener(childMouseListener);
        this.topRightTile.addMouseListener(childMouseListener);
        this.bottomLeftTile.addMouseListener(childMouseListener);
        this.bottomRightTile.addMouseListener(childMouseListener);

        this.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                if (e == null) return;
                if (containsScreenPoint(e.locationOnScreen)) {
                    if (!isHovered) {
                        absoluteMouseEntered(e);
                    }
                }
            }
            override fun mouseExited(e: MouseEvent?) {
                if (e == null) return;
                if (!containsScreenPoint(e.locationOnScreen)) {
                    if (isHovered) {
                        absoluteMouseExited(e);
                    }
                }
            }
        })
    }

    private fun containsScreenPoint(point: Point): Boolean {
        return Rectangle(this.locationOnScreen, Dimension(this.width, this.height)).contains(point);
    }

    fun handleChildMouseExitEvent(e: MouseEvent) {
        if (!this.containsScreenPoint(e.locationOnScreen)) {
            if (this.isHovered) {
                absoluteMouseExited(e);
            }
        }
    }
    fun handleChildMouseEnterEvent(e: MouseEvent) {
        if (this.containsScreenPoint(e.locationOnScreen)) {
            if (!this.isHovered) {
                absoluteMouseEntered(e);
            }
        }
    }

    fun absoluteMouseEntered(e: MouseEvent) {
        this.isHovered = true;
        this.repaint();
    }
    fun absoluteMouseExited(e: MouseEvent) {
        this.isHovered = false;
        this.repaint();
    }

    override fun paintComponent(g: Graphics?) {
        if (g == null) return;

        g.color = if (this.isHovered) Color.BLUE else Color.DARK_GRAY;
        g.fillRect(0, 0, width, height);
    }

    fun updateTiles(newTileBlock: TileBlock) {
        this.topLeftTile.updateTileType(newTileBlock.topLeft);
        this.topRightTile.updateTileType(newTileBlock.topRight);
        this.bottomLeftTile.updateTileType(newTileBlock.bottomLeft);
        this.bottomRightTile.updateTileType(newTileBlock.bottomRight);
    }
}

class ScreenBoardTileLayer : JPanel() {
    private val BLOCK_COLS = 4;
    private val BLOCK_ROWS = 4;

    init {
        this.layout = GridLayout(BLOCK_ROWS, BLOCK_ROWS, 5, 5);

        for (x in 0..<BLOCK_COLS) {
            for (y in 0..<BLOCK_ROWS) {
                this.add(ScreenTileBlock());
            }
        }
    }

    override fun paintComponent(g: Graphics?) {
        if (g == null) {
            println("G was null");
            return;
        }
        super.paintComponent(g);
        g.color = Color.RED;
        g.fillRect(0, 0, width, height);
    }
}

class ScreenBoard : JLayeredPane() {
    private val BOARD_SIZE = 500;

    init {
        val tileLayer = ScreenBoardTileLayer();
        this.add(tileLayer, DEFAULT_LAYER);

        this.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                updateBoardPosition();
            }
        })
    }

    private fun updateBoardPosition() {
        for (layer in this.components) {
            layer.setBounds(width / 2 - BOARD_SIZE / 2, height / 2 - BOARD_SIZE / 2, BOARD_SIZE, BOARD_SIZE);
        }
    }

    fun updateBoard(newBoard: Board) {
        println("Updating screen board with new board data");
    }

    fun onPhaseChange(newPhase: GuiPhase) {

    }
}

class ScreenBuilding(var building: Building) : JComponent() {
    private val borderSize = 5;
    var isHovered = false;
    var isSelected = false;

    init {
        this.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {}
        })
        this.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                isHovered = true;
                repaint();
            }
            override fun mouseExited(e: MouseEvent?) {
                isHovered = false;
                repaint();
            }

            override fun mouseClicked(e: MouseEvent?) {
                if (e != null) {
                    println("Clicked mouse!");
                    isSelected = contains(e.x, e.y);
                    repaint();
                }
            }
        })
        this.cursor = Cursor(Cursor.HAND_CURSOR);
    }

    override fun contains(x: Int, y: Int): Boolean {
        for (rect in this.getScreenRects()) {
            if (rect.contains(x, y)) return true;
        }
        return false;
    }

    override fun paintComponent(g: Graphics?) {
        if (g == null) return;
        super.paintComponent(g);

        for ((rectIndex, rect) in this.getScreenRects().withIndex()) {
            val neighbors = this.getNeighbourOfRect(rectIndex);

            val left = rect.x + if (neighbors.contains(Direction.WEST)) 0 else borderSize;
            val top = rect.y + if (neighbors.contains(Direction.NORTH)) 0 else borderSize;
            val right = rect.x + rect.width - if (neighbors.contains(Direction.EAST)) 0 else borderSize;
            val bottom = rect.y + rect.height - if (neighbors.contains(Direction.SOUTH)) 0  else borderSize;

            // Draw border
            g.color = if (this.isHovered) Color.BLACK else Color.WHITE;
           if (this.isSelected) g.color = Color.RED;

            g.fillRect(rect.x, rect.y, rect.width, rect.height);

            // Draw insides
            g.color = Color.MAGENTA;
            g.fillRect(left, top, right - left, bottom - top);
        }
    }

    private fun getScreenRects(): Vector<Rectangle> {
        val relativeDimension = building.getDimension();
        val xUnit = width / relativeDimension.x;
        val yUnit = height / relativeDimension.y;

        val rects = Vector<Rectangle>();

        for (part in this.building.parts) {
            rects.add(Rectangle(
                    part.x * xUnit,
                    part.y * yUnit,
                    xUnit,
                    yUnit,
            ));
        }
        return rects;
    }

    private fun getNeighbourOfRect(rectIndex: Int): Vector<Direction> {
        val targetRect = this.building.parts[rectIndex];
        val neighbors = Vector<Direction>();
        for (other in this.building.parts) {
            if (targetRect == other) continue;

            val diffX = other.x - targetRect.x;
            val diffY = other.y - targetRect.y;
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

class BuildingCollection : JPanel(null) {
    val borderSize = 20;
    val COLLECTION_WIDTH = 400 + borderSize;
    val UNIT_SIZE = (COLLECTION_WIDTH - 2*borderSize) / 7;
    val COLLECTION_HEIGHT = UNIT_SIZE * 8 + 2*borderSize;

    val screenBuildingChildren = Vector<ScreenBuilding>();

    init {
        this.preferredSize = Dimension(COLLECTION_WIDTH, COLLECTION_HEIGHT + 50);

        this.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                updateBuildingPositions();
            }
        });
    }

    private fun updateBuildingPositions() {
        val collectionLeft = this.width / 2 - COLLECTION_WIDTH / 2 + borderSize;
        val collectionTop = this.height / 2 - COLLECTION_HEIGHT / 2 + borderSize;

        for (screenBuilding in this.screenBuildingChildren) {
            val relativeOffset = getRelativeBuildingUnits(screenBuilding.building.name);
            val absoluteOffset = Vec2(
                    relativeOffset.x * UNIT_SIZE,
                    relativeOffset.y * UNIT_SIZE
            );

            val relativeDimension = screenBuilding.building.getDimension();
            val absoluteDimension = Vec2(
                    relativeDimension.x * UNIT_SIZE,
                    relativeDimension.y * UNIT_SIZE
            );

            screenBuilding.setBounds(
                    collectionLeft + absoluteOffset.x,
                    collectionTop + absoluteOffset.y,
                    absoluteDimension.x,
                    absoluteDimension.y);
        }
    }

    fun updateBuildings(buildings: List<BuildingName>) {
        this.removeAll();
        this.screenBuildingChildren.clear();

        for (buildingName in buildings) {
            val screenBuilding = ScreenBuilding(Building.fromName(buildingName));
            this.screenBuildingChildren.add(screenBuilding);
            this.add(screenBuilding);
        }
        this.updateBuildingPositions();
    }

    private fun getRelativeBuildingUnits(buildingName: BuildingName): Vec2 {
        return when (buildingName) {
            BuildingName.SMILE -> Vec2(0, 0)
            BuildingName.BIG_L -> Vec2(4, 0)
            BuildingName.SMALL_L -> Vec2(5, 1)
            BuildingName.BIG_T -> Vec2(4, 2)
            BuildingName.SMALL_T -> Vec2(2, 1)
            BuildingName.CORNER -> Vec2(0, 1)
            BuildingName.SQUARE -> Vec2(0, 3)
            BuildingName.STAIRS -> Vec2(1, 3)
            BuildingName.ZIGZAG -> Vec2(0, 5)
            BuildingName.CROSS -> Vec2(2, 5)
            BuildingName.LINE -> Vec2(4, 5)
            BuildingName.CHONK -> Vec2(4, 6)
        };
    }

    override fun paintComponent(g: Graphics?) {
        if (g == null) return;
        super.paintComponent(g)

        val collectionLeft = this.width / 2 - COLLECTION_WIDTH / 2;
        val collectionTop = this.height / 2 - COLLECTION_HEIGHT / 2;

        g.color = Color(100, 30, 30);
        g.fillRect(collectionLeft, collectionTop, COLLECTION_WIDTH, COLLECTION_HEIGHT);
    }


}






















//class BuildingCollection : JPanel() {
//
//    var buildings = Vector<Placable>();
//
//    init {
//        ALL_BUILDING_NAMES
//    }
//
//    override fun paintComponent(g: Graphics?) {
//        if (g == null) return;
//        super.paintComponent(g);
//
//        g.color = Color.GREEN;
//        g.fillRect(0, 0, width, height);
//    }
//}

//abstract class ParisScreenElement : MouseAdapter(), MouseMotionListener {
//    abstract fun draw(g: Graphics, screenSize: Vec2);
//}
//
//class UnpickedScreenBuilding(building: Placable, val board: Board) : ScreenBuilding(building) {
//    override fun mouseClicked(e: MouseEvent?) {
//        if (!ALLOW_TURN_1) {
//            return;
//        }
//        if (e != null) {
//            if (super.contains(e.x, e.y) && SELECTED_BLOCK == null) {
//                println("Me got clicked: %s".format(building.name.name));
//                board.pickBuilding(building.name, PlayerColor.PLAYER_BLUE);
//                ALLOW_TURN_1 = false;
//            } else if (SELECTED_SCREEN_BUILDING == this) {
////                SELECTED_SCREEN_BUILDING = null;
//            }
//        }
//    }
//}
//class PickedScreenBuilding(building: Placable, val board: Board) : ScreenBuilding(building) {
//    override fun mouseClicked(e: MouseEvent?) {
//        if (e != null) {
//            if (super.contains(e.x, e.y)) {
//                println("Picked me got clicked: %s".format(building.name.name));
//            } else if (SELECTED_SCREEN_BUILDING == this) {
////                SELECTED_SCREEN_BUILDING = null;
//            }
//        }
//    }
//}
//
//abstract class ScreenBuilding(val building: Placable) : MouseAdapter(), MouseMotionListener {
//    var isHovered = false;
//    val borderColor = Color.WHITE;
//    val borderColorHover = Color.BLACK;
//    val borderColorSelected = Color.RED;
//    val innerColor = Color.GREEN;
//
//    var origin = Vec2(0, 0);
//    var unitSize = 10;
//
//    fun draw(g: Graphics) {
//        var border_color = if (isHovered) borderColorHover else borderColor;
//        if (SELECTED_SCREEN_BUILDING == this) {
//            border_color = borderColorSelected;
//        }
//
//        val BORDER_WIDTH = 5; // pixels
//
//        for (pos in building.parts) {
//            val outerX = origin.x + unitSize * pos.x;
//            val outerY = origin.y + unitSize * pos.y;
//            val outerEndX = outerX + unitSize;
//            val outerEndY = outerY + unitSize;
//            g.color = border_color;
//            g.fillRect(outerX, outerY, outerEndX - outerX, outerEndY - outerY);
//
//            val neighbours = getNeighborDirections(pos, building.parts);
//
//            val coloredX = outerX + if (neighbours.contains(Direction.WEST)) 0 else BORDER_WIDTH;
//            val coloredY = outerY + if (neighbours.contains(Direction.NORTH)) 0 else BORDER_WIDTH;
//            val coloredEndX = outerEndX - if (neighbours.contains(Direction.EAST)) 0 else BORDER_WIDTH;
//            val coloredEndY = outerEndY - if (neighbours.contains(Direction.SOUTH)) 0 else BORDER_WIDTH;
//            g.color = innerColor;
//            g.fillRect(coloredX, coloredY, coloredEndX - coloredX, coloredEndY - coloredY);
//        }
//    }
//
//    override fun mouseMoved(e: MouseEvent?) {
//        if (e != null) {
//            this.isHovered = this.contains(e.x, e.y);
//        }
//    }
//
//    fun contains(x: Int, y: Int): Boolean {
//        for (rectOrigin in building.parts) {
//            val left = origin.x + rectOrigin.x * unitSize;
//            val top = origin.y + rectOrigin.y * unitSize;
//            val right = left + unitSize;
//            val bottom = top + unitSize;
//            if (x in left..right && y in top..bottom) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    private fun getNeighborDirections(pos: Vec2, others: Vector<Vec2>): Vector<Direction> {
//        val neighbors = Vector<Direction>();
//        for (other in others) {
//            val diffX = other.x - pos.x;
//            val diffY = other.y - pos.y;
//            if (abs(diffX) + abs(diffY) == 1) {
//                // This position is a neighbor!
//                if (diffX == -1) {
//                    neighbors.addElement(Direction.WEST);
//                } else if (diffX == 1) {
//                    neighbors.addElement(Direction.EAST);
//                }
//                if (diffY == 1) {
//                    neighbors.addElement(Direction.SOUTH);
//                } else if (diffY == -1) {
//                    neighbors.addElement(Direction.NORTH);
//                }
//            }
//        }
//        return neighbors;
//    }
//}
//
//class ScreenPickedBuildingArea(board: Board, val playerColor: PlayerColor) : ParisScreenElement() {
//    private val AREA_SIZE = (420* SCALE_FACTOR).toInt();
//    private val UNIT_SIZE = AREA_SIZE / 7;
//    private val yOffset = (30* SCALE_FACTOR).toInt();
//    private val xOffset = ((if (playerColor == PlayerColor.PLAYER_BLUE) -600 else 600)* SCALE_FACTOR).toInt();
//
//    private val pickedScreenBuildings = Vector<PickedScreenBuilding>();
//
//    init {
//        updateBoard(board);
//    }
//
//    override fun draw(g: Graphics, screenSize: Vec2) {
//        val origin = Vec2(screenSize.x / 2 - AREA_SIZE / 2 - xOffset, yOffset);
//
//        g.color = Color(100, 20, 20);
//        g.fillRect(origin.x, origin.y, AREA_SIZE, AREA_SIZE + UNIT_SIZE)
//
//        for (pickedBuilding in this.pickedScreenBuildings) {
//            pickedBuilding.unitSize = UNIT_SIZE;
//            pickedBuilding.origin = getBuildingOrigin(origin, UNIT_SIZE, pickedBuilding.building.name);
//            pickedBuilding.draw(g);
//        }
//    }
//
//    override fun mouseMoved(e: MouseEvent?) {
//        for (pickedScreenBuilding in pickedScreenBuildings) {
//            pickedScreenBuilding.mouseMoved(e);
//        }
//    }
//    override fun mouseClicked(e: MouseEvent?) {
//        for (pickedScreenBuilding in pickedScreenBuildings) {
//            pickedScreenBuilding.mouseClicked(e);
//        }
//    }
//
//    fun getBuildingOrigin(origin: Vec2, unitSize: Int, name: PlacableName): Vec2 {
//        val relativeUnits = when (name) {
//            PlacableName.SMILE -> Vec2(0, 0)
//            PlacableName.BIG_L -> Vec2(4, 0)
//            PlacableName.SMALL_L -> Vec2(5, 1)
//            PlacableName.BIG_T -> Vec2(4, 2)
//            PlacableName.SMALL_T -> Vec2(2, 1)
//            PlacableName.CORNER -> Vec2(0, 1)
//            PlacableName.SQUARE -> Vec2(0, 3)
//            PlacableName.STAIRS -> Vec2(1, 3)
//            PlacableName.ZIGZAG -> Vec2(0, 5)
//            PlacableName.CROSS -> Vec2(2, 5)
//            PlacableName.LINE -> Vec2(4, 5)
//            PlacableName.CHONK -> Vec2(4, 6)
//            else -> throw Exception("Failed to draw non-building");
//        };
//        return Vec2(
//                origin.x + relativeUnits.x * unitSize,
//                origin.y + relativeUnits.y * unitSize
//        );
//    }
//
//    fun updateBoard(board: Board) {
//        pickedScreenBuildings.clear();
//
//        val targetList = if (playerColor == PlayerColor.PLAYER_BLUE) board.blueInventoryBuildings else board.orangeInventoryBuildings;
//
//        for (pickedBuilding in targetList) {
//            val screenBuilding = PickedScreenBuilding(pickedBuilding.deepClone(), board);
//            pickedScreenBuildings.addElement(screenBuilding);
//        }
//    }
//}
//
//class ScreenUnpickedBuildingArea(board: Board) : ParisScreenElement() {
//    private val AREA_SIZE = (420* SCALE_FACTOR).toInt();
//    private val UNIT_SIZE = AREA_SIZE / 7;
//    private val yOffset = (650* SCALE_FACTOR).toInt();
//
//    private val unpickedScreenBuildings = Vector<UnpickedScreenBuilding>();
//
//    init {
//        updateBoard(board);
//    }
//
//    override fun draw(g: Graphics, screenSize: Vec2) {
//
//        val origin = Vec2(screenSize.x / 2 - AREA_SIZE / 2, yOffset);
//
//        g.color = Color(100, 20, 20);
//        g.fillRect(origin.x, origin.y, AREA_SIZE, AREA_SIZE + UNIT_SIZE)
//
//        for (unpickedBuilding in this.unpickedScreenBuildings) {
//            unpickedBuilding.unitSize = UNIT_SIZE;
//            unpickedBuilding.origin = getBuildingOrigin(origin, UNIT_SIZE, unpickedBuilding.building.name);
//            unpickedBuilding.draw(g);
//        }
//    }
//
//    override fun mouseMoved(e: MouseEvent?) {
//        for (unpickedScreenBuilding in unpickedScreenBuildings) {
//            unpickedScreenBuilding.mouseMoved(e);
//        }
//    }
//
//    override fun mouseClicked(e: MouseEvent?) {
//        for (unpickedScreenBuilding in unpickedScreenBuildings) {
//            unpickedScreenBuilding.mouseClicked(e);
//        }
//    }
//
//    fun updateBoard(board: Board) {
//        unpickedScreenBuildings.clear();
//
//        for (unpickedBuilding in board.unpickedBuildings) {
//            val screenBuilding = UnpickedScreenBuilding(unpickedBuilding.deepClone(), board);
//            unpickedScreenBuildings.addElement(screenBuilding);
//        }
//    }
//
//    fun getBuildingOrigin(origin: Vec2, unitSize: Int, name: PlacableName): Vec2 {
//        val relativeUnits = when (name) {
//            PlacableName.SMILE -> Vec2(0, 0)
//            PlacableName.BIG_L -> Vec2(4, 0)
//            PlacableName.SMALL_L -> Vec2(5, 1)
//            PlacableName.BIG_T -> Vec2(4, 2)
//            PlacableName.SMALL_T -> Vec2(2, 1)
//            PlacableName.CORNER -> Vec2(0, 1)
//            PlacableName.SQUARE -> Vec2(0, 3)
//            PlacableName.STAIRS -> Vec2(1, 3)
//            PlacableName.ZIGZAG -> Vec2(0, 5)
//            PlacableName.CROSS -> Vec2(2, 5)
//            PlacableName.LINE -> Vec2(4, 5)
//            PlacableName.CHONK -> Vec2(4, 6)
//            else -> throw Exception("Failed to draw non-building");
//        };
//        return Vec2(
//            origin.x + relativeUnits.x * unitSize,
//            origin.y + relativeUnits.y * unitSize
//        );
//    }
//}
//
//class ScreenBoard(var board: Board) : ParisScreenElement() {
//    val BOARD_SIZE_PIXELS = (600 * SCALE_FACTOR).toInt();
//    val yOffset = (20 * SCALE_FACTOR).toInt();
//    val unitSize = BOARD_SIZE_PIXELS / 8;
//
//    var hoveredCoord: Vec2? = null;
//    private var lastScreenSize = Vec2(0, 0);
//
//    override fun draw(g: Graphics, screenSize: Vec2) {
//        lastScreenSize = screenSize;
//
//        val boardOrigin = Vec2((screenSize.x / 2) - (BOARD_SIZE_PIXELS / 2), yOffset);
//
//        g.color = Color.GREEN;
//        g.fillRect(boardOrigin.x, boardOrigin.y, BOARD_SIZE_PIXELS, BOARD_SIZE_PIXELS);
//
//        for (x in 0..<this.board.SIZE) {
//            for (y in 0..<this.board.SIZE) {
//                val tileX = boardOrigin.x + x * unitSize;
//                val tileY = boardOrigin.y + y * unitSize;
//                g.color = Color.WHITE;
//                g.fillRect(tileX, tileY, unitSize, unitSize);
//
//                val tile = this.board.getTile(x, y);
//
//                if (tile == Tile.BRICKS) {
//                    g.color = Color.GRAY;
//                    g.fillRect(tileX+1, tileY+1, unitSize-2, unitSize-2);
//                } else {
//                    drawTile(this.board.getTile(x, y), g, Vec2(tileX, tileY), unitSize);
//                }
//            }
//        }
//        drawHoverStuff(g, screenSize);
//    }
//
//    fun canPlaceBlockHere(x: Int, y: Int): Boolean {
//        return board.getTile(x, y) == Tile.BRICKS
//    }
//
//    fun drawHoverStuff(g: Graphics, screenSize: Vec2) {
//        if (hoveredCoord != null) {
//            if (SELECTED_BLOCK != null) {
//                val clippedCoord = clipBoardCoord(hoveredCoord!!);
//
//                if (canPlaceBlockHere(clippedCoord.x, clippedCoord.y)) {
//                    val origin = boardCoordToScreen(clippedCoord.x, clippedCoord.y, screenSize);
//
//                    g.color = Color.GREEN;
//                    g.fillRect(origin.x, origin.y, unitSize*2, unitSize*2);
//
//                    SELECTED_BLOCK!!.block?.let { drawTile(it.topLeft, g, origin, unitSize) };
//                    SELECTED_BLOCK!!.block?.let { drawTile(it.topRight, g, Vec2(origin.x+unitSize, origin.y), unitSize) };
//                    SELECTED_BLOCK!!.block?.let { drawTile(it.bottomLeft, g, Vec2(origin.x, origin.y+unitSize), unitSize) };
//                    SELECTED_BLOCK!!.block?.let { drawTile(it.bottomRight, g, Vec2(origin.x+unitSize, origin.y+unitSize), unitSize) };
//                }
//            }
//        }
//    }
//
//    fun clipBoardCoord(pos: Vec2): Vec2 {
//        return Vec2(
//            if (pos.x % 2 == 0) pos.x else pos.x -1,
//            if (pos.y % 2 == 0) pos.y else pos.y -1,
//        );
//    }
//
//    fun boardCoordToScreen(x: Int, y: Int, screenSize: Vec2): Vec2 {
//        val boardOrigin = Vec2((screenSize.x / 2) - (BOARD_SIZE_PIXELS / 2), yOffset);
//        val tileX = boardOrigin.x + x * unitSize;
//        val tileY = boardOrigin.y + y * unitSize;
//        return Vec2(tileX, tileY);
//    }
//
//    fun updateBoard(board: Board) {
//        this.board = board;
//    }
//
//    override fun mouseMoved(e: MouseEvent?) {
//        if (e != null) {
//            val matchingCoord = getBoardCoord(e.x, e.y);
//            HOVERS_ON_BOARD = matchingCoord != null;
//            hoveredCoord = matchingCoord;
//        }
//    }
//
//    override fun mouseClicked(e: MouseEvent?) {
//        if (e != null) {
//            if (SELECTED_BLOCK != null && hoveredCoord != null) {
//                val place = clipBoardCoord(hoveredCoord!!)
//                if (canPlaceBlockHere(place.x, place.y)) {
//                    SELECTED_BLOCK!!.block?.let { board.placeBlock(it, place, PlayerColor.PLAYER_BLUE) };
//                    SELECTED_BLOCK = null;
//                    ALLOW_TURN_1 = false;
//                }
//            }
//        }
//    }
//
//    private fun getBoardCoord(x: Int, y: Int): Vec2? {
//        var matchingCoord: Vec2? = null;
//
//        for (column in 0..7) {
//            for (row in 0..7){
//                val tileOrigin = boardCoordToScreen(column, row, lastScreenSize);
//                val left = tileOrigin.x;
//                val right = tileOrigin.x + unitSize;
//                val top = tileOrigin.y;
//                val bottom = tileOrigin.y + unitSize;
//                if (x in left..right && y in top..bottom) {
//                    matchingCoord = Vec2(column, row);
//                    break;
//                }
//            }
//            if (matchingCoord != null) {
//                break;
//            }
//        }
//        return matchingCoord;
//    }
//}
//
//
//
//class ScreenCard(val card: Cards, val unitSize: Int) : MouseAdapter(), MouseMotionListener {
//    var isHovered = false;
//
//    var origin = Vec2(0, 0);
//
//    fun draw(g: Graphics) {
//        g.color = if (isHovered) Color.BLACK else Color.WHITE;
//        g.fillRect(origin.x, origin.y, unitSize*2, unitSize);
//
//        val border = 5;
//        g.color = Color.DARK_GRAY;
//        g.fillRect(origin.x + border, origin.y + border, (unitSize-border)*2, unitSize-border*2);
//
//        g.color = Color.WHITE;
//        g.drawString(card.name, origin.x + 10, origin.y + 50);
//    }
//
//    fun contains(x: Int, y: Int): Boolean {
//        val left = origin.x
//        val right = left + 2 * unitSize;
//        val top = origin.y;
//        val bottom = top + unitSize;
//        return x in left..right && y in top..bottom;
//    }
//
//    override fun mouseMoved(e: MouseEvent?) {
//        if (e != null) {
//            isHovered = contains(e.x, e.y);
//        }
//    }
//}
//
//class CardsArea(var board: Board) : ParisScreenElement() {
//    val AREA_SIZE = (500* SCALE_FACTOR).toInt();
//    var origin = Vec2(0, 0);
//    var offsetX = (600 * SCALE_FACTOR).toInt();
//    var offsetY = (650 * SCALE_FACTOR).toInt();
//
//    val screenCards = Vector<ScreenCard>();
//
//    init {
//        updateBoard(board);
//    }
//
//    override fun draw(g: Graphics, screenSize: Vec2) {
//        this.origin = Vec2(
//                (screenSize.x / 2) - (AREA_SIZE / 2) - offsetX,
//                offsetY
//        );
//
//        g.color = Color.PINK;
//        g.fillRect(origin.x, origin.y, AREA_SIZE, AREA_SIZE);
//
//        val cardUnitSize = AREA_SIZE / 4;
//        var row = 0;
//        var col = 0;
//        for (screenCard in this.screenCards) {
//            screenCard.origin = Vec2(
//                    this.origin.x + col * cardUnitSize * 2,
//                    this.origin.y + row * cardUnitSize
//            );
//            if (col == 0) {
//                col = 1
//            } else {
//                row++;
//                col = 0;
//            }
//
//
//            screenCard.draw(g);
//        }
//    }
//
//    override fun mouseMoved(e: MouseEvent?) {
//        for (screenCard in screenCards) {
//            screenCard.mouseMoved(e);
//        }
//    }
//
//    fun updateBoard(newBoard: Board) {
//        this.board = newBoard;
//
//        this.screenCards.clear();
//
//        val cardUnitSize = AREA_SIZE / 4;
//
//        if (board.selectedCardsForGame.size == 0) {return}
//
//        for (card in board.selectedCardsForGame) {
//            screenCards.add(ScreenCard(card, cardUnitSize));
//        }
//    }
//}
//

//
//class TopBlock : ParisScreenElement() {
//    val SIZE = (100 * SCALE_FACTOR).toInt();
//    val xOffset = (400*SCALE_FACTOR).toInt();
//    val yOffset = (700*SCALE_FACTOR).toInt();
//    var block: Block? = null;
//    var origin = Vec2(0, 0);
//    var isHovering = false;
//    var borderSize = 5;
//
//    override fun draw(g: Graphics, screenSize: Vec2) {
//        this.origin = Vec2(
//            screenSize.x / 2 - SIZE / 2 + xOffset,
//                yOffset
//        );
//
//        if (block == null) {
//            g.color = Color(100, 50, 50);
//            g.fillRect(origin.x, origin.y, SIZE, SIZE);
//        } else {
//            if (isHovering) {
//                g.color = Color.WHITE;
//                g.fillRect(origin.x - borderSize, origin.y - borderSize, SIZE+2*borderSize, SIZE+2*borderSize);
//            }
//            if (SELECTED_BLOCK == this) {
//                g.color = Color.RED;
//                g.fillRect(origin.x - borderSize, origin.y - borderSize, SIZE+2*borderSize, SIZE+2*borderSize);
//            }
//
//            val unit = SIZE/2;
//            drawTile(block!!.topLeft, g, origin, unit);
//
//            drawTile(block!!.topRight, g, Vec2(origin.x+unit, origin.y), unit);
//
//            drawTile(block!!.bottomLeft, g, Vec2(origin.x, origin.y+unit), unit);
//            drawTile(block!!.bottomRight, g, Vec2(origin.x+unit, origin.y+unit), unit);
//        }
//    }
//
//    private fun contains(x: Int, y: Int): Boolean {
//        val left = origin.x;
//        val right = origin.x + SIZE;
//        val top = origin.y;
//        val bottom = origin.y + SIZE;
//        return x in left..right && y in top..bottom;
//    }
//
//    override fun mouseMoved(e: MouseEvent?) {
//        if (e != null) {
//            isHovering = contains(e.x, e.y);
//        }
//    }
//
//    override fun mouseClicked(e: MouseEvent?) {
//        if (!ALLOW_TURN_1) {
//            return;
//        }
//        if (e != null) {
//            if (contains(e.x, e.y)) {
//                SELECTED_BLOCK = this;
//            } else if (SELECTED_BLOCK == this && !HOVERS_ON_BOARD) {
//                SELECTED_BLOCK = null;
//            }
//        }
//    }
//
//    fun updateBoard(newBoard: Board) {
//        block = newBoard.topBlueBlock;
//    }
//}
//
//
//class PassButton : ParisScreenElement() {
//    val width = (100 * SCALE_FACTOR).toInt();
//    val height = (30 * SCALE_FACTOR).toInt();
//    val xOffset = (670*SCALE_FACTOR).toInt();
//    val yOffset = (900*SCALE_FACTOR).toInt();
//    val color = Color.WHITE;
//    val background = Color.BLACK;
//    var lastScreenSize = Vec2(0, 0);
//    var isHovered = false;
//
//    var board = Board();
//
//    override fun draw(g: Graphics, screenSize: Vec2) {
//        if (!ALLOW_TURN_1) {
//            return;
//        }
//
//        lastScreenSize = screenSize;
//        val origin = Vec2(
//                (screenSize.x/2) - (width/2) + xOffset,
//                yOffset
//        );
//        g.color = if (isHovered) Color.RED else background;
//        g.fillRect(origin.x, origin.y, width, height);
//
//        g.color = if (isHovered) Color.BLACK else color;
//        g.drawString("Skip", origin.x + 15, origin.y + 15);
//    }
//
//    fun contains(x: Int, y: Int): Boolean {
//        val origin = Vec2(
//                (lastScreenSize.x/2) - (width/2) + xOffset,
//                yOffset
//        );
//        val left = origin.x;
//        val right = left + width;
//        val top = origin.y;
//        val bottom = top + height;
//        return x in left..right && y in top..bottom;
//    }
//
//    override fun mouseMoved(e: MouseEvent?) {
//        if (e != null) {
//            isHovered = contains(e.x, e.y);
//        }
//    }
//
//    override fun mouseClicked(e: MouseEvent?) {
//        if (e != null) {
//            if (isHovered) {
//                ALLOW_TURN_1 = false;
//                this.board.passTurn();
//            }
//        }
//    }
//
//    fun updateBoard(newBoard: Board) {
//        this.board = newBoard;
//    }
//}
//
//class DrawingCanvas : JPanel() {
//
//    var screenElements = Vector<ParisScreenElement>();
//
//    var screenBoard = ScreenBoard(Board());
//    var unpickedBuildings = ScreenUnpickedBuildingArea(Board());
//    var bluePickedBuildings = ScreenPickedBuildingArea(Board(), PlayerColor.PLAYER_BLUE);
//    var orangePickedBuildings = ScreenPickedBuildingArea(Board(), PlayerColor.PLAYER_ORANGE);
//    var cardsArea = CardsArea(Board());
//    var topBlock = TopBlock();
//    var passButton = PassButton();
//
//
//    init {
//        screenElements.addElement(this.screenBoard);
//        screenElements.addElement(this.unpickedBuildings);
//        screenElements.addElement(this.cardsArea);
//        screenElements.addElement(this.bluePickedBuildings);
//        screenElements.addElement(this.orangePickedBuildings);
//        screenElements.addElement(this.topBlock);
//        screenElements.addElement(this.passButton);
//
//        this.addMouseMotionListener(unpickedBuildings);
//        this.addMouseMotionListener(screenBoard);
//        this.addMouseMotionListener(cardsArea);
//        this.addMouseMotionListener(bluePickedBuildings);
//        this.addMouseMotionListener(orangePickedBuildings);
//        this.addMouseMotionListener(topBlock);
//        this.addMouseMotionListener(passButton);
//        this.addMouseListener(unpickedBuildings);
//        this.addMouseListener(screenBoard);
//        this.addMouseListener(cardsArea);
//        this.addMouseListener(bluePickedBuildings);
//        this.addMouseListener(orangePickedBuildings);
//        this.addMouseListener(topBlock);
//        this.addMouseListener(passButton);
//    }
//
//    override fun paintComponent(g: Graphics) {
//        g.color = Color.GRAY;
//        g.fillRect(0, 0, this.width, this.height);
//
//        for (screenBoi in this.screenElements) {
//            screenBoi.draw(g, Vec2(width, height));
//        }
//
//        var txt = if (ALLOW_TURN_1) "It is your turn!" else "Waiting for opponent...";
////        val nwidth = (100 * SCALE_FACTOR).toInt();
////        val nheight = (30 * SCALE_FACTOR).toInt();
////        val nxOffset = (670*SCALE_FACTOR).toInt();
////        val nyOffset = (900*SCALE_FACTOR).toInt();
//        g.color = Color.BLACK;
//        g.drawString(txt,
//            width/2 + (550* SCALE_FACTOR).toInt(),
//                (700* SCALE_FACTOR).toInt()
//        )
//    }
//
//    fun updateBoard(board: Board) {
//        screenBoard.updateBoard(board);
//        unpickedBuildings.updateBoard(board);
//        cardsArea.updateBoard(board);
//        bluePickedBuildings.updateBoard(board);
//        orangePickedBuildings.updateBoard(board);
//        topBlock.updateBoard(board);
//        passButton.updateBoard(board);
//    }
//}
//

class ConnectDialog : JDialog() {
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
        this.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {

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