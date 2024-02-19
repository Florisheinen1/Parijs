package protocol

import game.*
import java.text.ParseException

sealed class Packet {
    companion object {
        const val DELIMITER = ":";
        const val LIST_PREFIX = "[";
        const val LIST_SUFFIX = "]";
        const val LIST_SEPARATOR = ",";
    }

    enum class PACKET_HEADERS {
        WELCOME_TO_LOBBY,
        STARTED_PART_1,
        ASK_FOR_MOVE_PART_1,
        REPLY_WITH_MOVE_PART_1,
        RESPOND_TO_MOVE_PART_1,
        UPDATE_WITH_MOVE_PART_1
    }

    data object WelcomeToLobby : Packet();
    data class Part1Started(val selectedCards: List<Cards>) : Packet();
    data class AskForMovePart1(val availableBuildings: List<BuildingName>, val topTileBlock: TileBlock?) : Packet();
    data class ReplyWithMovePart1(val move: MovePart1) : Packet();
    data class RespondToMovePart1(val moveResponse: MoveResponse) : Packet();
    data class UpdateWithMovePart1(val move: MovePart1) : Packet();

    fun serialize(): String {
        when (this) {
            is WelcomeToLobby -> {
                return PACKET_HEADERS.WELCOME_TO_LOBBY.name;
            }
            is Part1Started -> {
                val cardsString = enumListToString(selectedCards);
                return PACKET_HEADERS.STARTED_PART_1.name + DELIMITER + cardsString;
            }
            is AskForMovePart1 -> {
                val buildingsString = enumListToString(this.availableBuildings);
                val tileBlockString = tileBlockToString(this.topTileBlock);
                return PACKET_HEADERS.ASK_FOR_MOVE_PART_1.name + DELIMITER + buildingsString + DELIMITER + tileBlockString;
            };
            is ReplyWithMovePart1 -> {
                return PACKET_HEADERS.REPLY_WITH_MOVE_PART_1.name + DELIMITER + movePart1ToString(this.move);
            }
            is RespondToMovePart1 -> {
                return PACKET_HEADERS.RESPOND_TO_MOVE_PART_1.name + DELIMITER + moveResponseToString(this.moveResponse);
            }
            is UpdateWithMovePart1 -> {
                return PACKET_HEADERS.UPDATE_WITH_MOVE_PART_1.name + DELIMITER + movePart1ToString(this.move);
            }
        }
    }

    private fun<T> enumListToString(l: List<T>): String {
        return l.joinToString(
                prefix = Packet.LIST_PREFIX,
                separator = Packet.LIST_SEPARATOR,
                postfix = Packet.LIST_SUFFIX,
                transform = { it.toString() }
        );
    }

    private fun tileBlockToString(b: TileBlock?): String {
        if (b == null) return "";
        return enumListToString(listOf(b.topLeft, b.topRight, b.bottomLeft, b.bottomRight));
    }

    private fun moveResponseToString(moveResponse: MoveResponse): String {
        return when (moveResponse) {
            MoveResponse.Accept -> "ACCEPT"
            is MoveResponse.Deny -> "DENY" + Packet.DELIMITER + sanitize(moveResponse.reason)
        }
    }

    private fun movePart1ToString(move: MovePart1): String {
        return when (move) {
            MovePart1.Pass -> "PASS"
            is MovePart1.PickBuilding -> "PICK" + Packet.DELIMITER + move.buildingName.name;
            is MovePart1.PlaceBlockAt -> "PLACE" + Packet.DELIMITER + vecToString(move.position);
        }
    }

    private fun vecToString(v: Vec2): String {
        return "(%d,%d)".format(v.x, v.y);
    }
}

fun sanitize(s: String): String {
    return s.replace(Packet.DELIMITER, "", false);
}

class Parser {
    fun parsePacket(s: String): Packet {
        println("Received packet: '%s'".format(s));
        val delimited = s.split(Packet.DELIMITER);
        return when (Packet.PACKET_HEADERS.valueOf(delimited[0])) {
            Packet.PACKET_HEADERS.WELCOME_TO_LOBBY -> Packet.WelcomeToLobby;
            Packet.PACKET_HEADERS.STARTED_PART_1 -> {
                val cardsString = s.removePrefix(Packet.PACKET_HEADERS.STARTED_PART_1.name + Packet.DELIMITER);
                val cards = parseList(cardsString).map { Cards.valueOf(it) }
                Packet.Part1Started(cards);
            }
            Packet.PACKET_HEADERS.ASK_FOR_MOVE_PART_1 -> {
                val parsedBuildingsList = parseList(delimited[1]);
                val buildings = if (parsedBuildingsList[0].isEmpty()) emptyList() else parsedBuildingsList.map { BuildingName.valueOf(it) }
                val tileBlock = tileBlockFromString(delimited[2]);
                return Packet.AskForMovePart1(buildings, tileBlock);
            }
            Packet.PACKET_HEADERS.REPLY_WITH_MOVE_PART_1 -> {
                val move = movePart1FromString(s.removePrefix(Packet.PACKET_HEADERS.REPLY_WITH_MOVE_PART_1.name + Packet.DELIMITER));
                return Packet.ReplyWithMovePart1(move);
            }
            Packet.PACKET_HEADERS.RESPOND_TO_MOVE_PART_1 -> {
                val moveResponse = moveResponseFromString(delimited[1]);
                return Packet.RespondToMovePart1(moveResponse);
            }
            Packet.PACKET_HEADERS.UPDATE_WITH_MOVE_PART_1 -> {
                val move = movePart1FromString(s.removePrefix(Packet.PACKET_HEADERS.UPDATE_WITH_MOVE_PART_1.name + Packet.DELIMITER));
                return Packet.UpdateWithMovePart1(move)
            }
        }
    }

    private fun parseList(s: String): List<String> {
        return s.take(s.length - 1).drop(1).split(Packet.LIST_SEPARATOR);
    }
    private fun moveResponseFromString(s: String): MoveResponse {
        val delimited = s.split(Packet.DELIMITER);
        return when (delimited[0]) {
            "ACCEPT" -> MoveResponse.Accept
            "DENY" -> MoveResponse.Deny(delimited[1])
            else -> throw ParseException("Failed to parse: '%s'".format(s), 0)
        };
    }

    private fun movePart1FromString(s: String): MovePart1 {
        val delimited = s.split(Packet.DELIMITER);
        return when (delimited[0]) {
            "PASS" -> MovePart1.Pass
            "PICK" -> MovePart1.PickBuilding(BuildingName.valueOf(delimited[1]))
            "PLACE" -> MovePart1.PlaceBlockAt(vecFromString(delimited[1]))
            else -> throw ParseException("Failed to parse: '%s'.".format(s), 0);
        }
    }

    private fun vecFromString(s: String): Vec2 {
        val points = s.take(s.length-1).drop(1).split(",").map { it.toInt() };
        return Vec2(points[0], points[1]);
    }

    private fun tileBlockFromString(s: String): TileBlock? {
        if (s.isEmpty()) return null;
        val tiles = parseList(s).map { Tile.valueOf(it) };
        return TileBlock(Direction.NORTH, tiles[0], tiles[1], tiles[2], tiles[3]);
    }
}

















//enum class PACKET_TYPES {
//    WELCOME_IN_LOBBY,
//    GAME_STARTED,
//    GIVE_SETUP_TURN,
//    APPROVE_SETUP_TURN,
//    DENY_SETUP_TURN,
//    SETUP_FINISHED,
//    ;
//
//    fun
//
//    fun setupFinishedToString(board: Board, playerColor: PlayerColor): String {
//        return SETUP_FINISHED.name + "=" + boardToString(board, playerColor);
//    }
//    fun setupFinishedFromString(s: String): Board {
//        return boardFromString(s.removePrefix(SETUP_FINISHED.name + "="));
//    }
//
//    fun gameStartedToString(board: Board, playerColor: PlayerColor): String {
//        return GAME_STARTED.name + "=" + boardToString(board, playerColor);
//    }
//    fun gameStartedFromString(s: String): Board {
//        return boardFromString(s.removePrefix(GAME_STARTED.name + "="));
//    }
//
//    fun giveSetupToString(board: Board, playerColor: PlayerColor): String {
//        return GIVE_SETUP_TURN.name + "=" + boardToString(board, playerColor);
//    }
//    fun giveSetupFromString(s: String): Board {
//        return boardFromString(s.removePrefix(GIVE_SETUP_TURN.name + "="));
//    }
//
//    fun approveSetupToString(board: Board, playerColor: PlayerColor): String {
//        return APPROVE_SETUP_TURN.name + "=" + boardToString(board, playerColor);
//    }
//    fun approveSetupFromString(s: String): Board {
//        return boardFromString(s.removePrefix(APPROVE_SETUP_TURN.name + "="));
//    }
//
//    // ================ HELPERS ==================== //
//
//    private fun boardToString(board: Board, playerColor: PlayerColor): String {
////        var unpickedBuildings = placeNamesToString(board.getNames(board.unpickedBuildings));
////        var bluePickedBuildings = placeNamesToString(board.getNames(board.blueInventoryBuildings));
////        var orangePickedBuildings = placeNamesToString(board.getNames(board.orangeInventoryBuildings));
////
////        if (playerColor == PlayerColor.PLAYER_ORANGE) {
////            val tmp = bluePickedBuildings;
////            bluePickedBuildings = orangePickedBuildings;
////            orangePickedBuildings = tmp;
////        }
////
////        var cards = cardsToString(board.selectedCardsForGame);
////        var pickedBlock = if (playerColor==PlayerColor.PLAYER_BLUE) board.topBlueBlock else board.topOrangeBlock;
////        var pickedBlockString: String = "";
////        if (pickedBlock != null) {
////            pickedBlockString = blockToString(if (playerColor == PlayerColor.PLAYER_ORANGE) pickedBlock.getInverted() else pickedBlock);
////        }
////
////        var tiles = tilesToString(board.tiles.toList(), playerColor);
////
////        return arrayOf(
////                unpickedBuildings,
////                bluePickedBuildings,
////                orangePickedBuildings,
////                cards,
////                pickedBlockString,
////                tiles,
////        ).joinToString(separator = ":");
//        return "";
//    }
//
//    private fun boardFromString(s: String): Board {
////        val board = Board()
////
////        val spl = s.split(":");
////        board.unpickedBuildings = board.getPlacablesFromNames(placeNamesFromString(spl[0]));
////        board.blueInventoryBuildings = board.getPlacablesFromNames(placeNamesFromString(spl[1]));
////        board.orangeInventoryBuildings = board.getPlacablesFromNames(placeNamesFromString(spl[2]));
////        board.selectedCardsForGame = cardsFromString(spl[3]);
////        board.topBlueBlock = blockFromString(spl[4]);
////
////        val tiles = tilesFromString(spl[5]);
////
////        for (i in 0..63) {
////            board.tiles[i] = tiles[i];
////        }
////
////        return board;
//        return Board();
//    }

//    private fun tilesToString(tiles: List<Tile>, playerColor: PlayerColor): String {
//        val shouldInvert = playerColor == PlayerColor.PLAYER_ORANGE;
//        val correctTileStrings = Vector<String>();
//
//        for (tile in tiles) {
//            if (shouldInvert) {
//                correctTileStrings.add(tile.getInverted().name);
//            } else {
//                correctTileStrings.add(tile.name);
//            }
//        }
//        return correctTileStrings.joinToString(prefix = "[", separator = ",", postfix = "]");
//    }
//    private fun tilesFromString(s: String): Vector<Tile> {
//        val tileStrings = s.take(s.length-1).drop(1).split(",");
//        val tiles = Vector<Tile>();
//        for (tileString in tileStrings) {
//            tiles.add(Tile.valueOf(tileString));
//        }
//        return tiles;
//    }
//
//    private fun cardsToString(cards: List<Cards>): String {
//        return cards.joinToString(prefix = "[", separator = ",", postfix = "]");
//    }
//    private fun cardsFromString(s: String): Vector<Cards> {
//        val trim = s.take(s.length-1).drop(1).split(",");
//        val cards = Vector<Cards>();
//        for (cardStr in trim) {
//            cards.add(Cards.valueOf(cardStr));
//        }
//        return cards;
//    }
//
//    fun blockToString(b: TileBlock?): String {
//        return if (b == null) {
//            ""
//        } else {
//            "[%s,%s,%s,%s]".format(b.topLeft, b.topRight, b.bottomLeft, b.bottomRight);
//        }
//    }
//
//    fun blockFromString(s: String): TileBlock? {
//        if (s.isEmpty()) return null;
//        val vals = s.take(s.length - 1).drop(1).split(",");
//        return TileBlock(
//                Tile.valueOf(vals[0]),
//                Tile.valueOf(vals[1]),
//                Tile.valueOf(vals[2]),
//                Tile.valueOf(vals[3]),
//        );
//    }
//
//    fun BuildingNamesFromString(buildings: List<BuildingName>): String {
//        return ""
//    }
//    fun <T> listToString(list: List<T>): String {
//        return list.joinToString(",", "[", "]", transform = { it.toString() });
//    }
