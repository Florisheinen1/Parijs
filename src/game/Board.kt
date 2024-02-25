package game

import client.GuiEvent
import java.security.InvalidParameterException
import java.util.Vector
import kotlin.math.abs

open class Board {
    var SIZE = 8; // In tiles, both width and height

    var inGameCards = Vector<Card>();

    var unplacedBlueBlocks = Vector<BoardPiece.TileBlock?>(); // nulls mean that we do not know what tile it is
    var unplacedOrangeBlocks = Vector<BoardPiece.TileBlock?>();

    var tiles = Array(SIZE*SIZE) {Tile.BRICKS};
    val placedTopPiecesByBlue = Vector<BoardPiece.Top>();
    val placedTopPiecesByOrange = Vector<BoardPiece.Top>();

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

    private fun containsTilePos(tileX: Int, tileY: Int): Boolean {
        return tileX in 0..<SIZE && tileY in 0..<SIZE;
    }

    fun updateCard(cardType: CardType, owner: PlayerColor, newCardState: CardState) {
        for (card in this.inGameCards) {
            if (card.type == cardType) {
                card.state = newCardState;
                card.owner = owner;
                break;
            }
        }
    }

    fun doesTileBlockFit(tileBlock: BoardPiece.TileBlock): Boolean {
        for (part in tileBlock.parts) {
            // If any part is outside the board, it does not fit
            if (!this.containsTilePos(part.x, part.y)) return false;
            // If any part is on top of an already defined tile block, it does not fit
            if (this.getTile(part.x, part.y) != Tile.BRICKS) return false;
        }
        return true;
    }

    private fun isTopPieceLayerEmptyAt(tileX: Int, tileY: Int): Boolean {
        for (topPiece in this.placedTopPiecesByBlue) {
            for (part in topPiece.parts) {
                if (part.x == tileX && part.y == tileY) return false;
            }
        }
        for (topPiece in this.placedTopPiecesByOrange) {
            for (part in topPiece.parts) {
                if (part.x == tileX && part.y == tileY) return false;
            }
        }
        return true;
    }

    fun doesTopPieceFit(piece: BoardPiece.Top, owner: PlayerColor): Boolean {
        // TODO: Add exception rules, e.g. effects of cards
        when (piece) {
            is BoardPiece.Top.Building -> {
                val acceptedUnderlyingTiles = when (owner) {
                    PlayerColor.BLUE -> listOf(Tile.BLUE, Tile.SHARED);
                    PlayerColor.ORANGE -> listOf(Tile.ORANGE, Tile.SHARED);
                }

                for (part in piece.parts) {
                    // Parts outside board means not fitting
                    if (!this.containsTilePos(part.x, part.y)) return false;

                    // Cannot fit piece here if tile is from opponent's color or lantern e.g.
                    if (!acceptedUnderlyingTiles.contains(this.getTile(part.x, part.y))) return false;

                    // Cannot fit piece if another piece exists here already
                    if (!isTopPieceLayerEmptyAt(part.x, part.y)) return false;
                }
                return true;
            }
            is BoardPiece.Top.Decoration -> {
                val ownTileColor = if (owner == PlayerColor.BLUE) Tile.BLUE else Tile.ORANGE;

                val acceptedUnderlyingTiles = when (piece.name) {
                    DecorationName.GARDEN -> listOf(ownTileColor, Tile.SHARED);
                    DecorationName.PAINTER -> listOf(ownTileColor);
                    DecorationName.EXTENSION -> listOf(ownTileColor);
                    DecorationName.LANTERN -> listOf(ownTileColor);
                    DecorationName.DANCER -> listOf(ownTileColor);
                    DecorationName.FOUNTAIN -> listOf(ownTileColor, Tile.SHARED);
                    DecorationName.STATUE -> listOf(ownTileColor);
                    DecorationName.BIG_LANTERN -> listOf(Tile.LANTERN);
                }

                for (part in piece.parts) {
                    // If part is outside the board, we cant place it
                    if (!this.containsTilePos(part.x, part.y)) return false;

                    val underlyingTile = this.getTile(part.x, part.y);
                    if (!acceptedUnderlyingTiles.contains(underlyingTile)) return false;

                    if (!isTopPieceLayerEmptyAt(part.x, part.y)) return false;
                }

                if (piece.name == DecorationName.EXTENSION) {
                    // Then this piece can only be placed if it is facing a building of the owner
                    val mainPart = piece.parts[0];
                    val facingPosition = mainPart.getAdjacent(piece.rotation);

                    val attachedTopPieces = if (owner == PlayerColor.BLUE) this.placedTopPiecesByBlue else this.placedTopPiecesByOrange;

                    var canExtendBuilding = false;
                    for (otherPiece in attachedTopPieces) {
                        if (otherPiece is BoardPiece.Top.Building) {
                            for (otherPart in otherPiece.parts) {
                                if (otherPart.x == facingPosition.x && otherPart.y == facingPosition.y) {
                                    canExtendBuilding = true;
                                    break;
                                }
                            }
                        }
                        if (canExtendBuilding) break;
                    }
                    if (!canExtendBuilding) return false;
                }

                return true;
            }
        }
    }

    fun deepClone(): Board {
        val newBoard = Board();

        newBoard.SIZE = this.SIZE;

        for (card in this.inGameCards) {
            newBoard.inGameCards.add(card);
        }

        for (block in this.unplacedBlueBlocks) {
            newBoard.unplacedBlueBlocks.add(block?.clone());
        }
        for (block in this.unplacedOrangeBlocks) {
            newBoard.unplacedOrangeBlocks.add(block?.clone());
        }

        for (i in 0..<this.tiles.size) {
            newBoard.tiles[i] = this.tiles[i];
        }

        for (piece in this.placedTopPiecesByBlue) {
            newBoard.placedTopPiecesByBlue.addElement(
                when (piece) {
                    is BoardPiece.Top.Building -> piece.clone()
                    is BoardPiece.Top.Decoration -> piece.clone()
                }
            )
        }
        for (piece in this.placedTopPiecesByOrange) {
            newBoard.placedTopPiecesByOrange.addElement(
                when (piece) {
                    is BoardPiece.Top.Building -> piece.clone();
                    is BoardPiece.Top.Decoration -> piece.clone();
                }
            )
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
    LEVITATION, // Swap picked building with unpicked building and replace
    METROPOLITAN, // Allow building be placed over lantern tile
    JARDIN_DES_PLANTES, // Place garden
    SACRE_COEUR, // No discount on leftover buildings
    LE_PEINTRE, // Place painter
    CHARTIER,   // Place shared tile on opponents tile
    BOUQUINISTES_SUR_LA_SEINE, // Add extension to building
    LAMPADAIRE, // Place lantern on street tile
    MOULIN_ROUGE, // Place dancer
    FONTAINE_DES_MERS, // Place fountain
    LE_PENSEUR, // Place statue on street tile
    LA_GRANDE_LUMIERE, // Increase reach of existing lantern
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

    fun getRotated(clockwise: Boolean): Direction {
        return when (this) {
            NORTH -> if (clockwise) EAST else WEST;
            EAST -> if (clockwise) SOUTH else NORTH;
            SOUTH -> if (clockwise) WEST else EAST;
            WEST -> if (clockwise) NORTH else SOUTH;
        }
    }
}

data class Vec2(var x: Int, var y: Int) {
    fun rotate(clockwise: Boolean) {
        if (clockwise) {
            val newX = this.y;
            val newY = -this.x;
            this.x = newX;
            this.y = newY;
        } else {
            val newX = -this.y;
            val newY = this.x;
            this.x = newX;
            this.y = newY;
        }
    }
    fun clone(): Vec2 = Vec2(x, y);
    fun getAdjacent(direction: Direction): Vec2 = when (direction) {
        Direction.NORTH -> Vec2(this.x, this.y - 1);
        Direction.EAST -> Vec2(this.x + 1, this.y);
        Direction.SOUTH -> Vec2(this.x, this.y + 1);
        Direction.WEST -> Vec2(this.x - 1, this.y);
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

enum class DecorationName {
    GARDEN,
    PAINTER,
    EXTENSION,
    LANTERN,
    DANCER,
    FOUNTAIN,
    STATUE,
    BIG_LANTERN;

    fun toCardType(): CardType {
        return when (this) {
            GARDEN -> CardType.JARDIN_DES_PLANTES;
            PAINTER -> CardType.LE_PEINTRE;
            EXTENSION -> CardType.BOUQUINISTES_SUR_LA_SEINE;
            LANTERN -> CardType.LAMPADAIRE;
            DANCER -> CardType.MOULIN_ROUGE;
            FOUNTAIN -> CardType.FONTAINE_DES_MERS;
            STATUE -> CardType.LE_PENSEUR;
            BIG_LANTERN -> CardType.LA_GRANDE_LUMIERE;
        }
    }
}

sealed class BoardPiece(var rotation: Direction, val parts: List<Vec2>) {
    fun move(offset: Vec2) {
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

    // Moves piece so its origin is at 0, 0
    protected fun normalizeParts() {
        val origin = this.getOrigin();
        val offset = Vec2(-origin.x, -origin.y);
        this.move(offset);
    }

    // Rotates piece around the 0, 0 axis
    protected fun rotateParts(clockwise: Boolean) {
        for (part in this.parts) {
            part.rotate(clockwise);
        }
        this.rotation = this.rotation.getRotated(clockwise);
    }

    class TileBlock(
        rotation: Direction,
        var topLeft: Tile,
        var topRight: Tile,
        var bottomLeft: Tile,
        var bottomRight: Tile) : BoardPiece(rotation, listOf(
            Vec2(0, 0),
            Vec2(1, 0),
            Vec2(0, 1),
            Vec2(1, 1),
            )
        ) {
        fun rotate(clockwise: Boolean) {
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

            super.rotateParts(true);
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
            val cp = this.clone();
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

        class Decoration(val name: DecorationName, parts: List<Vec2>, rotation: Direction) : Top(rotation, parts) {
            fun clone(): Decoration {
                val newName = this.name;
                val newParts = Vector<Vec2>();
                for (part in this.parts) {
                    newParts.addElement(part.clone());
                }
                val newRotation = this.rotation;
                return Decoration(newName, newParts, newRotation);
            }

            companion object {
                fun from(name: DecorationName, origin: Vec2, rotation: Direction): Decoration {
                    val exampleParts = when (name) {
                        DecorationName.GARDEN -> listOf(Vec2(0, 0), Vec2(1, 0));
                        DecorationName.PAINTER -> listOf(Vec2(0, 0));
                        DecorationName.EXTENSION -> listOf(Vec2(0, 0));
                        DecorationName.LANTERN -> listOf(Vec2(0, 0));
                        DecorationName.DANCER -> listOf(Vec2(0, 0));
                        DecorationName.FOUNTAIN -> listOf(Vec2(0, 0), Vec2(1, 0));
                        DecorationName.STATUE -> listOf(Vec2(0, 0));
                        DecorationName.BIG_LANTERN -> listOf(Vec2(0, 0));
                    }
                    val parts = Vector<Vec2>();
                    for (part in exampleParts) {
                        parts.add(part.clone());
                    }
                    val decoration = Decoration(name, parts, Direction.NORTH);

                    when (rotation) {
                        Direction.NORTH -> {} // Element already faces north
                        Direction.EAST -> decoration.rotateParts(true);
                        Direction.SOUTH -> {
                            decoration.rotateParts(true);
                            decoration.rotateParts(true);
                        }

                        Direction.WEST -> decoration.rotateParts(false);
                    }
                    decoration.normalizeParts();
                    decoration.move(origin);
                    return decoration;
                }
            }
        }

        class Building(val name: BuildingName, parts: List<Vec2>, rotation: Direction) : Top(rotation, parts) {
            fun clone(): Building {
                val newName = this.name;
                val newParts = Vector<Vec2>();
                for (part in this.parts) {
                    newParts.addElement(part.clone());
                }
                val newRotation = this.rotation;
                return Building(newName, newParts, newRotation);
            }

            companion object {
                fun from(name: BuildingName, origin: Vec2, rotation: Direction): Building {
                    val exampleParts = when (name) {
                        BuildingName.SMILE -> listOf(Vec2(0, 0), Vec2(0, 1), Vec2(1, 0), Vec2(2, 0), Vec2(3, 0), Vec2(3, 1));
                        BuildingName.BIG_L -> listOf(Vec2(0, 0), Vec2(0, 1), Vec2(0, 2), Vec2(0, 3), Vec2(1, 0), Vec2(2, 0));
                        BuildingName.SMALL_L -> listOf(Vec2(0, 0), Vec2(1, 0), Vec2(1, 1), Vec2(1, 2));
                        BuildingName.BIG_T -> listOf(Vec2(1, 0), Vec2(1, 1), Vec2(1, 2), Vec2(0, 2), Vec2(2, 2));
                        BuildingName.SMALL_T -> listOf(Vec2(0, 0), Vec2(0, 1), Vec2(0, 2), Vec2(1, 1));
                        BuildingName.CORNER -> listOf(Vec2(1, 1), Vec2(1, 0), Vec2(0, 1));
                        BuildingName.SQUARE -> listOf(Vec2(0, 0), Vec2(1, 0), Vec2(0, 1), Vec2(1, 1));
                        BuildingName.STAIRS -> listOf(Vec2(0, 2), Vec2(1, 2), Vec2(1, 1), Vec2(2, 1), Vec2(2, 0));
                        BuildingName.ZIGZAG -> listOf(Vec2(0, 0), Vec2(0, 1), Vec2(1, 1), Vec2(1, 2));
                        BuildingName.CROSS -> listOf(Vec2(1, 0), Vec2(1, 1), Vec2(0, 1), Vec2(1, 2), Vec2(2, 1));
                        BuildingName.LINE -> listOf(Vec2(0, 0), Vec2(1, 0), Vec2(2, 0));
                        BuildingName.CHONK -> listOf(Vec2(0, 1), Vec2(1, 0), Vec2(1, 1), Vec2(2, 0), Vec2(2, 1));
                    }
                    val parts = Vector<Vec2>();
                    for (part in exampleParts) {
                        parts.addElement(part.clone());
                    }
                    val building = Building(name, parts, Direction.NORTH);

                    // Now rotate the building accordingly
                    when (rotation) {
                        Direction.NORTH -> {} // No additional rotation necessary
                        Direction.EAST -> {
                            building.rotateParts(true);
                        }
                        Direction.SOUTH -> {
                            building.rotateParts(true);
                            building.rotateParts(true);
                        }
                        Direction.WEST -> {
                            building.rotateParts(false);
                        }
                    }
                    building.normalizeParts();

                    // And move its origin to the desired location
                    building.move(origin);

                    return building;
                }
            }
        }
    }
}