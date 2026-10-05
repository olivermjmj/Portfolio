package dk.portfolio.chat;

final class ApiError extends RuntimeException {
    final int status;
    final String code;
    ApiError(int status, String code, String message) {
        super(message); this.status = status; this.code = code;
    }
    static ApiError unavailable() {
        return new ApiError(503, "unavailable", "Chatten er midlertidigt utilgængelig. Prøv igen senere.");
    }
}
