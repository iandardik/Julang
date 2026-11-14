package julay

import (
	"bufio"
	"fmt"
	"math/rand"
	"os"
	"sync"
	"time"
)

// Program-wide WaitGroup for proc goroutines.
var procWG sync.WaitGroup

// AddProc registers one more running proc (call before go p.run()).
func AddProc() {
	procWG.Add(1)
}

// DoneProc marks a proc as finished (defer from run()).
func DoneProc() {
	procWG.Done()
}

// WaitAll blocks until every registered proc has called DoneProc.
func WaitAll() {
	procWG.Wait()
}

// JulayError is the runtime error type for Julay error: / Fail.
type JulayError struct {
	Message string
}

func (e JulayError) Error() string {
	return e.Message
}

// Fail prints msg and exits the process with status 1.
func Fail(msg string) {
	fmt.Println(msg)
	os.Exit(1)
}

// ExitProgram terminates the process with the given status code
// (Julay exitProgram(code)).
func ExitProgram(code int) {
	os.Exit(code)
}

// Println wraps fmt.Println for Julay println.
func Println(s string) {
	fmt.Println(s)
}

// Readln reads one line from stdin (without trailing newline).
func Readln() string {
	scanner := bufio.NewScanner(os.Stdin)
	if !scanner.Scan() {
		if err := scanner.Err(); err != nil {
			panic(err)
		}
		return ""
	}
	return scanner.Text()
}

// DelayMillis sleeps for ms milliseconds (Julay delayMillis).
func DelayMillis(ms int) {
	if ms <= 0 {
		return
	}
	time.Sleep(time.Duration(ms) * time.Millisecond)
}

// DelaySeconds sleeps for s seconds (Julay delaySeconds).
func DelaySeconds(s int) {
	if s <= 0 {
		return
	}
	time.Sleep(time.Duration(s) * time.Second)
}

// ChooseUniform picks one element uniformly at random.
// Panics if candidates is empty.
func ChooseUniform[T any](candidates []T) T {
	n := len(candidates)
	if n == 0 {
		panic("ChooseUniform: empty candidates")
	}
	return candidates[rand.Intn(n)]
}

// ChooseUniformFiltered keeps elements satisfying pred, then picks uniformly.
// Returns (zero, false) if no element qualifies (action not enabled).
func ChooseUniformFiltered[T any](candidates []T, pred func(T) bool) (T, bool) {
	var zero T
	filtered := make([]T, 0, len(candidates))
	for _, c := range candidates {
		if pred(c) {
			filtered = append(filtered, c)
		}
	}
	if len(filtered) == 0 {
		return zero, false
	}
	return ChooseUniform(filtered), true
}

// ChooseUniformList picks uniformly from a Julay List.
func ChooseUniformList[T any](l List[T]) T {
	return ChooseUniform(l.Elements())
}

// ChooseUniformSet picks uniformly from a Julay Set (order unspecified).
func ChooseUniformSet[T any](s Set[T]) T {
	return ChooseUniform(s.Elements())
}

// ChooseUniformSetFiltered filters set elements then picks uniformly.
func ChooseUniformSetFiltered[T any](s Set[T], pred func(T) bool) (T, bool) {
	return ChooseUniformFiltered(s.Elements(), pred)
}
