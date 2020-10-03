import java.util.ArrayList;
import java.util.List;

public class PathPlanner {
    public static void main(String[] args) {
        List<point> control = new ArrayList();
        for(int i = 0; i < 10; i++){
            control.add(new point(2,10));
            control.add(new subpoints(5,15));
        }



    }
}
