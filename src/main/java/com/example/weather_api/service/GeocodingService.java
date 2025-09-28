package com.example.weather_api.service;

import com.example.weather_api.model.Location;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class GeocodingService {

    private final WebClient webClient;
    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeocodingService(@Value("${openweathermap.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.webClient = WebClient.create("https://api.openweathermap.org/geo/1.0");
    }

    public List<Location> search(String query) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/direct")
                        .queryParam("q", query)
                        .queryParam("limit", 10)
                        .queryParam("appid", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::parseLocations)
                .onErrorResume(e -> Mono.just(new ArrayList<>()))
                .block();  // Block for simplicity; in prod, keep reactive
    }

    private List<Location> parseLocations(JsonNode node) {
        List<Location> locations = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode locNode : node) {
                Location loc = new Location();
                loc.setName(locNode.path("name").asText());
                loc.setLatitude(locNode.path("lat").asDouble());
                loc.setLongitude(locNode.path("lon").asDouble());
                loc.setCountry(locNode.path("country").asText());
                locations.add(loc);
            }
        }
        return locations;
    }
}