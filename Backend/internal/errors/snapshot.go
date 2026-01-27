package errors

import "errors"

// ErrSnapshotAlreadyExists is returned when a snapshot with the same bank ID, period, and report type already exists.
var ErrSnapshotAlreadyExists = errors.New("snapshot with specified bank ID, period, and report type already exists")

// ErrBankNotFound is returned when the specified bank ID does not exist.
var ErrBankNotFound = errors.New("bank not found")