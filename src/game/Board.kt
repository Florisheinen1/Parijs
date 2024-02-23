package game

import java.security.InvalidParameterException
import java.util.Vector
import kotlin.math.abs

open class Board {
    var SIZE = 8; // In tiles, both width and height

    var inGameCards = Vector<Card>();

    var unplacedBlueBlocks = Vector<BoardPiece.TileBlock?>(); // nulls mean that we do not know what tile it is
    var unplacedOrangeBlocks = Vector<BoardPiece.TileBlock?>();

    var tiles = Array(SIZE*SIZE) {Tile.BRICKS};
    val placedTopPieces = Vector<BoardPiece.Top>();

    var unpickedBuildings = Vector<BuildingName>();
    var blueInventoryBuildings = Vector<BuildingName>();
    var orangeInventoryBuildings = Vector<BuildingName>();

    fun placeTileBlock(tilePos: Vec2, tileBlock: BoardPiece.TileBlock) {
        if (
                tilePos.x < 0 || tilePos.x >= SIZE ||
                tilePos.y < 0 || tilePos.y >= SIZE ||
                tilePos.x % 2 != 0 ||
                tilePos.y % 2 != 0
        ) {
            // TODO: Deny user move if this went wrong
            throw InvalidParameterException("Invalid args for PlaceBlock");
        }

        this.setTile(tileBlock.topLeft, tilePos.x, tilePos.y);
        this.setTile(tileBlock.topRight, tilePos.x+1, tilePos.y);
        this.setTile(tileBlock.bottomLeft, tilePos.x, tilePos.y+1);
        this.setTile(tileBlock.bottomRight, tilePos.x+1, tilePos.y+1);
    }

    fun getTile(x: Int, y: Int): Tile {
        val index = y * SIZE + x;
        return tiles[index];
    }
    private fun setTile(tile: Tile, x: Int, y: Int) {
        val index = y * SIZE + x;
        tiles[index] = tile;
    }

    fun deepClone(): Board {
        val newBoard = Board();

        newBoard.SIZE = this.SIZE;

        for (card in this.inGameCards) {
            newBoard.inGameCards.add(card);
        }

        for (block in this.unplacedBlueBlocks) {
            newBoard.unplacedBlueBlocks.add(block?.copy());
        }
        for (block in this.unplacedOrangeBlocks) {
            newBoard.unplacedOrangeBlocks.add(block?.copy());
        }

        for (i in 0..<this.tiles.size) {
            newBoard.tiles[i] = this.tiles[i];
        }

        for (buildingName in this.unpickedBuildings) {
            newBoard.unpickedBuildings.add(buildingName);
        }
        for (name in this.blueInventoryBuildings) {
            newBoard.blueInventoryBuildings.add(name);
        }
        for (name in this.orangeInventoryBuildings) {
            newBoard.orangeInventoryBuildings.add(name);
        }

        return newBoard;
    }
}

enum class CardType {
    LEVITATION,
    METROPOLITAN,
    JARDIN_DES_PLANTES,
    SACRE_COEUR,
    LE_PEINTRE,
    CHARTIER,
    BOUQUINISTES_SUR_LA_SEINE,
    LAMPADAIRE,
    MOULIN_ROUGE,
    FONTAINE_DES_MERS,
    LE_PENSEUR,
    LA_GRANDE_LUMIERE,
}
enum class CardState {
    UNPICKED_AND_UNUSED,
    PICKED_BUT_UNUSED,
    PICKED_AND_USED
}

data class Card(
        val type: CardType,
        var state: CardState = CardState.UNPICKED_AND_UNUSED,
        var owner: PlayerColor? = null);

enum class PlayerColor {
    BLUE,
    ORANGE;

    fun getInverted(): PlayerColor {
        return if (this == BLUE) ORANGE else BLUE;
    }
}

enum class Direction {
    NORTH,
    EAST,
    SOUTH,
    WEST;

    fun rotate(clockwise: Boolean): Direction {
        return when (this) {
            NORTH -> if (clockwise) EAST else WEST;
            EAST -> if (clockwise) SOUTH else NORTH;
            SOUTH -> if (clockwise) WEST else EAST;
            WEST -> if (clockwise) NORTH else SOUTH;
        }
    }
}

class Vec2(var x: Int, var y: Int) {
    fun rotate(clockwise: Boolean) {
        if (clockwise) {
            val newX = this.y;
            val newY = -this.x;
            this.x = newX;
            this.y = newY;
        } else {
            val newX = this.y;
            val newY = -this.x;
            this.x = newX;
            this.y = newY;
        }
    }
    fun clone(): Vec2 = Vec2(x, y);
}

enum class Tile {
    BLUE,
    ORANGE,
    SHARED,
    LANTERN,
    BRICKS;

    fun invert(): Tile {
        return when (this) {
            BLUE -> ORANGE
            ORANGE -> BLUE
            else -> this
        }
    }
}

enum class BuildingName {
    SMILE,
    BIG_L,
    SMALL_L,
    BIG_T,
    SMALL_T,
    CORNER,
    SQUARE,
    STAIRS,
    ZIGZAG,
    CROSS,
    LINE,
    CHONK,
}

sealed class BoardPiece(val rotation: Direction, val parts: List<Vec2>) {

    private fun normalize() {
        val origin = this.getOrigin();
        val offset = Vec2(-origin.x, -origin.y);
        this.move(offset);
    }

    open fun rotate(clockwise: Boolean) {
        for (part in this.parts) {
            part.rotate(clockwise);
        }
        this.normalize();
        this.rotation.rotate(clockwise);
    }

    private fun move(offset: Vec2) {
        for (part in this.parts) {
            part.x += offset.x;
            part.y += offset.y;
        }
    }

    fun getOrigin(): Vec2 {
        var minX = this.parts[0].x;
        var minY = this.parts[0].y;
        for (part in this.parts) {
            if (part.x < minX) minX = part.x;
            if (part.y < minY) minY = part.y;
        }
        return Vec2(minX, minY);
    }

    fun getSize(): Vec2 {
        val origin = this.getOrigin();
        val end = Vec2(origin.x, origin.y);
        for (part in this.parts) {
            if (part.x > end.x) end.x = part.x;
            if (part.y > end.y) end.y = part.y;
        }
        return Vec2(end.x - origin.x + 1, end.y - origin.y + 1);
    }

    data class TileBlock(
        var _rotation: Direction,
        var topLeft: Tile,
        var topRight: Tile,
        var bottomLeft: Tile,
        var bottomRight: Tile) : BoardPiece(_rotation, listOf(
            Vec2(0, 0),
            Vec2(1, 0),
            Vec2(0, 1),
            Vec2(1, 1),
            )
        ) {
        override fun rotate(clockwise: Boolean) {
            val list = Vector(listOf(topLeft, topRight, bottomRight, bottomLeft));

            if (clockwise) {
                list.add(0, list.removeAt(list.size - 1));
            } else {
                list.add(list.removeAt(0));
            }

            this.topLeft = Tile.valueOf(list[0].name);
            this.topRight = Tile.valueOf(list[1].name);
            this.bottomRight = Tile.valueOf(list[2].name);
            this.bottomLeft = Tile.valueOf(list[3].name);

            super.rotate(clockwise);
        }
        fun invert() {
            this.topLeft = this.topLeft.invert();
            this.topRight = this.topRight.invert();
            this.bottomLeft = this.bottomLeft.invert();
            this.bottomRight = this.bottomRight.invert();
        }

        fun clone(): TileBlock {
            return TileBlock(
                    this.rotation,
                    this.topLeft,
                    this.topRight,
                    this.bottomLeft,
                    this.bottomRight
            );
        }

        fun getInverted(): TileBlock {
            val cp = this.copy();
            cp.invert();
            return cp;
        }

        fun isBricks(): Boolean {
            return this.topLeft == Tile.BRICKS && this.topRight == Tile.BRICKS && this.bottomLeft == Tile.BRICKS && this.bottomRight == Tile.BRICKS;
        }

        override fun toString(): String {
            return this.topLeft.name + "," + this.topRight.name + "," + this.bottomRight.name + "," + this.bottomLeft.name
        }
    }

    sealed class Top(rotation: Direction, parts: List<Vec2>) : BoardPiece(rotation, parts) {
        sealed class Decoration(parts: List<Vec2>) : Top(Direction.NORTH, parts) {
            data object Painter : Decoration(listOf(Vec2(0, 0)));
            data object Dancer : Decoration(listOf(Vec2(0, 0)));
            data object Fountain : Decoration(listOf(Vec2(0, 0), Vec2(1, 0)));
            data object Statue : Decoration(listOf(Vec2(0, 0)));
            data object Lantern : Decoration(listOf(Vec2(0, 0)));
            data object BigLantern : Decoration(listOf(Vec2(0, 0)));
            data object Garden : Decoration(listOf(Vec2(0, 0), Vec2(0, 0)));
            data object Extension : Decoration(listOf(Vec2(0, 0)));
        }

        sealed class Building(parts: List<Vec2>) : Top(Direction.NORTH, parts) {
            companion object {
                fun fromName(buildingName: BuildingName, targetRotation: Direction = Direction.NORTH) : Building {
                    val building = when (buildingName) {
                        BuildingName.SMILE -> Smile;
                        BuildingName.BIG_L -> BigL;
                        BuildingName.SMALL_L -> SmallL;
                        BuildingName.BIG_T -> BigT;
                        BuildingName.SMALL_T -> SmallT;
                        BuildingName.CORNER -> Corner;
                        BuildingName.SQUARE -> Square;
                        BuildingName.STAIRS -> Stairs;
                        BuildingName.ZIGZAG -> Zigzag;
                        BuildingName.CROSS -> Cross;
                        BuildingName.LINE -> Line;
                        BuildingName.CHONK -> Chonk;
                    };
                    when (targetRotation) {
                        Direction.NORTH -> {} // Do nothing. Buildings start at rotation to north
                        Direction.EAST -> building.rotate(true);
                        Direction.SOUTH -> {
                            building.rotate(true);
                            building.rotate(true);
                        }
                        Direction.WEST -> building.rotate(false);
                    }
                    return building;
                }
            }

            data object Smile : Building(listOf(Vec2(0, 0), Vec2(0, 1), Vec2(1, 0), Vec2(2, 0), Vec2(3, 0), Vec2(3, 1)));
            data object BigL : Building(listOf(Vec2(0, 0), Vec2(0, 1), Vec2(0, 2), Vec2(0, 3), Vec2(1, 0), Vec2(2, 0)));
            data object SmallL : Building(listOf(Vec2(0, 0), Vec2(1, 0), Vec2(1, 1), Vec2(1, 2)));
            data object BigT : Building(listOf(Vec2(1, 0), Vec2(1, 1), Vec2(1, 2), Vec2(0, 2), Vec2(2, 2)));
            data object SmallT : Building(listOf(Vec2(0, 0), Vec2(0, 1), Vec2(0, 2), Vec2(1, 1)));
            data object Corner : Building(listOf(Vec2(1, 1), Vec2(1, 0), Vec2(0, 1)));
            data object Square : Building(listOf(Vec2(0, 0), Vec2(1, 0), Vec2(0, 1), Vec2(1, 1)));
            data object Stairs : Building(listOf(Vec2(0, 2), Vec2(1, 2), Vec2(1, 1), Vec2(2, 1), Vec2(2, 0)));
            data object Zigzag : Building(listOf(Vec2(0, 0), Vec2(0, 1), Vec2(1, 1), Vec2(1, 2)));
            data object Cross : Building(listOf(Vec2(1, 0), Vec2(1, 1), Vec2(0, 1), Vec2(1, 2), Vec2(2, 1)));
            data object Line : Building(listOf(Vec2(0, 0), Vec2(1, 0), Vec2(2, 0)));
            data object Chonk : Building(listOf(Vec2(0, 1), Vec2(1, 0), Vec2(1, 1), Vec2(2, 0), Vec2(2, 1)));

            fun getName(): BuildingName {
                return when (this) {
                    BigL -> BuildingName.BIG_L
                    BigT -> BuildingName.BIG_T
                    Chonk -> BuildingName.CHONK
                    Corner -> BuildingName.CORNER
                    Cross -> BuildingName.CROSS
                    Line -> BuildingName.LINE
                    SmallL -> BuildingName.SMALL_L
                    SmallT -> BuildingName.SMALL_T
                    Smile -> BuildingName.SMILE
                    Square -> BuildingName.SQUARE
                    Stairs -> BuildingName.STAIRS
                    Zigzag -> BuildingName.ZIGZAG
                }
            }
        }
    }
}

//// A square block that consists of 4 tiles
//// Class to resemble anything that can be placed on the board
//abstract class Placeable(var direction: Direction = Direction.NORTH, var parts: Array<Vec2>) {
//    open fun rotate(clockwise: Boolean) {
//        this.direction = this.direction.rotate(clockwise);
//        for (part in this.parts) {
//            part.rotate(clockwise);
//        }
//    }
//
//    fun getDimension(): Vec2 {
//        var minX = this.parts[0].x;
//        var minY = this.parts[0].y;
//        var maxX = minX;
//        var maxY = minY;
//        for (part in this.parts) {
//            if (part.x < minX) minX = part.x;
//            if (part.x > maxX) maxX = part.x;
//            if (part.y < minY) minY = part.y;
//            if (part.y > maxY) maxY = part.y;
//        }
//        return Vec2(
//            abs(maxX - minX) + 1,
//            abs(maxY - minY) + 1,
//        )
//    }
//}


