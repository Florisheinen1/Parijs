package server

import game.Game
import protocol.Packet
import java.net.ServerSocket
import kotlin.concurrent.thread

class Server(private val port: Int) {
    private val serverSocket = ServerSocket(port);

    fun run() {
        println("Started server on: {}".format(this.port));
        while (true) {
            println("Waiting for 2 players...");

            val player1 = ClientHandler(this.serverSocket.accept());
            thread { player1.run() };
            player1.sendPacket(Packet.WelcomeToLobby);
            println(" - Player 1 joined");

            val player2 = ClientHandler(this.serverSocket.accept());
            thread { player2.run() };
            player2.sendPacket(Packet.WelcomeToLobby);
            println(" - Player 2 joined");
            println("The game can start!");

            thread { Game(player1, player2).run(); };
        }
    }
}