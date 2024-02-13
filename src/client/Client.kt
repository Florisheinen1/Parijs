package client

import protocol.PACKET_TYPES
import java.net.Socket
import java.nio.charset.Charset
import java.util.Scanner

class Client(val serverSocket: Socket) {
    val scanner = Scanner(this.serverSocket.getInputStream());
    val writer = this.serverSocket.getOutputStream();

    fun run() {
        // First, wait for welcome lobby message
        if (this.read() == PACKET_TYPES.WELCOME_IN_LOBBY.name) {
            println("You joined the lobby!!!!")
        } else {
            println("Error: Expected different packet");
        }

        // Then, wait for game start message
        if (this.read() == PACKET_TYPES.GAME_STARTED.name) {
            println("The game has started!");
        } else {
            println("Error: Expected different packet2");
        }

        var i = 0;

        while (true) {
            // Read turn message
            val turnMsg = this.read();
            println("[%d]".format(i));
            i++;

            when (turnMsg) {
                PACKET_TYPES.GIVE_SETUP_TURN.name -> {
                    println("We received a SETUP turn request. Enter your response:");
                    val response = readln();
                    println("Response: '%s'".format(response));
                    this.write(response)
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