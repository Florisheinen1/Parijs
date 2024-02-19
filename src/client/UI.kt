package client

import game.Board
import game.BuildingName
import game.TileBlock
import game.Vec2
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
    data object CloseWindow : UserAction();
    data class PickBuilding(var buildingName: BuildingName) : UserAction();
    data class PlaceTileBlock(var position: Vec2) : UserAction();
    data object Pass : UserAction();
}

interface PhaseChangeListener {
    fun onPhaseChange(newPhase: GuiPhase);
}

sealed class GuiPhase { // Rename to UIPhase
    data object Connecting : GuiPhase();
    data object InLobby : GuiPhase();
    data class GamePart1(val hasTurn: Boolean) : GuiPhase();
    data class GamePart2(val hasTurn: Boolean) : GuiPhase();
    data class GameEnd(val isWinner: Boolean) : GuiPhase();
}