package client

import game.*
import java.net.ConnectException
import java.net.Socket
import java.util.*

class Client(private val ui: UI) : Player {
    private var board = Board();

    private var shouldExit = false;
    init {
        this.ui.addUserActionListener(object : UserActionListener() {
            override fun onUserAction(action: UserAction) {
                when (action) {
                    is UserAction.CloseWindow -> {
                        println("Received window close event. Closing Client!");
                        shouldExit = true
                    }
                    is UserAction.Pass -> {
//                        println("Received user pass event");
                    }
                    is UserAction.PickBuilding -> {
//                        println("Received user pick building: %s".format(action.buildingName));
                    }
                    is UserAction.PlaceTileBlock -> {
//                        println("Received user place tile event at (%d, %d)".format(action.position.x, action.position.y));
                    }
                    is UserAction.PlaceBuilding -> {
                        println("Received place building event of %s on (%d, %d)".format(action.buildingName, action.position.x, action.position.y));
                    }
                }
            }
        });
    }

    fun run() {
        // Get a connection to the server
        val serverConnection = getServerConnection() ?: return;

        // Run the handling of it
        serverConnection.start();

        // Update main gui phase
        this.ui.updatePhase(GuiPhase.InLobby);

        // And wait until we are done
        while (!this.shouldExit) Thread.sleep(100);

        // Stop the server connection
        serverConnection.stop();
    }

    private fun getServerConnection(): ServerHandler? {
        var serverHandler: ServerHandler? = null;

        while (!this.shouldExit) {
            val address = this.ui.getServerAddress();
            try {
                val socket = Socket(address.first, address.second);
                serverHandler = ServerHandler(socket, this);
                break;
            } catch (e: ConnectException) {
                println("Failed to connect. Try again!");
            }
        }

        return serverHandler;
    }

    override fun startPhase1(cards: List<CardType>) {
        synchronized(this.board) {
            // Reset the board
            this.board = Board();

            this.board.inGameCards = Vector(cards.map { Card(it, CardState.UNPICKED_AND_UNUSED) });
            this.board.unpickedBuildings = Vector(BuildingName.entries);

            for (i in 0..<8) {
                this.board.unplacedBlueBlocks.add(null);
                this.board.unplacedOrangeBlocks.add(null);
            }

            this.ui.updateGameState(this.board);
        }
        this.ui.updatePhase(GuiPhase.GamePart1(false));
    }

    override fun startPhase2() {
        println(" ===== Phase 2 started! =====");
    }

    override fun askTurnPhase1(availableBuildings: List<BuildingName>, topTileBlock: TileBlock?): UserMove {
        println("\nWe received the turn. Available block: %s".format(topTileBlock?.toString()));
        // Update our board
        synchronized(this.board) {
            this.board.unpickedBuildings = Vector(availableBuildings);

            if (topTileBlock == null) {
                // That means we do not have unplaced tileBlocks left.
                println("No unplaced blocks left");
                this.board.unplacedBlueBlocks.clear();
            } else {
                this.board.unplacedBlueBlocks[0] = topTileBlock;
            }

            this.ui.updateGameState(this.board);
        }

        // Create action listener that stores user action here
        var move: UserMove? = null;
        val listener = object : UserActionListener() {
            override fun onUserAction(action: UserAction) {
                when (action) {
                    is UserAction.PickBuilding -> move = UserMove.PickBuilding(action.buildingName);
                    is UserAction.PlaceTileBlock -> move = UserMove.PlaceBlockAt(action.position, action.tileBlock);
                    is UserAction.Pass -> move = UserMove.Pass;
                    else -> {
                        println("Ignoring action that is not applicable: %s".format(action.toString()));
                    }
                }
            }
        };

        // Add action listener
        this.ui.addUserActionListener(listener);

        // Indicate the user is free to make the move
        this.ui.updatePhase(GuiPhase.GamePart1(true));

        // Wait for a move to be done
        while (move == null) Thread.sleep(100);

        // Remove the action listener again
        this.ui.removeUserActionListener(listener);

        val validatedMove: UserMove = move!!;
        // Update our own board representation
        when (validatedMove) {
            is UserMove.Pass -> {}
            is UserMove.PickBuilding -> {
                synchronized(this.board) {
                    this.board.unpickedBuildings.removeElement(validatedMove.buildingName);
                    this.board.blueInventoryBuildings.add(validatedMove.buildingName);
                    this.ui.updateGameState(this.board);
                }
                println(" -> Decided on: Pick building: " + validatedMove.buildingName.name);
            }
            is UserMove.PlaceBlockAt -> {
                synchronized(this.board) {
                    this.board.placeTileBlock(validatedMove.position, validatedMove.tileBlock);
                    this.board.unplacedBlueBlocks.removeAt(0);
                    println("Updating board...");
                    this.ui.updateGameState(this.board);
                }
                println(" -> Decided on: Place block: " + validatedMove.tileBlock.topLeft.name + "," + validatedMove.tileBlock.topRight.name + "," + validatedMove.tileBlock.bottomLeft.name + "," + validatedMove.tileBlock.bottomRight.name);
            }
            else -> {
                throw Exception("Received phase 2 move while in phase 1");
            }
        }

        return validatedMove;
    }

    override fun askTurnPhase2(): UserMove {
        println("\nWe received the turn for phase 2.");

        var move: UserMove? = null;
        val listener = object : UserActionListener() {
            override fun onUserAction(action: UserAction) {
                when (action) {
                    is UserAction.PlaceBuilding -> move = UserMove.PlaceBuilding(action.buildingName, action.position);
                    is UserAction.Pass -> move = UserMove.Pass;
                    else -> {
                        println("Received unexpected move!");
                    }
                }
            }
        }
        this.ui.addUserActionListener(listener);

        // TODO: Indicate the user is allowed to make a move

        while (move == null) Thread.sleep(100);

        this.ui.removeUserActionListener(listener);
        val validatedMove = move!!;
        when (validatedMove) {
            is UserMove.PlaceBuilding -> {
                synchronized(this.board) {

                }
            }
            is UserMove.Pass -> TODO()
            else -> println("Received phase 1 move while in phase 2");
        }
        return validatedMove;
    }

    override fun respondToMove(response: MoveResponse) {
        println("    %s".format(response.toString()))
    }

    override fun updateMove(move: UserMove) {
        when (move) {
            UserMove.Pass -> println("Other opponent skipped its turn");
            is UserMove.PickBuilding -> {
                println("Opponent picked building: %s".format(move.buildingName));
                synchronized(this.board) {
                    this.board.unpickedBuildings.removeElement(move.buildingName);
                    this.board.orangeInventoryBuildings.add(move.buildingName);

                    this.ui.updateGameState(this.board);
                }
            }
            is UserMove.PlaceBlockAt -> {
                println("Opponent placed tile block at (%d, %d): %s,%s,%s,%s".format(move.position.x, move.position.y, move.tileBlock.topLeft.name, move.tileBlock.topRight.name, move.tileBlock.bottomLeft.name, move.tileBlock.bottomRight.name));
                synchronized(this.board) {
                    this.board.placeTileBlock(move.position, move.tileBlock);
                    this.board.unplacedOrangeBlocks.removeAt(0);
                    this.ui.updateGameState(this.board);
                }
            } else -> {
                println("Updating phase 2 move while in phase 1");
            }
        }
    }

    override fun declareWinner(isWinner: Boolean) {
        TODO("Not yet implemented")
    }


//    fun run() {
//        // First, wait for welcome lobby message
//        if (this.read() == PACKET_TYPES.WELCOME_IN_LOBBY.name) {
//            println("You joined the lobby!")
//        } else {
//            println("Error: Expected different packet");
//            return;
//        }
//
//        this.board = PACKET_TYPES.GAME_STARTED.gameStartedFromString(this.read());
//        this.gui.updateBoard(board);
//        println("Game started and received state!");
//
//        handleSetup();
//    }
//
//    private fun handleSetup() {
//        var isInSetup = true;
//        while (isInSetup) {
//
//            val msg = this.read();
//
//            when (msg.split("=")[0]) {
//                PACKET_TYPES.GIVE_SETUP_TURN.name -> {
//                    println("We received a SETUP turn request. Enter your response:");
//                    handleTurnPart1(msg);
//                }
//                PACKET_TYPES.SETUP_FINISHED.name -> {
//                    println("Setup finished according to server");
//                    isInSetup = false;
//                }
//                else -> throw Exception("Did not expect this message: %s".format(msg));
//            }
//        }
//        println("Leaving setup function...");
//    }
//
//    private fun handleTurnPart1(msg: String) {
//        this.board = PACKET_TYPES.GIVE_SETUP_TURN.giveSetupFromString(msg);
//        this.gui.updateBoard(this.board);
//
////        gui.allowTurnPart1();
//        // Wait for last move to be present
//        while (true) {}
////        while (board.lastMove == null) {
////            Thread.sleep(100);
////        }
//
//        gui.updateBoard(board);
//
////        println("Detected being picked: '%s'".format(board.lastMove));
////        write(board.lastMove as String);
//
//        // Wait for confirmation:
//        val response = read();
//        when (response.split("=")[0]) {
//            PACKET_TYPES.APPROVE_SETUP_TURN.name -> {
//                println("Our move got approved!");
//                this.board = PACKET_TYPES.APPROVE_SETUP_TURN.approveSetupFromString(response);
//                this.gui.updateBoard(this.board);
//            }
//            PACKET_TYPES.DENY_SETUP_TURN.name -> {
//                println("Oh no, we failed according to the server");
//            }
//        }
//
//        return;
//    }
//
//    private fun read(): String {
//        println("Waiting for incoming message...");
//        return this.scanner.nextLine();
//    }
//    private fun write(message: String) {
//        val data = (message + "\n").toByteArray(Charset.defaultCharset());
//        this.writer.write(data);
////        this.writer.flush();
//    }

}