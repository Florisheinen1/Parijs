package server

import protocol.PACKET_TYPES
import java.net.ServerSocket
import kotlin.concurrent.thread

class Server(private val port: Int) {
    private val serverSocket = ServerSocket(port);

    fun run() {
        println("Started server on: {}".format(this.port));
        while (true) {
            println("Waiting for 2 players...");

            val player1 = ClientHandler(this.serverSocket.accept());
            player1.write(PACKET_TYPES.WELCOME_IN_LOBBY.name);
            println(" - Player 1 joined");

            val player2 = ClientHandler(this.serverSocket.accept());
            player2.write(PACKET_TYPES.WELCOME_IN_LOBBY.name);
            println(" - Player 2 joined");
            println("The game can start!");

            thread { Game(player1, player2).run(); };
        }


    }
}