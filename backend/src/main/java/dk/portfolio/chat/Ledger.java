package dk.portfolio.chat;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/** Durable reservations and rolling rate limits. All mutations use SQLite write transactions. */
final class Ledger {
    private final Path file;
    Ledger(Path file) { this.file = file.toAbsolutePath(); health(); }

    static void initialize(Path file, long ceiling) throws Exception {
        Files.createDirectories(file.toAbsolutePath().getParent());
        // Explicit initialization only: losing the volume must never silently grant a fresh budget.
        Files.createFile(file);
        try (var c = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath()); var s = c.createStatement()) {
            s.execute("PRAGMA journal_mode=WAL");
            s.execute("CREATE TABLE settings (id INTEGER PRIMARY KEY CHECK(id=1), ceiling INTEGER NOT NULL, salt TEXT NOT NULL, halted INTEGER NOT NULL DEFAULT 0)");
            s.execute("CREATE TABLE charges (id TEXT PRIMARY KEY, amount INTEGER NOT NULL, state TEXT NOT NULL, created INTEGER NOT NULL)");
            s.execute("CREATE TABLE requests (ip TEXT NOT NULL, created INTEGER NOT NULL)");
            s.execute("CREATE INDEX request_lookup ON requests(ip, created)");
            try (var p = c.prepareStatement("INSERT INTO settings(id,ceiling,salt) VALUES(1,?,?)")) {
                p.setLong(1, ceiling); p.setString(2, UUID.randomUUID().toString()); p.executeUpdate();
            }
        }
    }
    private Connection open() throws SQLException {
        if (!Files.isRegularFile(file)) throw new SQLException("Budget database missing");
        var c = DriverManager.getConnection("jdbc:sqlite:" + file);
        try (var s = c.createStatement()) { s.execute("PRAGMA busy_timeout=5000"); s.execute("PRAGMA synchronous=FULL"); }
        catch (SQLException e) { c.close(); throw e; }
        return c;
    }
    private interface Work<T> { T apply(Connection c) throws Exception; }
    private <T> T transaction(Work<T> work) {
        try (var c = open(); var s = c.createStatement()) {
            s.execute("BEGIN IMMEDIATE");
            try {
                T result = work.apply(c); s.execute("COMMIT"); return result;
            } catch (Exception e) {
                s.execute("ROLLBACK");
                if (e instanceof ApiError a) throw a;
                throw ApiError.unavailable();
            }
        } catch (SQLException e) { throw ApiError.unavailable(); }
    }
    void health() {
        try (var c = open(); var s = c.createStatement(); var r = s.executeQuery("SELECT ceiling,halted FROM settings WHERE id=1")) {
            if (!r.next() || r.getLong(1) <= 0 || r.getInt(2) != 0) throw ApiError.unavailable();
        } catch (SQLException e) { throw ApiError.unavailable(); }
    }
    private static long total(Connection c) throws SQLException {
        try (var s = c.createStatement(); var r = s.executeQuery("SELECT COALESCE(SUM(amount),0) FROM charges")) { r.next(); return r.getLong(1); }
    }
    long total() { return transaction(Ledger::total); }
    private static void checkBudget(Connection c, long amount) throws SQLException {
        try (var s = c.createStatement(); var r = s.executeQuery("SELECT ceiling,halted FROM settings WHERE id=1")) {
            if (!r.next() || r.getInt(2) != 0) throw ApiError.unavailable();
            if (total(c) + amount > Math.min(Config.BUDGET, r.getLong(1)))
                throw new ApiError(503, "budget_exhausted", "Chatten har nået sit forbrugsloft og er sat på pause.");
        }
    }
    String reserve(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("Reservation must be positive");
        return transaction(c -> {
            checkBudget(c, amount);
            String id = UUID.randomUUID().toString();
            try (var p = c.prepareStatement("INSERT INTO charges VALUES(?,?,'reserved',?)")) {
                p.setString(1, id); p.setLong(2, amount); p.setLong(3, Instant.now().getEpochSecond()); p.executeUpdate();
            }
            return id;
        });
    }
    void settle(String id, long amount) {
        if (amount < 0) throw new IllegalArgumentException("Charge must not be negative");
        transaction(c -> {
            try (var p = c.prepareStatement("UPDATE charges SET amount=?,state='settled' WHERE id=? AND state='reserved' AND amount>=?")) {
                p.setLong(1, amount); p.setString(2, id); p.setLong(3, amount);
                if (p.executeUpdate() != 1) throw new SQLException("Unknown reservation");
            }
            return null;
        });
    }
    void halt() { transaction(c -> { try (var s = c.createStatement()) { s.executeUpdate("UPDATE settings SET halted=1 WHERE id=1"); } return null; }); }
    void admit(String ip, long now) {
        transaction(c -> {
            checkBudget(c, Config.RESERVATION);
            String salt;
            try (var s = c.createStatement(); var r = s.executeQuery("SELECT salt FROM settings WHERE id=1")) { r.next(); salt = r.getString(1); }
            String key = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((salt + ip).getBytes(StandardCharsets.UTF_8)));
            try (var p = c.prepareStatement("DELETE FROM requests WHERE created<=?")) { p.setLong(1, now - 86400); p.executeUpdate(); }
            try (var p = c.prepareStatement("SELECT COUNT(*),COALESCE(SUM(CASE WHEN created>? THEN 1 ELSE 0 END),0) FROM requests WHERE ip=?")) {
                p.setLong(1, now - 60); p.setString(2, key);
                try (var r = p.executeQuery()) {
                    r.next();
                    if (r.getInt(1) >= 50 || r.getInt(2) >= 5)
                        throw new ApiError(429, "rate_limited", "Du har sendt mange beskeder. Prøv igen senere.");
                }
            }
            try (var p = c.prepareStatement("INSERT INTO requests VALUES(?,?)")) { p.setString(1, key); p.setLong(2, now); p.executeUpdate(); }
            return null;
        });
    }
}
