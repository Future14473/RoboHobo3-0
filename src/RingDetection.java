import com.sun.org.apache.xpath.internal.SourceTree;
import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.imgproc.Imgproc.*;

public class RingDetection {
    public static final double DISKR = 0.2;
    public static final double MAXR = DISKR * 1.4;
    public static final double MINR = DISKR * 0.6;
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        String[] pics = {"0stack.jpg", "0stack2.jpg", "1stack.jpg", "1stack2.jpg", "4stack.jpg", "4stack2.jpg"};

        for(String pic : pics){
            HighGui.imshow("ree", picSetup(pic));
            HighGui.waitKey();
            HighGui.imshow("ree", mark_rings(picSetup(pic)));
            HighGui.waitKey();
        }

        System.exit(10);
    }

    public static Mat picSetup(String file_name) {
        Mat raw = Imgcodecs.imread(file_name);
        Mat resized = new Mat();
        Imgproc.resize(raw, resized, new Size(Math.round(raw.width() * 0.30), Math.round(raw.height() * 0.30)));
        return resized;
    }

    public static Mat copyHSV(Mat input){
        Mat recolored = new Mat();
        cvtColor(input, recolored, COLOR_BGR2HSV);
        return recolored;
    }

    public static Mat find_yellows(Mat input){
        Mat threshhold = new Mat();
        Core.inRange(input, new Scalar(10, 95, 95), new Scalar(35, 255, 255), threshhold);
        return threshhold;
    }

    public static ArrayList<Rect> find_subcontours(Mat input, Rect main){
        Mat threshhold = find_yellows(copyHSV(input));
        Mat gray = new Mat();
        Mat edges = new Mat();
        Mat sub = new Mat();
        ArrayList<Rect> output = new ArrayList<>();
        cvtColor(input, gray, COLOR_BGR2GRAY);
        Imgproc.Sobel(gray, edges, -1, 0, 1);
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2));
        Imgproc.dilate(edges, edges, kernel);
        Core.subtract(threshhold, edges, sub);
        Core.inRange(sub, new Scalar(150, 150, 150), new Scalar(255, 255, 255), sub);
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(sub, contours, new Mat(), Imgproc.CHAIN_APPROX_NONE, Imgproc.CHAIN_APPROX_SIMPLE);
        contours.removeIf(m -> {
            Rect rect = Imgproc.boundingRect(m);
            double r = (double) rect.height/rect.width;
            return ((rect.height > rect.width) || (r > MAXR) || (r < MINR) || (rect.width < main.width * 0.8));
        });
        HighGui.imshow("ree", sub);
        HighGui.waitKey();

        for(MatOfPoint contour: contours){
            Rect rect = Imgproc.boundingRect(contour);
            Imgproc.rectangle(input, rect.tl(), rect.br(), new Scalar(255, 255, 0), 2);
            output.add(rect);
        }
        HighGui.imshow("ree", input);
        HighGui.waitKey();
        return output;
    }

    public static Mat mark_rings(Mat input){
        List<MatOfPoint> contours = new ArrayList<>();
        Mat threshhold = find_yellows(copyHSV(input));
        Imgproc.findContours(threshhold, contours, new Mat(), Imgproc.CHAIN_APPROX_NONE, Imgproc.CHAIN_APPROX_SIMPLE);

        //initial filtering
        contours.removeIf(m -> {
            Rect rect = Imgproc.boundingRect(m);
            double r = (double) rect.height/rect.width;
            return (rect.area() < 1000) || (rect.height > rect.width);
            });

        //finding stacked rectangles and marking
        List<MatOfPoint> finalRects = new ArrayList<>();
        double[][] rectsData = new double[contours.size()][6];
        int used = 0;

        //sorting rects by x

        for(MatOfPoint contour:contours){
            Rect rect = Imgproc.boundingRect(contour);
            int newX = (int)Math.max(rect.x - (rect.width*0.1), 0);
            int newY = (int)Math.max(rect.y - (rect.height*0.1), 0);
            int newW = (int)Math.min((rect.width*1.1) + rect.x, input.width()) - newX;
            int newH = (int)Math.min((rect.height*1.1) + rect.y, input.height()) - newY;
            ArrayList<Rect> moreRects = find_subcontours(new Mat(input.clone(), new Rect(newX, newY, newW, newH)), rect);
            for(Rect subrect: moreRects){
                double epsilonW = rect.width * 0.1;
                double epsilonH = rect.height * 0.1;
                subrect.x += newX;
                subrect.y += newY;
                int index = closeIn(rectsData, subrect.x, subrect.y, subrect.width, subrect.height, epsilonW, epsilonH);
                if(index == -2){
                    System.out.println("Overlap");
                }
                else if(index == -1){

                    rectsData[used][0] = 1;
                    rectsData[used][1] = subrect.x;
                    rectsData[used][2] = subrect.width;
                    rectsData[used][3] = subrect.y;
                    rectsData[used][4] = subrect.y;
                    rectsData[used][5] = subrect.height;
                    used += 1;
                }
                else{
                    double occ = rectsData[index][0];
                    double avgX = rectsData[index][1];
                    double avgW = rectsData[index][2];
                    double maxY = rectsData[index][3];
                    double minY = rectsData[index][4];
                    double avgH = rectsData[index][5];
                    rectsData[index][0] += 1;
                    rectsData[index][1] = (subrect.x + occ*avgX)/(occ + 1);
                    rectsData[index][2] = (subrect.width + occ*avgW)/(occ + 1);
                    rectsData[index][3] = Math.max(subrect.y, maxY);
                    rectsData[index][4] = Math.min(subrect.y, minY);
                    rectsData[index][5] = (subrect.height + occ*avgH)/(occ + 1);
                }
            }
        }

        System.out.println(Arrays.deepToString(rectsData));

        //labeling found stacks
        for(double[] data: rectsData){
            if(data[0] > 0){
                Rect rect = new Rect((int) data[1], (int) data[4], (int) data[2], (int) (data[3] - data[4] + data[5]));
                Imgproc.rectangle(input, rect.tl(), rect.br(), new Scalar(255, 255, 0), 2);
                Imgproc.putText(input, "" + (int)data[0], new Point(data[1] + data[2]/2, data[3]), Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 0, 255), 2);
            }
        }
        return input;
    }

    public static int closeIn(double[][] list, double x, double y, double width, double height, double epsilonW, double epsilonH){
        for(int i = 0; i< list.length; i++){
            if(Math.abs(list[i][1] - x) < epsilonW && Math.abs(list[i][4] - y) < epsilonW){
                return -2; //Too close
            }
            if(Math.abs(list[i][1] - x) < epsilonW && Math.abs(list[i][2] - width) < epsilonW && Math.abs(list[i][5] - height) < epsilonH){
                return i;
            }
        }
        return -1;
    }
}

