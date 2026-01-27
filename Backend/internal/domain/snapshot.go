package domain

// SnapshotStatus represents the status of a snapshot in the system.
type SnapshotStatus int

const (
	_ SnapshotStatus = iota
	DRAFT
	FAILED_VALIDATION
	VALIDATED
	CALCULATING
	CALCULATED
	APPROVED
	EXPORTED
)

// String returns the string representation of a SnapshotStatus.
func (s SnapshotStatus) String() string {
	if s < DRAFT || s > EXPORTED {
		return "" // Handle out-of-bounds statuses gracefully
	}
	return [...]string{"", "DRAFT", "FAILED_VALIDATION", "VALIDATED", "CALCULATING", "CALCULATED", "APPROVED", "EXPORTED"}[s]
}

// allowedTransitions defines the valid state transitions for a SnapshotStatus.
var allowedTransitions = map[SnapshotStatus][]SnapshotStatus{
	DRAFT:             {VALIDATED, FAILED_VALIDATION},
	VALIDATED:         {CALCULATING},
	CALCULATING:       {CALCULATED},
	CALCULATED:        {APPROVED},
	APPROVED:          {EXPORTED},
	FAILED_VALIDATION: {},
	EXPORTED:          {},
}

// IsValidTransition checks if a transition from one status to another is allowed.
func IsValidTransition(from, to SnapshotStatus) bool {
	for _, allowed := range allowedTransitions[from] {
		if to == allowed {
			return true
		}
	}
	return false
}
