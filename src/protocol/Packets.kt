package protocol

import game.*
import game.BoardPiece.*
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
        STARTED_PHASE_1,
        STARTED_PHASE_2,
        ASK_FOR_MOVE_PHASE_1,
        ASK_FOR_MOVE_PHASE_2,
        REPLY_WITH_MOVE,
        RESPOND_TO_MOVE,
        UPDATE_WITH_MOVE,
    }

    data object WelcomeToLobby : Packet();
    data class StartedPhase1(val selectedCards: List<CardType>) : Packet();
    data object StartedPhase2 : Packet();
    data class AskForMovePhase1(val availableBuildings: List<BuildingName>, val topTileBlock: TileBlock?) : Packet();
    data object AskForMovePhase2 : Packet();
    data class ReplyWithMove(val move: UserMove) : Packet();
    data class RespondToMove(val moveResponse: MoveResponse) : Packet();
    data class UpdateWithMove(val move: UserMove) : Packet();

    fun serialize(): String {
        when (this) {
            is WelcomeToLobby -> return PACKET_HEADERS.WELCOME_TO_LOBBY.name;
            is StartedPhase1 -> {
                val cardsString = enumListToString(selectedCards);
                return PACKET_HEADERS.STARTED_PHASE_1.name + DELIMITER + cardsString;
            }
            is StartedPhase2 -> return PACKET_HEADERS.STARTED_PHASE_2.name;
            is AskForMovePhase1 -> {
                val buildingsString = enumListToString(this.availableBuildings);
                val tileBlockString = tileBlockToString(this.topTileBlock);
                return PACKET_HEADERS.ASK_FOR_MOVE_PHASE_1.name + DELIMITER + buildingsString + DELIMITER + tileBlockString;
            };
            is AskForMovePhase2 -> {
                return PACKET_HEADERS.ASK_FOR_MOVE_PHASE_2.name;
            }
            is ReplyWithMove -> {
                return PACKET_HEADERS.REPLY_WITH_MOVE.name + DELIMITER + userMoveToString(this.move);
            }
            is RespondToMove -> {
                return PACKET_HEADERS.RESPOND_TO_MOVE.name + DELIMITER + moveResponseToString(this.moveResponse);
            }
            is UpdateWithMove -> {
                return PACKET_HEADERS.UPDATE_WITH_MOVE.name + DELIMITER + userMoveToString(this.move);
            }
        }
    }

    private fun<T> enumListToString(l: List<T>): String {
        return l.joinToString(
            prefix = LIST_PREFIX,
            separator = LIST_SEPARATOR,
            postfix = LIST_SUFFIX,
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

    private fun userMoveToString(move: UserMove): String {
        return when (move) {
            UserMove.Pass -> "PASS"
            is UserMove.PickBuilding -> "PICK_BUILDING" + DELIMITER + move.buildingName.name;
            is UserMove.PlaceBlockAt -> "PLACE_BLOCK" + DELIMITER + vecToString(move.position) + DELIMITER + tileBlockToString(move.tileBlock);

            is UserMove.PlaceBuilding -> "PLACE_BUILDING" + DELIMITER + vecToString(move.position) + DELIMITER + move.buildingName.name;
            is UserMove.CardAction -> TODO();
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
            Packet.PACKET_HEADERS.STARTED_PHASE_1 -> {
                val cardsString = s.removePrefix(Packet.PACKET_HEADERS.STARTED_PHASE_1.name + Packet.DELIMITER);
                val cards = parseList(cardsString).map { CardType.valueOf(it) }
                Packet.StartedPhase1(cards);
            }
            Packet.PACKET_HEADERS.STARTED_PHASE_2 -> Packet.StartedPhase2;
            Packet.PACKET_HEADERS.ASK_FOR_MOVE_PHASE_1 -> {
                val parsedBuildingsList = parseList(delimited[1]);
                val buildings = if (parsedBuildingsList[0].isEmpty()) emptyList() else parsedBuildingsList.map { BuildingName.valueOf(it) }
                val tileBlock = tileBlockFromString(delimited[2]);
                return Packet.AskForMovePhase1(buildings, tileBlock);
            }
            Packet.PACKET_HEADERS.ASK_FOR_MOVE_PHASE_2 -> return Packet.AskForMovePhase2;
            Packet.PACKET_HEADERS.REPLY_WITH_MOVE -> {
                val move = userMoveFromString(s.removePrefix(Packet.PACKET_HEADERS.REPLY_WITH_MOVE.name + Packet.DELIMITER));
                return Packet.ReplyWithMove(move);
            }
            Packet.PACKET_HEADERS.RESPOND_TO_MOVE -> {
                val moveResponse = moveResponseFromString(delimited[1]);
                return Packet.RespondToMove(moveResponse);
            }
            Packet.PACKET_HEADERS.UPDATE_WITH_MOVE -> {
                val move = userMoveFromString(s.removePrefix(Packet.PACKET_HEADERS.UPDATE_WITH_MOVE.name + Packet.DELIMITER));
                return Packet.UpdateWithMove(move)
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

    private fun userMoveFromString(s: String): UserMove {
        val delimited = s.split(Packet.DELIMITER);
        return when (delimited[0]) {
            "PASS" -> UserMove.Pass
            "PICK_BUILDING" -> UserMove.PickBuilding(BuildingName.valueOf(delimited[1]))
            "PLACE_BLOCK" -> UserMove.PlaceBlockAt(vecFromString(delimited[1]), tileBlockFromString(delimited[2])!!)

            "PLACE_BUILDING" -> UserMove.PlaceBuilding(BuildingName.valueOf(delimited[1]), vecFromString(delimited[2]))
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