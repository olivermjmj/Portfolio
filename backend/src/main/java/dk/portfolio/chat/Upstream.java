package dk.portfolio.chat;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;

final class Upstream {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NEVER).version(HttpClient.Version.HTTP_1_1).build();
    JsonNode post(URI base, String path, String key, Object payload, Duration timeout) {
        var request = HttpRequest.newBuilder(URI.create(base.toString().replaceAll("/+$", "") + path))
                .timeout(timeout).header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json").header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(Json.write(payload))).build();
        try {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300 || response.body().length() > 2_000_000)
                throw ApiError.unavailable();
            return Json.MAPPER.readTree(response.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); throw ApiError.unavailable();
        } catch (Exception e) { throw ApiError.unavailable(); }
    }
}
