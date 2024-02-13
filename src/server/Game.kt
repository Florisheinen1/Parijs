package server

import protocol.PACKET_TYPES
import java.util.*

class Game(private val player1: ClientHandler, private val player2: ClientHandler) {
    var turn = 1;

    fun run() {
        println("Started the game!");
        player1.write(PACKET_TYPES.GAME_STARTED.name);
        player2.write(PACKET_TYPES.GAME_STARTED.name);

        runPart1();
        runPart2();
        println("This game is over!");
    }

    private fun runPart1() {

        var isSettingUp = true;


        var i = 0;

        while (isSettingUp) {
            val playerWithTurn = if (this.turn == 1) this.player1 else this.player2;

            println("Giving player turn...");
            playerWithTurn.write(PACKET_TYPES.GIVE_SETUP_TURN.name);

            println("Waiting for player response...");
            val response = playerWithTurn.read()
            println("Received player response: '%s'".format(response));

            if (i > 2) {
                isSettingUp = false;
            }

            i++;

            this.flipTurn();
        }
    }

    private fun runPart2() {

    }

    private fun flipTurn() {
        if (turn == 1) {
            this.turn = 2;
        } else {
            this.turn = 1;
        }
    }
}