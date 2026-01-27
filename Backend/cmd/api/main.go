package main

import (
	"fmt"
	"net/http"
	"os"

	"compliance-automation-system/backend/internal/audit"
	"compliance-automation-system/backend/internal/chttp"
	"compliance-automation-system/backend/internal/config"
	"compliance-automation-system/backend/internal/db"
)

func main() {
	logger := audit.NewLogger()
	logger.Info("starting api")

	cfg, err := config.Load()
	if err != nil {
		logger.Error("failed to load config", "error", err)
		os.Exit(1)
	}

	database, err := db.Connect(cfg.DB.DSN, cfg.DB.MaxOpenConns, cfg.DB.MaxIdleConns, cfg.DB.MaxIdleTime)
	if err != nil {
		logger.Error("failed to connect to db", "error", err)
		os.Exit(1)
	}
	defer database.Close()

	logger.Info("database connection pool established")

	r := http.NewServeMux()
	r.HandleFunc("/healthz", chttp.HealthCheckHandler(database))

	server := &http.Server{
		Addr:    fmt.Sprintf(":%d", cfg.HTTP.Port),
		Handler: audit.CorrelationIDMiddleware(audit.LogRequestMiddleware(logger)(r)),
	}

	logger.Info("starting http server", "port", cfg.HTTP.Port)
	if err := server.ListenAndServe(); err != nil {
		logger.Error("failed to start http server", "error", err)
		os.Exit(1)
	}
}
