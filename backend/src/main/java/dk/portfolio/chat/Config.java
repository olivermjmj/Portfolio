package dk.portfolio.chat;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

record Config(int port, Path database, URI difyBase, String dataset, String difyKey,
              URI openaiBase, String openaiKey, URI portfolioBase, Set<String> origins,
              Set<String> trustedProxies, Duration timeout) {
    static final String MODEL = "gpt-6-luna";
    static final int MAX_INPUT = 6000, MAX_OUTPUT = 800;
    // nano-USD per token; standard short-context pricing, reviewed 2026-10-04.
    static final long INPUT_RATE = 100, CACHE_READ_RATE = 10, CACHE_WRITE_RATE = 125, OUTPUT_RATE = 500;
    static final long RESERVATION = MAX_INPUT * Math.max(INPUT_RATE, CACHE_WRITE_RATE) + MAX_OUTPUT * OUTPUT_RATE;
    static final long BUDGET = 4_900_000_000L;

    static Config fromEnv() {
        var env = System.getenv();
        var portfolio = https(env.getOrDefault("PORTFOLIO_BASE_URL", "https://olivermjmj.github.io/Portfolio/"));
        if (!portfolio.getPath().endsWith("/")) throw new IllegalArgumentException("PORTFOLIO_BASE_URL must end in /");
        var origins = csv(env.getOrDefault("ALLOWED_ORIGINS", "https://olivermjmj.github.io"));
        for (var origin : origins) {
            var uri = URI.create(origin);
            if (!origin.equals(uri.getScheme() + "://" + uri.getRawAuthority()) ||
                    !("https".equals(uri.getScheme()) || ("http".equals(uri.getScheme()) && Set.of("localhost", "127.0.0.1").contains(uri.getHost()))))
                throw new IllegalArgumentException("ALLOWED_ORIGINS must contain exact HTTPS origins (or local HTTP)");
        }
        String dataset = required(env, "DIFY_DATASET_ID");
        UUID.fromString(dataset);
        return new Config(Integer.parseInt(env.getOrDefault("PORT", "8080")),
                Path.of(env.getOrDefault("BUDGET_DB", "data/budget.db")),
                https(env.getOrDefault("DIFY_BASE_URL", "https://api.dify.ai/v1")), dataset,
                required(env, "DIFY_API_KEY"), URI.create("https://api.openai.com/v1"), required(env, "OPENAI_API_KEY"),
                portfolio, origins, csv(env.getOrDefault("TRUSTED_PROXY_IPS", "")), Duration.ofSeconds(35));
    }
    private static URI https(String value) {
        URI uri = URI.create(value);
        if (!"https".equals(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
            throw new IllegalArgumentException("Expected an HTTPS URL without credentials, query or fragment");
        return uri;
    }
    private static String required(Map<String, String> env, String key) {
        String value = env.getOrDefault(key, "").trim();
        if (value.isEmpty() || value.startsWith("replace-")) throw new IllegalArgumentException("Missing configuration: " + key);
        return value;
    }
    private static Set<String> csv(String value) {
        var set = new HashSet<String>();
        for (String part : value.split(",")) if (!part.isBlank()) set.add(part.trim());
        return Set.copyOf(set);
    }
}
