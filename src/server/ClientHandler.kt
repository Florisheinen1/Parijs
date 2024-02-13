package server

import java.net.Socket;
import java.nio.charset.Charset
import java.util.Scanner;

class ClientHandler(private val socket: Socket) {
    private val scanner = Scanner(socket.getInputStream());
    private val writer = socket.getOutputStream();

    fun run() {
        println("Client thread started!!!!");
        var running = true;
        while (running) {
            running = false;
        }
        println("Stopped client");
    }

    fun write(message: String) {
        val packet = (message + "\n").toByteArray(Charset.defaultCharset());
        this.writer.write(packet);
//        this.writer.flush();
    }

    fun read(): String {
        println("Reading incoming message...");
        return this.scanner.nextLine();
    }
}