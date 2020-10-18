import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class PathPlanner {
    public static void main(String[] args) {
        List<point> control = new ArrayList();
        for(int i = 0; i < 10; i++){
            control.add(new point(100,100));
            control.add(new subpoints(335,255));
            control.add(new point(244, 300));
        }

        // Create a frame
        JFrame f = new JFrame ();

        // Set the title and other parameters.
        f.setTitle ("Hello World Test");
        f.setResizable (true);
        // Background is going to be Panel's background.
        // f.getContentPane().setBackground (Color.cyan);
        f.setSize (500, 300);

        // Add the panel using the default BorderLayout
        NewPanel panel = new NewPanel (control);
        f.getContentPane().add (panel);

        // Show the frame.
        f.setVisible (true);




    }
}
