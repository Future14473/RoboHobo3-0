package pathPlanning;

import pathPlanning.display;

public class Main {
    public static void main(String[] args) {
        Path path = new Path(
                new Point(0, 0),
                new Point(100, 40),
                new Point(200, 50),
                new Point(4000, 400));

        display.show(path);
    }

}
