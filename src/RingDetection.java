import com.github.sarxos.webcam.WebcamResolution;
import com.sun.org.apache.xpath.internal.SourceTree;
import org.omg.Messaging.SYNC_WITH_TRANSPORT;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import static org.opencv.core.CvType.CV_8UC3;
import static org.opencv.imgproc.Imgproc.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.github.sarxos.webcam.Webcam;

import javax.imageio.ImageIO;

public class RingDetection {
    public static final double DISKR = 0.2;
    public static final double DMAXR = DISKR * 1.7;
    public static final double DMINR = DISKR * 0.5;

    public static final double STICKR = 9;
    public static final double SMAXR = STICKR * 1.2;
    public static final double SMINR = STICKR * 0.6;

    public static final int CURVE_EXTENSION = 5;

    public static final double x0 = 20;
    public static final double y0 = 30;
    public static final int yP = 720;
    public static final int xP = 960;
    public static final double theta0 = Math.atan(y0/x0);
    public static final double viewAngle = Math.PI - 2* theta0;
    public static final double realX0 = 35;
    public static final double slope = 0.46;

    Mat resized = new Mat();
    Mat recolored = new Mat();
    Mat threshold = new Mat();
    Mat subthreshold = new Mat();
    Mat submat = new Mat();
    int value = 0;
    int saturation = 0;
    Mat cam = new Mat();
    Webcam camera = Webcam.getDefault();

    public static void main(String[] args) throws IOException {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        RingDetection detection = new RingDetection();
        //detection.testPics("w");
        detection.testCam("wb");
    }
    public void testCam(String type) throws IOException {
        camera.setViewSize(WebcamResolution.VGA.getSize());
        camera.open();
        Mat kernel = Imgproc.getStructuringElement(CV_SHAPE_ELLIPSE, new Size(7, 7));

        if(type.equals("r")){
            for(int i = 0; i<1000; i++){
                ImageIO.write(camera.getImage(), "JPG", new File("temp.jpg"));
                Mat pic = picSetup("temp.jpg");
                HighGui.imshow("results", markRings(pic, find_rings(pic)));
                Mat thresh = find_yellows(picSetup("temp.jpg"));
                HighGui.imshow("thresholds", thresh);
                HighGui.waitKey(1);
            }
        }
        else if(type.equals("wr")){
            for(int i = 0; i<1000; i++) {
                ImageIO.write(camera.getImage(), "JPG", new File("temp.jpg"));
                Mat pic = picSetup("temp.jpg");
                HighGui.imshow("results", markWobble(pic, find_wobble(pic, "red")));
//                Mat thresh = find_reds(picSetup("temp.jpg"));
//                dilate(thresh, thresh, kernel);
//                HighGui.imshow("thresholds", thresh);
                HighGui.waitKey(1);
            }
        }

        else{
            for(int i = 0; i<1000; i++) {
                ImageIO.write(camera.getImage(), "JPG", new File("temp.jpg"));
                Mat pic = picSetup("temp.jpg");
                HighGui.imshow("results", markWobble(pic, find_wobble(pic, "blue")));
                Mat thresh = find_blues(picSetup("temp.jpg"));
                dilate(thresh, thresh, kernel);
                HighGui.imshow("thresholds", thresh);
                HighGui.waitKey(1);
            }
        }

        camera.close();
        HighGui.destroyAllWindows();
    }

    public void testPics(String set){
        String[] picsO = {"0stack.jpg", "0stack2.jpg", "1stack.jpg", "1stack2.jpg", "4stack.jpg", "4stack2.jpg"};
        String[] picsE = new String[4];
        String[] picsQ = new String[12];
        String[] picsW = new String[1];

        for(int i = 0; i < 4; i++){
            picsE[i] = "e" + (i+1) + ".png";
        }
        for(int i = 4; i < 16; i++){
            picsQ[i-4] = "q" + (i+1) + ".jpg";
        }
        for(int i = 0; i < 1; i++){
            picsW[i] = "w" + (i+1) + ".jpg";
        }


        if(set.equals("o")) {
            for (String pic : picsO) {
                Mat proc = picSetup(pic);
                HighGui.imshow("ree", markRings(proc, find_rings(proc)));
                HighGui.waitKey();
            }
        }
        if(set.equals("e")) {
            for(String pic : picsE){
                Mat proc = picSetup(pic);
                HighGui.imshow("ree", markRings(proc, find_rings(proc)));
                HighGui.waitKey();
            }
        }
        if(set.equals("q")) {
            for(String pic : picsQ){
                Mat proc = picSetup(pic);
                HighGui.imshow("ree", markRings(proc, find_rings(proc)));
                HighGui.waitKey();
            }
        }
        if(set.equals("w")) {
            for(String pic : picsW){
                Mat proc = picSetup(pic);
                HighGui.imshow("ree", markWobble(proc, find_wobble(proc, "blue")));
                HighGui.waitKey();
                HighGui.imshow("ree", markWobble(proc, find_wobble(proc, "red")));
                HighGui.waitKey();
            }
        }
        System.exit(10);
    }

    public Mat picSetup(String file_name) {
        Mat raw = Imgcodecs.imread(file_name);
        Mat resized = new Mat();
        double scale = 960.0/raw.height();
        Imgproc.resize(raw, resized, new Size(Math.round(raw.width() * scale), Math.round(raw.height() * scale)));
        avgValues(resized);
        return resized;
    }

    public Mat picSetup(Mat input) {
        Mat recolored = new Mat();
        cvtColor(input, recolored, COLOR_RGB2BGR);
        Mat resized = new Mat();
        double scale = 960.0/input.height();
        Imgproc.resize(recolored, resized, new Size(Math.round(input.width() * scale), Math.round(input.height() * scale)));
        recolored.release();
        return resized;
    }

    public Mat copyHSV(Mat input){
        cvtColor(input, recolored, COLOR_BGR2HSV);
        return recolored;
    }

    public void avgValues(Mat input){
        Mat copy = copyHSV(input);
        ArrayList<Integer> values = new ArrayList<>();
        ArrayList<Integer> saturations = new ArrayList<>();
        int area = input.height() * input.width()/16;
        for(int y = 0; y < input.height(); y+=3){
            for(int x = 0; x < input.width(); x+=3){
                values.add((Integer)(int)copy.get(y, x)[2]);
                saturations.add((Integer)(int)copy.get(y, x)[1]);
            }
        }
        copy.release();
        Collections.sort(values);
        Collections.sort(saturations);
        int medVal = values.get((int)(values.size()/2));
        int medSat = saturations.get((int)(saturations.size()/2));
        value = Math.max((int)(-0.65*medVal + 165.42), 0);
        saturation = Math.max((int)(1.19*medSat + 38.78), 0);
        System.out.println(value + ", " + saturation);
    }

    public Mat find_yellows(Mat input){
        Mat copy = copyHSV(input);
        Core.inRange(copy, new Scalar(7, saturation, value), new Scalar(30, 255, 255), threshold);
        return threshold;
    }

    public Mat find_blues(Mat input){
        Mat copy = copyHSV(input);
        Core.inRange(copy, new Scalar(110, saturation, value), new Scalar(125, 255, 255), threshold);
        return threshold;
    }
    public Mat find_reds(Mat input){
        Mat copy = copyHSV(input);
        Core.inRange(copy, new Scalar(0, saturation, value), new Scalar(11, 255, 255), threshold);
        return threshold;
    }

    public ArrayList<Rect> find_subcontours(Mat input, Rect main){
        subthreshold = find_yellows(input);
        Mat gray = new Mat();
        Mat grayblur = new Mat();
        Mat edgesX = new Mat();
        Mat edgesY = new Mat();
        Mat edges = new Mat();
        Mat sub = new Mat();
        Mat preFilter = input.clone();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(CURVE_EXTENSION, 1));

        ArrayList<Rect> output = new ArrayList<>();
        cvtColor(input, gray, COLOR_BGR2GRAY);
        Imgproc.blur(gray, grayblur, new Size(7,7));
        Imgproc.Sobel(grayblur, edgesX, -1, 0, 1);
        Imgproc.Sobel(grayblur, edgesY, -1, 1, 0);
        Core.add(edgesX, edgesY, edges);
        Core.subtract(subthreshold, edges, sub);
        Core.inRange(sub, new Scalar(245, 245, 245), new Scalar(255, 255, 255), sub);
        Imgproc.erode(sub, sub, kernel);
        List<MatOfPoint> contours = new ArrayList<>();


        Imgproc.findContours(sub, contours, new Mat(), Imgproc.CHAIN_APPROX_NONE, Imgproc.CHAIN_APPROX_SIMPLE);

        contours.removeIf(m -> {
            Rect rect = Imgproc.boundingRect(m);
            double r = (double) rect.height/rect.width;
            return ((r > DMAXR) || (r < DMINR) || (rect.width < main.width * 0.82));
        });

        for(MatOfPoint contour: contours){
            Rect rect = Imgproc.boundingRect(contour);
            Imgproc.rectangle(input, rect.tl(), rect.br(), new Scalar(0, 255, 255), 2);
            output.add(rect);
        }
        gray.release();
        grayblur.release();
        edgesX.release();
        edgesY.release();
        edges.release();
        sub.release();
        preFilter.release();

        return output;
    }

    public int closeIn(ArrayList<double[]> list, double x, double y, double width, double height, double epsilonW, double epsilonH){
        for(int i = 0; i< list.size(); i++){
            if(Math.abs(y - list.get(i)[4]) < epsilonH/2 && Math.abs(x - list.get(i)[1]) < epsilonW/2){
                return -2;
            }
            if(Math.abs(list.get(i)[1] - x) < epsilonW && Math.abs(list.get(i)[2] - width) < epsilonW && Math.abs(list.get(i)[5] - height) < epsilonH){
                return i;
            }
        }
        return -1;
    }

    public ArrayList<double[]> find_rings(Mat input){
        List<MatOfPoint> contours = new ArrayList<>();

        threshold = find_yellows(input);
        Imgproc.findContours(threshold, contours, new Mat(), Imgproc.CHAIN_APPROX_NONE, Imgproc.CHAIN_APPROX_SIMPLE);

        //initial filtering
        contours.removeIf(m -> {
            Rect rect = Imgproc.boundingRect(m);
            return (rect.area() < 1500) || (rect.height > rect.width);
        });

        //finding stacked rectangles and marking
        ArrayList<double[]> rectsData= new ArrayList<>();

        //sorting "rings" into stacks
        for(MatOfPoint contour:contours){
            Rect rect = Imgproc.boundingRect(contour);
            int newX = (int)Math.max(rect.x - (rect.width*0.3), 0);
            int newY = (int)Math.max(rect.y - (rect.height*0.1), 0);
            int newW = (int)Math.min((rect.width*1.3) + rect.x, input.width()) - newX;
            int newH = (int)Math.min((rect.height*1.1) + rect.y, input.height()) - newY;
            submat = new Mat(input.clone(), new Rect(newX, newY, newW, newH));
            ArrayList<Rect> moreRects = find_subcontours(submat, rect);
            for(Rect subrect: moreRects){
                double epsilonW = rect.width * 0.3;
                double epsilonH = rect.height * 0.3;
                subrect.x += newX;
                subrect.y += newY;
                int index = closeIn(rectsData, subrect.x, subrect.y, subrect.width, subrect.height, epsilonW, epsilonH);
                if(index == -2){
                    System.out.println("Overlap");
                }
                else if(index == -1){
                    rectsData.add(new double[6]);
                    rectsData.get(rectsData.size() - 1)[0] = 1;
                    rectsData.get(rectsData.size() - 1)[1] = subrect.x;
                    rectsData.get(rectsData.size() - 1)[2] = subrect.width;
                    rectsData.get(rectsData.size() - 1)[3] = subrect.y + subrect.height;
                    rectsData.get(rectsData.size() - 1)[4] = subrect.y;
                    rectsData.get(rectsData.size() - 1)[5] = subrect.height;
                }
                else{
                    double occ = rectsData.get(index)[0];
                    double avgX = rectsData.get(index)[1];
                    double avgW = rectsData.get(index)[2];
                    double maxY = rectsData.get(index)[3];
                    double minY = rectsData.get(index)[4];
                    double avgH = rectsData.get(index)[5];
                    rectsData.get(index)[0] += 1;
                    rectsData.get(index)[1] = (subrect.x + occ*avgX)/(occ + 1);
                    rectsData.get(index)[2] = (subrect.width + occ*avgW)/(occ + 1);
                    rectsData.get(index)[3] = Math.max(subrect.y + subrect.height, maxY);
                    rectsData.get(index)[4] = Math.min(subrect.y, minY);
                    rectsData.get(index)[5] = (subrect.height + occ*avgH)/(occ + 1);
                }
            }
        }
        //labeling found stacks
        threshold.release();
        return rectsData;
    }

    public Boolean wobble_stick(Mat input, MatOfPoint contour){
        Rect rect = Imgproc.boundingRect(contour);
        int newX = rect.x;
        int newY = (int)Math.max(rect.y - (rect.height*0.1), 0);
        int newW = rect.width;
        int newH = (int)Math.min((rect.height * 0.95) + newY, input.height()) - newY;
        submat = new Mat(input.clone(), new Rect(newX, newY, newW, newH));

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(submat, contours, new Mat(), Imgproc.CHAIN_APPROX_NONE, Imgproc.CHAIN_APPROX_SIMPLE);

        //initial filtering
        contours.removeIf(m -> {
            double height2 = Math.pow(Imgproc.boundingRect(m).height, 2);
            double area = contourArea(m);
            return ((area < 1000) || (area < height2/SMAXR) || (area > height2/SMINR));
        });

        return (contours.size() > 0);
    }

    public Rect find_wobble(Mat input, String side){
        List<MatOfPoint> contours = new ArrayList<>();
        if(side.equals("blue")){
            threshold = find_blues(input);
        }
        else if(side.equals("red")){
            threshold = find_reds(input);
        }
        else{
            System.out.println("ERROR: Not a valid side.");
        }
        Mat kernel = Imgproc.getStructuringElement(CV_SHAPE_ELLIPSE, new Size(7, 7));
        Mat kernelE = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(CURVE_EXTENSION, 1));
        Imgproc.dilate(threshold, threshold, kernel);
        Imgproc.erode(threshold, threshold, kernelE);
        Imgproc.findContours(threshold, contours, new Mat(), Imgproc.CHAIN_APPROX_NONE, Imgproc.CHAIN_APPROX_SIMPLE);

        //initial filtering
        contours.removeIf(m -> {
            Rect rect = Imgproc.boundingRect(m);
            double r = (double) rect.height/rect.width;
            return ((rect.area() < 1000) || (rect.width > rect.height)) || !wobble_stick(threshold, m);
        });

       MatOfPoint max = new MatOfPoint();
       double area = -1;
        for(MatOfPoint contour: contours){
            if(contourArea(contour) > area){
                area = contourArea(contour);
                max = contour;
            }
        }
        Rect maxRect = Imgproc.boundingRect(max);
        return new Rect(maxRect.x, maxRect.y, maxRect.width, (int)Math.round(maxRect.height*(28.0/24)));
    }

    public double find_Angle(Rect obj){
        double centerX = obj.x + (double)obj.width/2;
        double centerY = obj.y + obj.height;
        double y = pix2Y(centerY);
        double x = pix2RealX(centerX, centerY);
        double angle = Math.atan(y/x) - Math.PI/2;
        if(y/x < 0){
            angle += Math.PI;
        }
//        System.out.println("Angle: " + (Math.toDegrees(angle)));
//        System.out.println("Y: " + y);
//        System.out.println("X: " + x);
//        System.out.println();
        return Math.toDegrees(angle);
    }

    public double pix2Y(double pixY){
        double angle = (yP - pixY)/yP * viewAngle;
        return y0 * Math.tan(theta0 + angle) + x0;
    }

    public double pix2RealX(double pixX, double pixY){
        double y = pix2Y(pixY);
        double fullX = realX0 + y*slope;
        // System.out.println("real x: " + fullX);
        return (pixX - xP/2.0)/(xP) * fullX;
    }

    public Mat markRings(Mat input, ArrayList<double[]> rectsData){
        Mat copy = input.clone();
        for(double[] data: rectsData){
            if(data[0] > 0){
                Rect rect = new Rect((int) data[1], (int) data[4], (int) data[2], (int) (data[3] - data[4]));
                double Angle = find_Angle(rect);
                Imgproc.rectangle(copy, rect.tl(), rect.br(), new Scalar(255, 255, 0), 2);
                Imgproc.putText(copy, "" + (int)data[0], new Point(data[1] + data[2]/2, data[4]), Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 255, 0), 2);
                Imgproc.putText(copy, "Angle: " + (int)Angle, new Point(data[1] + data[2]/2, data[4] + 100), Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 255, 0), 2);

            }
        }
        return copy;
    }

    public Mat markWobble(Mat input, Rect rect){
        //labeling found wobble
        Mat copy = input.clone();
        double Angle = find_Angle(rect);
        Imgproc.rectangle(copy, rect.tl(), rect.br(), new Scalar(0, 255, 255), 2);
        Imgproc.putText(copy, "Wobble Goal", new Point(rect.x, rect.y -100), Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(255, 255, 0), 2);
        Imgproc.putText(copy, "Angle: " + (int)Angle, new Point(rect.x, rect.y -50 ), Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 0, 255), 2);
        return copy;
    }


}
