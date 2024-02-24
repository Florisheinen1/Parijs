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
    sealed class CardAction : UserMove() {
        data object claimSacreCoeur : CardAction();
    }

    fun clone(): UserMove {
        return when (this) {
            is Pass -> Pass;
            is PickBuilding -> PickBuilding(this.buildingName);
            is PlaceBlockAt -> PlaceBlockAt(this.position, this.tileBlock.copy());
            is CardAction.claimSacreCoeur -> TODO()
            is PlaceBuilding -> PlaceBuilding(this.buildingName, this.position, this.rotation);
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
