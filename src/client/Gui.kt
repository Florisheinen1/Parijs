package client

import game.*
import java.awt.Color
import java.awt.Dialog
import java.awt.Graphics
import java.awt.GridLayout
import java.awt.event.*
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JDialog
import javax.swing.SwingUtilities;
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.SwingConstants
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.BadLocationException
import kotlin.concurrent.thread
import kotlin.math.abs

class Gui : JFrame() {

    val drawingCanvas = DrawingCanvas();
    var isRunning = false;

    fun run() {
        isRunning = true;
        // First the dialog part
        val dialog = StartDialog();
        thread { dialog.run() };
        while (!dialog.submitted) {
            // wait
            Thread.sleep(100);
        }
        dialog.dispose();
        println("Server Address: %s:%d".format(dialog.serverAddress, dialog.serverPort));

        // Now the rest
        this.add(this.drawingCanvas);
        this.pack();

        this.setSize(300,300);
        this.setTitle("Parijs");
        this.isVisible = true;
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(900, 700);
        this.addWindowListener(object : WindowListener {
            override fun windowOpened(e: WindowEvent?) {}
            override fun windowIconified(e: WindowEvent?) {}
            override fun windowDeiconified(e: WindowEvent?) {}
            override fun windowActivated(e: WindowEvent?) {}
            override fun windowDeactivated(e: WindowEvent?) {}
            override fun windowClosing(e: WindowEvent?) {
                isRunning = false;
            }
            override fun windowClosed(e: WindowEvent?) {
                isRunning = false;
            }
        });

        println("Starting to render");
        while (this.isRunning) {
//            this.dispatchEvent(WindowEvent(this, WindowEvent.WINDOW_CLOSING));
            Thread.sleep(100);
            this.repaint();
        }
        println("Closed gui thread");
    }

    fun updateBoard(board: Board) {
        this.drawingCanvas.screenBoard.updateBoard(board);
    }
}

abstract class ParisScreenElement : MouseAdapter(), MouseMotionListener {
    abstract fun draw(g: Graphics, screenSize: Vec2);
}

class ScreenBuilding(val building: Placable) : MouseAdapter(), MouseMotionListener {
    var isHovered = false;
    val borderColor = Color.WHITE;
    val borderColorHover = Color.BLACK;
    val borderColorSelected = Color.RED;
    val innerColor = Color.GREEN;

    var origin = Vec2(0, 0);
    var unitSize = 10;

    fun draw(g: Graphics) {
        val border_color = if (isHovered) borderColorHover else borderColor;

        val BORDER_WIDTH = 5; // pixels

        for (pos in building.parts) {
            val outerX = origin.x + unitSize * pos.x;
            val outerY = origin.y + unitSize * pos.y;
            val outerEndX = outerX + unitSize;
            val outerEndY = outerY + unitSize;
            g.color = border_color;
            g.fillRect(outerX, outerY, outerEndX - outerX, outerEndY - outerY);

            val neighbours = getNeighborDirections(pos, building.parts);

            val coloredX = outerX + if (neighbours.contains(Direction.WEST)) 0 else BORDER_WIDTH;
            val coloredY = outerY + if (neighbours.contains(Direction.NORTH)) 0 else BORDER_WIDTH;
            val coloredEndX = outerEndX - if (neighbours.contains(Direction.EAST)) 0 else BORDER_WIDTH;
            val coloredEndY = outerEndY - if (neighbours.contains(Direction.SOUTH)) 0 else BORDER_WIDTH;
            g.color = innerColor;
            g.fillRect(coloredX, coloredY, coloredEndX - coloredX, coloredEndY - coloredY);
        }
    }

    override fun mouseMoved(e: MouseEvent?) {
        if (e != null) {
            this.isHovered = this.contains(e.x, e.y);
        }
    }

    private fun contains(x: Int, y: Int): Boolean {
        for (rectOrigin in building.parts) {
            val left = origin.x + rectOrigin.x * unitSize;
            val top = origin.y + rectOrigin.y * unitSize;
            val right = left + unitSize;
            val bottom = top + unitSize;
            if (x in left..right && y in top..bottom) {
                return true;
            }
        }
        return false;
    }

    private fun getNeighborDirections(pos: Vec2, others: Vector<Vec2>): Vector<Direction> {
        val neighbors = Vector<Direction>();
        for (other in others) {
            val diffX = other.x - pos.x;
            val diffY = other.y - pos.y;
            if (abs(diffX) + abs(diffY) == 1) {
                // This position is a neighbor!
                if (diffX == -1) {
                    neighbors.addElement(Direction.WEST);
                } else if (diffX == 1) {
                    neighbors.addElement(Direction.EAST);
                }
                if (diffY == 1) {
                    neighbors.addElement(Direction.SOUTH);
                } else if (diffY == -1) {
                    neighbors.addElement(Direction.NORTH);
                }
            }
        }
        return neighbors;
    }
}

class ScreenUnpickedBuildingArea(board: Board) : ParisScreenElement() {
    private val AREA_SIZE = 420;
    private val UNIT_SIZE = AREA_SIZE / 7;
    private val yOffset = 650;

    private val unpickedScreenBuildings = Vector<ScreenBuilding>();

    init {
        updateBoard(board);
    }

    override fun draw(g: Graphics, screenSize: Vec2) {
        println("Drawing unpicked buildings...")
        val origin = Vec2(screenSize.x / 2 - AREA_SIZE / 2, yOffset);

        g.color = Color(100, 20, 20);
        g.fillRect(origin.x, origin.y, AREA_SIZE, AREA_SIZE + UNIT_SIZE)

        for (unpickedBuilding in this.unpickedScreenBuildings) {
            unpickedBuilding.unitSize = UNIT_SIZE;
            unpickedBuilding.origin = getBuildingOrigin(origin, UNIT_SIZE, unpickedBuilding.building.name);
            unpickedBuilding.draw(g);
        }
    }

    override fun mouseMoved(e: MouseEvent?) {
        for (unpickedScreenBuilding in unpickedScreenBuildings) {
            unpickedScreenBuilding.mouseMoved(e);
        }
    }

    fun updateBoard(board: Board) {
        unpickedScreenBuildings.clear();

        for (unpickedBuilding in board.unpickedBuildings) {
            val screenBuilding = ScreenBuilding(unpickedBuilding.deepClone());
            unpickedScreenBuildings.addElement(screenBuilding);
        }
    }

    fun getBuildingOrigin(origin: Vec2, unitSize: Int, name: PlacableName): Vec2 {
        val relativeUnits = when (name) {
            PlacableName.SMILE -> Vec2(0, 0)
            PlacableName.BIG_L -> Vec2(4, 0)
            PlacableName.SMALL_L -> Vec2(5, 1)
            PlacableName.BIG_T -> Vec2(4, 2)
            PlacableName.SMALL_T -> Vec2(2, 1)
            PlacableName.CORNER -> Vec2(0, 1)
            PlacableName.SQUARE -> Vec2(0, 3)
            PlacableName.STAIRS -> Vec2(1, 3)
            PlacableName.ZIGZAG -> Vec2(0, 5)
            PlacableName.CROSS -> Vec2(2, 5)
            PlacableName.LINE -> Vec2(4, 5)
            PlacableName.CHONK -> Vec2(4, 6)
            else -> throw Exception("Failed to draw non-building");
        };
        return Vec2(
            origin.x + relativeUnits.x * unitSize,
            origin.y + relativeUnits.y * unitSize
        );
    }
}

class ScreenBoard(var board: Board) : ParisScreenElement() {
    val BOARD_SIZE_PIXELS = 600;
    val yOffset = 20;
    val unitSize = BOARD_SIZE_PIXELS / 8;
    override fun draw(g: Graphics, screenSize: Vec2) {
        val boardOrigin = Vec2((screenSize.x / 2) - (BOARD_SIZE_PIXELS / 2), yOffset);

        g.color = Color.GREEN;
        g.fillRect(boardOrigin.x, boardOrigin.y, BOARD_SIZE_PIXELS, BOARD_SIZE_PIXELS);

        for (x in 0..<this.board.SIZE) {
            for (y in 0..<this.board.SIZE) {
                val tileX = boardOrigin.x + x * unitSize;
                val tileY = boardOrigin.y + y * unitSize;
                g.color = Color.WHITE;
                g.fillRect(tileX, tileY, unitSize, unitSize);

                g.color = when (this.board.getTile(x, y)) {
                    Tile.BLUE -> Color.BLUE
                    Tile.ORANGE -> Color.ORANGE
                    Tile.LANTERN -> Color.YELLOW
                    Tile.BRICKS -> Color.GRAY;
                }

                g.fillRect(tileX+1, tileY+1, unitSize-2, unitSize-2);
            }
        }
    }

    fun updateBoard(board: Board) {
        this.board = board;
    }
}

class DrawingCanvas : JPanel() {

    var screenElements = Vector<ParisScreenElement>();

    var screenBoard = ScreenBoard(Board());
    var unpickedBuildings = ScreenUnpickedBuildingArea(Board());

    init {
//        println("Board width: %d, %d".format(BOARD_ORIGIN.x, BOARD_ORIGIN.y));

//        super.setBorder(BorderFactory.createLineBorder(Color.BLACK));

//        for (unpickedBuilding in screenBoard.board.unpickedBuildings) {
//            val sb = ScreenBuilding(UNPICKED_BUILDINGS_ORIGIN, UNPICKED_UNIT_SIZE, unpickedBuilding);
//            screenElements.addElement(sb);
//            this.addMouseMotionListener(sb);
//            this.addMouseListener(sb);
//        }
        screenElements.addElement(this.screenBoard);
        screenElements.addElement(this.unpickedBuildings);

        this.addMouseMotionListener(unpickedBuildings);
        this.addMouseMotionListener(screenBoard);
    }

    override fun paintComponent(g: Graphics) {
        println("Redrawing...");
        g.color = Color.GRAY;
        g.fillRect(0, 0, this.width, this.height);

        for (screenBoi in this.screenElements) {
            screenBoi.draw(g, Vec2(width, height));
        }
    }


//    private fun drawUnpickedBuildings(g: Graphics) {
//        val unitSize = UNPICKED_BUILDINGS_AREA_SIZE / 7;
//        val areaX = (this.width / 2) - (UNPICKED_BUILDINGS_AREA_SIZE / 2);
//        val areaY = this.BOARD_SIZE_PIXELS + 20*2;
//
//        g.color = Color.PINK;
//        g.fillRect(areaX-5, areaY-5, UNPICKED_BUILDINGS_AREA_SIZE+10, UNPICKED_BUILDINGS_AREA_SIZE+unitSize+10);
//
//        for (unpickedBuilding in this.board.unpickedBuildings) {
////            if (unpickedBuilding.name != PlacableName.BIG_T) {
////                continue;
////            }
//            val unpickedBuildingOrigin = getUnpickedBuildingOrigin(unpickedBuilding);
//            var screenOriginX = areaX + unpickedBuildingOrigin.x * unitSize;
//            var screenOriginY = areaY + unpickedBuildingOrigin.y * unitSize;
//
//            this.drawBuilding(g, unpickedBuilding, unitSize, Vec2(screenOriginX, screenOriginY), Color.GREEN, Color.WHITE);
//        }
//    }

}

class StartDialog : JDialog() {
    var DEFAULT_ADDRESS = "127.0.0.1";
    var DEFAULT_PORT = "39939";

    var serverAddress: InetAddress? = null;
    var serverPort: Int? = null;
    var submitted = false;

    fun run() {
        this.title = "Start Parijs";
        this.setSize(400, 150);
        this.setLocationRelativeTo(null);
        this.defaultCloseOperation = JDialog.DISPOSE_ON_CLOSE;
        this.layout = GridLayout(5, 1);

        val submit = JButton("Join server");
        submit.addActionListener(object : ActionListener {
            override fun actionPerformed(e: ActionEvent?) {
                if (serverAddress == null) serverAddress = InetAddress.getByName(DEFAULT_ADDRESS);
                if (serverPort == null) serverPort = DEFAULT_PORT.toInt();

                submitted = true;
            }
        })

        val addressLabel = JLabel("Server Address:");
        val addressInput = JTextField(DEFAULT_ADDRESS);
        addressInput.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) { updateValue(e); }
            override fun removeUpdate(e: DocumentEvent) { updateValue(e); }
            override fun changedUpdate(e: DocumentEvent) { updateValue(e); }

            fun updateValue(e: DocumentEvent) {
                try {
                    val newAddr = InetAddress.getByName(e.document.getText(0, e.document.length));
                    serverAddress = newAddr;
                } catch (e: BadLocationException) {
                    serverAddress = null;
                } catch (e: UnknownHostException) {
                    serverAddress = null;
                }

                if (serverAddress == null) {
                    addressInput.background = Color(255, 200, 200);
                } else {
                    addressInput.background = Color(200, 255, 200);
                }
                submit.isEnabled = hasValidData();
            }
        })

        val portLabel = JLabel("Port:");
        val portInput = JTextField(DEFAULT_PORT);
        portInput.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) { updateValue(e) }
            override fun removeUpdate(e: DocumentEvent) { updateValue(e) }
            override fun changedUpdate(e: DocumentEvent) { updateValue(e) }

            fun updateValue(e: DocumentEvent) {
                try {
                    val newPort = e.document.getText(0, e.document.length).toInt();
                    serverPort = newPort;
                } catch (e: NumberFormatException) {
                    serverPort = null;
                }
                if (serverPort != null) {
                    portInput.background = Color(200, 255, 200);
                } else {
                    portInput.background = Color(255, 200, 200);
                }
                submit.isEnabled = hasValidData();
            }
        })

        this.add(addressLabel);
        this.add(addressInput);
        this.add(portLabel);
        this.add(portInput);
        this.add(submit);



        this.isVisible = true;
    }

    fun hasValidData(): Boolean {
        return serverAddress != null && serverPort != null;
    }
}