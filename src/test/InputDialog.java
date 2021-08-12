package test;

import title.TitlePanel;

import javax.swing.*;

public class InputDialog {
    public static void main(String[] args) {
        Object[] possibleValues = { "First", "Second", "Third" };
        Object selectedValue = JOptionPane.showInputDialog(null,
                "Choose one", "Input",
                JOptionPane.INFORMATION_MESSAGE, null,
                possibleValues, possibleValues[0]);
    }
}
