package game

import client.UserAction
import game.BoardPiece.*;

interface Player {
    fun startPhase1(cards: List<CardType>);
    fun startPhase2();

    fun askTurnPhase1(availableBuildings: List<BuildingName>, topTileBlock: TileBlock?): UserMove;
    fun askTurnPhase2(): UserMove;

    fun respondToMove(response: MoveResponse);

    fun updateMove(move: UserMove);

    fun declareWinner(isWinner: Boolean);
}

sealed class MoveResponse {
    data object Accept : MoveResponse();
    data class Deny(val reason: String) : MoveResponse();
}

sealed class UserMove {

    data object Pass: UserMove();
    // Phase 1
    data class PickBuilding(val buildingName: BuildingName): UserMove();
    data class PlaceBlockAt(val position: Vec2, val tileBlock: TileBlock): UserMove();
    // Phase 2
    data class PlaceBuilding(val buildingName: BuildingName, val position: Vec2, val rotation: Direction) : UserMove();

    data class ClaimCard(val cardType: CardType) : UserMove();
    data class PlaceDecoration(val decorationName: DecorationName, val position: Vec2, val rotation: Direction) : UserMove();

    fun clone(): UserMove {
        // TODO: Merge this with user action
        return when (this) {
            is Pass -> Pass;
            is PickBuilding -> PickBuilding(this.buildingName);
            is PlaceBlockAt -> PlaceBlockAt(this.position, this.tileBlock.clone());
            is PlaceBuilding -> PlaceBuilding(this.buildingName, this.position, this.rotation);
            is ClaimCard -> ClaimCard(this.cardType);
            is PlaceDecoration -> PlaceDecoration(this.decorationName, this.position, this.rotation);
        }
    }

    fun invert() {
        if (this is PlaceBlockAt) {
            this.tileBlock.invert();
        }
    }

    fun getInverted(): UserMove {
        val cp = this.clone();
        cp.invert();
        return cp;
    }
}
