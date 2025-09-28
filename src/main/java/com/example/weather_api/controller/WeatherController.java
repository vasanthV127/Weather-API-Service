package com.example.weather_api.controller;



import com.example.weather_api.model.CurrentWeather;
import com.example.weather_api.model.Forecast;
import com.example.weather_api.model.Location;
import com.example.weather_api.service.WeatherAggregationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/weather")
public class WeatherController {

    private final WeatherAggregationService aggregationService;

    @Autowired
    public WeatherController(WeatherAggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @Operation(summary = "Get current weather for a location")
    @ApiResponse(responseCode = "200", description = "Current weather data")
    @GetMapping("/current")
    public CurrentWeather getCurrent(@RequestParam String location) {
        return aggregationService.getAggregatedCurrent(location);
    }

    @Operation(summary = "Get forecast for a location")
    @ApiResponse(responseCode = "200", description = "Forecast data")
    @GetMapping("/forecast")
    public Forecast getForecast(@RequestParam String location, @RequestParam(defaultValue = "5") int days) {
        return aggregationService.getAggregatedForecast(location, days);
    }

    @Operation(summary = "Search for locations")
    @ApiResponse(responseCode = "200", description = "List of matching locations")
    @GetMapping("/locations/search")
    public List<Location> searchLocations(@RequestParam String q) {
        return aggregationService.searchLocations(q);
    }
}