package client

import java.awt.event.WindowEvent
import java.net.ConnectException
import java.net.InetAddress
import java.net.Socket;
import java.util.NoSuchElementException
import kotlin.concurrent.thread

fun main() {
    // First the dialog part
    val dialog = StartDialog();
    thread { dialog.run() };
    while (!dialog.submitted) {
        // wait
        Thread.sleep(100);
    }
    dialog.dispose();
    println("Server Address: %s:%d".format(dialog.serverAddress, dialog.serverPort));


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
        gui.dispatchEvent(WindowEvent(gui, WindowEvent.WINDOW_CLOSING));
        println("The server closed the connection");
    }
}