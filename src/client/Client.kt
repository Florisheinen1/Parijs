package client

import game.Board
import protocol.PACKET_TYPES
import java.net.Socket
import java.nio.charset.Charset
import java.util.Scanner

class Client(val serverSocket: Socket, val gui: Gui) {
    val scanner = Scanner(this.serverSocket.getInputStream());
    val writer = this.serverSocket.getOutputStream();

    val board = Board();

    fun run() {
        // First, wait for welcome lobby message
        if (this.read() == PACKET_TYPES.WELCOME_IN_LOBBY.name) {
            println("You joined the lobby!")
        } else {
            println("Error: Expected different packet");
        }

        val gameCards = PACKET_TYPES.GAME_STARTED.gameStartFromString(this.read());
        this.board.selectedCardsForGame = gameCards;
        println("Game started!");

        var i = 0;

        while (true) {
            gui.updateBoard(board);

            // Read turn message
            val turnMsg = this.read();
            println("[%d]".format(i));
            i++;

            when (turnMsg.split(":")[0]) {
                PACKET_TYPES.GIVE_SETUP_TURN.name -> {
                    println("We received a SETUP turn request. Enter your response:");
                    handleTurnPart1(turnMsg);
                }
                PACKET_TYPES.GIVE_PLAY_TURN.name -> {
                    println("We received a PLAY turn request. Enter your response:");
                    val response = readln();
                    this.write(response);
                }
                else -> {
                    println("Received other message:");
                    println("'%s'".format(turnMsg));
                }
            }
        }
    }

    private fun handleTurnPart1(msg: String) {
        val buildingsAndBlock = PACKET_TYPES.GIVE_SETUP_TURN.setupTurnFromString(msg);

        board.updateUnpickedBuildings(buildingsAndBlock.first);
        board.topBlueBlock = buildingsAndBlock.second;

        board.lastMove = null;
        gui.updateBoard(board);
        gui.allowTurnPart1();
        // Wait for
        while (board.lastMove == null) {
            Thread.sleep(100);
        }
        gui.updateBoard(board);
        println("Detected being picked: '%s'".format(board.lastMove));
        write(board.lastMove as String);
        return;
    }

    private fun read(): String {
        println("Waiting for incoming message...");
        return this.scanner.nextLine();
    }
    private fun write(message: String) {
        val data = (message + "\n").toByteArray(Charset.defaultCharset());
        this.writer.write(data);
//        this.writer.flush();
    }
}