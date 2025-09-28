package com.example.weather_api.model;

import lombok.Data;

@Data
public class CurrentWeather {
    private double temperature;
    private double humidity;
    private String conditions;
    // Add more fields as needed, e.g., windSpeed
}