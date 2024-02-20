package game

interface Player {
    fun startPart1(cards: List<Cards>);

    fun askTurnPart1(availableBuildings: List<BuildingName>, topTileBlock: TileBlock?): MovePart1;

    fun respondToMove(response: MoveResponse);

    fun updateMovePart1(move: MovePart1);

    fun startPar2();

    fun askTurnPart2(): MovePart2;

    fun updateMovePart2(move: MovePart2);

    fun declareWinner(isWinner: Boolean);
}

sealed class MoveResponse {
    data object Accept : MoveResponse();
    data class Deny(val reason: String) : MoveResponse();
}

sealed class MovePart1 {
    data class PickBuilding(val buildingName: BuildingName): MovePart1();
    data class PlaceBlockAt(val position: Vec2, val tileBlock: TileBlock): MovePart1();

    data object Pass: MovePart1();

    fun clone(): MovePart1 {
        return when (this) {
            is Pass -> Pass;
            is PickBuilding -> PickBuilding(this.buildingName);
            is PlaceBlockAt -> PlaceBlockAt(this.position, this.tileBlock.copy());
        }
    }

    fun invert() {
        if (this is PlaceBlockAt) {
            this.tileBlock.invert();
        }
    }

    fun getInverted(): MovePart1 {
        val cp = this.clone();
        cp.invert();
        return cp;
    }
}

sealed class MovePart2 {

}