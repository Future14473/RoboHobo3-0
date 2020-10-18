import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

// Extend JPanel to override its paintComponent() method:

    class NewPanel extends JPanel {
        List<point> control;
        // Set background in constructor.

        public NewPanel (List<point> control)
        {
            this.control = control;
            this.setBackground (Color.cyan);
        }


        // Override paintComponent():

        public void paintComponent (Graphics g)
        {
            // Always call super.paintComponent (g):
            super.paintComponent(g);

            // drawString() is a Graphics method.
            // Draw the string "Hello World" at location 100,100
            g.drawString ("Hello World!", 100, 100);
            g.setColor(Color.red);
            for (int i = 0; i < control.size(); i++) {
                this.control = control;
                g.fillOval(control.get(i).x,control.get(i).y,5,5);
            }
            // Let's find out when paintComponent() is called.
            System.out.println ("Inside paintComponent");
        }

    }


