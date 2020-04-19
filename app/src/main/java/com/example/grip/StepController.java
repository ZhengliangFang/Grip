package com.example.grip;

import android.util.Log;

public class StepController {

    //Walking state constant
    public static int STAY=0;
    public static int MOVE=1;

    //State judging array
    private float[] stateValue=new float[20];

    //Acceleration data array
    private float[] Accs=new float[100];

    //The time that acceleration sensor calls
    private long[] time=new long[100];

    //Array count
    private int count=0;

    //Gait step
    public int Step=0;

    //Gait length
    private float Length=0;

    //Total distance
    private float Distance=0;

    //State variable
    private int State=STAY;

    //Array for T and H
    //T(stepController.value[0]):The time between two valley of the instantaneous acceleration curve;
    //H(stepController.value[1]):The height of the peak of instantaneous acceleration curve;
    public float[] value=new float[2];

    //Gait speed
    private float Speed=0;

    //Length data array
    private float[] Lengthdata = new float[10];

    //Symmetry counter
    private int judge=0;

    //Gait Symmetry
    private boolean Symmetry;

    //Callback to mainactivity
    //Callback interface
    public interface StepCallback{
        void refreshStep(int step,float stepLength,float distance, float speed,
                         boolean symmetry);
    }
    //Callback setting
    private StepCallback callback;
    public StepController(StepCallback callback){
        this.callback=callback;
    }

    //Calculating the gait data when acceleration sensor receives data
    public void refreshAcc(float[] values,long timestamp){
        //Receive acceleration time and time data from acceleration sensor
        Accs[count]=(float) Math.sqrt(values[0]*values[0]+
                values[1]*values[1]+
                values[2]*values[2]);
        time[count]=timestamp;

        //Check the walking state
        stateValue[count%20]=Accs[count];
        checkState();

        //Checkpoint for decreasing calculating
        final int ckpoint = Accs.length / 5;

        //Judge the walking state and ensure the valley in the acceleration curve
        if (State==MOVE&&Accs[(count-ckpoint+Accs.length)%Accs.length]<Accs[(count-ckpoint+Accs.length+1)%Accs.length]
                &&Accs[(count-ckpoint+Accs.length)%Accs.length]<Accs[(count-ckpoint+Accs.length-1)%Accs.length]) {
            //Calculate the average
            float ave = Utils.ave(Accs);
            for (int i = 0; i < Accs.length; ++i) {
                ave += Accs[i];
            }
            ave /= Accs.length;

            float[] data = new float[Accs.length];
            for (int i = 0, j = count; i < data.length; ++i, --j) {
                if (j < 0) j += data.length;
                data[i] = Accs[j] - ave;
            }

            //Search the adjacent valley point and peak point in the acceleration curve
            float[] sign = new float[Accs.length];
            for (int i = 1; i < Accs.length - 1; i++) {
                if (Math.abs(data[i]) > 0.8 && Math.abs(2 * data[i]) > Math.abs((data[i - 1] + data[i + 1]))) {
                    if (data[i] > 0) {
                        sign[i] = 1;
                    } else {
                        sign[i] = -1;
                    }
                }
            }

            //Find the Maximum peak point and minimum valley point and save in the array
            for (int i = 1; i < sign.length - 1; ) {
                int index = i;
                while (++i < sign.length - 1 && (sign[i] == 0 || sign[i] == sign[index])) {
                    if (sign[i] != 0 && Math.abs(data[i]) > Math.abs(data[index])) {
                        sign[index] = 0;
                        index = i;
                    } else {
                        sign[i] = 0;
                    }
                }
            }

            //Judge the valley point
            if (sign[ckpoint] < 0) {
                int index = ckpoint;

                //Search next Peak point
                while (++index < sign.length && sign[index] == 0) ;

                if (index < sign.length && sign[index] > 0) {
                    int peak = index;

                    //Search next valley point
                    while (++index < sign.length && sign[index] == 0) ;

                    if (index < sign.length && sign[index] < 0) {
                        int valley = index;

                        //Calculate H and T
                        float H = data[peak] - 0.5f * data[ckpoint] - 0.5f * data[valley];
                        long T = time[(count - ckpoint + time.length) % time.length] - time[(count - valley + time.length) % time.length];

                        Log.e("StepController","H:"+H+",T:"+T);
                        //Judge the value of H and T, eliminate the small acceleration value because of the waggle or shake
                        if (H > 3 && T > 200 && T < 1400) {
                            //Calculation of the Gait data
                            DetectStepLength((int) T, H);
                            value[0]=value[0]*Step+T;
                            value[1]=value[1]*Step+H;
                            ++Step;
                            value[0]/=Step;
                            value[1]/=Step;
                            Speed=Length/T;
                            SymmetryJudge(Length);
                            //Send the gait data back to mainactivity
                            callback.refreshStep(Step,Length,Distance,Speed,Symmetry);
                        }
                    }
                }
            }
        }

        //Reset the count
        if (++count==Accs.length) count=0;
    }

    //Walking state judging
    private void checkState(){
        float var=Utils.var(stateValue,Utils.ave(stateValue));
        State=var>0.5?MOVE:STAY;
    }

    //Gait length calculation using the least square method
    private void DetectStepLength(int time,float f){
        float steplength=0.35f-0.000155f*time+0.1638f*(float) Math.sqrt(f);
        this.Length=(this.Length+steplength)/2;
        Distance+=steplength;
    }

    //Gait symmetry judging
    private void SymmetryJudge(float L){
        //Save the gait length data in the array
        Lengthdata[judge]=L;
        //Reset the count
        judge++;
        if (judge == 10)  judge=0;
        //Calculate the left foot average gait length and right foot average gait length
        float left = (Lengthdata[0]+Lengthdata[2]+Lengthdata[4]+Lengthdata[6]+Lengthdata[8])/5;
        float right = (Lengthdata[1]+Lengthdata[3]+Lengthdata[5]+Lengthdata[7]+Lengthdata[9])/5;
        //If the difference between two feet length is larger than 5cm, the gait symmetry is not good
        if (Math.abs(left-right)>0.05) Symmetry = true;
        else Symmetry = false;
    }
}