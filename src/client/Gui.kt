package client

import game.*
import game.BoardPiece.*
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
import kotlin.math.min

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
            data class Decoration(val decorationName: DecorationName) : Stage();
        }

        data object Clear : BuildStage();
        data object Rotate : BuildStage();

        data class Updated(val stagedObject: StagedObject) : BuildStage();
    }

    data class UpdateBoard(val newBoard: Board) : GuiEvent();
    data class UpdateGuiPhase(val phase: GuiPhase) : GuiEvent();
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
            synchronized(this.listeners) {
                for (listener in this.listeners) {
                    listener.onEvent(event);
                }
            }
        }
        override fun addEventListener(listener: GuiEventListener) {
            synchronized(this.listeners) {
                this.listeners.add(listener);
            }
        }
    }

    private val window = GameWindow(eventManager);
    private var guiPhase: GuiPhase = INITIAL_GUI_PHASE;
    private var stagedObject: StagedObject = StagedObject.None;
    private var board = Board();

    init {
        this.eventManager.addEventListener(object : GuiEventListener {
            override fun onEvent(event: GuiEvent) { handleEvent(event);}
        })
    }

    private fun handleEvent(event: GuiEvent) {
        when (event) {
            is GuiEvent.ClickOn -> when (event) {
                is GuiEvent.ClickOn.CloseButton -> this.onUserAction(UserAction.CloseWindow);
                is GuiEvent.ClickOn.UnpickedBuilding -> {
                    if (this.guiPhase is GuiPhase.GamePhase1) {
                        val phase = this.guiPhase as GuiPhase.GamePhase1;
                        if (phase.hasTurn) {
                            this.onUserAction(UserAction.PickBuilding(event.name));
                        }
                    }
                }
                is GuiEvent.ClickOn.UnplacedTileBlock -> {
                    if (this.guiPhase is GuiPhase.GamePhase1) {
                        val phase = this.guiPhase as GuiPhase.GamePhase1;
                        if (phase.hasTurn) {
                            if (!event.tileBlock.isBricks()) {
                                this.eventManager.emitEvent(GuiEvent.BuildStage.Stage.UnplacedTileBlock(event.tileBlock));
                            }
                        }
                    }
                }
                is GuiEvent.ClickOn.Board -> {
                    when (this.stagedObject) {
                        is StagedObject.None -> {}
                        is StagedObject.StagedBuilding -> {
                            val staged = this.stagedObject as StagedObject.StagedBuilding;
                            // Buildings can only be staged if we have the turn in phase 2, so no need to check it again
                            val position = Vec2(event.tileX, event.tileY);
                            val supposedPlacedBuilding = Top.Building.from(staged.buildingName, position, staged.rotation);
                            if (this.board.doesTopPieceFit(supposedPlacedBuilding, PlayerColor.BLUE)) {
                                this.onUserAction(UserAction.PlaceBuilding(staged.buildingName, position, staged.rotation));
                            }
                        }
                        is StagedObject.StagedTileBlock -> {
                            val stagedTileBlock = (this.stagedObject as StagedObject.StagedTileBlock).tileBlock;
                            val position = Vec2(
                                    if (event.tileX % 2 == 0) event.tileX else event.tileX - 1,
                                    if (event.tileY % 2 == 0) event.tileY else event.tileY - 1
                            );

                            val supposedTileBlock = TileBlock(
                                stagedTileBlock.rotation,
                                stagedTileBlock.topLeft,
                                stagedTileBlock.topRight,
                                stagedTileBlock.bottomLeft,
                                stagedTileBlock.bottomRight,
                            )
                            supposedTileBlock.move(position);

                            if (this.board.doesTileBlockFit(supposedTileBlock)) {
                                this.onUserAction(UserAction.PlaceTileBlock(position, stagedTileBlock));
                            }
                        }
                        is StagedObject.StagedDecoration -> {
                            val staged = this.stagedObject as StagedObject.StagedDecoration;

                            // Decoration can only be staged if allowed now
                            val position = Vec2(event.tileX, event.tileY);
                            val supposedPlacedDecoration = Top.Decoration.from(staged.name, position, staged.rotation);

                            if (this.board.doesTopPieceFit(supposedPlacedDecoration, PlayerColor.BLUE)) {
                                this.onUserAction(UserAction.PlaceDecoration(supposedPlacedDecoration));
                            }
                        }
                    }
                }
                is GuiEvent.ClickOn.InventoryBuilding -> {
                    // Only Blue, (us, the client), owns buildings that can be placed
                    if (event.owner == PlayerColor.BLUE) {
                        // And clicking only works in phase 2
                        if (this.guiPhase is GuiPhase.GamePhase2) {
                            // When we have the turn
                            val phase = this.guiPhase as GuiPhase.GamePhase2;
                            if (phase.hasTurn) {
                                this.eventManager.emitEvent(GuiEvent.BuildStage.Stage.Building(event.buildingName));
                            }
                        }
                    }
                }

                is GuiEvent.ClickOn.PassButton -> {
                    val hasTurn = when (this.guiPhase) {
                        is GuiPhase.GamePhase1 -> (this.guiPhase as GuiPhase.GamePhase1).hasTurn;
                        is GuiPhase.GamePhase2 -> (this.guiPhase as GuiPhase.GamePhase2).hasTurn;
                        else -> false;
                    }
                    if (hasTurn) {
                        this.onUserAction(UserAction.Pass);
                    }
                }

                is GuiEvent.ClickOn.ActionCard -> {
                    val hasTurn = this.guiPhase is GuiPhase.GamePhase2 && (this.guiPhase as GuiPhase.GamePhase2).hasTurn;
                    if (!hasTurn) {
                        println("Cannot perform card action when you do not have the turn in phase 2");
                        return;
                    }

                    if (event.cardOwner != null) {
                        println("Cannot click this card because it is already owned");
                        return;
                    }

                    when (event.cardType) {
                        CardType.JARDIN_DES_PLANTES -> eventManager.emitEvent(GuiEvent.BuildStage.Stage.Decoration(DecorationName.GARDEN));
                        CardType.LE_PEINTRE -> eventManager.emitEvent(GuiEvent.BuildStage.Stage.Decoration(DecorationName.PAINTER));
                        CardType.MOULIN_ROUGE -> eventManager.emitEvent(GuiEvent.BuildStage.Stage.Decoration(DecorationName.DANCER));
                        CardType.LAMPADAIRE -> eventManager.emitEvent(GuiEvent.BuildStage.Stage.Decoration(DecorationName.LANTERN));
                        CardType.LE_PENSEUR -> eventManager.emitEvent(GuiEvent.BuildStage.Stage.Decoration(DecorationName.STATUE));
                        CardType.LA_GRANDE_LUMIERE -> eventManager.emitEvent(GuiEvent.BuildStage.Stage.Decoration(DecorationName.BIG_LANTERN));
                        CardType.FONTAINE_DES_MERS -> eventManager.emitEvent(GuiEvent.BuildStage.Stage.Decoration(DecorationName.FOUNTAIN));
                        CardType.BOUQUINISTES_SUR_LA_SEINE -> eventManager.emitEvent(GuiEvent.BuildStage.Stage.Decoration(DecorationName.EXTENSION));
                        else -> onUserAction(UserAction.ClaimCard(event.cardType));
                    }
                }
                is GuiEvent.ClickOn.ClearStage -> this.eventManager.emitEvent(GuiEvent.BuildStage.Clear);
                is GuiEvent.ClickOn.RotateStage -> this.eventManager.emitEvent(GuiEvent.BuildStage.Rotate);
            }
            is GuiEvent.BuildStage -> when (event) {
                is GuiEvent.BuildStage.Clear -> {}
                is GuiEvent.BuildStage.Rotate -> {}
                is GuiEvent.BuildStage.Stage.Building -> {}
                is GuiEvent.BuildStage.Stage.UnplacedTileBlock -> {}
                is GuiEvent.BuildStage.Updated -> this.stagedObject = event.stagedObject;
                is GuiEvent.BuildStage.Stage.Decoration -> {}
            }
            is GuiEvent.UpdateBoard -> this.board = event.newBoard;
            is GuiEvent.UpdateGuiPhase -> {
                when (event.phase) {
                    is GuiPhase.InLobby -> openGameWindow()
                    else -> {}
                }
                this.guiPhase = event.phase;
                println("Phase changed to: %s".format(guiPhase.toString()));
            }
        }
    }
    override fun getServerAddress(): Pair<InetAddress, Int> {
        while (true) {
            val startDialog = ConnectDialog();
            startDialog.run();
            while (!startDialog.submitted && !startDialog.isManuallyClosed) Thread.sleep(100);
            startDialog.dispose();

            if (startDialog.isManuallyClosed) {
                println("User closed connection window");
                System.exit(0);
            }

            try {
                val actualAddress = InetAddress.getByName(startDialog.serverAddress);
                val actualPort: Int =  startDialog.serverPort!!;
                return Pair(actualAddress, actualPort);
            } catch (e: BadLocationException) {
                println("Incorrect server address. Please try again.");
            } catch (e: UnknownHostException) {
                println("Failed to connect to server. Please try again");
            }
        }
    }

    override fun updatePhase(newPhase: GuiPhase) {
        SwingUtilities.invokeLater {
            this.eventManager.emitEvent(GuiEvent.UpdateGuiPhase(newPhase));
        }
    }

    override fun updateGameState(board: Board) {
        SwingUtilities.invokeLater {
            this.eventManager.emitEvent(GuiEvent.UpdateBoard(board.deepClone()));
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
    data class StagedBuilding(val buildingName: BuildingName, val rotation: Direction) : StagedObject();
    data class StagedTileBlock(val tileBlock: TileBlock, val screenTileBlock: ScreenTileBlock) : StagedObject();
    data class StagedDecoration(val name: DecorationName, val rotation: Direction) : StagedObject();
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
                        GuiEvent.BuildStage.Clear -> clearStage();
                        is GuiEvent.BuildStage.Rotate -> rotateStagedObject();
                        is GuiEvent.BuildStage.Stage.Building -> stageBuilding(event.buildingName, Direction.NORTH);
                        is GuiEvent.BuildStage.Stage.UnplacedTileBlock -> stageTileBlock(event.tileBlock);
                        is GuiEvent.BuildStage.Updated -> {}
                        is GuiEvent.BuildStage.Stage.Decoration -> stageDecoration(event.decorationName, Direction.NORTH);
                    }
                    is GuiEvent.UpdateBoard -> clearStage();
                    else -> {}
                }
            }
        })
    }

    private fun rotateStagedObject() {
        when (val currentStaged = this.stagedObject) {
            is StagedObject.None -> {}
            is StagedObject.StagedBuilding -> {
                val newRotation = currentStaged.rotation.getRotated(true);
                println("Old rotation: %s, new rotation: %s".format(currentStaged.rotation, newRotation));
                this.stageBuilding(currentStaged.buildingName, newRotation);
            }
            is StagedObject.StagedTileBlock -> {
                val newTileBlock = currentStaged.tileBlock.clone();
                newTileBlock.rotate(true);
                stageTileBlock(newTileBlock);
            }
            is StagedObject.StagedDecoration -> {
                val newRotation = currentStaged.rotation.getRotated(true);
                this.stageDecoration(currentStaged.name, newRotation);
            }
        }
    }

    // Clears the stage
    private fun clearStage() {
        this.stageObject(StagedObject.None, JPanel());
    }

    private fun stageDecoration(name: DecorationName, rotation: Direction) {
        val screenDecoration = ScreenDecoration(name, rotation);
        val stageObject = StagedObject.StagedDecoration(name, rotation);
        this.stageObject(stageObject, screenDecoration);
    }

    // Stages the given building
    private fun stageBuilding(buildingName: BuildingName, rotation: Direction) {
        val screenBuilding = ScreenBuilding(buildingName, rotation);
        val stagedObject = StagedObject.StagedBuilding(buildingName, rotation);
        this.stageObject(stagedObject, screenBuilding);
    }

    // Will stage the given tileBlock
    private fun stageTileBlock(tileBlock: TileBlock) {
        val screenTileBlock = ScreenTileBlock();
        screenTileBlock.updateTiles(tileBlock);
        val stagedObject = StagedObject.StagedTileBlock(tileBlock, screenTileBlock);
        this.stageObject(stagedObject, screenTileBlock);
    }

    // Stages the given StagedComponent, and inserts the corresponding screen component
    private fun stageObject(newStagedObject: StagedObject, screenComponent: JComponent) {
        this.stagedObject = newStagedObject;

        this.remove(1);
        this.add(screenComponent, 1);
        this.revalidate();
        this.repaint();

        this.eventManager.emitEvent(GuiEvent.BuildStage.Updated(newStagedObject));
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

        eventManager.addEventListener(object : GuiEventListener {
            override fun onEvent(event: GuiEvent) {
                if (event is GuiEvent.UpdateBoard) {
                    for (card in event.newBoard.inGameCards) {
                        if (card.type == cardType) {
                            // This is our card! Set out state to whatever we find here
                            cardState = card.state;
                            cardOwner = card.owner;
                            repaint();
                        }
                    }
                }
            }
        })
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
            }
        })
    }

    private fun initializeCards(cards: List<Card>, eventManager: GuiEventManager) {
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

        this.passButton.addActionListener { eventManager.emitEvent(GuiEvent.ClickOn.PassButton); }

        this.preferredSize = Dimension(WIDTH, WIDTH);

        this.border = EmptyBorder(0, BORDER_SIZE*2, BORDER_SIZE, BORDER_SIZE*2);
        this.layout = GridLayout(3, 1, 10, 10);

        this.add(message);
        this.add(unplacedTileBlock);
        this.passButton.isFocusable = false;
        this.add(passButton);

        eventManager.addEventListener(object : GuiEventListener {
            override fun onEvent(event: GuiEvent) {
                when (event) {
                    is GuiEvent.UpdateBoard -> updateTileBlock(event.newBoard);
                    is GuiEvent.UpdateGuiPhase -> updateText(event.phase);
                    else -> {}
                }
            }
        })
    }

    fun updateText(phase: GuiPhase) {
        val newText = when (phase) {
            GuiPhase.Connecting -> "Connecting...";
            is GuiPhase.GameEnd -> "Game ended";
            is GuiPhase.GamePhase1 -> if (phase.hasTurn) "Pick a building or place a tile" else "Waiting for opponent...";
            is GuiPhase.GamePhase2 -> if (phase.hasTurn) "Place a building or play a card" else "Waiting for opponent...";
            GuiPhase.InLobby -> "Waiting for game to start...";
        }
        this.message.text = "<html>$newText</html>";
    }

    fun updateTileBlock(newBoard: Board) {
        val emptyTileBlock = TileBlock(Direction.NORTH, Tile.BRICKS, Tile.BRICKS, Tile.BRICKS, Tile.BRICKS);
        if (newBoard.unplacedBlueBlocks.isEmpty()) {
            this.unplacedTileBlock.updateTiles(emptyTileBlock);
        } else {
            val topBlock = newBoard.unplacedBlueBlocks[0];
            if (topBlock == null) {
                this.unplacedTileBlock.updateTiles(emptyTileBlock);
            } else {
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
    private var hoveredTilePos: Vec2? = null;

    private var stagedObject: StagedObject = StagedObject.None;

    private var board = Board();

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
                val clickPos = getTilePosition(e.x, e.y);
                eventManager.emitEvent(GuiEvent.ClickOn.Board(clickPos.x, clickPos.y));
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
        this.board = newBoard;
        repaint();
    }

    // Returns the tile type and tile position on the board
    private fun getTilePosition(mouseX: Int, mouseY: Int): Vec2 {
        val tileSize = this.width / this.board.SIZE;
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

    private fun getTileAt(column: Int, row: Int): Tile {
        return this.board.getTile(column, row);
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

    // ========== Painting of board components ========== //
    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        if (g == null) return;

        // First, paint all street tiles
        this.paintTiles(g);
        this.paintHoveredTile(g);
        // Then, paint all built buildings
        this.paintPlacedTopPieces(g);
        // Last, paint the "hovered" staged objects
        this.paintStagedObject(g);
    }

    private fun paintPlacedTopPieces(g: Graphics) {
        for (piece in this.board.placedTopPiecesByBlue) {
            when (piece) {
                is Top.Building -> paintBuilding(g, piece, true, PlayerColor.BLUE);
                is Top.Decoration -> paintDecoration(g, piece, true, PlayerColor.BLUE);
            }
        }
        for (piece in this.board.placedTopPiecesByOrange) {
            when (piece) {
                is Top.Building -> paintBuilding(g, piece, true, PlayerColor.ORANGE);
                is Top.Decoration -> paintDecoration(g, piece, true, PlayerColor.ORANGE);
            }
        }
    }

    private fun paintStagedObject(g: Graphics) {
        val tilePos = this.hoveredTilePos ?: return;

        when (this.stagedObject) {
            is StagedObject.StagedBuilding -> {
                val staged = (this.stagedObject as StagedObject.StagedBuilding);
                val building = Top.Building.from(staged.buildingName, tilePos, staged.rotation);
                val fits = this.board.doesTopPieceFit(building, PlayerColor.BLUE);

                this.paintBuilding(g, building, fits, null);
            }
            is StagedObject.StagedTileBlock -> {
                // Do not draw over already placed tileBlocks
                // TODO: Use check defined in Board class
                if (getTileAt(tilePos.x, tilePos.y) != Tile.BRICKS) return;

                val tileBlock = (this.stagedObject as StagedObject.StagedTileBlock).tileBlock;
                val tileBlockPos = this.getTileBlockPosition(tilePos.x, tilePos.y);
                this.paintTileBlock(g, tileBlock, tileBlockPos);
            }
            is StagedObject.StagedDecoration -> {
                val staged = this.stagedObject as StagedObject.StagedDecoration;
                val decoration = Top.Decoration.from(staged.name, tilePos, staged.rotation);
                val fits = this.board.doesTopPieceFit(decoration, PlayerColor.BLUE);

                this.paintDecoration(g, decoration, fits, null);
            }
            StagedObject.None -> {}
        }
    }

    private fun paintDecoration(g: Graphics, decoration: Top.Decoration, fits: Boolean, owner: PlayerColor?) {
        val tileSize = this.width / this.board.SIZE;

        val targetColor = when (decoration.name) {
            DecorationName.GARDEN -> Color(0, 100, 0);
            DecorationName.PAINTER -> Color(50, 0, 0);
            DecorationName.EXTENSION -> Color(100, 60, 60);
            DecorationName.LANTERN -> Color(200, 200, 0);
            DecorationName.DANCER -> Color(255, 100, 100);
            DecorationName.FOUNTAIN -> Color(0, 50, 200);
            DecorationName.STATUE -> Color(100, 100, 100);
            DecorationName.BIG_LANTERN -> Color(255, 255, 150);
        }

        val borderSize = 5;

        for (part in decoration.parts) {
            val neighbors = this.getNeighborsOfPart(part, decoration.parts.toList());

            val screenPos = Vec2(part.x * tileSize, part.y * tileSize);

            val left = screenPos.x + if (neighbors.contains(Direction.WEST)) 0 else borderSize;
            val top = screenPos.y + if (neighbors.contains(Direction.NORTH)) 0 else borderSize;
            val right = screenPos.x + tileSize - if (neighbors.contains(Direction.EAST)) 0 else borderSize;
            val bottom = screenPos.y + tileSize - if (neighbors.contains(Direction.SOUTH)) 0 else borderSize;

            g.color = if (fits) Color.BLACK else Color.RED;
            g.fillRect(screenPos.x, screenPos.y, tileSize, tileSize);

            g.color = targetColor;
            g.fillRect(left, top, right - left, bottom - top);

            if (owner != null) {
                // Draw owner
                val leftOwner = screenPos.x + 2*borderSize;
                val topOwner = screenPos.y + 2*borderSize;
                val rightOwner = screenPos.x + tileSize - 2*borderSize;
                val bottomOwner = screenPos.y + tileSize - 2*borderSize;

                g.color = Color.BLACK;
                g.fillOval(leftOwner, topOwner, rightOwner - leftOwner, bottomOwner - topOwner);

                g.color = when (owner) {
                    PlayerColor.BLUE -> Color.BLUE;
                    PlayerColor.ORANGE -> Color.ORANGE;
                }
                g.fillOval(leftOwner + 2, topOwner + 2, rightOwner - leftOwner - 4, bottomOwner - topOwner - 4);
            }
        }

        // Draw on top the marker that indicates the rotation of this piece of decoration
        if (decoration.name == DecorationName.STATUE || decoration.name == DecorationName.EXTENSION) {
            val part = decoration.parts[0];
            val screenPos = Vec2(part.x * tileSize, part.y * tileSize);

            val left = screenPos.x + borderSize;
            val top = screenPos.y + borderSize;
            val right = screenPos.x + tileSize - borderSize;
            val bottom = screenPos.y + tileSize - borderSize;

            g.color = Color.WHITE;

            when (decoration.rotation) {
                Direction.NORTH -> g.fillRect(left, screenPos.y, right - left, borderSize);
                Direction.EAST -> g.fillRect(right, top, borderSize, bottom - top);
                Direction.SOUTH -> g.fillRect(left, bottom,right - left, borderSize);
                Direction.WEST -> g.fillRect(screenPos.x, top, borderSize, bottom - top);
            }
        }
    }

    private fun paintBuilding(g: Graphics, building: Top.Building, allowed: Boolean, owner: PlayerColor?) {
        val borderSize = 5;
        val tileSize = this.width / this.board.SIZE;

        for (part in building.parts) {
            val neighbors = this.getNeighborsOfPart(part, building.parts.toList());

            val screenPos = Vec2(part.x * tileSize, part.y * tileSize);

            val left = screenPos.x + if (neighbors.contains(Direction.WEST)) 0 else borderSize;
            val top = screenPos.y + if (neighbors.contains(Direction.NORTH)) 0 else borderSize;
            val right = screenPos.x + tileSize - if (neighbors.contains(Direction.EAST)) 0 else borderSize;
            val bottom = screenPos.y + tileSize - if (neighbors.contains(Direction.SOUTH)) 0 else borderSize;

            // Draw building border
            g.color = Color.WHITE;
            g.fillRect(screenPos.x, screenPos.y, tileSize, tileSize);

            // Draw inner building
            g.color = if (allowed) Color.GREEN else Color.RED;
            g.fillRect(left, top, right - left, bottom - top);

            if (owner != null) {
                // Draw owner
                val leftOwner = screenPos.x + 2*borderSize;
                val topOwner = screenPos.y + 2*borderSize;
                val rightOwner = screenPos.x + tileSize - 2*borderSize;
                val bottomOwner = screenPos.y + tileSize - 2*borderSize;

                g.color = Color.BLACK;
                g.fillOval(leftOwner, topOwner, rightOwner - leftOwner, bottomOwner - topOwner);

                g.color = when (owner) {
                    PlayerColor.BLUE -> Color.BLUE;
                    PlayerColor.ORANGE -> Color.ORANGE;
                }
                g.fillOval(leftOwner + 2, topOwner + 2, rightOwner - leftOwner - 4, bottomOwner - topOwner - 4);
            }
        }
    }

    private fun paintTileBlock(g: Graphics, tileBlock: TileBlock, blockPos: Vec2) {
        val tileSize = this.width / this.board.SIZE;
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

        val tileSize = this.width / this.board.SIZE;

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
        val tileSize = this.width / this.board.SIZE;

        val tileBlockScreenPos = Vec2(
            tileBlockPos.x * tileSize,
            tileBlockPos.y * tileSize
        );

        g.color = Color(0, 100, 0);
        g.fillRect(tileBlockScreenPos.x, tileBlockScreenPos.y, 2*tileSize, 2*tileSize);
    }

    private fun paintTiles(g: Graphics) {
        val tileSize = this.width / this.board.SIZE;
        for (row in 0..<this.board.SIZE) {
            for (column in 0..<this.board.SIZE) {
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
                g.color = Color(128, 128, 128);
//                g.color = Color(255, 255, 100);
                g.fillRect(rect.x, rect.y, rect.width, rect.height);

                val xUnit = rect.width / 4;
                val yUnit = rect.height / 4;
                g.color = Color(255, 255 ,100);
                g.fillRect(
                    rect.x + xUnit,
                    rect.y + yUnit,
                    rect.width - 2*xUnit,
                    rect.height - 2*yUnit
                );
            }

            Tile.BRICKS -> {
                g.color = Color(128, 128, 128);
                g.fillRect(rect.x, rect.y, rect.width, rect.height);
            }
        }
    }
}

class ScreenDecoration(val decorationName: DecorationName, val rotation: Direction) : JComponent() {
    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)
        if (g == null) return;

        g.color = Color(127, 50, 0);
        g.fillRect(0, 0, width, height);

        g.color = Color.BLACK;
        g.drawString(decorationName.name, 10, height/2);
    }
}

class ScreenBuilding(val buildingName: BuildingName, rotation: Direction) : JComponent() {
    private val borderSize = 5;
    var isHovered = false;
    var selectionListener: BuildingSelectionListener? = null;

    private val building = Top.Building.from(buildingName, Vec2(0, 0), rotation);

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
                selectionListener!!.onSelect(buildingName);
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
    private val borderSize = 2;
    private val COLLECTION_WIDTH = 200 + borderSize;
    private val UNIT_SIZE = (COLLECTION_WIDTH - 2*borderSize) / 7;
    private val COLLECTION_HEIGHT = UNIT_SIZE * 8 + 2*borderSize;

    private val screenBuildingChildren = Vector<ScreenBuilding>();

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
            val relativeOffset = getRelativeBuildingUnits(screenBuilding.buildingName);
            val absoluteOffset = Vec2(
                    relativeOffset.x * UNIT_SIZE,
                    relativeOffset.y * UNIT_SIZE
            );

            val building = Top.Building.from(screenBuilding.buildingName, Vec2(0, 0), Direction.NORTH);
            val relativeDimension = building.getSize();

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
            val screenBuilding = ScreenBuilding(buildingName, Direction.NORTH);
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

    var serverAddress: String? = null;
    var serverPort: Int? = null;
    var submitted = false;
    var isManuallyClosed = false;

    init {
        this.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                super.windowClosing(e);
                isManuallyClosed = true;
            }
        })
    }

    fun run() {
        this.title = "Start Parijs";
        this.setSize(400, 150);
        this.setLocationRelativeTo(null);
        this.defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE;
        this.layout = GridLayout(5, 1);

        val submit = JButton("Join server");
        submit.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent?) {
                if (serverAddress == null) serverAddress = DEFAULT_ADDRESS;
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
                val newAddr = e.document.getText(0, e.document.length);

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