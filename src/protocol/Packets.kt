package protocol

import java.nio.charset.Charset

enum class PACKET_TYPES {
    WELCOME_IN_LOBBY,
    GAME_STARTED,
    GIVE_SETUP_TURN,
    GIVE_PLAY_TURN,
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