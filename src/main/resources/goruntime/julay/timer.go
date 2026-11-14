package julay

import (
	"sync"
	"time"
)

// ProcTimers is a per-proc table of named timers for timer transitions.
// Each timer has a *time.Timer and a channel closed when the timer fires.
type ProcTimers struct {
	mu     sync.Mutex
	timers map[string]*time.Timer
	fired  map[string]chan struct{}
}

// NewProcTimers creates an empty timer table for one proc instance.
func NewProcTimers() *ProcTimers {
	return &ProcTimers{
		timers: make(map[string]*time.Timer),
		fired:  make(map[string]chan struct{}),
	}
}

// SetTimer (re)arms the named timer for ms milliseconds.
// Cancels any previous arming of the same name. When the timer expires,
// FiredChan(name) becomes selectable (channel closed).
func (t *ProcTimers) SetTimer(name string, ms int64) {
	t.mu.Lock()
	defer t.mu.Unlock()

	t.cancelLocked(name)

	ch := make(chan struct{})
	t.fired[name] = ch
	if ms < 0 {
		ms = 0
	}
	t.timers[name] = time.AfterFunc(time.Duration(ms)*time.Millisecond, func() {
		t.mu.Lock()
		defer t.mu.Unlock()
		// Only close if this channel is still the current one for name.
		if cur, ok := t.fired[name]; ok && cur == ch {
			select {
			case <-ch:
				// already closed
			default:
				close(ch)
			}
			delete(t.timers, name)
		}
	})
}

// CancelTimer stops and removes the named timer if present.
func (t *ProcTimers) CancelTimer(name string) {
	t.mu.Lock()
	defer t.mu.Unlock()
	t.cancelLocked(name)
}

func (t *ProcTimers) cancelLocked(name string) {
	if tm, ok := t.timers[name]; ok {
		tm.Stop()
		delete(t.timers, name)
	}
	if ch, ok := t.fired[name]; ok {
		// Drop the channel without closing if not yet fired, so in-flight
		// AfterFunc callbacks that still hold an old ch do not wake select.
		select {
		case <-ch:
			// already closed / fired
		default:
		}
		delete(t.fired, name)
	}
}

// FiredChan returns a receive-only channel closed when the named timer fires.
// If the timer is not armed and has not fired, returns nil (never selectable).
// If it has already fired, returns a closed channel (immediately selectable).
func (t *ProcTimers) FiredChan(name string) <-chan struct{} {
	t.mu.Lock()
	defer t.mu.Unlock()
	ch, ok := t.fired[name]
	if !ok {
		return nil
	}
	return ch
}

// IsFired reports whether the named timer has fired and not been cleared/cancelled.
func (t *ProcTimers) IsFired(name string) bool {
	t.mu.Lock()
	defer t.mu.Unlock()
	ch, ok := t.fired[name]
	if !ok {
		return false
	}
	select {
	case <-ch:
		return true
	default:
		return false
	}
}

// ClearFired removes a fired timer's channel after the timer transition runs,
// so the case is not re-enabled until SetTimer is called again.
func (t *ProcTimers) ClearFired(name string) {
	t.mu.Lock()
	defer t.mu.Unlock()
	if ch, ok := t.fired[name]; ok {
		select {
		case <-ch:
			delete(t.fired, name)
		default:
			// still armed; leave it
		}
	}
	delete(t.timers, name)
}
