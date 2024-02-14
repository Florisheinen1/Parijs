package server

import game.*
import protocol.PACKET_TYPES
import java.net.ServerSocket;
import kotlin.concurrent.thread;

import server.Server;
import java.util.Vector

const val SERVER_PORT: Int = 39939;

fun main() {

    Server(SERVER_PORT).run();
}

