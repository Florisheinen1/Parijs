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
        data class InventoryBuilding(val buildingName: BuildingName) : ClickOn();
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
                is GuiEvent.ClickOn.PassButton -> TODO()

                is GuiEvent.ClickOn.ActionCard -> TODO()
                is GuiEvent.ClickOn.Board -> TODO()
                is GuiEvent.ClickOn.ClearStage -> this.eventManager.emitEvent(GuiEvent.BuildStage.Clear);
                is GuiEvent.ClickOn.RotateStage -> this.eventManager.emitEvent(GuiEvent.BuildStage.Rotate);
                is GuiEvent.ClickOn.InventoryBuilding -> TODO()
            }
            is GuiEvent.BuildStage -> when (event) {
                is GuiEvent.BuildStage.Clear -> {}
                is GuiEvent.BuildStage.Rotate -> {}
                is GuiEvent.BuildStage.Stage.Building -> TODO()
                is GuiEvent.BuildStage.Stage.UnplacedTileBlock -> {}
                is GuiEvent.BuildStage.Updated -> {}
            }
        }
    }

//        when (click) {
//            is GuiEvent.ClickOn.UnpickedBuilding -> this.onUserAction(UserAction.PickBuilding(click.name));
//            is GuiEvent.ClickOn.CloseButton -> this.onUserAction(UserAction.CloseWindow)
//            is GuiEvent.ClickOn.UnplacedTileBlock -> {
//                TODO()
//                if (this.selectClick != null) {
//                    this.selectedClick!!.second.deselect();
//                    if (this.selectedClick!!.first is ClickedOn.UnplacedTileBlock) {
//                        this.selectedClick = null;
//                        return;
//                    }
//                }
//                this.selectedClick = Pair(click, click.selectable);
//                click.selectable.select();
//            }
//            is GuiEvent.ClickOn.Board -> {
//                TODO()
//                if (selectedClick == null) return;
//                val previousClick = selectedClick!!.first;
//                when (previousClick) {
//                    is ClickedOn.UnplacedTileBlock -> {
//                        val pos = Vec2(
//                            if (click.tileX % 2 == 0) click.tileX else click.tileX - 1,
//                            if (click.tileY % 2 == 0) click.tileY else click.tileY - 1
//                        )
//                        val tile = previousClick.tileBlock;
//                        onUserAction(UserAction.PlaceTileBlock(pos, tile));
//                        previousClick.selectable.deselect();
//                        selectedClick = null;
//                    }
//                    is ClickedOn.InventoryBuilding -> {
//                        val tilePos = Vec2(click.tileX, click.tileY);
//                        onUserAction(UserAction.PlaceBuilding(tilePos, previousClick.buildingName));
//                        previousClick.selectable.deselect();
//                    }
//                    else -> {}
//                }
//            }
//            is GuiEvent.ClickOn.InventoryBuilding -> {
//                // TODO: Unless this is clicked after the right card has been clicked
//                TODO()
//                selectedClick?.second?.deselect();
//                selectedClick = Pair(click, click.selectable);
//                click.selectable.select();
//            }
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
//
//                }
//            }
//
//            is GuiEvent.ClickOn.ClearStage -> TODO()
//            is GuiEvent.ClickOn.RotateStage -> TODO()
//            is GuiEvent.ClearStage -> TODO()
//            is GuiEvent.RotateStage -> TODO()
//            is GuiEvent.Stage.Building -> TODO()
//        }

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

class CenterPanel(clickManager: GuiEventManager) : JPanel() {
    private val screenBoard = ScreenBoard(clickManager);
    private val buildStage = BuildStage(clickManager);

    init {
        this.layout = BorderLayout();
        this.add(screenBoard, BorderLayout.CENTER);
        this.add(buildStage, BorderLayout.NORTH);
    }

    fun updateBoard(newBoard: Board) {
        this.screenBoard.updateBoard(newBoard);
    }
}

class GameWindow(private val clickManager: GuiEventManager) : JFrame() {

    private val centerPanel = CenterPanel(clickManager);
    private val bluePlayerArea = PlayerPanel(PlayerColor.BLUE);
    private val orangePlayerArea = PlayerPanel(PlayerColor.ORANGE);
    private val bottomPanel = BottomPanel(clickManager);

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
                clickManager.emitEvent(GuiEvent.ClickOn.CloseButton);
            }
        })

        this.pack();
    }

    fun updateBoard(newBoard: Board) {
        this.bottomPanel.updateBoard(newBoard);
        this.bluePlayerArea.updateBoard(newBoard);
        this.orangePlayerArea.updateBoard(newBoard);
        this.centerPanel.updateBoard(newBoard);
    }

    fun onPhaseChange(newPhase: GuiPhase) {

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
                        is GuiEvent.BuildStage.Stage.Building -> println("Staging building");
                        is GuiEvent.BuildStage.Stage.UnplacedTileBlock -> {
                            println("Staging tileBlock!");
                            stageTileBlock(event.tileBlock);
                        }
                        is GuiEvent.BuildStage.Updated -> {}
                    }
                    else -> {}
                }
            }
        })
    }

    private fun rotateBuilding() {
        when (val currentStaged = this.stagedObject) {
            is StagedObject.None -> {}
            is StagedObject.StagedBuilding -> TODO()
            is StagedObject.StagedTileBlock -> {
                println("Before rotating: %s".format(currentStaged.tileBlock.toString()));
                currentStaged.tileBlock.rotate(true);
                currentStaged.screenTileBlock.updateTiles(currentStaged.tileBlock);

                this.eventManager.emitEvent(GuiEvent.BuildStage.Updated(currentStaged));
                println("Rotated tileblock: %s".format(currentStaged.tileBlock.toString()));
            }
        }
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

class BottomPanel(private val clickManager: GuiEventManager) : JPanel() {
    private val unpickedBuildings = BuildingCollection(object : BuildingSelectionListener {
        override fun onSelect(building: BuildingName) {
            clickManager.emitEvent(GuiEvent.ClickOn.UnpickedBuilding(building));
        }
    });

    private val rightPanel = BottomRightPanel(clickManager);
    private val cardsPanel = CardsCollection(clickManager);

    init {
        this.layout = BorderLayout();
        unpickedBuildings.updateBuildings(BuildingName.entries);
        this.add(unpickedBuildings, BorderLayout.CENTER);
        this.add(rightPanel, BorderLayout.EAST);
        this.add(cardsPanel, BorderLayout.WEST);
    }

    fun updateBoard(newBoard: Board) {
        this.unpickedBuildings.updateBuildings(newBoard.unpickedBuildings);
        this.rightPanel.updateBoard(newBoard);
        this.cardsPanel.updateCards(newBoard.inGameCards);
    }
}

class ScreenCard(val clickManager: GuiEventManager) : JComponent() {
    private var cardType: CardType? = null;
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
                if (cardType == null) return;
                clickManager.emitEvent(GuiEvent.ClickOn.ActionCard(cardType!!, cardState, cardOwner))
            }
        })
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g);
        if (g == null) return;

        if (this.cardType == null) {
            g.color = Color.BLACK;
            g.fillRect(0, 0, width, height);
        } else this.drawCard(g, this.cardType!!, this.cardOwner);
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

    fun updateCard(card: Card) {
        this.cardType = card.type;
        this.cardState = card.state;
        this.cardOwner = card.owner;
        repaint();
    }
}

class CardsCollection(clickManager: GuiEventManager) : JPanel() {
    private val screenCards = Vector<ScreenCard>();
    private val CARD_COLS = 2;
    private val CARD_ROWS = 4;
    private val SIZE = 400;

    init {
        this.layout = GridLayout(CARD_ROWS, CARD_COLS);
        this.preferredSize = Dimension(SIZE, SIZE);

        for (i in 0..<8) {
            val card = ScreenCard(clickManager);
            this.screenCards.add(card);
            this.add(card);
        }
    }

    fun updateCards(cards: List<Card>) {
        for (i in 0..<8) {
            val screenCard = this.screenCards[i];
            val card = cards[i];
            screenCard.updateCard(card);
        }
    }
}

class BottomRightPanel(private val clickManager: GuiEventManager) : JPanel() {
    private val WIDTH = 200;
    private val BORDER_SIZE = 10;
    private val message = JLabel("Text goes here");

    private val unplacedTileBlock = ScreenTileBlock();

    private val passButton = JButton("Skip turn");

    private var tileBlockIsHovered = false;
    private var tileBlockIsSelected = false;

    init {
        this.unplacedTileBlock.addTileBlockClickListener(object : TileBlockMouseListener {
            override fun onSelectedTile(tileBlock: TileBlock, relTileX: Int, relTileY: Int) {
                clickManager.emitEvent(GuiEvent.ClickOn.UnplacedTileBlock(tileBlock));
            }
            override fun onAbsoluteMouseEnter(e: MouseEvent) {
                tileBlockIsHovered = true;
                updateTileBlockBorder();
            }
            override fun onAbsoluteMouseExit(e: MouseEvent) {
                tileBlockIsHovered = false;
                updateTileBlockBorder();
            }
        })
        this.updateTileBlockBorder();

        this.preferredSize = Dimension(WIDTH, WIDTH);

        this.border = EmptyBorder(0, BORDER_SIZE*2, BORDER_SIZE, BORDER_SIZE*2);
        this.layout = GridLayout(3, 1, 10, 10);

        this.add(message);
        this.add(unplacedTileBlock);
        this.passButton.isFocusable = false;
        this.add(passButton);

    }

    private fun updateTileBlockBorder() {
        var borderColor = if (this.tileBlockIsHovered) Color.BLACK else Color.WHITE;
        if (this.tileBlockIsSelected) borderColor = Color.RED;
        unplacedTileBlock.border = BorderFactory.createLineBorder(borderColor, 5);
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

    fun addTileBlockClickListener(listener: TileBlockMouseListener) {
        synchronized(this.tileBlockMouseListeners) {
            this.tileBlockMouseListeners.add(listener);
        }
    }
    private fun fireTileBlockClickEvent(tileBlock: TileBlock, relTileX: Int, relTileY: Int) {
        synchronized(this.tileBlockMouseListeners) {
            for (listener in this.tileBlockMouseListeners) {
                listener.onSelectedTile(tileBlock, relTileX, relTileY);
            }
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

class ScreenBoardTileLayer(val clickManager: GuiEventManager) : JPanel() {
    private val BLOCK_COLS = 4;
    private val BLOCK_ROWS = 4;

    private val tileBlocks = Vector<ScreenTileBlock>();

    init {
        this.layout = GridLayout(BLOCK_ROWS, BLOCK_COLS, 0, 0);
        for (row in 0..<BLOCK_ROWS) {
            for (column in 0..<BLOCK_COLS) {
                val tileBlock = ScreenTileBlock();

                val hoverManager = object : TileBlockMouseListener, GuiEventListener {
                    var unplacedTileBlock: TileBlock? = null;
                    var originalTileBlock: TileBlock? = null;
                    var isReplaced = false;

                    override fun onEvent(event: GuiEvent) {
                        synchronized(tileBlock) {
                            if (event is GuiEvent.ClickOn.UnplacedTileBlock) {
                                unplacedTileBlock = event.tileBlock.copy();
                                // We want to place a tileblock
                                if (tileBlock.getDisplayedTileBlock().topLeft != Tile.BRICKS) {
                                    tileBlock.cursor = Cursor.getSystemCustomCursor("Invalid.32x32");
//                                    tileBlock.cursor = Cursor(Cursor.WAIT_CURSOR);
                                } else {
                                    tileBlock.cursor = Cursor(Cursor.HAND_CURSOR);
                                }
                            } else {
                                unplacedTileBlock = null;
                                tileBlock.cursor = Cursor.getDefaultCursor();
                            }
                            isReplaced = false;
                        }
                    }
                    override fun onSelectedTile(tileBlock: TileBlock, relTileX: Int, relTileY: Int) {
                        synchronized(tileBlock) {
                            clickManager.emitEvent(GuiEvent.ClickOn.Board(column*2 + relTileX, row*2 + relTileY))
                        }
                    }
                    override fun onAbsoluteMouseEnter(e: MouseEvent) {
                        synchronized(tileBlock) {
                            if (unplacedTileBlock != null) {
                                val current = tileBlock.getDisplayedTileBlock();
                                if (current.topLeft == Tile.BRICKS) {
                                    originalTileBlock = current.copy();
                                    tileBlock.updateTiles(unplacedTileBlock!!.copy());
                                    isReplaced = true;
//                                    cursor = Cursor(Cursor.HAND_CURSOR);
                                } else {
//                                    cursor = Cursor.getSystemCustomCursor("Invalid.32x32");
                                }
                            }
                        }
                    }
                    override fun onAbsoluteMouseExit(e: MouseEvent) {
                        synchronized(tileBlock) {
                            if (isReplaced) {
                                tileBlock.updateTiles(originalTileBlock!!);
                                isReplaced = false;
                                originalTileBlock = null;
                            }
                        }
                    }
                }

                tileBlock.addTileBlockClickListener(hoverManager);
                clickManager.addEventListener(hoverManager);

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

class ScreenBoard(clickManager: GuiEventManager) : JLayeredPane() {
    private val BOARD_SIZE = 300;

    val tileLayer = ScreenBoardTileLayer(clickManager);

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