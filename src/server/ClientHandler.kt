package server

import game.*
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

    override fun startPart1(cards: List<CardType>) {
        sendPacket(Packet.Part1Started(cards));
    }

    override fun askTurnPart1(availableBuildings: List<BuildingName>, topTileBlock: TileBlock?): MovePart1 {
        var receivedMove: MovePart1? = null;

        val listener = object : PacketListener() {
            override fun onIncomingMessage(packet: Packet) {
                if (packet is Packet.ReplyWithMovePart1) {
                    receivedMove = packet.move;
                }
            }
        }

        // Add the packet listener so we start listening
        this.addPacketListener(listener)
        // Send the request for a move
        sendPacket(Packet.AskForMovePart1(availableBuildings, topTileBlock))
        // Wait for the incoming move
        while (receivedMove == null) Thread.sleep(100);
        // And stop listening for these messages again
        this.removePacketListener(listener);

        return receivedMove!!;
    }

    override fun respondToMove(response: MoveResponse) {
        sendPacket(Packet.RespondToMovePart1(response));
    }

    override fun updateMovePart1(move: MovePart1) {
        sendPacket(Packet.UpdateWithMovePart1(move));
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
}

abstract class PacketListener {
    abstract fun onIncomingMessage(packet: Packet);
}