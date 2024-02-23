package client

import game.*
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

sealed class GuiEvent {
    sealed class ClickOn : GuiEvent() {
        data object CloseButton: ClickOn();
        data object PassButton : ClickOn();

        // Build stage
        data object RotateStage : ClickOn();
        data object ClearStage : ClickOn();

        // Phase 1
        data class UnpickedBuilding(val name: BuildingName) : ClickOn();
        data class UnplacedTileBlock(val tileBlock: TileBlock) : ClickOn();
        data class Board(val tileX: Int, val tileY: Int) : ClickOn();

        // Phase 2
        data class InventoryBuilding(val buildingName: BuildingName, val owner: PlayerColor) : ClickOn();
        data class ActionCard(val cardType: CardType, val cardState: CardState, val cardOwner: PlayerColor?) : ClickOn();
    }

    sealed class BuildStage : GuiEvent() {
        sealed class Stage : BuildStage() {
            data class UnplacedTileBlock(val tileBlock: TileBlock) : Stage();
            data class Building(val buildingName: BuildingName) : Stage();
        }

        data object Clear : BuildStage();
        data object Rotate : BuildStage();

        data class Updated(val stagedObject: StagedObject) : BuildStage();
    }

    data class UpdateBoard(val newBoard: Board) : GuiEvent();
}

interface GuiEventManager {
    fun emitEvent(event: GuiEvent);
    fun addEventListener(listener: GuiEventListener);
}

interface GuiEventListener {
    fun onEvent(event: GuiEvent);
}

class Gui : UI() {
    private val eventManager = object : GuiEventManager {
        private val listeners = Vector<GuiEventListener>();
        override fun emitEvent(event: GuiEvent) {
            for (listener in this.listeners) {
                listener.onEvent(event);
            }
        }
        override fun addEventListener(listener: GuiEventListener) {
            this.listeners.add(listener);
        }
    }

    private val window = GameWindow(eventManager);
    private var guiPhase: GuiPhase = INITIAL_GUI_PHASE;
    private var selectClick: GuiEvent? = null;
    private var stagedObject: StagedObject = StagedObject.None;

    init {
        this.eventManager.addEventListener(object : GuiEventListener {
            override fun onEvent(event: GuiEvent) {
                handleEvent(event);
            }
        })
    }

    private fun handleEvent(event: GuiEvent) {
        when (event) {
            is GuiEvent.ClickOn -> when (event) {
                is GuiEvent.ClickOn.CloseButton -> this.onUserAction(UserAction.CloseWindow);
                is GuiEvent.ClickOn.UnpickedBuilding -> this.onUserAction(UserAction.PickBuilding(event.name));
                is GuiEvent.ClickOn.UnplacedTileBlock -> {
                    // TODO: Only allow when we have the turn!
                    if (!event.tileBlock.isBricks())
                        this.eventManager.emitEvent(GuiEvent.BuildStage.Stage.UnplacedTileBlock(event.tileBlock));
                }
                is GuiEvent.ClickOn.Board -> {
                    println("Clicked on board...");
                    when (this.stagedObject) {
                        is StagedObject.None -> {}
                        is StagedObject.StagedBuilding -> TODO()
                        is StagedObject.StagedTileBlock -> {
                            val stagedTileBlock = (this.stagedObject as StagedObject.StagedTileBlock).tileBlock;
                            val position = Vec2(
                                    if (event.tileX % 2 == 0) event.tileX else event.tileX - 1,
                                    if (event.tileY % 2 == 0) event.tileY else event.tileY - 1
                            );
                            this.onUserAction(UserAction.PlaceTileBlock(position, stagedTileBlock));
                        }
                    }
                }
                is GuiEvent.ClickOn.InventoryBuilding -> {
                    // TODO: Only allow when we have the turn + in phase 2
                    // Only Blue, (us, the client), owns buildings that can be placed
                    if (event.owner == PlayerColor.BLUE)
                        this.eventManager.emitEvent(GuiEvent.BuildStage.Stage.Building(event.buildingName));
                }

                is GuiEvent.ClickOn.PassButton -> TODO()

                is GuiEvent.ClickOn.ActionCard -> TODO()
                is GuiEvent.ClickOn.ClearStage -> this.eventManager.emitEvent(GuiEvent.BuildStage.Clear);
                is GuiEvent.ClickOn.RotateStage -> this.eventManager.emitEvent(GuiEvent.BuildStage.Rotate);
            }
            is GuiEvent.BuildStage -> when (event) {
                is GuiEvent.BuildStage.Clear -> {}
                is GuiEvent.BuildStage.Rotate -> {}
                is GuiEvent.BuildStage.Stage.Building -> {}
                is GuiEvent.BuildStage.Stage.UnplacedTileBlock -> {}
                is GuiEvent.BuildStage.Updated -> this.stagedObject = event.stagedObject;
            }
            is GuiEvent.UpdateBoard -> {}
        }
    }

//            /*
//                NO_ACTION_TYPE -> Sacre Coeur, just dibs. No select
//                SINGLE_ACTION_TYPE -> Jardin des plantes, select and place
//                DUAL_ACTION_TYPE -> Metropolitan, dibs and place building later. No select!
//                TRIPLE_ACTION_TYPE -> Select, select, select, place
//             */
//            is GuiEvent.ClickOn.ActionCard -> {
//                TODO()
//                when (click.cardType) {
//                    // No action types: Just dibs
//                    CardType.SACRE_COEUR -> {
//                        when (click.cardOwner) {
//                            PlayerColor.ORANGE -> {
//                                println("Clicked on card of opponent. Cant do this!");
//                                return;
//                            }
//                            PlayerColor.BLUE -> {
//                                println("Already owned this card. Cant do this!");
//                                return;
//                            }
//                            null -> {
//                                println("Clicked unselected card.");
//
//                            }
//                        }
//                    }
//
//                    // Single action: Select and place.
//                    CardType.LA_GRANDE_LUMIERE -> TODO()
//                    CardType.LE_PENSEUR -> TODO()
//                    CardType.FONTAINE_DES_MERS -> TODO()
//                    CardType.MOULIN_ROUGE -> TODO()
//                    CardType.JARDIN_DES_PLANTES -> TODO()
//                    CardType.LE_PEINTRE -> TODO()
//                    CardType.BOUQUINISTES_SUR_LA_SEINE -> TODO()
//                    CardType.LAMPADAIRE -> TODO()
//
//                    // Dual action: Dibs, and use later
//                    CardType.METROPOLITAN -> TODO() // Allow building over lantern
//                    CardType.CHARTIER -> TODO() // Allow build on opponent color
//
//                    // Triple action: Select card, select own building, select unpicked building, click board
//                    CardType.LEVITATION -> TODO()

    override fun getServerAddress(): Pair<InetAddress, Int> {
        val startDialog = ConnectDialog();
        startDialog.run();
        while (!startDialog.submitted) Thread.sleep(100);
        startDialog.dispose();
        return Pair(startDialog.serverAddress!!, startDialog.serverPort!!);
    }

    override fun updatePhase(newPhase: GuiPhase) {
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
            this.eventManager.emitEvent(GuiEvent.UpdateBoard(board));
        }
    }

    private fun openGameWindow() {
        this.window.isVisible = true;
    }
}

class CenterPanel(clickManager: GuiEventManager) : JPanel() {
    private val screenBoard = BoardPanel(clickManager);
    private val buildStage = BuildStage(clickManager);

    init {
        this.layout = BorderLayout();
        this.add(screenBoard, BorderLayout.CENTER);
        this.add(buildStage, BorderLayout.NORTH);
    }
}

class GameWindow(private val eventManager: GuiEventManager) : JFrame() {

    private val centerPanel = CenterPanel(eventManager);
    private val bluePlayerArea = PlayerPanel(PlayerColor.BLUE, eventManager);
    private val orangePlayerArea = PlayerPanel(PlayerColor.ORANGE, eventManager);
    private val bottomPanel = BottomPanel(eventManager);

    init {
        this.setTitle("Parijs");
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setSize(900, 700);
        this.preferredSize = Dimension(900, 700);
        this.layout = BorderLayout(10, 10);

        this.add(centerPanel, BorderLayout.CENTER);

        this.add(bottomPanel, BorderLayout.SOUTH);

        this.add(bluePlayerArea, BorderLayout.EAST);
        this.add(orangePlayerArea, BorderLayout.WEST);

        this.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                super.windowClosing(e)
                eventManager.emitEvent(GuiEvent.ClickOn.CloseButton);
            }
        })

        this.pack();
    }
}

sealed class StagedObject() {
    data object None : StagedObject();
    data class StagedBuilding(val building: Building, val screenBuilding: ScreenBuilding) : StagedObject();
    data class StagedTileBlock(val tileBlock: TileBlock, val screenTileBlock: ScreenTileBlock) : StagedObject();
}

class BuildStage(private val eventManager: GuiEventManager) : JPanel() {
    private val clearButton = JButton("Clear");
    private val rotateButton = JButton("Rotate");

    private var stagedObject: StagedObject = StagedObject.None;

    init {
        this.preferredSize = Dimension(300, 200);
        this.layout = GridLayout(1, 3, 10, 10);

        clearButton.addActionListener { eventManager.emitEvent(GuiEvent.ClickOn.ClearStage); }
        rotateButton.addActionListener{ eventManager.emitEvent(GuiEvent.ClickOn.RotateStage); }

        this.add(clearButton);
        this.add(JPanel());
        this.add(rotateButton);

        eventManager.addEventListener(object : GuiEventListener {
            override fun onEvent(event: GuiEvent) {
                when (event) {
                    is GuiEvent.BuildStage -> when (event) {
                        GuiEvent.BuildStage.Clear -> {
                            println("Clearing stage!");
                            stageObject(StagedObject.None);
                        }
                        is GuiEvent.BuildStage.Rotate -> rotateBuilding();
                        is GuiEvent.BuildStage.Stage.Building -> {
                            println("Staging building");
                            stageBuilding(event.buildingName);
                        }
                        is GuiEvent.BuildStage.Stage.UnplacedTileBlock -> {
                            println("Staging tileBlock!");
                            stageTileBlock(event.tileBlock);
                        }
                        is GuiEvent.BuildStage.Updated -> {}
                    }
                    is GuiEvent.UpdateBoard -> {
                        stageObject(StagedObject.None);
                    }
                    else -> {}
                }
            }
        })
    }

    private fun rotateBuilding() {
        when (val currentStaged = this.stagedObject) {
            is StagedObject.None -> {}
            is StagedObject.StagedBuilding -> {
                currentStaged.screenBuilding.rotateBuilding(true);
            }
            is StagedObject.StagedTileBlock -> {
                currentStaged.tileBlock.rotate(true);
                currentStaged.screenTileBlock.updateTiles(currentStaged.tileBlock);

                this.eventManager.emitEvent(GuiEvent.BuildStage.Updated(currentStaged));
            }
        }
    }

    private fun stageBuilding(buildingName: BuildingName) {
        val building = Building.fromName(buildingName);
        val screenBuilding = ScreenBuilding(building);
        this.stageObject(StagedObject.StagedBuilding(building, screenBuilding))
    }

    private fun stageTileBlock(tileBlock: TileBlock) {
        val screenTileBlock = ScreenTileBlock();
        screenTileBlock.updateTiles(tileBlock);
        this.stageObject(StagedObject.StagedTileBlock(tileBlock, screenTileBlock));
    }

    private fun stageObject(newObject: StagedObject) {
        this.stagedObject = newObject;

        val newComponent: JComponent = when (newObject) {
            is StagedObject.None -> JPanel();
            is StagedObject.StagedBuilding -> newObject.screenBuilding;
            is StagedObject.StagedTileBlock -> newObject.screenTileBlock;
        }

        this.remove(1);
        this.add(newComponent, 1);
        this.revalidate();
        this.repaint();

        this.eventManager.emitEvent(GuiEvent.BuildStage.Updated(newObject));
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        if (g == null) return;
        g.color = Color.PINK;
        g.fillRect(0, 0, width, height);
    }
}

class BottomPanel(private val eventManager: GuiEventManager) : JPanel() {
    private val unpickedBuildings = BuildingCollection(object : BuildingSelectionListener {
        override fun onSelect(building: BuildingName) {
            eventManager.emitEvent(GuiEvent.ClickOn.UnpickedBuilding(building));
        }
    });

    private val rightPanel = BottomRightPanel(eventManager);
    private val cardsPanel = CardsCollection(eventManager);

    init {
        this.layout = BorderLayout();
        unpickedBuildings.updateBuildings(BuildingName.entries);
        this.add(unpickedBuildings, BorderLayout.CENTER);
        this.add(rightPanel, BorderLayout.EAST);
        this.add(cardsPanel, BorderLayout.WEST);

        eventManager.addEventListener(object : GuiEventListener {
            override fun onEvent(event: GuiEvent) {
                if (event is GuiEvent.UpdateBoard)
                    unpickedBuildings.updateBuildings(event.newBoard.unpickedBuildings);
            }
        })
    }
}

class ScreenCard(private val cardType: CardType, eventManager: GuiEventManager) : JComponent() {
    private var cardState = CardState.UNPICKED_AND_UNUSED;
    private var cardOwner: PlayerColor? = null;

    private val ownerMarkerSize = 30;

    init {
        this.border = BorderFactory.createLineBorder(Color.WHITE, 5);

        this.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                if (e == null) return;
                border = BorderFactory.createLineBorder(Color.BLACK, 5);
            }

            override fun mouseExited(e: MouseEvent?) {
                if (e == null) return;
                border = BorderFactory.createLineBorder(Color.WHITE, 5);
            }

            override fun mouseClicked(e: MouseEvent?) {
                if (e == null) return;
                eventManager.emitEvent(GuiEvent.ClickOn.ActionCard(cardType, cardState, cardOwner))
            }
        });
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g);
        if (g == null) return;
        this.drawCard(g, this.cardType, this.cardOwner);
    }

    private fun drawCard(g: Graphics, cardType: CardType, cardOwner: PlayerColor?) {
        g.color = when (this.cardState) {
            CardState.UNPICKED_AND_UNUSED -> Color.GREEN
            CardState.PICKED_BUT_UNUSED -> Color.YELLOW
            CardState.PICKED_AND_USED -> Color.RED
        }
        g.fillRect(0, 0, width, height);

        if (cardOwner != null) {
            g.color = Color.BLACK;
            val origin = Vec2(width - this.ownerMarkerSize, 0);
            g.fillRect(origin.x, origin.y, ownerMarkerSize, ownerMarkerSize);

            g.color = when (cardOwner) {
                PlayerColor.BLUE -> Color.BLUE
                PlayerColor.ORANGE -> Color.ORANGE;
            }
            g.fillRect(origin.x + 2, origin.y + 2, ownerMarkerSize - 4, ownerMarkerSize - 4);
        }

        g.color = Color.BLACK;
        g.drawString(cardType.name, 20, 40);
    }
}

class CardsCollection(eventManager: GuiEventManager) : JPanel() {
    private val CARD_COLS = 2;
    private val CARD_ROWS = 4;
    private val SIZE = 400;

    init {
        this.layout = GridLayout(CARD_ROWS, CARD_COLS);
        this.preferredSize = Dimension(SIZE, SIZE);

        eventManager.addEventListener(object : GuiEventListener {
            override fun onEvent(event: GuiEvent) {
                if (event is GuiEvent.UpdateBoard) {
                    val isInitialized = components.isNotEmpty();
                    val cardsAreKnown = event.newBoard.inGameCards.isNotEmpty();

                    if (!isInitialized && cardsAreKnown) {
                        initializeCards(event.newBoard.inGameCards, eventManager);
                    }
                }
                // TODO: Handle card update events
            }
        })
    }

    private fun initializeCards(cards: List<Card>, eventManager: GuiEventManager) {
        println("Initializing cards!");
        for (card in cards) {
            this.add(ScreenCard(card.type, eventManager));
        }
        this.revalidate();
        this.repaint();
    }
}

class BottomRightPanel(private val eventManager: GuiEventManager) : JPanel() {
    private val WIDTH = 200;
    private val BORDER_SIZE = 10;
    private val message = JLabel("Text goes here");

    private val unplacedTileBlock = ScreenTileBlock();

    private val passButton = JButton("Skip turn");

    init {
        this.unplacedTileBlock.addTileBlockMouseListener(object : TileBlockMouseListener {
            override fun onSelectedTile(tileBlock: TileBlock, relTileX: Int, relTileY: Int) {
                eventManager.emitEvent(GuiEvent.ClickOn.UnplacedTileBlock(tileBlock));
            }
            override fun onAbsoluteMouseEnter(e: MouseEvent) {
                unplacedTileBlock.border = BorderFactory.createLineBorder(Color.BLACK, 5);
            }
            override fun onAbsoluteMouseExit(e: MouseEvent) {
                unplacedTileBlock.border = BorderFactory.createLineBorder(Color.WHITE, 5);
            }
        })
        unplacedTileBlock.border = BorderFactory.createLineBorder(Color.WHITE, 5);

        this.preferredSize = Dimension(WIDTH, WIDTH);

        this.border = EmptyBorder(0, BORDER_SIZE*2, BORDER_SIZE, BORDER_SIZE*2);
        this.layout = GridLayout(3, 1, 10, 10);

        this.add(message);
        this.add(unplacedTileBlock);
        this.passButton.isFocusable = false;
        this.add(passButton);

        eventManager.addEventListener(object : GuiEventListener {
            override fun onEvent(event: GuiEvent) {
                if (event is GuiEvent.UpdateBoard) {
                    updateTileBlock(event.newBoard);
                }
            }
        })
    }

    fun updateTileBlock(newBoard: Board) {
        val emptyTileBlock = TileBlock(Direction.NORTH, Tile.BRICKS, Tile.BRICKS, Tile.BRICKS, Tile.BRICKS);
        if (newBoard.unplacedBlueBlocks.isEmpty()) {
            this.unplacedTileBlock.updateTiles(emptyTileBlock);
            println("Setting unplaced tileblock to empty because no more tile blocks left");
        } else {
            val topBlock = newBoard.unplacedBlueBlocks[0];
            if (topBlock == null) {
                println("Setting unplaced tileBlock to empty because top block is unknown");
                this.unplacedTileBlock.updateTiles(emptyTileBlock);
            } else {
                println("Setting unplaced tileBlock because top block is known!");
                this.unplacedTileBlock.updateTiles(topBlock);
            }
        }
    }
}

class PlayerPanel(private val playerColor: PlayerColor, eventManager: GuiEventManager) : JPanel() {
    private val buildingInventory = BuildingCollection(object : BuildingSelectionListener {
        override fun onSelect(building: BuildingName) {
            eventManager.emitEvent(GuiEvent.ClickOn.InventoryBuilding(building, playerColor));
        }
    })

    init {
        this.layout = FlowLayout();
        this.setBorder(EmptyBorder(30, 30, 30, 30));
        this.minimumSize = Dimension(50, 50);
        this.add(buildingInventory);

        eventManager.addEventListener(object : GuiEventListener {
            override fun onEvent(event: GuiEvent) {
                if (event is GuiEvent.UpdateBoard) updateBuildingInventory(event.newBoard);
            }
        })
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

    fun updateBuildingInventory(newBoard: Board) {
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
//                g.color = Color(128, 128, 128);
                g.color = Color(255, 255 ,100);
                g.fillRect(0, 0, width, height);

//                val xUnit = width / 4;
//                val yUnit = height / 4;
//                g.color = Color(255, 255 ,100);
//                g.fillRect(0 + xUnit, y + yUnit, width - 2*xUnit, height - 2*yUnit);
            }
            Tile.BRICKS -> {
                g.color = Color(128, 128, 128);
                g.fillRect(0, 0, width, height);
            }
        }
    }
}

interface TileBlockMouseListener {
    fun onSelectedTile(tileBlock: TileBlock, relTileX: Int, relTileY: Int);
    fun onAbsoluteMouseEnter(e: MouseEvent);
    fun onAbsoluteMouseExit(e: MouseEvent);
}

open class ScreenTileBlock : JPanel() {
    private val topLeftTile = ScreenTile();
    private val topRightTile = ScreenTile();
    private val bottomLeftTile = ScreenTile();
    private val bottomRightTile = ScreenTile();
    private var isHovered = false;

    private val tileBlockMouseListeners = Vector<TileBlockMouseListener>();

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
            override fun mouseClicked(e: MouseEvent?) { fireTileBlockMouseEvent(TileBlock(Direction.NORTH, topLeftTile.tileType, topRightTile.tileType, bottomLeftTile.tileType, bottomRightTile.tileType), 0, 0); }
        })
        this.topRightTile.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) { fireTileBlockMouseEvent(TileBlock(Direction.NORTH, topLeftTile.tileType, topRightTile.tileType, bottomLeftTile.tileType, bottomRightTile.tileType), 1, 0); }
        })
        this.bottomLeftTile.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) { fireTileBlockMouseEvent(TileBlock(Direction.NORTH, topLeftTile.tileType, topRightTile.tileType, bottomLeftTile.tileType, bottomRightTile.tileType), 0, 1); }
        })
        this.bottomRightTile.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) { fireTileBlockMouseEvent(TileBlock(Direction.NORTH, topLeftTile.tileType, topRightTile.tileType, bottomLeftTile.tileType, bottomRightTile.tileType), 1, 1); }
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

    fun addTileBlockMouseListener(listener: TileBlockMouseListener) {
        this.tileBlockMouseListeners.add(listener);
    }
    private fun fireTileBlockMouseEvent(tileBlock: TileBlock, relTileX: Int, relTileY: Int) {
        for (listener in this.tileBlockMouseListeners) {
            listener.onSelectedTile(tileBlock, relTileX, relTileY);
        }
    }

    private fun containsScreenPoint(point: Point) = Rectangle(this.locationOnScreen, Dimension(this.width, this.height)).contains(point);

    fun handleChildMouseExitEvent(e: MouseEvent) {
        if (!this.containsScreenPoint(e.locationOnScreen)) {
            if (this.isHovered) {
                absoluteMouseExited(e);
    }}}
    fun handleChildMouseEnterEvent(e: MouseEvent) {
        if (this.containsScreenPoint(e.locationOnScreen)) {
            if (!this.isHovered) {
                absoluteMouseEntered(e);
    }}}

    fun absoluteMouseEntered(e: MouseEvent) {
        this.isHovered = true;
        synchronized(this.tileBlockMouseListeners) {
            for (listener in this.tileBlockMouseListeners) { listener.onAbsoluteMouseEnter(e); }
        }
        this.repaint();
    }
    fun absoluteMouseExited(e: MouseEvent) {
        this.isHovered = false;
        synchronized(this.tileBlockMouseListeners) {
            for (listener in this.tileBlockMouseListeners) { listener.onAbsoluteMouseExit(e); }
        }
        this.repaint();
    }

    fun updateTiles(newTileBlock: TileBlock) {
        this.topLeftTile.updateTileType(newTileBlock.topLeft);
        this.topRightTile.updateTileType(newTileBlock.topRight);
        this.bottomLeftTile.updateTileType(newTileBlock.bottomLeft);
        this.bottomRightTile.updateTileType(newTileBlock.bottomRight);
    }

    fun getDisplayedTileBlock(): TileBlock {
        return TileBlock(Direction.NORTH, this.topLeftTile.tileType, this.topRightTile.tileType, this.bottomLeftTile.tileType, this.bottomRightTile.tileType);
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        if (g == null) return;
        if (this.isHovered) {
            g.color = Color.MAGENTA;
        } else {
            g.color = Color.BLACK;
        }
        g.fillRect(0, 0, width, height);
    }
}

//                        synchronized(this) {
//
//
////                            if (event is GuiEvent.ClickOn.UnplacedTileBlock) {
////                                unplacedTileBlock = event.tileBlock.copy();
////                                // We want to place a tileblock
////                                if (tileBlock.getDisplayedTileBlock().topLeft != Tile.BRICKS) {
////                                    tileBlock.cursor = Cursor.getSystemCustomCursor("Invalid.32x32");
//////                                    tileBlock.cursor = Cursor(Cursor.WAIT_CURSOR);
////                                } else {
////                                    tileBlock.cursor = Cursor(Cursor.HAND_CURSOR);
////                                }
////                            } else {
////                                unplacedTileBlock = null;
////                                tileBlock.cursor = Cursor.getDefaultCursor();
////                            }
////                            isReplaced = false;
//                        }

class BoardPanel(eventManager: GuiEventManager) : JPanel() {
    private val tilesPerSide = 8;
    private val targetBoardSizePixels = 300;

    private val board = ManualBoard(eventManager);

    init {
        this.layout = null;

        this.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                super.componentResized(e)
                if (e == null) return;
                onResize(e.component.width, e.component.height);
            }
        })

        this.add(board);
    }

    private fun onResize(newWidth: Int, newHeight: Int) {
        // Make sure board size is divisible by 'tilesPerSide'
        val boardSizePixels = this.targetBoardSizePixels / tilesPerSide * tilesPerSide;

        val boardOrigin = Vec2(
            newWidth / 2 - boardSizePixels / 2,
            newHeight / 2 - boardSizePixels / 2
        );

        board.setBounds(boardOrigin.x, boardOrigin.y, boardSizePixels, boardSizePixels);
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        if (g == null) return;
        g.color = Color.BLACK;
        g.fillRect(0, 0, width, height);
    }
}

class ManualBoard(eventManager: GuiEventManager) : JComponent(), GuiEventListener {
    private val tilesPerSide = 8;

    private val tiles = Array(tilesPerSide * tilesPerSide) {Tile.BRICKS};

    private var hoveredTilePos: Vec2? = null;

    private var stagedObject: StagedObject = StagedObject.None;

    init {
        eventManager.addEventListener(this);
        this.addMouseMotionListener(object : MouseMotionListener {
            override fun mouseDragged(e: MouseEvent?) {}
            override fun mouseMoved(e: MouseEvent?) {
                if (e == null) return;
                hoveredTilePos = getTilePosition(e.x, e.y);
                repaint();
            }
        });
        this.addMouseListener(object : MouseAdapter() {
            override fun mouseExited(e: MouseEvent?) {
                super.mouseExited(e);
                if (e == null) return;
                hoveredTilePos = null;
                repaint();
            }
            override fun mouseClicked(e: MouseEvent?) {
                super.mouseClicked(e)
                if (e == null) return;
                val pos = getTilePosition(e.x, e.y);
                eventManager.emitEvent(GuiEvent.ClickOn.Board(pos.x, pos.y));
            }
        });
    }

    override fun onEvent(event: GuiEvent) {
        when (event) {
            is GuiEvent.UpdateBoard -> updateBoard(event.newBoard);
            is GuiEvent.BuildStage.Updated -> stagedObject = event.stagedObject;
            else -> {}
        }
    }

    private fun updateBoard(newBoard: Board) {
        newBoard.tiles.copyInto(this.tiles);
        repaint();
    }

    // Returns the tile type and tile position on the board
    private fun getTilePosition(mouseX: Int, mouseY: Int): Vec2 {
        val tileSize = this.width / this.tilesPerSide;
        return Vec2(
            mouseX / tileSize,
            mouseY / tileSize,
        );
    }
    private fun getTileBlockPosition(tileX: Int, tileY: Int): Vec2 {
        return Vec2(
            if (tileX % 2 == 0) tileX else tileX - 1,
            if (tileY % 2 == 0) tileY else tileY - 1
        );
    }

    private fun getTileBlockAt(tileX: Int, tileY: Int): TileBlock {
        val tileBlockOrigin = Vec2(
            if (tileX % 2 == 0) tileX else tileX - 1,
            if (tileY % 2 == 0) tileY else tileY - 1
        );
        return TileBlock(
            Direction.NORTH,
            this.getTileAt(tileBlockOrigin.x, tileBlockOrigin.y),
            this.getTileAt(tileBlockOrigin.x+1, tileBlockOrigin.y),
            this.getTileAt(tileBlockOrigin.x, tileBlockOrigin.y+1),
            this.getTileAt(tileBlockOrigin.x+1, tileBlockOrigin.y+1),
        );
    }

    // ========== Drawing of board components ========== //
    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        if (g == null) return;

        // First, paint all street tiles
        this.paintTiles(g);
        this.paintHoveredTile(g);
        // Then, paint all built buildings
        // TODO: this
        // Last, paint the "hovered" staged objects
        this.paintStagedObject(g);
    }

    private fun paintStagedObject(g: Graphics) {
        val tilePos = this.hoveredTilePos ?: return;

        when (this.stagedObject) {
            is StagedObject.StagedBuilding -> {
                val building = (this.stagedObject as StagedObject.StagedBuilding).building;
                val buildingOrigin = building.getOrigin();
                val tileOrigin = Vec2(
                    buildingOrigin.x + tilePos.x,
                    buildingOrigin.y + tilePos.y
                );
                drawBuilding(g, building, tileOrigin);
            }
            is StagedObject.StagedTileBlock -> {
                val tileBlock = (this.stagedObject as StagedObject.StagedTileBlock).tileBlock;
                val tileBlockPos = this.getTileBlockPosition(tilePos.x, tilePos.y);
                this.drawTileBlock(g, tileBlock, tileBlockPos);
            }
            else -> {}
        }
    }

    private fun drawBuilding(g: Graphics, building: Building, tileOrigin: Vec2) {
        val borderSize = 5;
        val tileSize = this.width / this.tilesPerSide;

        for (part in building.parts) {
            val tilePos = Vec2(part.x + tileOrigin.x, part.y + tileOrigin.y);
            val neighbors = this.getNeighborsOfPart(part, building.parts.toList());

            val screenPos = Vec2(tilePos.x * tileSize, tilePos.y * tileSize);

            val left = screenPos.x + if (neighbors.contains(Direction.WEST)) 0 else borderSize;
            val top = screenPos.y + if (neighbors.contains(Direction.NORTH)) 0 else borderSize;
            val right = screenPos.x + tileSize - if (neighbors.contains(Direction.EAST)) 0 else borderSize;
            val bottom = screenPos.y + tileSize - if (neighbors.contains(Direction.SOUTH)) 0 else borderSize;

            // Draw building border
            g.color = Color.WHITE;
            g.fillRect(screenPos.x, screenPos.y, tileSize, tileSize);

            // Draw inner building
            g.color = Color.GREEN;
            g.fillRect(left, top, right - left, bottom - top);
        }
    }

    // Lists all directions from given part that directly encounter a neighboring part
    private fun getNeighborsOfPart(part: Vec2, parts: List<Vec2>): Vector<Direction> {
        val neighbors = Vector<Direction>();
        for (other in parts) {
            if (other.x == part.x && other.y == part.y) continue;

            val diffX = other.x - part.x;
            val diffY = other.y - part.y;
            if (abs(diffX) + abs(diffY) == 1) {
                // This is a neighbor!
                if (diffX == -1) neighbors.add(Direction.WEST);
                else if (diffX == 1) neighbors.add(Direction.EAST);
                else if (diffY == 1) neighbors.add(Direction.SOUTH);
                else if (diffY == -1) neighbors.add(Direction.NORTH);
            }
        }

        return neighbors;
    }

    private fun drawTileBlock(g: Graphics, tileBlock: TileBlock, blockPos: Vec2) {
        val tileSize = this.width / this.tilesPerSide;
        val topLeftRect = Rectangle(
                blockPos.x * tileSize,
                blockPos.y * tileSize, tileSize, tileSize);
        val topRightRect = Rectangle(
                (blockPos.x + 1) * tileSize,
                blockPos.y * tileSize, tileSize, tileSize);
        val bottomLeft = Rectangle(
                blockPos.x * tileSize,
                (blockPos.y + 1) * tileSize, tileSize, tileSize);
        val bottomRight = Rectangle(
                (blockPos.x + 1) * tileSize,
                (blockPos.y + 1) * tileSize, tileSize, tileSize);

        this.paintTile(g, topLeftRect, tileBlock.topLeft);
        this.paintTile(g, topRightRect, tileBlock.topRight);
        this.paintTile(g, bottomLeft, tileBlock.bottomLeft);
        this.paintTile(g, bottomRight, tileBlock.bottomRight);
    }

    private fun paintHoveredTile(g: Graphics) {
        val tilePos = this.hoveredTilePos ?: return;

        val tileSize = this.width / this.tilesPerSide;

        val tileScreenPos = Vec2(
            tilePos.x * tileSize,
            tilePos.y * tileSize,
        );

        g.color = Color(0, 50, 0);
        g.fillRect(tileScreenPos.x, tileScreenPos.y, tileSize, tileSize);
    }

    private fun paintHoveredTileBlock(g: Graphics) {
        val tilePos = this.hoveredTilePos ?: return;
        val tileBlockPos = getTileBlockPosition(tilePos.x, tilePos.y);
        val tileSize = this.width / this.tilesPerSide;

        val tileBlockScreenPos = Vec2(
            tileBlockPos.x * tileSize,
            tileBlockPos.y * tileSize
        );

        g.color = Color(0, 100, 0);
        g.fillRect(tileBlockScreenPos.x, tileBlockScreenPos.y, 2*tileSize, 2*tileSize);
    }

    private fun paintTiles(g: Graphics) {
        val tileSize = this.width / this.tilesPerSide;
        for (row in 0..<this.tilesPerSide) {
            for (column in 0..<this.tilesPerSide) {
                val tileType = this.getTileAt(column, row);
                val tileOrigin = Vec2(
                    column * tileSize,
                    row * tileSize
                );
                val tileArea = Rectangle(tileOrigin.x, tileOrigin.y, tileSize, tileSize);
                this.paintTile(g, tileArea, tileType);
            }
        }
    }
    private fun getTileAt(column: Int, row: Int): Tile {
        return this.tiles[row * this.tilesPerSide + column];
    }
    private fun paintTile(g: Graphics, rect: Rectangle, tileType: Tile) {
        when (tileType) {
            Tile.BLUE -> {
                g.color = Color(61, 85, 173);
                g.fillRect(rect.x, rect.y, rect.width, rect.height);
            }

            Tile.ORANGE -> {
                g.color = Color(227, 121, 18);
                g.fillRect(rect.x, rect.y, rect.width, rect.height);
            }

            Tile.SHARED -> {
                g.color = Color(138, 0, 138);
                g.fillRect(rect.x, rect.y, rect.width, rect.height);
            }

            Tile.LANTERN -> {
//                g.color = Color(128, 128, 128);
                g.color = Color(255, 255, 100);
                g.fillRect(rect.x, rect.y, rect.width, rect.height);

//                val xUnit = width / 4;
//                val yUnit = height / 4;
//                g.color = Color(255, 255 ,100);
//                g.fillRect(0 + xUnit, y + yUnit, width - 2*xUnit, height - 2*yUnit);
            }

            Tile.BRICKS -> {
                g.color = Color(128, 128, 128);
                g.fillRect(rect.x, rect.y, rect.width, rect.height);
            }
        }
    }
}

class ScreenBoardTileLayer(eventManager: GuiEventManager) : JPanel() {
    private val BLOCK_COLS = 4;
    private val BLOCK_ROWS = 4;

    init {
        this.layout = GridLayout(BLOCK_ROWS, BLOCK_COLS, 0, 0);
        for (row in 0..<BLOCK_ROWS) {
            for (column in 0..<BLOCK_COLS) {
                val tileBlock = this.createScreenTileBlock(column, row, eventManager);
                this.add(tileBlock);
            }
        }
    }

    private fun createScreenTileBlock(blockColumn: Int, blockRow: Int, eventManager: GuiEventManager): ScreenTileBlock {
        val tileBlock = ScreenTileBlock();

        val blockEventListener = object : TileBlockMouseListener, GuiEventListener {
            // Keep track of staged object
            var stagedObject: StagedObject = StagedObject.None;

            // And keep track of the original - according to the board - tiles of this block
            var originalTileBlock = tileBlock.getDisplayedTileBlock();
            // Can be temporarily replaced because we might display the suggested new TileBlock here.
            var isReplaced = false;

            override fun onEvent(event: GuiEvent) {
                when (event) {
                    is GuiEvent.BuildStage.Updated -> {
                        stagedObject = event.stagedObject;
                        if (stagedObject is StagedObject.StagedTileBlock) {
                            cursor = if (originalTileBlock.isBricks()) {
                                Cursor(Cursor.HAND_CURSOR);
                            } else {
                                Cursor.getSystemCustomCursor("Invalid.32x32");
                            }
                        }
                    }
                    is GuiEvent.UpdateBoard -> {
                        val tileOrigin = Vec2(blockColumn * 2, blockRow * 2);
                        val newTileBlock = TileBlock(
                                Direction.NORTH,
                                event.newBoard.getTile(tileOrigin.x, tileOrigin.y),
                                event.newBoard.getTile(tileOrigin.x+1, tileOrigin.y),
                                event.newBoard.getTile(tileOrigin.x, tileOrigin.y+1),
                                event.newBoard.getTile(tileOrigin.x+1, tileOrigin.y+1),
                        );
                        originalTileBlock = newTileBlock;
                        tileBlock.updateTiles(newTileBlock);
                    }
                    else -> {}
                }
            }
            override fun onSelectedTile(tileBlock: TileBlock, relTileX: Int, relTileY: Int) {
                eventManager.emitEvent(GuiEvent.ClickOn.Board(blockColumn*2 + relTileX, blockRow*2 + relTileY))
            }
            override fun onAbsoluteMouseEnter(e: MouseEvent) {
                when (stagedObject) {
                    is StagedObject.None -> {}
                    is StagedObject.StagedBuilding -> {

                    }
                    is StagedObject.StagedTileBlock -> {
                        val temporaryTiles = (stagedObject as StagedObject.StagedTileBlock).tileBlock.copy();
                        tileBlock.updateTiles(temporaryTiles);
                        isReplaced = true;
                    }
                }
            }

            override fun onAbsoluteMouseExit(e: MouseEvent) {
                if (isReplaced) {
                    tileBlock.updateTiles(originalTileBlock);
                    isReplaced = false;
                }
            }
        }
        tileBlock.addTileBlockMouseListener(blockEventListener);
        eventManager.addEventListener(blockEventListener);
        return tileBlock;
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g);
        if (g == null) return;
        g.color = Color.RED;
        g.fillRect(0, 0, width, height);
    }
}

class ScreenBoardBuildingLayer : JPanel() {
    init {
//        this.setBounds(50, 50, width/2, height/2);
        this.isOpaque = false;
    }

    override fun paint(g: Graphics?) {
//        super.paintComponent(g)
        if (g == null) return;
        println("Painting building layer: %d, %d".format(width, height));
        g.color = Color.BLUE;
        g.fillRect(0, 0, width, height);
    }
}

class ScreenBoard(eventManager: GuiEventManager) : JLayeredPane() {
    private val BOARD_SIZE = 300;

    private val tileLayer = ScreenBoardTileLayer(eventManager);

    init {
        this.add(tileLayer, PALETTE_LAYER);

        this.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                updateBoardPosition();
            }
        })

        val a = ScreenBoardBuildingLayer();
        a.setBounds(0, 0, width, height)
        this.add(a, DEFAULT_LAYER);
//        this.add(ScreenBoardBuildingLayer(), DEFAULT_LAYER, 0);
//        val buildingTest = ScreenBuilding(Building.fromName(BuildingName.ZIGZAG));
//        buildingTest.setBounds(5, 5, width/2, height/2);
//        this.add(JButton("Hello there"), DEFAULT_LAYER);
    }

    private fun updateBoardPosition() {
        for (layer in this.components) {
            layer.setBounds(width / 2 - BOARD_SIZE / 2, height / 2 - BOARD_SIZE / 2, BOARD_SIZE, BOARD_SIZE);
        }
    }
//
//    override fun paintComponent(g: Graphics?) {
//        super.paintComponent(g)
//        if (g == null) return;
//        g.color = Color.MAGENTA;
//        g.fillRect(10, 10, width/2, height/2);
//    }
}

class ScreenBuilding(val building: Building) : JComponent() {
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
                selectionListener!!.onSelect(building.getName());
            }
        })
        this.cursor = Cursor(Cursor.HAND_CURSOR);
    }

    fun rotateBuilding(clockwise: Boolean) {
        this.building.rotate(clockwise);
//        this.invalidate();
        this.repaint();
    }

    override fun contains(x: Int, y: Int): Boolean {
        for (rect in this.getScreenRects()) {
            if (rect.contains(x, y)) return true;
        }
        return false;
    }

    override fun paint(g: Graphics?) {
//        super.paintComponent(g);
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
        val relativeDimension = building.getSize();
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
            val relativeOffset = getRelativeBuildingUnits(screenBuilding.building.getName());
            val absoluteOffset = Vec2(
                    relativeOffset.x * UNIT_SIZE,
                    relativeOffset.y * UNIT_SIZE
            );

            val relativeDimension = screenBuilding.building.getSize();
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