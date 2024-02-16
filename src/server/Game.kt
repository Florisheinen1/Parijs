package server

import game.*
import protocol.PACKET_TYPES
import java.util.*

class Game(private val player1: ClientHandler, private val player2: ClientHandler) {
    var turn = PlayerColor.BLUE;

    val board = Board();

    var lastToPlaceBlock = PlayerColor.BLUE;

    fun run() {
        println("Started the game!");
        board.initializePartZero();

        val bluePlayer = getClientHandler(PlayerColor.BLUE);
        bluePlayer.write(PACKET_TYPES.GAME_STARTED.gameStartedToString(board, PlayerColor.BLUE));

        val orangePlayer = getClientHandler(PlayerColor.ORANGE);
        orangePlayer.write(PACKET_TYPES.GAME_STARTED.gameStartedToString(board, PlayerColor.ORANGE));

        runPart1();

        turn = lastToPlaceBlock;
        flipTurn();
        println("Starting the second round with: %s".format(turn.name));

        bluePlayer.write(PACKET_TYPES.SETUP_FINISHED.setupFinishedToString(board, PlayerColor.BLUE));
        orangePlayer.write(PACKET_TYPES.SETUP_FINISHED.setupFinishedToString(board, PlayerColor.ORANGE));

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
                val buildingName = BuildingName.valueOf(responseSplit[1]);
                board.pickBuilding(buildingName, turn);
                println("Player %s picked building: %s".format(turn, buildingName.name));
            }
            "PLACED_BLOCK" -> {
                println("This player %s placed the block".format(turn))
                val pos = Vec2(
                    responseSplit[1].toInt(),
                    responseSplit[2].toInt()
                );
                val block = if (turn == PlayerColor.BLUE) board.topBlueBlock else board.topOrangeBlock;
                board.placeTileBlock(block!!, pos, turn);
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
            PlayerColor.BLUE -> player1
            PlayerColor.ORANGE -> player2
        }
    }

    private fun flipTurn() {
        this.turn = when (this.turn) {
            PlayerColor.BLUE -> PlayerColor.ORANGE
            PlayerColor.ORANGE -> PlayerColor.BLUE
        }
        println("Changed turn to: %s".format(turn.name));
    }
}