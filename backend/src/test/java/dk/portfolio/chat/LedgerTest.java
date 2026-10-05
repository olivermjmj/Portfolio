package dk.portfolio.chat;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class LedgerTest {
    @TempDir Path dir;
    @Test void reservationSurvivesRestartAndInitializationCannotResetIt() throws Exception {
        Path file = dir.resolve("budget.db"); Ledger.initialize(file, 1000);
        var ledger = new Ledger(file); ledger.reserve(900);
        assertEquals(900, new Ledger(file).total());
        assertEquals("budget_exhausted", assertThrows(ApiError.class, () -> new Ledger(file).reserve(101)).code);
        assertThrows(FileAlreadyExistsException.class, () -> Ledger.initialize(file, 1000));
        assertEquals(900, ledger.total());
    }
    @Test void atomicReservationsAcrossConnectionsNeverExceedCeiling() throws Exception {
        Path file = dir.resolve("budget.db"); Ledger.initialize(file, 1000);
        try (var pool = Executors.newFixedThreadPool(8)) {
            var tasks = new ArrayList<Callable<Boolean>>();
            for (int i = 0; i < 24; i++) tasks.add(() -> {
                try { new Ledger(file).reserve(300); return true; }
                catch (ApiError e) { assertEquals("budget_exhausted", e.code); return false; }
            });
            int successes = 0;
            for (var future : pool.invokeAll(tasks)) if (future.get()) successes++;
            assertEquals(3, successes); assertEquals(900, new Ledger(file).total());
        }
    }
    @Test void settlementIsSingleUseAndCannotIncreaseOrMakeNegativeCharges() throws Exception {
        Path file = dir.resolve("budget.db"); Ledger.initialize(file, 1000);
        var ledger = new Ledger(file); String id = ledger.reserve(900);
        assertThrows(ApiError.class, () -> ledger.settle(id, 901));
        assertThrows(IllegalArgumentException.class, () -> ledger.settle(id, -1));
        ledger.settle(id, 120); assertEquals(120, ledger.total());
        assertThrows(ApiError.class, () -> ledger.settle(id, 0));
    }
    @Test void missingCorruptAndHaltedDatabasesFailClosed() throws Exception {
        Path file = dir.resolve("missing.db");
        assertThrows(ApiError.class, () -> new Ledger(file)); assertFalse(Files.exists(file));
        Files.writeString(file, "not sqlite"); assertThrows(ApiError.class, () -> new Ledger(file));
        Path valid = dir.resolve("valid.db"); Ledger.initialize(valid, 1000);
        var ledger = new Ledger(valid); ledger.halt();
        assertThrows(ApiError.class, () -> ledger.reserve(1));
        assertThrows(ApiError.class, () -> new Ledger(valid));
    }
    @Test void rollingRateLimitsPersistAcrossInstancesAndExpire() throws Exception {
        Path file = dir.resolve("budget.db"); Ledger.initialize(file, Config.BUDGET);
        var ledger = new Ledger(file); long now = 1_800_000_000;
        for (int i = 0; i < 5; i++) ledger.admit("127.0.0.1", now);
        assertEquals(429, assertThrows(ApiError.class, () -> new Ledger(file).admit("127.0.0.1", now + 10)).status);
        ledger.admit("127.0.0.2", now);
        for (int minute = 1; minute < 10; minute++) for (int i = 0; i < 5; i++) ledger.admit("127.0.0.1", now + minute * 61);
        assertEquals(429, assertThrows(ApiError.class, () -> ledger.admit("127.0.0.1", now + 700)).status);
        ledger.admit("127.0.0.1", now + 86401);
    }
}
