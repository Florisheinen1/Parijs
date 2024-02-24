package client

import game.BoardPiece
import game.BuildingName
import game.Direction
import game.Vec2

fun main() {

    test(BuildingName.CORNER, Vec2(0, 0), Direction.NORTH);
    test(BuildingName.CORNER, Vec2(0, 0), Direction.EAST);

    val ui = Gui();

    Client(ui).run();
}

fun test(name: BuildingName, pos: Vec2, rot: Direction) {
    val building = BoardPiece.Top.Building.from(name, pos, rot);
    println("Building '%s' at (%d, %d) facing '%s' has parts: %s".format(building.name, building.getOrigin().x, building.getOrigin().y, building.rotation, building.parts.joinToString { "[%d,%d]".format(it.x, it.y) }));
}