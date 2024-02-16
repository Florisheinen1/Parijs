package client

import game.Board
import protocol.PACKET_TYPES
import java.net.Socket
import java.nio.charset.Charset
import java.util.Scanner

class Client(val serverSocket: Socket, val gui: Gui) {
    val scanner = Scanner(this.serverSocket.getInputStream());
    val writer = this.serverSocket.getOutputStream();

    var board = Board();

    fun run() {
        // First, wait for welcome lobby message
        if (this.read() == PACKET_TYPES.WELCOME_IN_LOBBY.name) {
            println("You joined the lobby!")
        } else {
            println("Error: Expected different packet");
            return;
        }

        this.board = PACKET_TYPES.GAME_STARTED.gameStartedFromString(this.read());
        this.gui.updateBoard(board);
        println("Game started and received state!");

        handleSetup();
    }

    private fun handleSetup() {
        var isInSetup = true;
        while (isInSetup) {

            val msg = this.read();

            when (msg.split("=")[0]) {
                PACKET_TYPES.GIVE_SETUP_TURN.name -> {
                    println("We received a SETUP turn request. Enter your response:");
                    handleTurnPart1(msg);
                }
                PACKET_TYPES.SETUP_FINISHED.name -> {
                    println("Setup finished according to server");
                    isInSetup = false;
                }
                else -> throw Exception("Did not expect this message: %s".format(msg));
            }
        }
        println("Leaving setup function...");
    }

    private fun handleTurnPart1(msg: String) {
        this.board = PACKET_TYPES.GIVE_SETUP_TURN.giveSetupFromString(msg);
        this.gui.updateBoard(this.board);

//        gui.allowTurnPart1();
        // Wait for last move to be present
        while (true) {}
//        while (board.lastMove == null) {
//            Thread.sleep(100);
//        }

        gui.updateBoard(board);

//        println("Detected being picked: '%s'".format(board.lastMove));
//        write(board.lastMove as String);

        // Wait for confirmation:
        val response = read();
        when (response.split("=")[0]) {
            PACKET_TYPES.APPROVE_SETUP_TURN.name -> {
                println("Our move got approved!");
                this.board = PACKET_TYPES.APPROVE_SETUP_TURN.approveSetupFromString(response);
                this.gui.updateBoard(this.board);
            }
            PACKET_TYPES.DENY_SETUP_TURN.name -> {
                println("Oh no, we failed according to the server");
            }
        }

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