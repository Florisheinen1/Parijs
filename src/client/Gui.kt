package client

import java.awt.Color
import java.awt.Dialog
import java.awt.Graphics
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.InetAddress
import java.net.UnknownHostException
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

class Gui : JFrame() {

    fun run() {
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
        this.add(DrawingCanvas());
        this.pack();

        this.setSize(300,300);
        this.setTitle("Parijs");
        this.isVisible = true;
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}



class DrawingCanvas : JPanel() {

    var squareX = 50;
    var squareY = 50;
    var squareWidth = 20;
    var squareHeight = 20;

    init {
        super.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        val mouseListener = object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                println("Mouse clicked at: %d, %d".format(e.x, e.y));
                moveSquare(e.x, e.y);
            }
            override fun mouseDragged(e: MouseEvent) {
                println("Mouse dragged at: %d, %d".format(e.x, e.y));
                moveSquare(e.x, e.y);
            }
        }

        this.addMouseListener(mouseListener);

    }

    private fun moveSquare(x: Int, y: Int) {
        this.squareX = x;
        this.squareY = y;
        this.repaint();
    }

    override fun paintComponent(g: Graphics) {
        println("Redrawing...");
        super.paintComponent(g);
        // Draw test
        g.drawString("Hello", 20, 20);

        g.color = Color.RED;
        g.fillRect(this.squareX, this.squareY, this.squareWidth, this.squareHeight);

    }
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