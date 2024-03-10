package client

import game.*
import game.BoardPiece.*
import java.net.InetAddress
import java.util.*

abstract class UI {
    abstract fun getServerAddress(): Pair<InetAddress, Int>;

    abstract fun updatePhase(newPhase: GuiPhase);

    abstract fun updateGameState(board: Board);

    private val userActionListeners = Vector<UserActionListener>();
    fun addUserActionListener(listener: UserActionListener) {
        synchronized(this.userActionListeners) {
            this.userActionListeners.add(listener);
        }
    }
    fun removeUserActionListener(listener: UserActionListener) {
        synchronized(this.userActionListeners) {
            this.userActionListeners.removeElement(listener);
        }
    }
    protected fun onUserAction(action: UserAction) {
        synchronized(this.userActionListeners) {
            for (listener in this.userActionListeners) {
                listener.onUserAction(action);
            }
        }
    }
}

abstract class UserActionListener {
    abstract fun onUserAction(action: UserAction);
    // TODO: Have code that checks if this action was not used
}

sealed class UserAction {
    // TODO: Merge this with UserMove?
    data object CloseWindow : UserAction();
    data class PickBuilding(var buildingName: BuildingName) : UserAction();
    data class PlaceTileBlock(var position: Vec2, val tileBlock: TileBlock) : UserAction();
    data class PlaceBuilding(val buildingName: BuildingName, var position: Vec2, val rotation: Direction) : UserAction();
    data object Pass : UserAction();

    data class PlaceDecoration(val decoration: Top.Decoration) : UserAction();
    data class ClaimCard(val cardType: CardType) : UserAction();
}

sealed class GuiPhase { // Rename to UIPhase
    data object Connecting : GuiPhase();
    data object InLobby : GuiPhase();
    data class GamePhase1(val hasTurn: Boolean) : GuiPhase();
    data class GamePhase2(val hasTurn: Boolean) : GuiPhase();
    data class GameEnd(val isWinner: Boolean) : GuiPhase();
}