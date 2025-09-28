package com.example.weather_api.service;


import com.example.weather_api.model.CurrentWeather;
import com.example.weather_api.model.Forecast;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class OpenWeatherMapService {

    private final WebClient webClient;
    private final String apiKey;

    public OpenWeatherMapService(@Value("${openweathermap.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.webClient = WebClient.create("https://api.openweathermap.org/data/2.5");
    }

    public Mono<CurrentWeather> getCurrentWeather(double lat, double lon) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/weather")
                        .queryParam("lat", lat)
                        .queryParam("lon", lon)
                        .queryParam("units", "metric")
                        .queryParam("appid", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::parseCurrentFromOWM);
    }

    public Mono<Forecast> getForecast(double lat, double lon, int days) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/forecast")
                        .queryParam("lat", lat)
                        .queryParam("lon", lon)
                        .queryParam("units", "metric")
                        .queryParam("appid", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(node -> parseForecastFromOWM(node, days));
    }

    private CurrentWeather parseCurrentFromOWM(JsonNode node) {
        CurrentWeather weather = new CurrentWeather();
        JsonNode main = node.path("main");
        JsonNode weatherArray = node.path("weather").get(0);
        weather.setTemperature(main.path("temp").asDouble());
        weather.setHumidity(main.path("humidity").asDouble());
        weather.setConditions(weatherArray.path("description").asText());
        return weather;
    }

    private Forecast parseForecastFromOWM(JsonNode node, int days) {
        JsonNode list = node.path("list");
        Forecast forecast = new Forecast();
        List<com.example.weather_api.model.DailyForecast> dailyForecasts = new ArrayList<>();
        // OWM forecast is 3-hourly; aggregate to daily (simple: take first per day)
        LocalDate currentDate = null;
        com.example.weather_api.model.DailyForecast df = null;
        int count = 0;
        for (JsonNode item : list) {
            LocalDate date = Instant.ofEpochSecond(item.path("dt").asLong()).atZone(ZoneId.systemDefault()).toLocalDate();
            if (!date.equals(currentDate)) {
                if (df != null) {
                    dailyForecasts.add(df);
                }
                if (++count > days) break;
                currentDate = date;
                df = new com.example.weather_api.model.DailyForecast();
                df.setDate(date.toString());
            }
            JsonNode main = item.path("main");
            if (df.getMaxTemp() < main.path("temp_max").asDouble()) df.setMaxTemp(main.path("temp_max").asDouble());
            if (df.getMinTemp() == 0 || df.getMinTemp() > main.path("temp_min").asDouble()) df.setMinTemp(main.path("temp_min").asDouble());
            df.setPrecipitation(df.getPrecipitation() + item.path("rain").path("3h").asDouble(0));
            df.setConditions(item.path("weather").get(0).path("description").asText());
        }
        if (df != null) dailyForecasts.add(df);
        forecast.setDays(dailyForecasts);
        return forecast;
    }
}