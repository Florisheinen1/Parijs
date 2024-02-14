package protocol

import game.Block
import game.Cards
import game.PlacableName
import game.Tile
import java.nio.charset.Charset
import java.util.*

enum class PACKET_TYPES {
    WELCOME_IN_LOBBY,
    GAME_STARTED,
    GIVE_SETUP_TURN,
    GIVE_PLAY_TURN,
    ;

    fun gameStartToString(selectedCards: List<Cards>): String {
        return GAME_STARTED.name + ":" + "[%s,%s,%s,%s,%s,%s,%s,%s]".format(
                selectedCards[0],
                selectedCards[1],
                selectedCards[2],
                selectedCards[3],
                selectedCards[4],
                selectedCards[5],
                selectedCards[6],
                selectedCards[7],
        );
    }
    fun gameStartFromString(s: String): Vector<Cards> {
        val cards = Vector<Cards>();
        val without_s = s.removePrefix(GAME_STARTED.name + ":");
        val cardStrings = without_s.take(without_s.length-1).drop(1).split(",");
        for (cardString in cardStrings) {
            cards.add(Cards.valueOf(cardString));
        }
        return cards;
    }

    fun setupTurnToString(unpickedBuildings: List<PlacableName>, openBlock: Block): String {
        val buildings = placeNamesToString(unpickedBuildings);
        val block = blockToString(openBlock);

        return GIVE_SETUP_TURN.name + ":" + buildings + ":" + block
    }

    fun setupTurnFromString(s: String): Pair<Vector<PlacableName>, Block> {
        println("Parsing turn: %s".format(s));
        val spl = s.split(":");
        val names = placeNamesFromString(spl[1]);
        val block = blockFromString(spl[2]);

        return Pair(names, block);
    }

    fun blockToString(b: Block): String {
        return "[%s,%s,%s,%s]".format(b.topLeft, b.topRight, b.bottomLeft, b.bottomRight);
    }

    fun blockFromString(s: String): Block {
        println("Parsing: %s".format(s));
        val vals = s.take(s.length - 1).drop(1).split(",");
        return Block(
                Tile.valueOf(vals[0]),
                Tile.valueOf(vals[1]),
                Tile.valueOf(vals[2]),
                Tile.valueOf(vals[3]),
        );
    }

    fun placeNamesToString(placeNames: List<PlacableName>): String {
        var placesString = "[";
        for (placeName in placeNames) {
            placesString += ",";
            placesString += placeName.name
        }
        placesString += "]";
        return placesString;
    }
    fun placeNamesFromString(s: String): Vector<PlacableName> {
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
//    private fun blockToString()
}

val SEPARATOR: Char = ':';














interface Packet {
    fun toBytes(): ByteArray;
    fun fromLine(line: String): Packet;
}

//class WelcomeInLobby: Packet {
//    val ID: String = "WELCOME_IN_LOBBY";
//    override fun toBytes(): ByteArray {
//        return this.ID.toByteArray(Charset.defaultCharset());
//    }
//    override fun fromLine(line: String): WelcomeInLobby {
//        if (!line.equals(this.ID)) {
//
//        }
//    }
//}

//class ChatPacket(val message: String) : Packet {
//    override val PREFIX = "CHAT";
//
//    override fun toBytes(): ByteArray {
//        val data = this.PREFIX + SEPARATOR + message;
//        return data.toByteArray(Charset.defaultCharset());
//    }
//}