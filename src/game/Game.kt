package game

import java.util.*
import game.BoardPiece.*

class Game(val player1: Player, val player2: Player) {
    val board: Board = Board();

    init {
        this.board.inGameCards = this.selectCardsForGame();

        this.board.unpickedBuildings = Vector<BuildingName>(BuildingName.entries);
        this.board.unplacedBlueBlocks = this.selectBlockListsForGame(PlayerColor.BLUE);
        this.board.unplacedOrangeBlocks = this.selectBlockListsForGame(PlayerColor.ORANGE);
    }

    private fun selectCardsForGame(): Vector<Card> {
        val allCardTypes = CardType.entries;
        val selectedCardTypes = allCardTypes.shuffled().take(8);
        val selectedCards = selectedCardTypes.map { Card(it, CardState.UNPICKED_AND_UNUSED) }
        return Vector(selectedCards);
    }

    private fun selectBlockListsForGame(player: PlayerColor): Vector<TileBlock?> {
        val blocks = Vector<TileBlock?>();
        for (block in GameConstants.ALL_BLOCKS_OF_BLUE) {
            when (player) {
                PlayerColor.BLUE -> blocks.add(block.copy())
                PlayerColor.ORANGE -> {
                    val copy = block.copy();
                    copy.invert();
                    blocks.add(copy)
                }
            }
        }
        return Vector(blocks.shuffled());
    }

    fun run() {
        val nextPlayerWithTurn = this.doPart1();
        this.doPart2(nextPlayerWithTurn);
    }

    // Executes part 1 of the game. Returns which player has the next turn.
    private fun doPart1(): PlayerColor {
        // State that the first part has started
        val selectedCardTypes = this.board.inGameCards.map { it.type };
        this.player1.startPhase1(selectedCardTypes);
        this.player2.startPhase1(selectedCardTypes);

        // Pick a player that starts
        var playerColorWithTurn = this.pickRandomPlayerColor();

        // Remember who placed the last TileBlock
        var lastPlayerColorThatPlacedTileBlock = PlayerColor.BLUE;

        while (!this.partOneIsFinished()) {
            // Get a move that is allowed according to the rules
            val validMove = this.getValidMovePart1(playerColorWithTurn);

            // If the player placed a block, remember this event
            if (validMove is UserMove.PlaceBlockAt) lastPlayerColorThatPlacedTileBlock = playerColorWithTurn;

            // Execute the move
            this.handleMovePart1(validMove, playerColorWithTurn);

            // Update the other player
            val otherColor = playerColorWithTurn.getInverted();
            val otherPlayer = getPlayerOfColor(otherColor);
            otherPlayer.updateMove(if (otherColor == PlayerColor.ORANGE) validMove.getInverted() else validMove);

            // And flip the turns
            playerColorWithTurn = playerColorWithTurn.getInverted();
        }

        return lastPlayerColorThatPlacedTileBlock.getInverted();
    }

    private fun doPart2(playerColor: PlayerColor) {
        println("Part 2 starts with player: " + playerColor.name);
        this.player1.startPhase2();
        this.player2.startPhase2();
    }

    // Inverts colors when necessary. Returns non-inverted
    private fun getValidMovePart1(playerColor: PlayerColor): UserMove {
        val playerWithTurn = this.getPlayerOfColor(playerColor);

        val openTileBlock: TileBlock? = when (playerColor) {
            PlayerColor.BLUE -> if (this.board.unplacedBlueBlocks.isEmpty()) null else this.board.unplacedBlueBlocks[0];
            PlayerColor.ORANGE -> if (this.board.unplacedOrangeBlocks.isEmpty()) null else this.board.unplacedOrangeBlocks[0];
        }

        while (true) {
            // Ask the player for a move
            val move = playerWithTurn.askTurnPhase1(
                this.board.unpickedBuildings,
                if (playerColor == PlayerColor.ORANGE) openTileBlock?.getInverted() else openTileBlock
            );
            // Invert move back if it is from orange
            if (playerColor == PlayerColor.ORANGE) move.invert();

            // Check if this move is valid
            val moveResponse = this.isMoveAllowed(move);

            // And send the response back to the player
            playerWithTurn.respondToMove(moveResponse);

            if (moveResponse is MoveResponse.Accept) return move;
        }
    }

    private fun isMoveAllowed(move: UserMove): MoveResponse {
        return MoveResponse.Accept; // TODO: Implement this
    }

    private fun handleMovePart1(move: UserMove, playerColor: PlayerColor) {
        when (move) {
            is UserMove.Pass -> println("User passed");
            is UserMove.PickBuilding -> {
                this.board.unpickedBuildings.removeElement(move.buildingName);

                val inventory = if (playerColor == PlayerColor.BLUE) this.board.blueInventoryBuildings else this.board.orangeInventoryBuildings;
                inventory.add(move.buildingName);
            }
            is UserMove.PlaceBlockAt -> {
                val targetList = when (playerColor) {
                    PlayerColor.BLUE -> this.board.unplacedBlueBlocks
                    PlayerColor.ORANGE -> this.board.unplacedOrangeBlocks
                };

                this.board.placeTileBlock(move.position, targetList[0]!!);
                targetList.removeAt(0);
            }
            else -> {
                throw Exception("Handling phase 2 move while in phase 1");
            }
        }
    }

    private fun partOneIsFinished(): Boolean {
        return this.board.unplacedBlueBlocks.isEmpty() && this.board.unplacedOrangeBlocks.isEmpty();
    }

    private fun pickRandomPlayerColor(): PlayerColor {
        return PlayerColor.entries.shuffled().first();
    }

    private fun getPlayerOfColor(playerColor: PlayerColor): Player {
        return when (playerColor) {
            PlayerColor.BLUE -> this.player1
            PlayerColor.ORANGE -> this.player2
        };
    }
}

object GameConstants {
    val ALL_BLOCKS_OF_BLUE: Array<TileBlock> = arrayOf(
            TileBlock(Direction.NORTH, Tile.ORANGE, Tile.BLUE, Tile.SHARED, Tile.BLUE),
            TileBlock(Direction.NORTH, Tile.SHARED, Tile.BLUE, Tile.BLUE, Tile.ORANGE),
            TileBlock(Direction.NORTH, Tile.BLUE, Tile.LANTERN, Tile.BLUE, Tile.ORANGE),
            TileBlock(Direction.NORTH, Tile.LANTERN, Tile.BLUE, Tile.ORANGE, Tile.SHARED),
            TileBlock(Direction.NORTH, Tile.SHARED, Tile.BLUE, Tile.ORANGE, Tile.BLUE),
            TileBlock(Direction.NORTH, Tile.BLUE, Tile.LANTERN, Tile.ORANGE, Tile.BLUE),
            TileBlock(Direction.NORTH, Tile.LANTERN, Tile.BLUE, Tile.BLUE, Tile.SHARED),
            TileBlock(Direction.NORTH, Tile.BLUE, Tile.BLUE, Tile.BLUE, Tile.ORANGE)
    )
}