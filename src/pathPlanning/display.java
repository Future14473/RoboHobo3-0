package pathPlanning;

import javax.swing.*;
import java.awt.*;

public class display {
    public static void show(Path path){
        JFrame frame = new JFrame("pathPlanning.display");
        frame.setSize(500, 500);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel canvas = new JPanel(){
            public void paintComponent(Graphics g){
                g.fillRect(0, 0, 100, 100);
            }
        };

        frame.add(canvas);
    }
}
