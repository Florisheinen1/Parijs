package game

import java.security.InvalidParameterException
import java.util.Vector
import kotlin.math.abs

open class Board {
    var SIZE = 8;

    var ingameCards = Vector<Cards>();

    var unplacedBlueBlocks = Vector<TileBlock>();
    var unplacedOrangeBlocks = Vector<TileBlock>();
    var topOrangeBlock: TileBlock? = null;
    var topBlueBlock: TileBlock? = null;

    var tiles = Array<Tile>(SIZE*SIZE) {Tile.BRICKS};

    var unpickedBuildings = Vector(BuildingName.entries);
    var blueInventoryBuildings = Vector<BuildingName>();
    var orangeInventoryBuildings = Vector<BuildingName>();

    fun placeTileBlock(block: TileBlock, pos: Vec2, player: PlayerColor) {
        if (
                pos.x < 0 || pos.x >= SIZE ||
                pos.y < 0 || pos.y >= SIZE ||
                pos.x % 2 != 0 ||
                pos.y % 2 != 0
        ) {
            throw InvalidParameterException("Invalid args for PlaceBlock");
        } else {
            this.setTile(block.topLeft, pos.x, pos.y);
            this.setTile(block.topRight, pos.x+1, pos.y);
            this.setTile(block.bottomLeft, pos.x, pos.y+1);
            this.setTile(block.bottomRight, pos.x+1, pos.y+1);
        }

        when (player) {
            PlayerColor.ORANGE -> {
                unplacedOrangeBlocks.removeElement(topOrangeBlock);
                topOrangeBlock = if (unplacedOrangeBlocks.isEmpty()) null else  unplacedOrangeBlocks[0];
            }
            PlayerColor.BLUE -> {
                unplacedBlueBlocks.removeElement(topBlueBlock)
                topBlueBlock = if (unplacedBlueBlocks.isEmpty()) null else unplacedBlueBlocks[0];
            }
        }
    }

    fun pickBuilding(buildingName: BuildingName, playerColor: PlayerColor) {
        this.unpickedBuildings.remove(buildingName);

        val targetInventory = when (playerColor) {
            PlayerColor.ORANGE -> this.orangeInventoryBuildings
            PlayerColor.BLUE -> this.blueInventoryBuildings
        };
        targetInventory.addElement(buildingName);
    }

    fun getTile(x: Int, y: Int): Tile {
        val index = y * SIZE + x;
        return tiles[index];
    }
    private fun setTile(tile: Tile, x: Int, y: Int) {
        val index = y * SIZE + x;
        tiles[index] = tile;
    }
}

enum class Cards {
    LEVITATION, // Switch building
    METROPOLITAN, // Cover street lantern
    JARDIN_DES_PLANTES, // Normal building
    SACRE_COEUR, // No point reduction of unplaced buildings
    LE_PEINTRE, // The painter
    CHARTIER, // Mixed color
    BOUQUINISTES_SUR_LA_SEINE, // Expansion
    LAMPADAIRE, // Lantern
    MOULIN_ROUGE, // Dancer
    FONTAINE_DES_MERS, // Decoration
    LE_PENSEUR, // Le penseur
    LA_GRANDE_LUMIERE, // Big lantern
}

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

class Building(
        val name: BuildingName,
        direction: Direction,
        parts: Array<Vec2>,
        val owner: PlayerColor? = null) : Placeable(direction, parts) {

    companion object {
        fun fromName(name: BuildingName): Building {
            return when (name) {
                BuildingName.SMILE -> Building(
                        name,
                        Direction.NORTH,
                        arrayOf(
                                Vec2(0, 0),
                                Vec2(0, 1),
                                Vec2(1, 0),
                                Vec2(2, 0),
                                Vec2(3, 0),
                                Vec2(3, 1)
                        )
                )
                BuildingName.BIG_L -> Building(
                        name,
                        Direction.NORTH,
                        arrayOf(
                                Vec2(0, 0),
                                Vec2(0, 1),
                                Vec2(0, 2),
                                Vec2(0, 3),
                                Vec2(1, 0),
                                Vec2(2, 0),
                        )
                )
                BuildingName.SMALL_L -> Building(
                        name,
                        Direction.NORTH,
                        arrayOf(
                                Vec2(0, 0),
                                Vec2(1, 0),
                                Vec2(1, 1),
                                Vec2(1, 2),
                        )
                )
                BuildingName.BIG_T -> Building(
                        name,
                        Direction.NORTH,
                        arrayOf(
                                Vec2(1, 0),
                                Vec2(1, 1),
                                Vec2(1, 2),
                                Vec2(0, 2),
                                Vec2(2, 2),
                        )
                )
                BuildingName.SMALL_T -> Building(
                        name,
                        Direction.NORTH,
                        arrayOf(
                                Vec2(0, 0),
                                Vec2(0, 1),
                                Vec2(0, 2),
                                Vec2(1, 1),
                        )
                )
                BuildingName.CORNER -> Building(
                        name,
                        Direction.NORTH,
                        arrayOf(
                                Vec2(1, 1),
                                Vec2(1, 0),
                                Vec2(0, 1),
                        )
                )
                BuildingName.SQUARE -> Building(
                        name,
                        Direction.NORTH,
                        arrayOf(
                                Vec2(0, 0),
                                Vec2(1, 0),
                                Vec2(0, 1),
                                Vec2(1, 1),
                        )
                )
                BuildingName.STAIRS -> Building(
                        name,
                        Direction.NORTH,
                        arrayOf(
                                Vec2(0, 2),
                                Vec2(1, 2),
                                Vec2(1, 1),
                                Vec2(2, 1),
                                Vec2(2, 0),
                        )
                )
                BuildingName.ZIGZAG -> Building(
                        name,
                        Direction.NORTH,
                        arrayOf(
                                Vec2(0, 0),
                                Vec2(0, 1),
                                Vec2(1, 1),
                                Vec2(1, 2),
                        )
                )
                BuildingName.CROSS -> Building(
                        name,
                        Direction.NORTH,
                        arrayOf(
                                Vec2(1, 0),
                                Vec2(1, 1),
                                Vec2(0, 1),
                                Vec2(1, 2),
                                Vec2(2, 1),
                        )
                )
                BuildingName.LINE -> Building(
                        name,
                        Direction.NORTH,
                        arrayOf(
                                Vec2(0, 0),
                                Vec2(1, 0),
                                Vec2(2, 0),
                        )
                )
                BuildingName.CHONK -> Building(
                        name,
                        Direction.NORTH,
                        arrayOf(
                                Vec2(0, 1),
                                Vec2(1, 0),
                                Vec2(1, 1),
                                Vec2(2, 0),
                                Vec2(2, 1),
                        )
                )
            }
        }
    }
}

// A square block that consists of 4 tiles
class TileBlock(
        direction: Direction,
        var topLeft: Tile,
        var topRight: Tile,
        var bottomLeft: Tile,
        var bottomRight: Tile) : Placeable(direction, arrayOf(
            Vec2(0, 0),
            Vec2(1, 0),
            Vec2(0, 1),
            Vec2(1, 1),
        )) {
    override fun rotate(clockwise: Boolean) {
        val temporaryTile = if (clockwise) this.topRight else this.bottomLeft;

        this.topLeft = if (clockwise) this.bottomLeft else topRight;
        this.topRight = if (clockwise) this.topLeft else bottomRight;
        this.bottomLeft = if (clockwise) this.bottomRight else topLeft;
        this.bottomRight = temporaryTile;

        super.rotate(clockwise);
    }
    fun invert() {
        this.topLeft = this.topLeft.invert();
        this.topRight = this.topRight.invert();
        this.bottomLeft = this.bottomLeft.invert();
        this.bottomRight = this.bottomRight.invert();
    }

    fun copy(): TileBlock {
        return TileBlock(
            this.direction,
            this.topLeft,
            this.topRight,
            this.bottomLeft,
            this.bottomRight
        );
    }
}

// Class to resemble anything that can be placed on the board
abstract class Placeable(var direction: Direction = Direction.NORTH, var parts: Array<Vec2>) {
    protected open fun rotate(clockwise: Boolean) {
        this.direction = this.direction.rotate(clockwise);
        for (part in this.parts) {
            part.rotate(clockwise);
        }
    }

    fun getDimension(): Vec2 {
        var minX = this.parts[0].x;
        var minY = this.parts[0].y;
        var maxX = minX;
        var maxY = minY;
        for (part in this.parts) {
            if (part.x < minX) minX = part.x;
            if (part.x > maxX) maxX = part.x;
            if (part.y < minY) minY = part.y;
            if (part.y > maxY) maxY = part.y;
        }
        return Vec2(
            abs(maxX - minX) + 1,
            abs(maxY - minY) + 1,
        )
    }
}


