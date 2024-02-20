package client

import game.*
import protocol.Packet
import protocol.Parser
import java.awt.*
import java.awt.event.*
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.BadLocationException
import kotlin.math.abs

val INITIAL_GUI_PHASE = GuiPhase.Connecting;

interface Selectable {
    fun select();
    fun deselect();
}

sealed class UserClick {
    data object CloseButton: UserClick();
    data class UnpickedBuilding(val name: BuildingName) : UserClick();
    data class SelectUnplacedTileBlock(val tileBlock: TileBlock, val selectable: Selectable) : UserClick();
    data class OnBoard(val tileX: Int, val tileY: Int) : UserClick();
}

interface UserClickListener {
    fun onUserClick(click: UserClick);
    fun getSelectedClick(): UserClick?;
}

class Gui : UI() {
    val userClickListener = object : UserClickListener {
        override fun onUserClick(click: UserClick) {
            handleUserClick(click);
        }
        override fun getSelectedClick(): UserClick? {
            if (selectedClick == null) return null;
            return selectedClick!!.first;
        }
    }

    val window = GameWindow(userClickListener);
    private var guiPhase: GuiPhase = INITIAL_GUI_PHASE;

    private var selectedClick: Pair<UserClick, Selectable>? = null;

    private fun handleUserClick(click: UserClick) {
        when (click) {
            is UserClick.UnpickedBuilding -> this.onUserAction(UserAction.PickBuilding(click.name));
            is UserClick.CloseButton -> this.onUserAction(UserAction.CloseWindow)
            is UserClick.SelectUnplacedTileBlock -> {
                if (this.selectedClick != null) {
                    this.selectedClick!!.second.deselect();
                    if (this.selectedClick!!.first is UserClick.SelectUnplacedTileBlock) {
                        this.selectedClick = null;
                        return;
                    }
                }
                this.selectedClick = Pair(click, click.selectable);
                click.selectable.select();
            }
            is UserClick.OnBoard -> {
                if (selectedClick == null) return;
                val previousClick = selectedClick!!.first;
                when (previousClick) {
                    is UserClick.SelectUnplacedTileBlock -> {
                        val pos = Vec2(
                                if (click.tileX % 2 == 0) click.tileX else click.tileX - 1,
                                if (click.tileY % 2 == 0) click.tileY else click.tileY - 1
                        )
                        val tile = previousClick.tileBlock;
                        onUserAction(UserAction.PlaceTileBlock(pos, tile));
                        previousClick.selectable.deselect();
                        selectedClick = null;
                    }
                    else -> {}
                }
            }
        }
    }

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
//                println("Part 1 with turn: %s".format(newPhase.hasTurn));
            }
            is GuiPhase.GamePart2 -> TODO()
            is GuiPhase.GameEnd -> TODO()
        }

        this.guiPhase = newPhase;
    }

    override fun updateGameState(board: Board) {
        SwingUtilities.invokeLater {
            window.updateBoard(board.deepClone());
        }
    }

    private fun openGameWindow() {
        this.window.isVisible = true;
    }
}

class GameWindow(private val userClickListener: UserClickListener) : JFrame() {

    private val screenBoard = ScreenBoard(userClickListener);
    private val bluePlayerArea = PlayerPanel(PlayerColor.BLUE);
    private val orangePlayerArea = PlayerPanel(PlayerColor.ORANGE);
    private val bottomPanel = BottomPanel(userClickListener);

    init {
        this.setTitle("Parijs");
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setSize(900, 700);
        this.preferredSize = Dimension(900, 700);
        this.layout = BorderLayout(10, 10);

        this.add(screenBoard, BorderLayout.CENTER);

        this.add(bottomPanel, BorderLayout.SOUTH);

        this.add(bluePlayerArea, BorderLayout.EAST);
        this.add(orangePlayerArea, BorderLayout.WEST);

        this.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                super.windowClosing(e)
                userClickListener.onUserClick(UserClick.CloseButton);
            }
        })

        this.pack();
    }

    fun updateBoard(newBoard: Board) {
        this.bottomPanel.updateBoard(newBoard);
        this.bluePlayerArea.updateBoard(newBoard);
        this.orangePlayerArea.updateBoard(newBoard);
        this.screenBoard.updateBoard(newBoard);
    }

    fun onPhaseChange(newPhase: GuiPhase) {
        this.screenBoard.onPhaseChange(newPhase);
    }
}

class BottomPanel(private val userClickListener: UserClickListener) : JPanel() {
    private val unpickedBuildings = BuildingCollection(object : BuildingSelectionListener {
        override fun onSelect(building: BuildingName) {
            userClickListener.onUserClick(UserClick.UnpickedBuilding(building));
        }
    });

    private val rightPanel = BottomRightPanel(userClickListener);
//    private val leftPanel = BottomRightPanel(userClickListener);

    init {
        this.layout = BorderLayout();
        unpickedBuildings.updateBuildings(BuildingName.entries);
        this.add(unpickedBuildings, BorderLayout.CENTER);
        this.add(rightPanel, BorderLayout.EAST);
//        this.add(leftPanel, BorderLayout.WEST);
    }

    fun updateBoard(newBoard: Board) {
        this.unpickedBuildings.updateBuildings(newBoard.unpickedBuildings);
        this.rightPanel.updateBoard(newBoard);
//        this.leftPanel.updateBoard(newBoard);
    }
}

class BottomRightPanel(private val userClickListener: UserClickListener) : JPanel() {
    private val WIDTH = 200;
    private val BORDER_SIZE = 10;
    private val message = JLabel("Text goes here");

    private val unplacedTileBlock = ScreenTileBlock();

    private val passButton = JButton("Skip turn");

    init {
        this.unplacedTileBlock.addTileBlockClickListener(object : TileBlockClickListener {
            override fun onSelectedTile(tileBlock: TileBlock, relTileX: Int, relTileY: Int) {
                userClickListener.onUserClick(UserClick.SelectUnplacedTileBlock(tileBlock, object : Selectable {
                    override fun select() {
                        unplacedTileBlock.border = BorderFactory.createLineBorder(Color.BLACK, 5);
                    }

                    override fun deselect() {
                        unplacedTileBlock.border = BorderFactory.createLineBorder(Color.WHITE, 5);
                    }
                }));
            }
        })

        this.preferredSize = Dimension(WIDTH, WIDTH);

        this.border = EmptyBorder(0, BORDER_SIZE*2, BORDER_SIZE, BORDER_SIZE*2);
        this.layout = GridLayout(3, 1, 10, 10);

        this.add(message);
        this.add(unplacedTileBlock);
        this.add(passButton);

    }

    fun updateBoard(newBoard: Board) {
        if (newBoard.unplacedBlueBlocks.isEmpty()) {
            this.unplacedTileBlock.updateTiles(TileBlock(Direction.NORTH, Tile.BRICKS, Tile.BRICKS, Tile.BRICKS, Tile.BRICKS));
        } else {
            val topBlock = newBoard.unplacedBlueBlocks[0];
            if (topBlock == null) {
                this.unplacedTileBlock.updateTiles(TileBlock(Direction.NORTH, Tile.BRICKS, Tile.BRICKS, Tile.BRICKS, Tile.BRICKS));
            } else {
                this.unplacedTileBlock.updateTiles(topBlock);
            }
        }
    }
}

class PlayerPanel(private val playerColor: PlayerColor) : JPanel() {
    private val buildingInventory = BuildingCollection(object : BuildingSelectionListener {
        override fun onSelect(building: BuildingName) {}
    })

    init {
        this.layout = FlowLayout();
        this.setBorder(EmptyBorder(30, 30, 30, 30));
        this.minimumSize = Dimension(50, 50);
        this.add(buildingInventory);
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        if (g == null) return;

        g.color = when (playerColor) {
            PlayerColor.BLUE -> Color(150,150, 255);
            PlayerColor.ORANGE -> Color(255, 200,150);
        }
        g.fillRect(0, 0, width, height);
    }

    fun updateBoard(newBoard: Board) {
        this.buildingInventory.updateBuildings(
            when (playerColor) {
                PlayerColor.BLUE -> newBoard.blueInventoryBuildings
                PlayerColor.ORANGE -> newBoard.orangeInventoryBuildings
            }
        )
    }
}

class ScreenTile(type: Tile = Tile.BRICKS) : JComponent() {
    var tileType = type;
    var isHovered = false;

    init {
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
        super.paintComponent(g);
        if (g == null) return;

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

interface TileBlockClickListener {
    fun onSelectedTile(tileBlock: TileBlock, relTileX: Int, relTileY: Int);
}

open class ScreenTileBlock : JPanel() {
    private val topLeftTile = ScreenTile();
    private val topRightTile = ScreenTile();
    private val bottomLeftTile = ScreenTile();
    private val bottomRightTile = ScreenTile();
    private var isHovered = false;

    private val tileBlockClickListeners = Vector<TileBlockClickListener>();

    init {
        this.layout = GridLayout(2, 2, 0, 0);

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

        this.topLeftTile.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) { fireTileBlockClickEvent(TileBlock(Direction.NORTH, topLeftTile.tileType, topRightTile.tileType, bottomLeftTile.tileType, bottomRightTile.tileType), 0, 0); }
        })
        this.topRightTile.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) { fireTileBlockClickEvent(TileBlock(Direction.NORTH, topLeftTile.tileType, topRightTile.tileType, bottomLeftTile.tileType, bottomRightTile.tileType), 1, 0); }
        })
        this.bottomLeftTile.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) { fireTileBlockClickEvent(TileBlock(Direction.NORTH, topLeftTile.tileType, topRightTile.tileType, bottomLeftTile.tileType, bottomRightTile.tileType), 0, 1); }
        })
        this.bottomRightTile.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) { fireTileBlockClickEvent(TileBlock(Direction.NORTH, topLeftTile.tileType, topRightTile.tileType, bottomLeftTile.tileType, bottomRightTile.tileType), 1, 1); }
        })

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
        });

        this.add(topLeftTile);
        this.add(topRightTile);
        this.add(bottomLeftTile);
        this.add(bottomRightTile);
    }

    fun addTileBlockClickListener(listener: TileBlockClickListener) {
        synchronized(this.tileBlockClickListeners) {
            this.tileBlockClickListeners.add(listener);
        }
    }
    fun removeTileBlockClickListener(listener: TileBlockClickListener) {
        synchronized(this.tileBlockClickListeners) {
            this.tileBlockClickListeners.removeElement(listener);
        }
    }
    private fun fireTileBlockClickEvent(tileBlock: TileBlock, relTileX: Int, relTileY: Int) {
        synchronized(this.tileBlockClickListeners) {
            for (listener in this.tileBlockClickListeners) {
                listener.onSelectedTile(tileBlock, relTileX, relTileY);
            }
        }
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

    fun updateTiles(newTileBlock: TileBlock) {
        this.topLeftTile.updateTileType(newTileBlock.topLeft);
        this.topRightTile.updateTileType(newTileBlock.topRight);
        this.bottomLeftTile.updateTileType(newTileBlock.bottomLeft);
        this.bottomRightTile.updateTileType(newTileBlock.bottomRight);
    }
}

class ScreenBoardTileLayer(val clickListener: UserClickListener) : JPanel() {
    private val BLOCK_COLS = 4;
    private val BLOCK_ROWS = 4;

    private val tileBlocks = Vector<ScreenTileBlock>();

    init {
        this.layout = GridLayout(BLOCK_ROWS, BLOCK_COLS, 5, 5);
        for (row in 0..<BLOCK_ROWS) {
            for (column in 0..<BLOCK_COLS) {
                val tileBlock = ScreenTileBlock();
                tileBlock.addTileBlockClickListener(object : TileBlockClickListener {
                    override fun onSelectedTile(tileBlock: TileBlock, relTileX: Int, relTileY: Int) {
                        val tileX = column * 2 + relTileX;
                        val tileY = row * 2 + relTileY;

                        clickListener.onUserClick(UserClick.OnBoard(tileX, tileY));
                    }
                });
                this.add(tileBlock);
                this.tileBlocks.add(tileBlock);
            }
        }
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g);
        if (g == null) return;
        g.color = Color.RED;
        g.fillRect(0, 0, width, height);
    }


    fun updateBoard(newBoard: Board) {
        for (row in 0..<BLOCK_ROWS) {
            for (column in 0..<BLOCK_COLS) {
                val origin = Vec2(column * 2, row * 2);
                val newTileBlock = TileBlock(
                    Direction.NORTH,
                    newBoard.getTile(origin.x, origin.y),
                    newBoard.getTile(origin.x + 1, origin.y),
                    newBoard.getTile(origin.x, origin.y + 1),
                    newBoard.getTile(origin.x + 1, origin.y + 1),
                )

                this.tileBlocks[row * BLOCK_COLS + column].updateTiles(newTileBlock);
            }
        }
    }
}

class ScreenBoard(clickListener: UserClickListener) : JLayeredPane() {
    private val BOARD_SIZE = 200;

    val tileLayer = ScreenBoardTileLayer(clickListener);

    init {
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
        this.tileLayer.updateBoard(newBoard);
    }

    fun onPhaseChange(newPhase: GuiPhase) {

    }
}

class ScreenBuilding(var building: Building) : JComponent() {
    private val borderSize = 5;
    var isHovered = false;
    var selectionListener: BuildingSelectionListener? = null;

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
                if (selectionListener == null) return;
                selectionListener!!.onSelect(building.name);
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
        super.paintComponent(g);
        if (g == null) return;

        for ((rectIndex, rect) in this.getScreenRects().withIndex()) {
            val neighbors = this.getNeighbourOfRect(rectIndex);

            val left = rect.x + if (neighbors.contains(Direction.WEST)) 0 else borderSize;
            val top = rect.y + if (neighbors.contains(Direction.NORTH)) 0 else borderSize;
            val right = rect.x + rect.width - if (neighbors.contains(Direction.EAST)) 0 else borderSize;
            val bottom = rect.y + rect.height - if (neighbors.contains(Direction.SOUTH)) 0  else borderSize;

            // Draw border
            g.color = if (this.isHovered) Color.BLACK else Color.WHITE;

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

interface BuildingSelectionListener {
    fun onSelect(building: BuildingName);
}

class BuildingCollection(private val buildingSelectionListener: BuildingSelectionListener) : JPanel(null) {
    val borderSize = 2;
    val COLLECTION_WIDTH = 200 + borderSize;
    val UNIT_SIZE = (COLLECTION_WIDTH - 2*borderSize) / 7;
    val COLLECTION_HEIGHT = UNIT_SIZE * 8 + 2*borderSize;

    val screenBuildingChildren = Vector<ScreenBuilding>();

    init {
        this.preferredSize = Dimension(COLLECTION_WIDTH, COLLECTION_HEIGHT);

        this.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                updateBuildingPositions();
            }
        });
    }

    private fun updateBuildingPositions() {
        val collectionLeft = this.width / 2 - COLLECTION_WIDTH / 2 + borderSize;
        val collectionTop = borderSize;

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
        this.repaint();
    }

    fun updateBuildings(buildings: List<BuildingName>) {
        this.removeAll();
        this.screenBuildingChildren.clear();

        for (buildingName in buildings) {
            val screenBuilding = ScreenBuilding(Building.fromName(buildingName));
            screenBuilding.selectionListener = buildingSelectionListener;
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
        super.paintComponent(g)
        if (g == null) return;

        val collectionLeft = this.width / 2 - COLLECTION_WIDTH / 2;
        val collectionTop = 0;

        g.color = Color(20, 20, 50);
        g.fillRect(0, 0, width, height);

        g.color = Color(100, 30, 30);
        g.fillRect(collectionLeft, collectionTop, COLLECTION_WIDTH, COLLECTION_HEIGHT);
    }
}

















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