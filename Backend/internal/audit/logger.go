package audit

import (
	"context"
	"log/slog"
	"net/http"
	"os"

	"github.com/google/uuid"
)

type correlationIDKey string

const CorrelationIDKey correlationIDKey = "correlationID"

func NewLogger() *slog.Logger {
	return slog.New(slog.NewJSONHandler(os.Stdout, nil))
}

func CorrelationIDMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		id := uuid.New()
		ctx := context.WithValue(r.Context(), CorrelationIDKey, id.String())
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

func LogRequestMiddleware(logger *slog.Logger) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			correlationID, _ := r.Context().Value(CorrelationIDKey).(string)
			logger.Info("request started",
				"method", r.Method,
				"url", r.URL.String(),
				"correlationID", correlationID,
			)
			next.ServeHTTP(w, r)
			logger.Info("request completed",
				"method", r.Method,
				"url", r.URL.String(),
				"correlationID", correlationID,
			)
		})
	}
}
