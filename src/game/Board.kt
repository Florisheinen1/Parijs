package game

import protocol.PACKET_TYPES
import java.security.InvalidParameterException
import java.util.Vector
import java.util.*;

class Board {
    var SIZE = 8;

    val gamePhase = GamePhase.PART_1;

    // ============= Part 0: Pick the Cards that we will use in the game ======================== //
    var selectedCardsForGame = Vector<Cards>();

    var unplacedBlueBlocks = Vector<Block>();
    var unplacedOrangeBlocks = Vector<Block>();
    var topOrangeBlock: Block? = null;
    var topBlueBlock: Block? = null;

    fun selectCardsForGame(): Vector<Cards> {
        val cards = Vector<Cards>();
        cards.addAll(Cards.entries.shuffled().take(8));
        return cards;
    }

    fun selectBlockListsForGame(player: PlayerColor): Vector<Block> {
        val blocks = Vector<Block>();
        for (block in ALL_BLOCKS_OF_BLUE) {
            when (player) {
                PlayerColor.PLAYER_BLUE -> blocks.add(block)
                PlayerColor.PLAYER_ORANGE -> blocks.add(block.getInverted())
            }
        }
        return Vector(blocks.shuffled());
    }

    fun initializePartZero() { // Meant for the server
        unplacedBlueBlocks = selectBlockListsForGame(PlayerColor.PLAYER_BLUE);
        unplacedOrangeBlocks = selectBlockListsForGame(PlayerColor.PLAYER_ORANGE);

        selectedCardsForGame = selectCardsForGame();

        topBlueBlock = unplacedBlueBlocks.firstElement();
        topOrangeBlock = unplacedOrangeBlocks.firstElement();
    }
    fun setupPartZero(topBlock: Block, cards: Vector<Cards>) {
        selectedCardsForGame = cards;
        topBlueBlock = topBlock;
    }

    // ============= Part 1: Handle part one ======================== //
    var tiles: Array<Tile> = Array<Tile>(SIZE*SIZE) {Tile.BRICKS};

    var lastMove: String? = null;
    var unpickedBuildings = this.getAllBuildings();
    var blueInventoryBuildings = Vector<Placable>();
    var orangeInventoryBuildings = Vector<Placable>();

    fun placeBlock(block: Block, pos: Vec2, player: PlayerColor) {
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
            PlayerColor.PLAYER_ORANGE -> {
                unplacedOrangeBlocks.removeElement(topOrangeBlock);
                topOrangeBlock = if (unplacedOrangeBlocks.isEmpty()) null else  unplacedOrangeBlocks[0];
            }
            PlayerColor.PLAYER_BLUE -> {
                unplacedBlueBlocks.removeElement(topBlueBlock)
                topBlueBlock = if (unplacedBlueBlocks.isEmpty()) null else unplacedBlueBlocks[0];
            }
        }

        lastMove = "PLACED_BLOCK:" + pos.x + ":" + pos.y;
    }

    fun pickBuilding(buildingName: PlacableName, playerColor: PlayerColor) {
        var target: Placable? = null;
        for (building in unpickedBuildings) {
            if (building.name == buildingName) {
                target = building;
                break;
            }
        }
        if (target == null) {
            throw Exception("Building was not found!");
        }
        unpickedBuildings.removeElement(target);
        val targetInventory = when (playerColor) {
            PlayerColor.PLAYER_ORANGE -> this.orangeInventoryBuildings
            PlayerColor.PLAYER_BLUE -> this.blueInventoryBuildings
        };
        targetInventory.addElement(target);
        lastMove = "PICKED_BUILDING:%s".format(buildingName.name);
    }

    fun passTurn() {
        lastMove = "PASS";
    }

    fun updateUnpickedBuildings(names: Vector<PlacableName>) {
        this.unpickedBuildings.clear();

        for (building in ALL_PLACABLES) {
            if (names.contains(building.name)) {
                unpickedBuildings.add(building);
            }
        }
    }

    fun getNames(placables: List<Placable>): Vector<PlacableName> {
        val names = Vector<PlacableName>();
        for (placable in placables) {
            names.add(placable.name);
        }
        return names;
    }
    fun getPlacablesFromNames(names: List<PlacableName>): Vector<Placable> {
        val placables = Vector<Placable>();
        for (placable in ALL_PLACABLES) {
            if (names.contains(placable.name)) {
                placables.add(placable);
            }
        }
        return placables;
    }





    fun getTile(x: Int, y: Int): Tile {
        val index = y * SIZE + x;
        return tiles[index];
    }
    private fun setTile(tile: Tile, x: Int, y: Int) {
        val index = y * SIZE + x;
        tiles[index] = tile;
    }


    private fun selectObjectsForGame() {
        for (obj in ALL_PLACABLES) {
            val buildings = arrayOf(
                    PlacableName.SMILE,
                    PlacableName.BIG_L,
                    PlacableName.SMALL_L,
                    PlacableName.BIG_T,
                    PlacableName.SMALL_T,
                    PlacableName.CORNER,
                    PlacableName.SQUARE,
                    PlacableName.STAIRS,
            )
        }
    }

    private fun getAllBuildings(): Vector<Placable> {
        val buildings = Vector<Placable>();
        for (placable in ALL_PLACABLES) {
            if (ALL_BUILDING_NAMES.contains(placable.name)) {
                buildings.addElement(placable);
            }
        }
        return buildings;
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

enum class GamePhase {
    PART_1,
    PART_2,
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

enum class Tile {
    BLUE,
    ORANGE,
    SHARED,
    LANTERN,
    BRICKS;

    fun getInverted(): Tile {
        return when (this) {
            BLUE -> ORANGE
            ORANGE -> BLUE
            else -> this
        }
    }
}

class Block(
    val topLeft: Tile,
    val topRight: Tile,
    val bottomLeft: Tile,
    val bottomRight: Tile,
) {
    fun getInverted(): Block {
        return Block(
            topLeft.getInverted(),
            topRight.getInverted(),
            bottomLeft.getInverted(),
            bottomRight.getInverted()
        );
    }
};

enum class Direction {
    NORTH,
    EAST,
    SOUTH,
    WEST;

    fun rotate(clockwise: Boolean) {
        when (this) {
            NORTH -> if (clockwise) EAST else WEST;
            EAST -> if (clockwise) SOUTH else NORTH;
            SOUTH -> if (clockwise) WEST else EAST;
            WEST -> if (clockwise) NORTH else SOUTH;
        }
    }
}

enum class PlayerColor {
    PLAYER_BLUE,
    PLAYER_ORANGE,
}

enum class PlacableName {
    // Buildings
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

    // Special pieces
    PAINTER,
    DANCER,
//    MIXED_FIELD,
    FOUNTAIN,
    PENSEUR,
    LANTERN,
    BIG_LANTERN,
    GARDEN,
    EXPANSION
}

class Placable(
        val name: PlacableName,
        val parts: Vector<Vec2>,
        val direction: Direction,
        val owner: PlayerColor?
) {
    fun rotate(clockwise: Boolean) {
        for (i in 0..parts.size) {
            parts[i].rotate(clockwise);
        }

        var minX: Int? = null;
        var minY: Int? = null;
        for (pos in parts) {
            if (minX == null || pos.x < minX) {
                minX = pos.x;
            }
            if (minY == null || pos.y < minY) {
                minY = pos.y;
            }
        }

        val distanceToOrigin = Vec2(-minX!!, -minY!!);
        this.move(distanceToOrigin);
        this.direction.rotate(clockwise);
    }

    fun move(dist: Vec2) {
        for (pos in this.parts) {
            pos.x += pos.x;
            pos.y += pos.y;
        }
    }

    fun deepClone(): Placable {
        val copyParts = Vector<Vec2>();
        for (part in parts) {
            copyParts.addElement(Vec2(part.x, part.y));
        }
        val cloned = Placable(
                name,
                copyParts,
                direction,
                owner);
        return cloned;
    }
}

fun <T> toVec(arr: Array<T>): Vector<T> {
    val vec = Vector<T>();
    vec.addAll(arr.asList());
    return vec;
}

val ALL_BUILDING_NAMES: Array<PlacableName> = arrayOf(
        PlacableName.SMILE,
        PlacableName.BIG_L,
        PlacableName.SMALL_L,
        PlacableName.BIG_T,
        PlacableName.SMALL_T,
        PlacableName.CORNER,
        PlacableName.SQUARE,
        PlacableName.STAIRS,
        PlacableName.ZIGZAG,
        PlacableName.CROSS,
        PlacableName.LINE,
        PlacableName.CHONK,
)

val ALL_BLOCKS_OF_BLUE: Array<Block> = arrayOf(
        Block(Tile.ORANGE, Tile.BLUE, Tile.SHARED, Tile.BLUE),
        Block(Tile.SHARED, Tile.BLUE, Tile.BLUE, Tile.ORANGE),
        Block(Tile.BLUE, Tile.LANTERN, Tile.BLUE, Tile.ORANGE),
        Block(Tile.LANTERN, Tile.BLUE, Tile.ORANGE, Tile.SHARED),
        Block(Tile.SHARED, Tile.BLUE, Tile.ORANGE, Tile.BLUE),
        Block(Tile.BLUE, Tile.LANTERN, Tile.ORANGE, Tile.BLUE),
        Block(Tile.LANTERN, Tile.BLUE, Tile.BLUE, Tile.SHARED),
        Block(Tile.BLUE, Tile.BLUE, Tile.BLUE, Tile.ORANGE)
)

val ALL_PLACABLES: Array<Placable> = arrayOf(
    Placable(PlacableName.SMILE, toVec(arrayOf(
            Vec2(0, 0),
            Vec2(0, 1),
            Vec2(1, 0),
            Vec2(2, 0),
            Vec2(3, 0),
            Vec2(3, 1)
    )), Direction.NORTH, null),

    Placable(PlacableName.BIG_L, toVec(arrayOf(
            Vec2(0, 0),
            Vec2(0, 1),
            Vec2(0, 2),
            Vec2(0, 3),
            Vec2(1, 0),
            Vec2(2, 0),
    )), Direction.NORTH, null),

    Placable(PlacableName.SMALL_L, toVec(arrayOf(
            Vec2(0, 0),
            Vec2(1, 0),
            Vec2(1, 1),
            Vec2(1, 2),
    )), Direction.NORTH, null),

    Placable(PlacableName.BIG_T, toVec(arrayOf(
            Vec2(1, 0),
            Vec2(1, 1),
            Vec2(1, 2),
            Vec2(0, 2),
            Vec2(2, 2),
    )), Direction.NORTH, null),

    Placable(PlacableName.SMALL_T, toVec(arrayOf(
            Vec2(0, 0),
            Vec2(0, 1),
            Vec2(0, 2),
            Vec2(1, 1),
    )), Direction.NORTH, null),

    Placable(PlacableName.CORNER, toVec(arrayOf(
            Vec2(1, 1),
            Vec2(1, 0),
            Vec2(0, 1),
    )), Direction.NORTH, null),

    Placable(PlacableName.SQUARE, toVec(arrayOf(
            Vec2(0, 0),
            Vec2(1, 0),
            Vec2(0, 1),
            Vec2(1, 1),
    )), Direction.NORTH, null),

    Placable(PlacableName.STAIRS, toVec(arrayOf(
            Vec2(0, 2),
            Vec2(1, 2),
            Vec2(1, 1),
            Vec2(2, 1),
            Vec2(2, 0),
    )), Direction.NORTH, null),

    Placable(PlacableName.ZIGZAG, toVec(arrayOf(
            Vec2(0, 0),
            Vec2(0, 1),
            Vec2(1, 1),
            Vec2(1, 2),
    )), Direction.NORTH, null),

    Placable(PlacableName.CROSS, toVec(arrayOf(
            Vec2(1, 0),
            Vec2(1, 1),
            Vec2(0, 1),
            Vec2(1, 2),
            Vec2(2, 1),
    )), Direction.NORTH, null),

    Placable(PlacableName.LINE, toVec(arrayOf(
            Vec2(0, 0),
            Vec2(1, 0),
            Vec2(2, 0),
    )), Direction.NORTH, null),

    Placable(PlacableName.CHONK, toVec(arrayOf(
            Vec2(0, 1),
            Vec2(1, 0),
            Vec2(1, 1),
            Vec2(2, 0),
            Vec2(2, 1),
    )), Direction.NORTH, null),

    Placable(PlacableName.PAINTER, toVec(arrayOf(
            Vec2(0, 0),
    )), Direction.NORTH, null),

    Placable(PlacableName.DANCER, toVec(arrayOf(
            Vec2(0, 0),
    )), Direction.NORTH, null),

    Placable(PlacableName.FOUNTAIN, toVec(arrayOf(
            Vec2(0, 0),
            Vec2(1, 0),
            Vec2(1, 1),
            Vec2(1, 2),
    )), Direction.NORTH, null),

    Placable(PlacableName.PENSEUR, toVec(arrayOf(
            Vec2(0, 0),
    )), Direction.NORTH, null),

    Placable(PlacableName.LANTERN, toVec(arrayOf(
            Vec2(0, 0),
    )), Direction.NORTH, null),

    Placable(PlacableName.BIG_LANTERN, toVec(arrayOf(
            Vec2(0, 0),
    )), Direction.NORTH, null),

    Placable(PlacableName.GARDEN, toVec(arrayOf(
            Vec2(0, 0),
            Vec2(1, 0),
    )), Direction.NORTH, null),

    Placable(PlacableName.EXPANSION, toVec(arrayOf(
            Vec2(0, 0),
    )), Direction.NORTH, null),
);