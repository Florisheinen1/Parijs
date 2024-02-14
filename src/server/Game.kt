package server

import game.*
import protocol.PACKET_TYPES
import java.util.*

class Game(private val player1: ClientHandler, private val player2: ClientHandler) {
    var turn = PlayerColor.PLAYER_BLUE;

    val board = Board();

    fun run() {
        println("Started the game!");
        board.initializePartZero();

        player1.write(PACKET_TYPES.GAME_STARTED.gameStartToString(board.selectedCardsForGame));
        player2.write(PACKET_TYPES.GAME_STARTED.gameStartToString(board.selectedCardsForGame));

        runPart1();
        runPart2();
        println("This game is over!");
    }

    private fun runPart1() {

        var isSettingUp = true;

        var i = 0;

        while (isSettingUp) {
            handlePart1Turn();

            if (i > 2) {
                isSettingUp = false;
            }

            i++;

            this.flipTurn();
        }
    }

    private fun handlePart1Turn() {
        val player = getClientHandler(turn);
        val topBlock = if (turn == PlayerColor.PLAYER_BLUE) board.topBlueBlock else board.topOrangeBlock;

        val message = PACKET_TYPES.GIVE_SETUP_TURN.setupTurnToString(board.getNames(board.unpickedBuildings), topBlock!!);


        player.write(message);

        val response_split = player.read().split(":");
        when (response_split[0]) {
            "PICKED_BUILDING" -> {
                val buildingName = PlacableName.valueOf(response_split[1]);
                board.pickBuilding(buildingName, turn);
                println("Player %s picked building: %s".format(turn, buildingName.name));
            }
            "PLACED_BLOCK" -> {
                println("This player %s placed the block".format(turn))
            }
            else -> println("No fucking clue what this player did");
        }
    }

    private fun runPart2() {

    }

    private fun getClientHandler(playerColor: PlayerColor): ClientHandler {
        return when (playerColor) {
            PlayerColor.PLAYER_BLUE -> player1
            PlayerColor.PLAYER_ORANGE -> player2
        }
    }

    private fun flipTurn() {
        this.turn = when (this.turn) {
            PlayerColor.PLAYER_BLUE -> PlayerColor.PLAYER_ORANGE
            PlayerColor.PLAYER_ORANGE -> PlayerColor.PLAYER_BLUE
        }
    }
}