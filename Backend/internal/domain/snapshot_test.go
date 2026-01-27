package domain

import "testing"

func TestIsValidTransition(t *testing.T) {
	testCases := []struct {
		name     string
		from     SnapshotStatus
		to       SnapshotStatus
		expected bool
	}{
		// Valid transitions
		{"DRAFT to VALIDATED", DRAFT, VALIDATED, true},
		{"DRAFT to FAILED_VALIDATION", DRAFT, FAILED_VALIDATION, true},
		{"VALIDATED to CALCULATING", VALIDATED, CALCULATING, true},
		{"CALCULATING to CALCULATED", CALCULATING, CALCULATED, true},
		{"CALCULATED to APPROVED", CALCULATED, APPROVED, true},
		{"APPROVED to EXPORTED", APPROVED, EXPORTED, true},

		// Invalid transitions
		{"DRAFT to CALCULATING", DRAFT, CALCULATING, false},
		{"VALIDATED to APPROVED", VALIDATED, APPROVED, false},
		{"CALCULATING to DRAFT", CALCULATING, DRAFT, false},
		{"APPROVED to DRAFT", APPROVED, DRAFT, false},
		{"EXPORTED to DRAFT", EXPORTED, DRAFT, false},
		{"FAILED_VALIDATION to DRAFT", FAILED_VALIDATION, DRAFT, false},

		// Same state transition
		{"DRAFT to DRAFT", DRAFT, DRAFT, false},
	}

	for _, tc := range testCases {
		t.Run(tc.name, func(t *testing.T) {
			actual := IsValidTransition(tc.from, tc.to)
			if actual != tc.expected {
				t.Errorf("expected transition from %s to %s to be %v, but got %v", tc.from, tc.to, tc.expected, actual)
			}
		})
	}
}

func TestSnapshotStatus_String(t *testing.T) {
	testCases := []struct {
		status   SnapshotStatus
		expected string
	}{
		{DRAFT, "DRAFT"},
		{FAILED_VALIDATION, "FAILED_VALIDATION"},
		{VALIDATED, "VALIDATED"},
		{CALCULATING, "CALCULATING"},
		{CALCULATED, "CALCULATED"},
		{APPROVED, "APPROVED"},
		{EXPORTED, "EXPORTED"},
		{SnapshotStatus(0), ""},
		{SnapshotStatus(8), ""},
	}

	for _, tc := range testCases {
		t.Run(tc.expected, func(t *testing.T) {
			actual := tc.status.String()
			if actual != tc.expected {
				t.Errorf("expected string for status %d to be %q, but got %q", tc.status, tc.expected, actual)
			}
		})
	}
}
