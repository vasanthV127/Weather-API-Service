package com.example.weather_api.model;

import lombok.Data;

import java.util.List;

@Data
public class Forecast {
    private List<DailyForecast> days;
}
