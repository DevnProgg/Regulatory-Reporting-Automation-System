package errors

import "fmt"

// Error represents a custom error type for the application.
type Error struct {
	Code    string
	Message string
	Op      string
	Err     error
}

// Error returns the string representation of the error.
func (e *Error) Error() string {
	if e.Err != nil {
		return fmt.Sprintf("op: %s, code: %s, msg: %s, err: %v", e.Op, e.Code, e.Message, e.Err)
	}
	return fmt.Sprintf("op: %s, code: %s, msg: %s", e.Op, e.Code, e.Message)
}

const (
	// ErrCodeNotFound is the code for a not found error.
	ErrCodeNotFound = "not_found"
	// ErrCodeInvalid is the code for an invalid error.
	ErrCodeInvalid = "invalid"
	// ErrCodeInternal is the code for an internal error.
	ErrCodeInternal = "internal"
)

// New creates a new application error.
func New(op, code, message string) error {
	return &Error{
		Op:      op,
		Code:    code,
		Message: message,
	}
}

// Wrap wraps an existing error with an application error.
func Wrap(op, code, message string, err error) error {
	return &Error{
		Op:      op,
		Code:    code,
		Message: message,
		Err:     err,
	}
}
