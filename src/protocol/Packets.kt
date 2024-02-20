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
        UPDATE_WITH_MOVE_PART_1,
        STARTED_PART_2,
    }

    data object WelcomeToLobby : Packet();
    data class Part1Started(val selectedCards: List<Cards>) : Packet();
    data class AskForMovePart1(val availableBuildings: List<BuildingName>, val topTileBlock: TileBlock?) : Packet();
    data class ReplyWithMovePart1(val move: MovePart1) : Packet();
    data class RespondToMovePart1(val moveResponse: MoveResponse) : Packet();
    data class UpdateWithMovePart1(val move: MovePart1) : Packet();
    data object Part2Started : Packet();

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
            is Part2Started -> return PACKET_HEADERS.STARTED_PART_2.name;
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
            is MovePart1.PickBuilding -> "PICK" + DELIMITER + move.buildingName.name;
            is MovePart1.PlaceBlockAt -> "PLACE" + DELIMITER + vecToString(move.position) + DELIMITER + tileBlockToString(move.tileBlock);
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
            Packet.PACKET_HEADERS.STARTED_PART_2 -> Packet.Part2Started;
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
            "PLACE" -> MovePart1.PlaceBlockAt(vecFromString(delimited[1]), tileBlockFromString(delimited[2])!!)
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