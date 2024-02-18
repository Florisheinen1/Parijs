package client

import game.Player
import protocol.Packet
import protocol.Parser
import java.net.Socket
import java.nio.charset.Charset
import java.util.*
import kotlin.concurrent.thread

class ServerHandler(socket: Socket, private val player: Player) {
    private val scanner = Scanner(socket.getInputStream());
    private val writer = socket.getOutputStream();

    private var isRunning = false;

    fun start() {
        thread { this.run() };
    }

    private fun run() {
        this.isRunning = true;

        println("Started server handler");
        while (this.isRunning) this.handleIncomingPacket(readPacket());
        println("Stopped server handler")
    }

    fun stop() {
        this.isRunning = false;
    }

    fun sendPacket(packet: Packet) {
        val data = (packet.toString() + "\n").toByteArray(Charset.defaultCharset());
        this.writer.write(data);
    }

    private fun readPacket(): Packet {
        println("Waiting for incoming message...");
        val data = this.scanner.nextLine();
        return Parser().parsePacket(data);
    }

    private fun handleIncomingPacket(packet: Packet) {
        when (packet) {
            Packet.WelcomeToLobby -> println("Entered lobby!");
            is Packet.Part1Started -> this.player.startPart1(packet.selectedCards);
            is Packet.AskForMovePart1 -> this.player.askTurnPart1(packet.availableBuildings, packet.topTileBlock);
            is Packet.ReplyWithMovePart1 -> println("Received weird message from server: '%s'".format(packet.toString()));
            is Packet.RespondToMovePart1 -> this.player.respondToMove(packet.moveResponse);
            is Packet.UpdateWithMovePart1 -> this.player.updateMovePart1(packet.move);
        }
    }
}