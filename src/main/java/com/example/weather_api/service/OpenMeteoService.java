package com.example.weather_api.service;


import com.example.weather_api.model.CurrentWeather;
import com.example.weather_api.model.DailyForecast;
import com.example.weather_api.model.Forecast;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class OpenMeteoService {

    private final WebClient webClient = WebClient.create("https://api.open-meteo.com/v1");

    public Mono<CurrentWeather> getCurrentWeather(double lat, double lon) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/forecast")
                        .queryParam("latitude", lat)
                        .queryParam("longitude", lon)
                        .queryParam("current", "temperature_2m,relative_humidity_2m,weather_code")
                        .queryParam("timezone", "auto")
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::parseCurrentFromMeteo);
    }

    public Mono<Forecast> getForecast(double lat, double lon, int days) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/forecast")
                        .queryParam("latitude", lat)
                        .queryParam("longitude", lon)
                        .queryParam("daily", "temperature_2m_max,temperature_2m_min,precipitation_sum,weather_code")
                        .queryParam("forecast_days", days)
                        .queryParam("timezone", "auto")
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(node -> parseForecastFromMeteo(node, days));
    }

    private CurrentWeather parseCurrentFromMeteo(JsonNode node) {
        JsonNode current = node.path("current");
        CurrentWeather weather = new CurrentWeather();
        weather.setTemperature(current.path("temperature_2m").asDouble());
        weather.setHumidity(current.path("relative_humidity_2m").asDouble());
        weather.setConditions(getWmoDescription(current.path("weather_code").asInt()));
        return weather;
    }

    private Forecast parseForecastFromMeteo(JsonNode node, int days) {
        JsonNode daily = node.path("daily");
        Forecast forecast = new Forecast();
        List<DailyForecast> dailyForecasts = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            com.example.weather_api.model.DailyForecast df = new com.example.weather_api.model.DailyForecast();
            df.setDate(daily.path("time").get(i).asText());
            df.setMaxTemp(daily.path("temperature_2m_max").get(i).asDouble());
            df.setMinTemp(daily.path("temperature_2m_min").get(i).asDouble());
            df.setPrecipitation(daily.path("precipitation_sum").get(i).asDouble());
            df.setConditions(getWmoDescription(daily.path("weather_code").get(i).asInt()));
            dailyForecasts.add(df);
        }
        forecast.setDays(dailyForecasts);
        return forecast;
    }

    // WMO code to description map (based on standard WMO 4677 from Gist and docs)
    private String getWmoDescription(int code) {
        return switch (code) {
            case 0 -> "Clear sky";
            case 1 -> "Mainly clear";
            case 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45 -> "Fog";
            case 48 -> "Depositing rime fog";
            case 51 -> "Light drizzle";
            case 53 -> "Moderate drizzle";
            case 55 -> "Dense drizzle";
            case 61 -> "Slight rain";
            case 63 -> "Moderate rain";
            case 65 -> "Heavy rain";
            case 71 -> "Slight snow fall";
            case 73 -> "Moderate snow fall";
            case 75 -> "Heavy snow fall";
            case 80 -> "Slight rain showers";
            case 81 -> "Moderate rain showers";
            case 82 -> "Violent rain showers";
            case 95 -> "Thunderstorm";
            // Add more as needed; fallback
            default -> "Unknown (" + code + ")";
        };
    }
}


