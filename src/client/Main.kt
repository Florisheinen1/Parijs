package client

import java.net.ConnectException
import java.net.InetAddress
import java.net.Socket;
import java.util.NoSuchElementException
import kotlin.concurrent.thread

fun main() {
    println("Hello from client! blabla");

    val gui = Gui();

    thread { gui.run() };


    val serverAddress = InetAddress.getLocalHost();
    val serverPort = 39939;

    try {
        val serverSocket = Socket(serverAddress, serverPort);

        Client(serverSocket, gui).run();

    } catch (e: ConnectException) {
        println("Could not connect to server. Is server running?");
    } catch (e: NoSuchElementException) {
        println("The server closed the connection");
    }
}