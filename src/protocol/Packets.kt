package protocol

import game.*
import java.nio.charset.Charset
import java.util.*

enum class PACKET_TYPES {
    WELCOME_IN_LOBBY,
    GAME_STARTED,
    GIVE_SETUP_TURN,
    APPROVE_SETUP_TURN,
    DENY_SETUP_TURN,
    SETUP_FINISHED,
    ;

    fun setupFinishedToString(board: Board, playerColor: PlayerColor): String {
        return SETUP_FINISHED.name + "=" + boardToString(board, playerColor);
    }
    fun setupFinishedFromString(s: String): Board {
        return boardFromString(s.removePrefix(SETUP_FINISHED.name + "="));
    }

    fun gameStartedToString(board: Board, playerColor: PlayerColor): String {
        return GAME_STARTED.name + "=" + boardToString(board, playerColor);
    }
    fun gameStartedFromString(s: String): Board {
        return boardFromString(s.removePrefix(GAME_STARTED.name + "="));
    }

    fun giveSetupToString(board: Board, playerColor: PlayerColor): String {
        return GIVE_SETUP_TURN.name + "=" + boardToString(board, playerColor);
    }
    fun giveSetupFromString(s: String): Board {
        return boardFromString(s.removePrefix(GIVE_SETUP_TURN.name + "="));
    }

    fun approveSetupToString(board: Board, playerColor: PlayerColor): String {
        return APPROVE_SETUP_TURN.name + "=" + boardToString(board, playerColor);
    }
    fun approveSetupFromString(s: String): Board {
        return boardFromString(s.removePrefix(APPROVE_SETUP_TURN.name + "="));
    }

    // ================ HELPERS ==================== //

    private fun boardToString(board: Board, playerColor: PlayerColor): String {
        var unpickedBuildings = placeNamesToString(board.getNames(board.unpickedBuildings));
        var bluePickedBuildings = placeNamesToString(board.getNames(board.blueInventoryBuildings));
        var orangePickedBuildings = placeNamesToString(board.getNames(board.orangeInventoryBuildings));

        if (playerColor == PlayerColor.PLAYER_ORANGE) {
            val tmp = bluePickedBuildings;
            bluePickedBuildings = orangePickedBuildings;
            orangePickedBuildings = tmp;
        }

        var cards = cardsToString(board.selectedCardsForGame);
        var pickedBlock = if (playerColor==PlayerColor.PLAYER_BLUE) board.topBlueBlock else board.topOrangeBlock;
        var pickedBlockString: String = "";
        if (pickedBlock != null) {
            pickedBlockString = blockToString(if (playerColor == PlayerColor.PLAYER_ORANGE) pickedBlock.getInverted() else pickedBlock);
        }

        var tiles = tilesToString(board.tiles.toList(), playerColor);

        return arrayOf(
                unpickedBuildings,
                bluePickedBuildings,
                orangePickedBuildings,
                cards,
                pickedBlockString,
                tiles,
        ).joinToString(separator = ":");
    }

    private fun boardFromString(s: String): Board {
        val board = Board()

        val spl = s.split(":");
        board.unpickedBuildings = board.getPlacablesFromNames(placeNamesFromString(spl[0]));
        board.blueInventoryBuildings = board.getPlacablesFromNames(placeNamesFromString(spl[1]));
        board.orangeInventoryBuildings = board.getPlacablesFromNames(placeNamesFromString(spl[2]));
        board.selectedCardsForGame = cardsFromString(spl[3]);
        board.topBlueBlock = blockFromString(spl[4]);

        val tiles = tilesFromString(spl[5]);

        for (i in 0..63) {
            board.tiles[i] = tiles[i];
        }

        return board;
    }

    private fun tilesToString(tiles: List<Tile>, playerColor: PlayerColor): String {
        val shouldInvert = playerColor == PlayerColor.PLAYER_ORANGE;
        val correctTileStrings = Vector<String>();

        for (tile in tiles) {
            if (shouldInvert) {
                correctTileStrings.add(tile.getInverted().name);
            } else {
                correctTileStrings.add(tile.name);
            }
        }
        return correctTileStrings.joinToString(prefix = "[", separator = ",", postfix = "]");
    }
    private fun tilesFromString(s: String): Vector<Tile> {
        val tileStrings = s.take(s.length-1).drop(1).split(",");
        val tiles = Vector<Tile>();
        for (tileString in tileStrings) {
            tiles.add(Tile.valueOf(tileString));
        }
        return tiles;
    }

    private fun cardsToString(cards: List<Cards>): String {
        return cards.joinToString(prefix = "[", separator = ",", postfix = "]");
    }
    private fun cardsFromString(s: String): Vector<Cards> {
        val trim = s.take(s.length-1).drop(1).split(",");
        val cards = Vector<Cards>();
        for (cardStr in trim) {
            cards.add(Cards.valueOf(cardStr));
        }
        return cards;
    }

    fun blockToString(b: Block?): String {
        return if (b == null) {
            ""
        } else {
            "[%s,%s,%s,%s]".format(b.topLeft, b.topRight, b.bottomLeft, b.bottomRight);
        }
    }

    fun blockFromString(s: String): Block? {
        if (s.isEmpty()) return null;
        val vals = s.take(s.length - 1).drop(1).split(",");
        return Block(
                Tile.valueOf(vals[0]),
                Tile.valueOf(vals[1]),
                Tile.valueOf(vals[2]),
                Tile.valueOf(vals[3]),
        );
    }

    private fun placeNamesToString(placeNames: List<PlacableName>): String {
        var placesString = "[";
        for (placeName in placeNames) {
            placesString += ",";
            placesString += placeName.name
        }
        placesString += "]";
        return placesString;
    }
    private fun placeNamesFromString(s: String): Vector<PlacableName> {
        val placeNames = Vector<PlacableName>();

        val placeNameStrings = s.take(s.length - 1).drop(2).split(",");
        for (placeNameString in placeNameStrings) {
            if (placeNameString.isEmpty()) {
                continue;
            }
            placeNames.addElement(PlacableName.valueOf(placeNameString));
        }

        return placeNames;
    }
}
