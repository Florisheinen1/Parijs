package client

import java.net.ConnectException
import java.net.InetAddress
import java.net.Socket;
import java.util.NoSuchElementException
import java.util.Scanner

fun main() {
    println("Hello from client! blabla");

    val serverAddress = InetAddress.getLocalHost();
    val serverPort = 39939;

    try {
        val serverSocket = Socket(serverAddress, serverPort);

        Client(serverSocket).run();

    } catch (e: ConnectException) {
        println("Could not connect to server. Is server running?");
    } catch (e: NoSuchElementException) {
        println("The server closed the connection");
    }
}