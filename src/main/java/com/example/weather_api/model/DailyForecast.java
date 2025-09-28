package com.example.weather_api.model;

import lombok.Data;

@Data
public class DailyForecast {
    private String date;
    private double maxTemp;
    private double minTemp;
    private double precipitation;
    private String conditions;
}
