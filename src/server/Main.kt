package server

import java.net.ServerSocket;
import kotlin.concurrent.thread;

import server.Server;

const val SERVER_PORT: Int = 39939;

fun main() {
    Server(SERVER_PORT).run();
}

