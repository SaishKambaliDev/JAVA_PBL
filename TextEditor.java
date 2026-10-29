
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;

public class TextEditor extends JFrame {

    private final JTextArea textArea = new JTextArea();
    private final JLabel status = new JLabel("  Ready");
    private Path currentFile = null;
    private boolean modified = false;

    public TextEditor() {
        setTitle("My Text Editor");
        setSize(800, 550);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        textArea.setFont(new Font("Monospaced", Font.PLAIN, 16));
        textArea.setTabSize(4);
        textArea.setMargin(new Insets(12, 12, 12, 12));

        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        status.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        add(status, BorderLayout.SOUTH);

        createMenus();

        textArea.getDocument().addDocumentListener(
            new DocumentListener() {
                public void insertUpdate(DocumentEvent e) {
                    changed();
                }

                public void removeUpdate(DocumentEvent e) {
                    changed();
                }

                public void changedUpdate(DocumentEvent e) {
                    changed();
                }
            }
        );

        textArea.addCaretListener(e -> updateStatus());

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                exitEditor();
            }
        });

        updateStatus();
        setVisible(true);
    }

    private void createMenus() {
        JMenuBar menuBar = new JMenuBar();

        JMenu file = new JMenu("File");
        JMenuItem newFile = new JMenuItem("New");
        JMenuItem open = new JMenuItem("Open");
        JMenuItem save = new JMenuItem("Save");
        JMenuItem saveAs = new JMenuItem("Save As");
        JMenuItem exit = new JMenuItem("Exit");

        newFile.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_N,
                InputEvent.CTRL_DOWN_MASK));
        open.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_O,
                InputEvent.CTRL_DOWN_MASK));
        save.setAccelerator(
            KeyStroke.getKeyStroke(KeyEvent.VK_S,
                InputEvent.CTRL_DOWN_MASK));

        newFile.addActionListener(e -> newDocument());
        open.addActionListener(e -> openFile());
        save.addActionListener(e -> saveFile());
        saveAs.addActionListener(e -> saveAsFile());
        exit.addActionListener(e -> exitEditor());

        file.add(newFile);
        file.add(open);
        file.add(save);
        file.add(saveAs);
        file.addSeparator();
        file.add(exit);

        JMenu edit = new JMenu("Edit");
        JMenuItem cut = new JMenuItem("Cut");
        JMenuItem copy = new JMenuItem("Copy");
        JMenuItem paste = new JMenuItem("Paste");
        JMenuItem selectAll = new JMenuItem("Select All");

        cut.addActionListener(e -> textArea.cut());
        copy.addActionListener(e -> textArea.copy());
        paste.addActionListener(e -> textArea.paste());
        selectAll.addActionListener(e -> textArea.selectAll());

        edit.add(cut);
        edit.add(copy);
        edit.add(paste);
        edit.addSeparator();
        edit.add(selectAll);

        JMenu help = new JMenu("Help");
        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e ->
            JOptionPane.showMessageDialog(
                this,
                "My Text Editor\nCreated using Java Swing.",
                "About",
                JOptionPane.INFORMATION_MESSAGE
            )
        );
        help.add(about);

        menuBar.add(file);
        menuBar.add(edit);
        menuBar.add(help);

        setJMenuBar(menuBar);
    }

    private void changed() {
        modified = true;
        updateStatus();
        updateTitle();
    }

    private void updateTitle() {
        String name = currentFile == null
            ? "Untitled"
            : currentFile.getFileName().toString();

        setTitle((modified ? "* " : "") + name + " - Text Editor");
    }

    private void updateStatus() {
        int position = textArea.getCaretPosition();
        int line = 1;
        int column = 1;

        try {
            line = textArea.getLineOfOffset(position) + 1;
            column = position
                - textArea.getLineStartOffset(line - 1) + 1;
        } catch (Exception ignored) {
        }

        String content = textArea.getText();
        int words = content.trim().isEmpty()
            ? 0 : content.trim().split("\\s+").length;

        status.setText("  Line: " + line
            + " | Column: " + column
            + " | Words: " + words
            + " | Characters: " + content.length());
    }

    private boolean confirmSave() {
        if (!modified) {
            return true;
        }

        int choice = JOptionPane.showConfirmDialog(
            this,
            "Do you want to save your changes?",
            "Unsaved Changes",
            JOptionPane.YES_NO_CANCEL_OPTION
        );

        if (choice == JOptionPane.CANCEL_OPTION
                || choice == JOptionPane.CLOSED_OPTION) {
            return false;
        }

        if (choice == JOptionPane.YES_OPTION) {
            return saveFile();
        }

        return true;
    }

    private void newDocument() {
        if (!confirmSave()) return;

        textArea.setText("");
        currentFile = null;
        modified = false;
        updateTitle();
        updateStatus();
    }

    private void openFile() {
        if (!confirmSave()) return;

        JFileChooser chooser = new JFileChooser();

        if (chooser.showOpenDialog(this)
                == JFileChooser.APPROVE_OPTION) {
            try {
                currentFile = chooser.getSelectedFile().toPath();
                textArea.setText(Files.readString(currentFile));
                modified = false;
                updateTitle();
                updateStatus();
            } catch (IOException ex) {
                showError(ex);
            }
        }
    }

    private boolean saveFile() {
        if (currentFile == null) {
            return saveAsFile();
        }

        try {
            Files.writeString(currentFile, textArea.getText());
            modified = false;
            updateTitle();
            return true;
        } catch (IOException ex) {
            showError(ex);
            return false;
        }
    }

    private boolean saveAsFile() {
        JFileChooser chooser = new JFileChooser();

        if (chooser.showSaveDialog(this)
                != JFileChooser.APPROVE_OPTION) {
            return false;
        }

        Path selected = chooser.getSelectedFile().toPath();

        if (Files.exists(selected)) {
            int answer = JOptionPane.showConfirmDialog(
                this,
                "File already exists. Overwrite it?",
                "Confirm Save",
                JOptionPane.YES_NO_OPTION
            );

            if (answer != JOptionPane.YES_OPTION) {
                return false;
            }
        }

        currentFile = selected;
        return saveFile();
    }

    private void exitEditor() {
        if (confirmSave()) {
            dispose();
        }
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(
            this,
            "Error: " + ex.getMessage(),
            "File Error",
            JOptionPane.ERROR_MESSAGE
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TextEditor::new);
    }
}
