package com.example.grip;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {

    //Parameter used in the app
    //Clear Button
    private Button bt;
    //Main textview
    private TextView tv;
    //Steps from system build-in sensor, not directly used in the app
    private int Osysstep=0;
    private int Rsysstep=0;
    //Gait Length; Gait Speed; Total distance and the Gait Symmetry
    private float Length=0;
    private float Speed=0;
    private float Distance=0;
    private boolean Symmetry=false;
    private String Symmetrystring;
    //Standard data form to keep two decimals
    private java.text.DecimalFormat df=new java.text.DecimalFormat("0.00");
    //Firebase reference setting
    private DatabaseReference mDatabaseReference = FirebaseDatabase.getInstance().getReference();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*The initial Setting for Sensor Controller class
        The Accelerator sensor is used in the gait calculation, I also used the android build-in
        step counter to compare with my calculation.
        */
        sensorController=new SensorController((SensorManager)getSystemService(Context.SENSOR_SERVICE),sensorCallback);
        sensorController.registerSensor(Sensor.TYPE_ACCELEROMETER,SensorManager.SENSOR_DELAY_UI);
        sensorController.registerSensor(Sensor.TYPE_STEP_COUNTER,SensorManager.SENSOR_DELAY_UI);
        stepController=new StepController(stepCallback);

        /* Firebase Initial Setting
         I set four parameter of Total Distance, Gait Length, Gait Speed, Gait Symmetry to store in
         the child "User Gait" in the Firebase.
        */
        mDatabaseReference = FirebaseDatabase.getInstance().getReference().child("User Gait");
        Gait UserGait= new Gait("1.16m","42.83cm",
                "0.50m/s","You are very healthy!");
        mDatabaseReference.setValue(UserGait);

        //TextView Initial Setting
        tv=findViewById(R.id.textView);
        tv.setText("\nStep:"+"0"+"\nTotal Distance:"+"0m"+
                "\nGrip Length:"+"0cm"+ "\nSpeed:"+"0m/s"
                +"\nGait Symmetry:"+"Wait for your walking");

        //Initial Setting for CLEAR Button
        bt=findViewById(R.id.button);
        bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stepController.Step=0;
                stepController.value[0]=0;
                stepController.value[1]=0;
                Osysstep=Rsysstep;
                Length=0;
                Distance=0;
                Speed=0;
                Symmetry=false;
                GetSymmetryString(Symmetry);
                tv.setText("\nStep:"+stepController.Step+"\nTotal Distance:"+df.format(Distance)+"m"+
                        "\nGrip Length:"+df.format(Length)+"cm"+"\nSpeed:"+df.format(Speed)+"m/s"
                        +"\nGait Symmetry:"+"Wait for your walking");
            }
        });
    }

    //Responde for Sensor Event
    private SensorController sensorController;
    private SensorController.SensorCallback sensorCallback=new SensorController.SensorCallback() {
        // Accelerator Sensor Event responde
        @Override
        public void refreshAcc(float[] accs) {
            if (stepController!=null){
                stepController.refreshAcc(accs,System.currentTimeMillis());
            }

        }

        // System Build-in step controller Sensor Event responde
        /* The reason I didn't directly use the step-counter sensor is that time between the two
        step is not accessible, and the gait length as well as speed couldn't be calculated. So I
        only use it as a reference. If you want to see the step-controller data, replace the TextView
        TV setting with the annotated one.
        */
        @Override
        public void refreshStep(int step) {
            /* Rsystep is the current total step since your device starting up, Osyystep is the
            initial step, the difference between this two data is the step you starting up the app
             */
            Rsysstep=step;
            if (Osysstep==0){
                Osysstep=step;
            }else{
                /*
                tv.setText("System Step:"+(Rsysstep-Osysstep)+ "\nCalculated Step:"+stepController.Step+
                        "\nT:"+stepController.value[0]+"\nH:"+stepController.value[1]+"\nTotal Distance:
                        "+df.format(Distance)+"m"+"\nGrip Length:"+df.format(Length)+"cm"+"\nSpeed:"
                        +df.format(Speed)+"m/s"+"\nGait Symmetry:"+Symmetrystring);
                */
                tv.setText("\nStep:"+stepController.Step+"\nTotal Distance:"+df.format(Distance)+"m"+
                        "\nGrip Length:"+df.format(Length)+"cm"+"\nSpeed:"+df.format(Speed)+"m/s"
                        +"\nGait Symmetry:"+Symmetrystring);
            }
        }
    };

    //Receive the Gait data from the StepController
    private StepController stepController;
    private StepController.StepCallback stepCallback=new StepController.StepCallback() {
        @Override
        public void refreshStep(int step, float stepLength, float distance, float speed, boolean symmetry) {
            /*The StepController will callback with five Gait data:Step number;
            StepLength; Total distance Gait Speed and Gait Symmetry
            */
            //Simple math calculation to set the standard units
            Length=stepLength*100;
            Speed=speed*1000;
            Distance=distance;
            GetSymmetryString(Symmetry);
            // Set the data needed for Firebase and upload
            Gait UserGait= new Gait(df.format(Distance)+"m",df.format(Length)
                    +"cm",df.format(Speed)+"m/s",Symmetrystring);
            mDatabaseReference.setValue(UserGait);
            /*
            View all the data used in my app:
            T(stepController.value[0]):The time between two valley of the instantaneous acceleration curve;
            H(stepController.value[1]):The height of the peak of instantaneous acceleration curve;

            tv.setText("System Step:"+(Rsysstep-Osysstep)+ "\nCalculated Step:"+stepController.Step+
                        "\nT:"+stepController.value[0]+"\nH:"+stepController.value[1]+"\nTotal Distance:
                        "+df.format(Distance)+"m"+"\nGrip Length:"+df.format(Length)+"cm"+"\nSpeed:"
                        +df.format(Speed)+"m/s"+"\nGait Symmetry:"+Symmetrystring);
                */
            tv.setText("\nStep:"+stepController.Step+"\nTotal Distance:"+df.format(Distance)+"m"+
                    "\nGrip Length:"+df.format(Length)+"cm"+"\nSpeed:"+df.format(Speed)+"m/s"
                    +"\nGait Symmetry:"+Symmetrystring);
        }

    };
    // Get the SymmetryString for textview display
    private void GetSymmetryString(Boolean Sym){
        if (Sym) Symmetrystring = "Your gaits are probably asymmetry, please seek help from doctor.";
            else Symmetrystring = "You are very healthy!";
    }

    // Setting of Gait class for firebase upload
    public class Gait{
        //Four Gait data needed to upload
        private String TotalDistance;
        private String GaitLength;
        private String GaitSpeed;
        private String GaitSymmetry;
        private DatabaseReference mDatabaseReference = FirebaseDatabase.getInstance().getReference();
        public Gait(){
        }

        public Gait(String TotalDistance, String GaitLength, String GaitSpeed, String GaitSymmetry){
            this.TotalDistance=TotalDistance;
            this.GaitLength=GaitLength;
            this.GaitSpeed=GaitSpeed;
            this.GaitSymmetry=GaitSymmetry;
        }

        public String getTotalDistance(){
            return TotalDistance;
        }

        public void setTotalDistance(String TotalDistance){
            this.TotalDistance=TotalDistance;
        }

        public String getGaitLength(){
            return GaitLength;
        }

        public void setGaitLength(String GaitLength){
            this.GaitLength=GaitLength;
        }
        public String getGaitSpeed(){
            return GaitSpeed;
        }

        public void setGaitSpeed(String GaitSpeed){
            this.GaitLength=GaitSpeed;
        }
        public String getGaitSymmetry(){
            return GaitSymmetry;
        }

        public void setGaitSymmetry(String GaitLength){
            this.GaitLength=GaitSymmetry;
        }
    }
}
