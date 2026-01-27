package config

import "time"

// Config holds the configuration for the application.
type Config struct {
	DB struct {
		DSN          string
		MaxOpenConns int
		MaxIdleConns int
		MaxIdleTime  time.Duration
	}
	HTTP struct {
		Port int
	}
}

// Load loads the configuration from environment variables.
func Load() (*Config, error) {
	return &Config{
		DB: struct {
			DSN          string
			MaxOpenConns int
			MaxIdleConns int
			MaxIdleTime  time.Duration
		}{
			DSN:          "postgres://lauren:dev@localhost:5432/centralbank?sslmode=disable",
			MaxOpenConns: 25,
			MaxIdleConns: 25,
			MaxIdleTime:  15 * time.Minute,
		},
		HTTP: struct {
			Port int
		}{
			Port: 8081,
		},
	}, nil
}
