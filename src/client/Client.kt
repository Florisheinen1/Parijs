package client

import game.*
import java.net.ConnectException
import java.net.Socket
import java.util.*

class Client(private val ui: UI) : Player {
    private val board = Board();

    private var shouldExit = false;
    init {
        this.ui.addUserActionListener(object : UserActionListener() {
            override fun onUserAction(action: UserAction) {
                if (action is UserAction.CloseWindow) {
                    println("Received window close event. Closing Client!");
                    shouldExit = true
                };
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

    override fun startPart1(cards: List<Cards>) {
        this.board.ingameCards = Vector(cards);
        this.ui.updatePhase(GuiPhase.GamePart1(false));
    }

    override fun askTurnPart1(availableBuildings: List<BuildingName>, topTileBlock: TileBlock?): MovePart1 {
        // Update our board
        this.board.unpickedBuildings = Vector(availableBuildings);
        this.board.topBlueBlock = topTileBlock;

        // Create action listener that stores user action here
        var move: MovePart1? = null;
        val listener = object : UserActionListener() {
            override fun onUserAction(action: UserAction) {
                when (action) {
                    is UserAction.PickBuilding -> move = MovePart1.PickBuilding(action.buildingName);
                    is UserAction.PlaceTileBlock -> move = MovePart1.PlaceBlockAt(action.position);
                    is UserAction.Pass -> move = MovePart1.Pass;
                    else -> {}
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

        return move!!;
    }

    override fun respondToMove(response: MoveResponse) {
        TODO("Not yet implemented")
    }

    override fun updateMovePart1(move: MovePart1) {
        TODO("Not yet implemented")
    }

    override fun startPar2() {
        TODO("Not yet implemented")
    }

    override fun askTurnPart2(): MovePart2 {
        TODO("Not yet implemented")
    }

    override fun updateMovePart2(move: MovePart2) {
        TODO("Not yet implemented")
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