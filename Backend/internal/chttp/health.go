package chttp

import (
	"context"
	"database/sql"
	"encoding/json"
	"net/http"
	"time"
)

func HealthCheckHandler(db *sql.DB) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		ctx, cancel := context.WithTimeout(r.Context(), 1*time.Second)
		defer cancel()

		status := "ok"
		if err := db.PingContext(ctx); err != nil {
			status = "error"
		}

		w.Header().Set("Content-Type", "application/json")
		json.NewEncoder(w).Encode(map[string]string{"status": status})
	}
}
