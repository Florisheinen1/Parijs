package client

import game.BoardPiece
import game.BuildingName
import game.Direction
import game.Vec2

fun main() {
    val ui = Gui();

    Client(ui).run();
}