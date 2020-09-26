package pathPlanning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Path {
    List<Point> points = new ArrayList<>();

    public Path(Point... points){
        this.points.addAll(Arrays.asList(points));
    }
}
