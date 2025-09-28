package com.example.weather_api.service;

import com.example.weather_api.exception.RateLimitException;
import com.example.weather_api.model.CurrentWeather;
import com.example.weather_api.model.Forecast;
import com.example.weather_api.model.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class WeatherAggregationService {

    private static final Logger log = LoggerFactory.getLogger(WeatherAggregationService.class);

    private final GeocodingService geocodingService;
    private final OpenMeteoService meteoService;
    private final OpenWeatherMapService owmService;

    @Autowired
    public WeatherAggregationService(GeocodingService geocodingService, OpenMeteoService meteoService, OpenWeatherMapService owmService) {
        this.geocodingService = geocodingService;
        this.meteoService = meteoService;
        this.owmService = owmService;
    }

    @Cacheable(value = "currentWeather", key = "#location")
    public CurrentWeather getAggregatedCurrent(String location) {
        log.debug("Cache miss for location: {}", location); // Before API calls
        Location loc = getFirstLocation(location);
        if (loc == null) {
            throw new RuntimeException("Location not found");
        }

        CompletableFuture<CurrentWeather> meteoFuture = CompletableFuture.supplyAsync(() ->
                meteoService.getCurrentWeather(loc.getLatitude(), loc.getLongitude()).block(Duration.ofSeconds(5)));

        CompletableFuture<CurrentWeather> owmFuture = CompletableFuture.supplyAsync(() ->
                owmService.getCurrentWeather(loc.getLatitude(), loc.getLongitude()).block(Duration.ofSeconds(5)));

        try {
            CurrentWeather meteo = meteoFuture.get();
            CurrentWeather owm = owmFuture.get();

            CurrentWeather aggregated = new CurrentWeather();
            aggregated.setTemperature((meteo.getTemperature() + owm.getTemperature()) / 2);
            aggregated.setHumidity((meteo.getHumidity() + owm.getHumidity()) / 2);
            aggregated.setConditions(owm.getConditions());  // Prefer OWM string description
            log.debug("Cache hit or new entry for location: {}", location); // After aggregation
            log.debug("Aggregated current weather for {}: {}", location, aggregated);
            return aggregated;
        } catch (Exception e) {
            log.error("Error aggregating current weather", e);
            throw new RuntimeException("Failed to fetch weather data");
        }
    }

    @Cacheable(value = "forecast", key = "#location + #days")
    public Forecast getAggregatedForecast(String location, int days) {
        Location loc = getFirstLocation(location);
        if (loc == null) {
            throw new RuntimeException("Location not found");
        }

        CompletableFuture<Forecast> meteoFuture = CompletableFuture.supplyAsync(() ->
                meteoService.getForecast(loc.getLatitude(), loc.getLongitude(), days).block(Duration.ofSeconds(5)));

        CompletableFuture<Forecast> owmFuture = CompletableFuture.supplyAsync(() ->
                owmService.getForecast(loc.getLatitude(), loc.getLongitude(), days).block(Duration.ofSeconds(5)));

        try {
            Forecast meteo = meteoFuture.get();
            Forecast owm = owmFuture.get();

            Forecast aggregated = new Forecast();
            List<com.example.weather_api.model.DailyForecast> aggDays = new ArrayList<>();
            for (int i = 0; i < days; i++) {
                com.example.weather_api.model.DailyForecast mDf = meteo.getDays().get(i);
                com.example.weather_api.model.DailyForecast oDf = owm.getDays().get(i);

                com.example.weather_api.model.DailyForecast aggDf = new com.example.weather_api.model.DailyForecast();
                aggDf.setDate(mDf.getDate());
                aggDf.setMaxTemp((mDf.getMaxTemp() + oDf.getMaxTemp()) / 2);
                aggDf.setMinTemp((mDf.getMinTemp() + oDf.getMinTemp()) / 2);
                aggDf.setPrecipitation((mDf.getPrecipitation() + oDf.getPrecipitation()) / 2);
                aggDf.setConditions(oDf.getConditions());  // Prefer OWM
                aggDays.add(aggDf);
            }
            aggregated.setDays(aggDays);
            log.debug("Aggregated forecast for {} ({} days): {}", location, days, aggregated);
            return aggregated;
        } catch (Exception e) {
            log.error("Error aggregating forecast", e);
            throw new RuntimeException("Failed to fetch forecast data");
        }
    }

    public List<Location> searchLocations(String query) {
        return geocodingService.search(query);
    }

    private Location getFirstLocation(String location) {
        List<Location> locations = searchLocations(location);
        return locations.isEmpty() ? null : locations.get(0);
    }
}