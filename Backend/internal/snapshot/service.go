package snapshot

import (
	"context"
	"database/sql"
	"fmt"
	"log/slog"
	"time"

	"compliance-automation-system/backend/internal/audit"
	"compliance-automation-system/backend/internal/domain"
	"compliance-automation-system/backend/internal/errors"

	"github.com/google/uuid"
)

// SnapshotService defines the interface for snapshot-related operations.
type SnapshotService interface {
	CreateSnapshot(ctx context.Context, bankID uuid.UUID, period time.Time, reportType string) (uuid.UUID, error)
}

// Service implements SnapshotService.
type Service struct {
	db     *sql.DB
	logger *slog.Logger
}

// NewService creates a new SnapshotService.
func NewService(db *sql.DB, logger *slog.Logger) *Service {
	return &Service{
		db:     db,
		logger: logger,
	}
}

// CreateSnapshot creates an immutable, transaction-safe, and audited snapshot.
func (s *Service) CreateSnapshot(ctx context.Context, bankID uuid.UUID, period time.Time, reportType string) (uuid.UUID, error) {
	s.logger.Info("Validating bank existence", "bank_id", bankID)
	// In a real scenario, you would query the 'banks' table to ensure bankID is valid.
	// if !s.bankExists(ctx, bankID) {
	//     return uuid.Nil, errors.ErrBankNotFound
	// }

	// 2. ASSERT no existing snapshot for same keys
	var existingSnapshotID uuid.UUID
	query := `SELECT id FROM snapshots WHERE bank_id = $1 AND period = $2 AND report_type = $3 AND status != $4`
	err := s.db.QueryRowContext(ctx, query, bankID, period, reportType, domain.FAILED_VALIDATION).Scan(&existingSnapshotID)
	if err != nil {
		if err != sql.ErrNoRows {
			s.logger.Error("Failed to check for existing snapshot", "error", err)
			return uuid.Nil, fmt.Errorf("failed to check for existing snapshot: %w", err)
		}
	}
	if existingSnapshotID != uuid.Nil {
		s.logger.Warn("Existing snapshot found for bank, period, report type", "snapshot_id", existingSnapshotID)
		return uuid.Nil, errors.ErrSnapshotAlreadyExists
	}

	// 3. BEGIN TRANSACTION
	tx, err := s.db.BeginTx(ctx, nil)
	if err != nil {
		s.logger.Error("Failed to begin transaction", "error", err)
		return uuid.Nil, fmt.Errorf("failed to begin transaction: %w", err)
	}
	defer func() {
		if r := recover(); r != nil {
			err := tx.Rollback()
			if err != nil {
				return
			}
			panic(r) // Re-throw panic after rollback
		} else if err != nil {
			err := tx.Rollback()
			if err != nil {
				return
			}
		}
	}()

	// 4. snapshot_id = UUID() and INSERT snapshot with status DRAFT
	snapshotID := uuid.New()
	insertSnapshotQuery := `
		INSERT INTO snapshots (id, bank_id, period, report_type, status, created_at)
		VALUES ($1, $2, $3, $4, $5, $6)
	`
	_, err = tx.ExecContext(ctx, insertSnapshotQuery,
		snapshotID,
		bankID,
		period,
		reportType,
		domain.DRAFT,
		time.Now(),
	)
	if err != nil {
		s.logger.Error("Failed to insert new snapshot", "snapshot_id", snapshotID, "error", err)
		return uuid.Nil, fmt.Errorf("failed to insert new snapshot: %w", err)
	}

	// 5. COPY source data into snapshot tables USING snapshot_date <= period
	// This is a placeholder. In a real system, this would involve complex SQL
	// queries to select data from various source tables (e.g., 'accounts', 'transactions')
	// and insert them into corresponding snapshot tables (e.g., 'snapshot_accounts', 'snapshot_transactions').
	// The `period` parameter would be used to filter the source data.
	s.logger.Info("Copying source data into snapshot tables (placeholder)", "snapshot_id", snapshotID)
	err = s.copySourceData(ctx, tx, snapshotID, period)
	if err != nil {
		s.logger.Error("Failed to copy source data", "snapshot_id", snapshotID, "error", err)
		return uuid.Nil, fmt.Errorf("failed to copy source data: %w", err)
	}

	// 6. WRITE audit log: action = CREATE_SNAPSHOT
	audit.NewLogger().Info("CREATE_SNAPSHOT",
		"action", "CREATE_SNAPSHOT",
		"snapshot_id", snapshotID,
		"bank_id", bankID,
		"period", period.Format(time.RFC3339),
		"report_type", reportType,
		"status", domain.DRAFT.String(),
		"correlationID", ctx.Value(audit.CorrelationIDKey),
	)

	// 7. COMMIT
	if err = tx.Commit(); err != nil {
		s.logger.Error("Failed to commit transaction", "snapshot_id", snapshotID, "error", err)
		return uuid.Nil, fmt.Errorf("failed to commit transaction: %w", err)
	}

	s.logger.Info("Snapshot created successfully", "snapshot_id", snapshotID)
	return snapshotID, nil
}

// copySourceData is a placeholder for the actual data copying logic.
// In a real implementation, this would involve executing a series of SQL statements
// within the transaction to populate the snapshot-specific tables.
func (s *Service) copySourceData(ctx context.Context, tx *sql.Tx, snapshotID uuid.UUID, period time.Time) error {
	// Example placeholder for copying data
	// For instance, if you have a 'transactions' table and 'snapshot_transactions' table:
	// insertQuery := `INSERT INTO snapshot_transactions (snapshot_id, original_transaction_id, amount, date, ...)
	//                 SELECT $1, id, amount, date, ... FROM transactions WHERE date <= $2`
	// _, err := tx.ExecContext(ctx, insertQuery, snapshotID, period)
	// if err != nil {
	//     return fmt.Errorf("failed to copy transactions: %w", err)
	// }

	// Return nil for now, assuming success in this placeholder.
	return nil
}
