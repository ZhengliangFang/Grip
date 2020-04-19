package com.example.grip;

//Calculation class
public class Utils {
    //State judge calculation
    public static float var(float[] value,float ave){
        float re=0;
        for (int i=0;i<value.length;i++){
            re+=(value[i]-ave)*(value[i]-ave);
        }
        return re;
    }

    //Average calculation
    public static float ave(float[] value){
        float re=0;
        for (int i=0;i<value.length;++i){
            re+=value[i];
        }
        return re/value.length;
    }
}