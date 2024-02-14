package server

import game.*
import protocol.PACKET_TYPES
import java.util.*

class Game(private val player1: ClientHandler, private val player2: ClientHandler) {
    var turn = PlayerColor.PLAYER_BLUE;

    val board = Board();

    var lastToPlaceBlock = PlayerColor.PLAYER_BLUE;

    fun run() {
        println("Started the game!");
        board.initializePartZero();

        val bluePlayer = getClientHandler(PlayerColor.PLAYER_BLUE);
        bluePlayer.write(PACKET_TYPES.GAME_STARTED.gameStartedToString(board, PlayerColor.PLAYER_BLUE));

        val orangePlayer = getClientHandler(PlayerColor.PLAYER_ORANGE);
        orangePlayer.write(PACKET_TYPES.GAME_STARTED.gameStartedToString(board, PlayerColor.PLAYER_ORANGE));

        runPart1();

        turn = lastToPlaceBlock;
        flipTurn();
        println("Starting the second round with: %s".format(turn.name));

        runPart2();
        println("This game is over!");
    }

    private fun runPart1() {
        while (!this.board.unplacedBlueBlocks.isEmpty() || !this.board.unplacedOrangeBlocks.isEmpty()) {
            handlePart1Turn();

            this.flipTurn();
        }
    }



    private fun handlePart1Turn() {
        val player = getClientHandler(turn);
        player.write(PACKET_TYPES.GIVE_SETUP_TURN.giveSetupToString(board, turn));

        var failure = false;
        val responseSplit = player.read().split(":");
        when (responseSplit[0]) {
            "PICKED_BUILDING" -> {
                val buildingName = PlacableName.valueOf(responseSplit[1]);
                board.pickBuilding(buildingName, turn);
                println("Player %s picked building: %s".format(turn, buildingName.name));
            }
            "PLACED_BLOCK" -> {
                println("This player %s placed the block".format(turn))
                val pos = Vec2(
                    responseSplit[1].toInt(),
                    responseSplit[2].toInt()
                );
                val block = if (turn == PlayerColor.PLAYER_BLUE) board.topBlueBlock else board.topOrangeBlock;
                board.placeBlock(block!!, pos, turn);
                lastToPlaceBlock = turn;
            }
            "PASS" -> {
                println("Player %s passed its turn".format(turn));
            }
            else -> {
                println("No fucking clue what this player did");
                failure = true;
            }
        }
        if (!failure) {
            player.write(PACKET_TYPES.APPROVE_SETUP_TURN.approveSetupToString(board, turn));
        } else {
            player.write(PACKET_TYPES.DENY_SETUP_TURN.name);
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
        println("Changed turn to: %s".format(turn.name));
    }
}