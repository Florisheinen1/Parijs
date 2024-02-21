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
        val data = (packet.serialize() + "\n").toByteArray(Charset.defaultCharset());
        this.writer.write(data);
    }

    private fun readPacket(): Packet {
        val data = this.scanner.nextLine();
        return Parser().parsePacket(data);
    }

    private fun handleIncomingPacket(packet: Packet) {
        when (packet) {
            Packet.WelcomeToLobby -> println("Entered lobby!");
            is Packet.StartedPhase1 -> this.player.startPhase1(packet.selectedCards);
            is Packet.StartedPhase2 -> this.player.startPhase2();
            is Packet.AskForMovePhase1 -> {
                val move = this.player.askTurnPhase1(packet.availableBuildings, packet.topTileBlock);
                this.sendPacket(Packet.ReplyWithMove(move));
            }
            is Packet.AskForMovePhase2 -> {
                val move = this.player.askTurnPhase2();
                this.sendPacket(Packet.ReplyWithMove(move));
            }
            is Packet.ReplyWithMove -> println("Received weird message from server: '%s'".format(packet.toString()));
            is Packet.RespondToMove -> this.player.respondToMove(packet.moveResponse);
            is Packet.UpdateWithMove -> this.player.updateMove(packet.move);
        }
    }
}