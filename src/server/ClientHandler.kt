package server

import game.*
import game.BoardPiece.*
import protocol.*;
import java.net.Socket;
import java.nio.charset.Charset
import java.util.*

class ClientHandler(socket: Socket) : Player {
    private val scanner = Scanner(socket.getInputStream());
    private val writer = socket.getOutputStream();

    private val packetListeners = Vector<PacketListener>();

    fun run() {
        println("Client handler thread started!");
        var running = true; // TODO: Handle closure with player forfeit

        while (running) {
            val packet = this.readPacket();
            if (packet == null) {
                println("Client closed the socket. Stopping client handler");
                running = false;
            } else {
                synchronized(this.packetListeners) {
                    for (packetListener in this.packetListeners) {
                        packetListener.onIncomingMessage(packet);
                    }
                }
            }
        }
        println("Stopped client handler");
    }

    fun sendPacket(packet: Packet) {
        println("Sending: %s".format(packet.serialize()));
        val data = (packet.serialize() + "\n").toByteArray(Charset.defaultCharset());
        this.writer.write(data);
    }

    private fun readPacket(): Packet? {
        try {
            val data = this.scanner.nextLine();
            return Parser().parsePacket(data);
        } catch (e: NoSuchElementException) {
            return null;
        }
    }

    private fun addPacketListener(listener: PacketListener) {
        synchronized(this.packetListeners) {
            this.packetListeners.add(listener);
        }
    }
    private fun removePacketListener(listener: PacketListener) {
        synchronized(this.packetListeners) {
            this.packetListeners.removeElement(listener);
        }
    }

    // ==== Player specific functions ==== //

    override fun startPhase1(cards: List<CardType>) {
        sendPacket(Packet.StartedPhase1(cards));
    }
    override fun startPhase2() {
        this.sendPacket(Packet.StartedPhase2);
    }

    override fun askTurnPhase1(availableBuildings: List<BuildingName>, topTileBlock: TileBlock?): UserMove {
        var receivedMove: UserMove? = null;

        val listener = object : PacketListener() {
            override fun onIncomingMessage(packet: Packet) {
                if (packet is Packet.ReplyWithMove) {
                    receivedMove = packet.move;
                }
            }
        }

        // Add the packet listener so we start listening
        this.addPacketListener(listener)
        // Send the request for a move
        sendPacket(Packet.AskForMovePhase1(availableBuildings, topTileBlock))
        // Wait for the incoming move
        while (receivedMove == null) Thread.sleep(100);
        // And stop listening for these messages again
        this.removePacketListener(listener);

        return receivedMove!!;
    }

    override fun askTurnPhase2(): UserMove {
        var receivedMove: UserMove? = null;

        val listener = object : PacketListener() {
            override fun onIncomingMessage(packet: Packet) {
                if (packet is Packet.ReplyWithMove) {
                    receivedMove = packet.move;
                }
            }
        }

        // Add the packet listener, so we start listening to moves from the user
        this.addPacketListener(listener);
        // Send the request for a move to the user
        this.sendPacket(Packet.AskForMovePhase2);
        // Wait for the user to have replied
        while (receivedMove == null) Thread.sleep(100);
        // After the user has replied, remove the listener again
        this.removePacketListener(listener);

        return receivedMove!!;
    }

    override fun respondToMove(response: MoveResponse) {
        sendPacket(Packet.RespondToMove(response));
    }

    override fun updateMove(move: UserMove) {
        this.sendPacket(Packet.UpdateWithMove(move));
    }

    override fun declareWinner(isWinner: Boolean) {
        TODO("Not yet implemented")
    }
}

abstract class PacketListener {
    abstract fun onIncomingMessage(packet: Packet);
}