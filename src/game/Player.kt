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
    data class PlaceBlockAt(val position: Vec2): MovePart1();

    data object Pass: MovePart1();
}

sealed class MovePart2 {

}