import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Calculations {

    /* y
       ^  right 2          right 1
       |
       |  left 2          left  1
       ~ -  - > x
     */

    static int rightFrontI = 0;
    static int leftFrontI = 1;

    static int rightBackI = 2;
    static int leftBackI = 3;

    static double motorVelocitys[] = new double[]{0.0, 0.0, 0.0, 0.0}; // just filler values

    //turning variables
    static double maxVel = 245; //TODO not the real measurement

    public static void setMotorVelocities(double forward, double strafe, double turn) {

        motorVelocitys[rightFrontI] = (forward - strafe - turn);
        motorVelocitys[leftFrontI] = (forward + strafe + turn);

        motorVelocitys[rightBackI] = (forward + strafe - turn);
        motorVelocitys[leftBackI] = (forward - strafe + turn);

        normalize();
    }

  public static void normalize(){
        double desiredMaxVel = getMax(motorVelocitys);
      // if the desired max Velocity is less than the max velocity then it is still in valid range
        if (desiredMaxVel > maxVel){
            double normalizeFactor = maxVel/desiredMaxVel;
            for (int i; i < motorVelocitys.length; i++){
                motorVelocitys[i] *= normalizeFactor;
            }
        }
  }

  public static double getMax(double[] listOfValues){
        double maxValue = 0;
        for (int i; i < listOfValues.length; i++){
            if(maxValue < listOfValues[i]){
                maxValue = listOfValues[i];
            }
        }
        return maxValue;
  }

}

