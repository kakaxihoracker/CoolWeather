package com.example.horacker.coolweather.gson;

/**
 * Created by horacker on 2017/7/5.
 */

public class AQI {

    public AQICity city;

    public class AQICity {
        public String aqi;
        public String pm25;
    }
}
