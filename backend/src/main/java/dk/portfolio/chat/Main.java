package dk.portfolio.chat;

import io.javalin.Javalin;
import io.javalin.http.Context;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.net.InetAddress;
import java.util.*;
import org.slf4j.LoggerFactory;

public final class Main {
    public static void main(String[] args) throws Exception {
        System.setProperty("jdk.httpclient.disableRetryConnect", "true");
        if (args.length == 1 && args[0].equals("--healthcheck")) {
            var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:8080/health"))
                    .timeout(java.time.Duration.ofSeconds(4)).build();
            var response = java.net.http.HttpClient.newHttpClient().send(request, java.net.http.HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() != 200) System.exit(1);
            return;
        }
        if (args.length == 1 && args[0].equals("--init-budget")) {
            Ledger.initialize(Path.of(System.getenv().getOrDefault("BUDGET_DB", "data/budget.db")), Config.BUDGET);
            System.out.println("Budget initialized at USD 4.90. Preserve this database and its volume.");
            return;
        }
        Config config = Config.fromEnv();
        // One process per budget volume ensures the two-call semaphore is global for this deployment.
        try (var channel = FileChannel.open(config.database().resolveSibling("chat.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = channel.tryLock()) {
            if (lock == null) throw new IllegalStateException("Another chatbot instance already uses this volume");
            var app = create(config, new Ledger(config.database())).start(config.port());
            Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
            new java.util.concurrent.CountDownLatch(1).await();
        }
    }
    static Javalin create(Config config, Ledger ledger) {
        var service = new ChatService(config, ledger);
        return Javalin.create(c -> {
            c.http.maxRequestSize = 32_768;
            c.routes.before(ctx -> {
                ctx.attribute("started", System.nanoTime());
                ctx.header("Cache-Control", "no-store").header("X-Content-Type-Options", "nosniff");
                if (ctx.path().equals("/api/chat")) {
                    String origin = ctx.header("Origin");
                    if (origin == null || !config.origins().contains(origin))
                        throw new ApiError(403, "origin_denied", "Denne hjemmeside har ikke adgang til chatten.");
                    ctx.header("Access-Control-Allow-Origin", origin).header("Vary", "Origin");
                }
            });
            c.routes.options("/api/chat", ctx -> ctx.header("Access-Control-Allow-Methods", "POST, OPTIONS")
                    .header("Access-Control-Allow-Headers", "Content-Type").header("Access-Control-Max-Age", "600").status(204));
            c.routes.get("/health", ctx -> { ledger.health(); respond(ctx, Map.of("status", "ok")); });
            c.routes.post("/api/chat", ctx -> {
                if (ctx.contentType() == null || !ctx.contentType().split(";")[0].trim().equalsIgnoreCase("application/json"))
                    throw new ApiError(415, "invalid_content_type", "Brug application/json.");
                var request = ChatService.parse(Json.read(ctx.body()));
                respond(ctx, service.chat(request, clientIp(ctx, config)));
            });
            c.routes.exception(ApiError.class, (e, ctx) -> {
                if (e.status == 429) ctx.header("Retry-After", "60");
                ctx.status(e.status); respond(ctx, Map.of("error", e.code, "message", e.getMessage()));
            });
            c.routes.exception(Exception.class, (e, ctx) -> {
                LoggerFactory.getLogger(Main.class).warn("request_failed type={}", e.getClass().getSimpleName());
                ctx.status(503); respond(ctx, Map.of("error", "unavailable", "message", "Chatten er midlertidigt utilgængelig."));
            });
            c.routes.after(ctx -> {
                Long started = ctx.attribute("started");
                if (ctx.path().equals("/api/chat") && started != null)
                    LoggerFactory.getLogger(Main.class).info("chat status={} elapsed_ms={}", ctx.status(), (System.nanoTime() - started) / 1_000_000);
            });
        });
    }
    private static void respond(Context ctx, Object body) { ctx.contentType("application/json; charset=utf-8").result(Json.write(body)); }
    private static String clientIp(Context ctx, Config config) {
        // Never use ctx.ip()/arbitrary forwarded headers: only an explicitly trusted proxy may supply this header.
        String peer = ctx.req().getRemoteAddr();
        String candidate = config.trustedProxies().contains(peer) ? ctx.header("X-Real-IP") : peer;
        if (candidate == null || candidate.length() > 64 || !candidate.matches("[0-9a-fA-F:.]+")) throw new ApiError(400, "invalid_ip", "Ugyldig klientadresse.");
        try { return InetAddress.getByName(candidate).getHostAddress(); } catch (Exception e) { throw ApiError.unavailable(); }
    }
}
