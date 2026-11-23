package julay

import (
	"fmt"
	"sort"
	"strconv"
	"strings"
)

// Keyer is implemented by Julay values that can appear as map/set keys
// (objs, lists, sets, maps). Primitives are handled by KeyOf.
type Keyer interface {
	Key() string
}

// StringerJulay is implemented by Julay collection/obj values for display.
type StringerJulay interface {
	ToString() string
}

// KeyOf returns a canonical encoding for use as a Go map key.
func KeyOf[T any](v T) string {
	if k, ok := any(v).(Keyer); ok {
		return k.Key()
	}
	switch x := any(v).(type) {
	case nil:
		return "nil"
	case bool:
		if x {
			return "b:true"
		}
		return "b:false"
	case int:
		return "i:" + strconv.Itoa(x)
	case int8:
		return "i8:" + strconv.FormatInt(int64(x), 10)
	case int16:
		return "i16:" + strconv.FormatInt(int64(x), 10)
	case int32:
		return "i32:" + strconv.FormatInt(int64(x), 10)
	case int64:
		return "i64:" + strconv.FormatInt(x, 10)
	case uint:
		return "u:" + strconv.FormatUint(uint64(x), 10)
	case uint8:
		return "u8:" + strconv.FormatUint(uint64(x), 10)
	case uint16:
		return "u16:" + strconv.FormatUint(uint64(x), 10)
	case uint32:
		return "u32:" + strconv.FormatUint(uint64(x), 10)
	case uint64:
		return "u64:" + strconv.FormatUint(x, 10)
	case float32:
		return "f32:" + strconv.FormatFloat(float64(x), 'g', -1, 32)
	case float64:
		return "f64:" + strconv.FormatFloat(x, 'g', -1, 64)
	case string:
		return "s:" + strconv.Quote(x)
	default:
		return "v:" + fmt.Sprintf("%#v", v)
	}
}

// Equal reports structural equality via canonical keys.
func Equal[T any](a, b T) bool {
	return KeyOf(a) == KeyOf(b)
}

// ToStringOf formats a value for Julay println / collection display.
func ToStringOf[T any](v T) string {
	if s, ok := any(v).(StringerJulay); ok {
		return s.ToString()
	}
	if s, ok := any(v).(fmt.Stringer); ok {
		return s.String()
	}
	switch x := any(v).(type) {
	case string:
		return x
	case bool:
		return strconv.FormatBool(x)
	case nil:
		return "null"
	default:
		return fmt.Sprint(v)
	}
}

// ---------------------------------------------------------------------------
// List[T] — immutable, 1-based indexing (Julay convention)
// ---------------------------------------------------------------------------

// List is an immutable sequence. Valid indexes are 1..Len().
type List[T any] struct {
	items []T
}

// ListOf builds a list from elements (copy).
func ListOf[T any](elems ...T) List[T] {
	if len(elems) == 0 {
		return List[T]{}
	}
	out := make([]T, len(elems))
	copy(out, elems)
	return List[T]{items: out}
}

// EmptyList returns an empty list.
func EmptyList[T any]() List[T] {
	return List[T]{}
}

// Len returns the number of elements (.length).
func (l List[T]) Len() int {
	return len(l.items)
}

// Get returns the element at 1-based index i.
// Panics with IndexOutOfBoundsException-style message if out of range.
func (l List[T]) Get(i int) T {
	if i < 1 || i > len(l.items) {
		panic(fmt.Sprintf("IndexOutOfBoundsException: index %d, length %d", i, len(l.items)))
	}
	return l.items[i-1]
}

// SetAt returns a new list with 1-based index i replaced by v (does not grow).
func (l List[T]) SetAt(i int, v T) List[T] {
	if i < 1 || i > len(l.items) {
		panic(fmt.Sprintf("IndexOutOfBoundsException: index %d, length %d", i, len(l.items)))
	}
	out := make([]T, len(l.items))
	copy(out, l.items)
	out[i-1] = v
	return List[T]{items: out}
}

// Contains reports whether v is an element (e in xs).
func (l List[T]) Contains(v T) bool {
	k := KeyOf(v)
	for _, e := range l.items {
		if KeyOf(e) == k {
			return true
		}
	}
	return false
}

// Add appends v (copy-on-write).
func (l List[T]) Add(v T) List[T] {
	out := make([]T, len(l.items)+1)
	copy(out, l.items)
	out[len(l.items)] = v
	return List[T]{items: out}
}

// Concat returns l + other (list concatenation).
func (l List[T]) Concat(other List[T]) List[T] {
	if len(l.items) == 0 {
		return other.Clone()
	}
	if len(other.items) == 0 {
		return l.Clone()
	}
	out := make([]T, len(l.items)+len(other.items))
	copy(out, l.items)
	copy(out[len(l.items):], other.items)
	return List[T]{items: out}
}

// Clone returns a shallow copy of the backing slice.
func (l List[T]) Clone() List[T] {
	if len(l.items) == 0 {
		return List[T]{}
	}
	out := make([]T, len(l.items))
	copy(out, l.items)
	return List[T]{items: out}
}

// Filter keeps elements for which pred is true.
func (l List[T]) Filter(pred func(T) bool) List[T] {
	out := make([]T, 0, len(l.items))
	for _, e := range l.items {
		if pred(e) {
			out = append(out, e)
		}
	}
	if len(out) == 0 {
		return List[T]{}
	}
	return List[T]{items: out}
}

// MapList applies f to each element, preserving order.
func MapList[T, U any](l List[T], f func(T) U) List[U] {
	if len(l.items) == 0 {
		return List[U]{}
	}
	out := make([]U, len(l.items))
	for i, e := range l.items {
		out[i] = f(e)
	}
	return List[U]{items: out}
}

// FoldList folds left over the list.
func FoldList[T, A any](l List[T], init A, f func(A, T) A) A {
	acc := init
	for _, e := range l.items {
		acc = f(acc, e)
	}
	return acc
}

// ToSet converts the list to a set (duplicates dropped; first occurrence kept).
func (l List[T]) ToSet() Set[T] {
	s := EmptySet[T]()
	for _, e := range l.items {
		s = s.Add(e)
	}
	return s
}

// Splice returns a 1-based inclusive slice (Julay / TLA SubSeq semantics).
// start must be >= 1; end must be >= 0. end==0 yields empty. Oversized end is clamped.
func (l List[T]) Splice(start, end int) List[T] {
	if start < 1 || end < 0 {
		panic("slice start must be >= 1 and end >= 0")
	}
	if end < 1 {
		return List[T]{}
	}
	hi := end
	if hi > len(l.items) {
		hi = len(l.items)
	}
	lo := start
	if lo > hi {
		return List[T]{}
	}
	out := make([]T, hi-lo+1)
	copy(out, l.items[lo-1:hi])
	return List[T]{items: out}
}

// AllDistinct is true iff every element is unique.
func (l List[T]) AllDistinct() bool {
	seen := make(map[string]struct{}, len(l.items))
	for _, e := range l.items {
		k := KeyOf(e)
		if _, ok := seen[k]; ok {
			return false
		}
		seen[k] = struct{}{}
	}
	return true
}

// Elements returns a defensive copy of the 0-based backing slice (for ChooseUniform etc.).
func (l List[T]) Elements() []T {
	if len(l.items) == 0 {
		return nil
	}
	out := make([]T, len(l.items))
	copy(out, l.items)
	return out
}

// Equals compares lists structurally.
func (l List[T]) Equals(other List[T]) bool {
	if len(l.items) != len(other.items) {
		return false
	}
	for i := range l.items {
		if !Equal(l.items[i], other.items[i]) {
			return false
		}
	}
	return true
}

// ToString formats like Kotlin lists: [a, b].
func (l List[T]) ToString() string {
	var b strings.Builder
	b.WriteByte('[')
	for i, e := range l.items {
		if i > 0 {
			b.WriteString(", ")
		}
		b.WriteString(ToStringOf(e))
	}
	b.WriteByte(']')
	return b.String()
}

// String implements fmt.Stringer.
func (l List[T]) String() string { return l.ToString() }

// Key implements Keyer for nested collection keys.
func (l List[T]) Key() string {
	var b strings.Builder
	b.WriteString("L[")
	for i, e := range l.items {
		if i > 0 {
			b.WriteByte(',')
		}
		b.WriteString(KeyOf(e))
	}
	b.WriteByte(']')
	return b.String()
}

// ---------------------------------------------------------------------------
// Set[T] — immutable, copy-on-write
// ---------------------------------------------------------------------------

// Set is an immutable unordered collection of unique elements.
type Set[T any] struct {
	m map[string]T
}

// SetOf builds a set from elements.
func SetOf[T any](elems ...T) Set[T] {
	s := EmptySet[T]()
	for _, e := range elems {
		s = s.Add(e)
	}
	return s
}

// EmptySet returns an empty set.
func EmptySet[T any]() Set[T] {
	return Set[T]{}
}

// Len returns the cardinality (.length).
func (s Set[T]) Len() int {
	return len(s.m)
}

// Contains reports whether v is a member (e in s).
func (s Set[T]) Contains(v T) bool {
	if s.m == nil {
		return false
	}
	_, ok := s.m[KeyOf(v)]
	return ok
}

// Add returns a set that includes v.
func (s Set[T]) Add(v T) Set[T] {
	k := KeyOf(v)
	if s.m != nil {
		if _, ok := s.m[k]; ok {
			return s
		}
	}
	n := make(map[string]T, len(s.m)+1)
	for kk, vv := range s.m {
		n[kk] = vv
	}
	n[k] = v
	return Set[T]{m: n}
}

// Remove returns a set without v.
func (s Set[T]) Remove(v T) Set[T] {
	k := KeyOf(v)
	if s.m == nil {
		return s
	}
	if _, ok := s.m[k]; !ok {
		return s
	}
	n := make(map[string]T, len(s.m)-1)
	for kk, vv := range s.m {
		if kk != k {
			n[kk] = vv
		}
	}
	if len(n) == 0 {
		return Set[T]{}
	}
	return Set[T]{m: n}
}

// Union returns s + other.
func (s Set[T]) Union(other Set[T]) Set[T] {
	if len(s.m) == 0 {
		return other.clone()
	}
	if len(other.m) == 0 {
		return s.clone()
	}
	n := make(map[string]T, len(s.m)+len(other.m))
	for k, v := range s.m {
		n[k] = v
	}
	for k, v := range other.m {
		n[k] = v
	}
	return Set[T]{m: n}
}

// Diff returns s - other.
func (s Set[T]) Diff(other Set[T]) Set[T] {
	if len(s.m) == 0 {
		return Set[T]{}
	}
	if len(other.m) == 0 {
		return s.clone()
	}
	n := make(map[string]T)
	for k, v := range s.m {
		if _, ok := other.m[k]; !ok {
			n[k] = v
		}
	}
	if len(n) == 0 {
		return Set[T]{}
	}
	return Set[T]{m: n}
}

func (s Set[T]) clone() Set[T] {
	if len(s.m) == 0 {
		return Set[T]{}
	}
	n := make(map[string]T, len(s.m))
	for k, v := range s.m {
		n[k] = v
	}
	return Set[T]{m: n}
}

// Filter keeps elements for which pred is true.
func (s Set[T]) Filter(pred func(T) bool) Set[T] {
	n := make(map[string]T)
	for k, v := range s.m {
		if pred(v) {
			n[k] = v
		}
	}
	if len(n) == 0 {
		return Set[T]{}
	}
	return Set[T]{m: n}
}

// MapSet applies f to each element (result set may be smaller if f collapses keys).
func MapSet[T, U any](s Set[T], f func(T) U) Set[U] {
	out := EmptySet[U]()
	for _, v := range s.m {
		out = out.Add(f(v))
	}
	return out
}

// FoldSet folds over the set; iteration order is unspecified.
func FoldSet[T, A any](s Set[T], init A, f func(A, T) A) A {
	acc := init
	for _, v := range s.m {
		acc = f(acc, v)
	}
	return acc
}

// AssociateWith builds a map from set elements to f(element).
func AssociateWith[T, U any](s Set[T], f func(T) U) Map[T, U] {
	m := EmptyMap[T, U]()
	for _, v := range s.m {
		m = m.Put(v, f(v))
	}
	return m
}

// ToList converts the set to a list; order is unspecified.
func (s Set[T]) ToList() List[T] {
	if len(s.m) == 0 {
		return List[T]{}
	}
	out := make([]T, 0, len(s.m))
	for _, v := range s.m {
		out = append(out, v)
	}
	return List[T]{items: out}
}

// Elements returns the set members as a slice (order unspecified).
func (s Set[T]) Elements() []T {
	if len(s.m) == 0 {
		return nil
	}
	out := make([]T, 0, len(s.m))
	for _, v := range s.m {
		out = append(out, v)
	}
	return out
}

// Equals compares sets structurally.
func (s Set[T]) Equals(other Set[T]) bool {
	if len(s.m) != len(other.m) {
		return false
	}
	for k := range s.m {
		if _, ok := other.m[k]; !ok {
			return false
		}
	}
	return true
}

// ToString formats like {a, b} (Julay style; order unspecified).
func (s Set[T]) ToString() string {
	var b strings.Builder
	b.WriteByte('{')
	first := true
	for _, v := range s.m {
		if !first {
			b.WriteString(", ")
		}
		first = false
		b.WriteString(ToStringOf(v))
	}
	b.WriteByte('}')
	return b.String()
}

// String implements fmt.Stringer.
func (s Set[T]) String() string { return s.ToString() }

// Key implements Keyer. Elements are sorted by their key encoding for stability.
func (s Set[T]) Key() string {
	keys := make([]string, 0, len(s.m))
	for k := range s.m {
		keys = append(keys, k)
	}
	sort.Strings(keys)
	var b strings.Builder
	b.WriteString("S{")
	for i, k := range keys {
		if i > 0 {
			b.WriteByte(',')
		}
		b.WriteString(k)
	}
	b.WriteByte('}')
	return b.String()
}

// ---------------------------------------------------------------------------
// Map[K,V] — immutable, copy-on-write; keys via KeyOf
// ---------------------------------------------------------------------------

type mapEntry[K, V any] struct {
	key   K
	value V
}

// Map is an immutable associative map. Complex keys use KeyOf.
type Map[K, V any] struct {
	m map[string]mapEntry[K, V]
}

// MapOf builds a map from alternating key/value pairs: MapOf(k1, v1, k2, v2, ...).
// Prefer Put chaining or MapFromPairs for typed construction.
func EmptyMap[K, V any]() Map[K, V] {
	return Map[K, V]{}
}

// MapEntry is a key/value pair for MapFromEntries.
type MapEntry[K, V any] struct {
	Key   K
	Value V
}

// MapFromEntries builds a map from entries.
func MapFromEntries[K, V any](entries ...MapEntry[K, V]) Map[K, V] {
	m := EmptyMap[K, V]()
	for _, e := range entries {
		m = m.Put(e.Key, e.Value)
	}
	return m
}

// Len returns the number of entries (.length).
func (m Map[K, V]) Len() int {
	return len(m.m)
}

// ContainsKey reports whether k is present (k in mp).
func (m Map[K, V]) ContainsKey(k K) bool {
	if m.m == nil {
		return false
	}
	_, ok := m.m[KeyOf(k)]
	return ok
}

// Contains is an alias for ContainsKey (membership is over keys).
func (m Map[K, V]) Contains(k K) bool {
	return m.ContainsKey(k)
}

// Get returns the value for k. Panics if the key is missing.
func (m Map[K, V]) Get(k K) V {
	if m.m != nil {
		if e, ok := m.m[KeyOf(k)]; ok {
			return e.value
		}
	}
	panic(fmt.Sprintf("Key %v is missing in the map", k))
}

// TryGet returns the value and whether the key was present.
func (m Map[K, V]) TryGet(k K) (V, bool) {
	var zero V
	if m.m == nil {
		return zero, false
	}
	e, ok := m.m[KeyOf(k)]
	if !ok {
		return zero, false
	}
	return e.value, true
}

// Put inserts or overwrites (copy-on-write).
func (m Map[K, V]) Put(k K, v V) Map[K, V] {
	key := KeyOf(k)
	n := make(map[string]mapEntry[K, V], len(m.m)+1)
	for kk, vv := range m.m {
		n[kk] = vv
	}
	n[key] = mapEntry[K, V]{key: k, value: v}
	return Map[K, V]{m: n}
}

// Remove deletes k if present.
func (m Map[K, V]) Remove(k K) Map[K, V] {
	key := KeyOf(k)
	if m.m == nil {
		return m
	}
	if _, ok := m.m[key]; !ok {
		return m
	}
	n := make(map[string]mapEntry[K, V], len(m.m)-1)
	for kk, vv := range m.m {
		if kk != key {
			n[kk] = vv
		}
	}
	if len(n) == 0 {
		return Map[K, V]{}
	}
	return Map[K, V]{m: n}
}

// Keys returns the key set (.keys).
func (m Map[K, V]) Keys() Set[K] {
	s := EmptySet[K]()
	for _, e := range m.m {
		s = s.Add(e.key)
	}
	return s
}

// Values returns the values as a list (order unspecified).
func (m Map[K, V]) Values() List[V] {
	if len(m.m) == 0 {
		return List[V]{}
	}
	out := make([]V, 0, len(m.m))
	for _, e := range m.m {
		out = append(out, e.value)
	}
	return List[V]{items: out}
}

// Entries returns key/value pairs (order unspecified).
func (m Map[K, V]) Entries() []MapEntry[K, V] {
	if len(m.m) == 0 {
		return nil
	}
	out := make([]MapEntry[K, V], 0, len(m.m))
	for _, e := range m.m {
		out = append(out, MapEntry[K, V]{Key: e.key, Value: e.value})
	}
	return out
}

// Equals compares maps structurally.
func (m Map[K, V]) Equals(other Map[K, V]) bool {
	if len(m.m) != len(other.m) {
		return false
	}
	for k, e := range m.m {
		oe, ok := other.m[k]
		if !ok || !Equal(e.value, oe.value) {
			return false
		}
	}
	return true
}

// ToString formats like {k -> v} (Julay style; order unspecified).
func (m Map[K, V]) ToString() string {
	var b strings.Builder
	b.WriteByte('{')
	first := true
	for _, e := range m.m {
		if !first {
			b.WriteString(", ")
		}
		first = false
		b.WriteString(ToStringOf(e.key))
		b.WriteString(" -> ")
		b.WriteString(ToStringOf(e.value))
	}
	b.WriteByte('}')
	return b.String()
}

// String implements fmt.Stringer.
func (m Map[K, V]) String() string { return m.ToString() }

// Key implements Keyer. Entries are sorted by key encoding for stability.
func (m Map[K, V]) Key() string {
	keys := make([]string, 0, len(m.m))
	for k := range m.m {
		keys = append(keys, k)
	}
	sort.Strings(keys)
	var b strings.Builder
	b.WriteString("M{")
	for i, k := range keys {
		if i > 0 {
			b.WriteByte(',')
		}
		b.WriteString(k)
		b.WriteByte('=')
		b.WriteString(KeyOf(m.m[k].value))
	}
	b.WriteByte('}')
	return b.String()
}
