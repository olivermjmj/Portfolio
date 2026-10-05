package dk.portfolio.chat;

import com.fasterxml.jackson.databind.*;

final class Json {
    static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    static String write(Object value) {
        try { return MAPPER.writeValueAsString(value); }
        catch (Exception e) { throw new IllegalStateException("JSON serialization failed"); }
    }
    static JsonNode read(String value) {
        try { return MAPPER.readTree(value); }
        catch (Exception e) { throw new ApiError(400, "invalid_json", "Beskeden er ikke gyldig JSON."); }
    }
}
