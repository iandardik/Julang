package julay

import (
	"fmt"
	"io"
	"net/http"
	"strings"
)

// HttpServerRequest matches Julay HttpServerRequest (path, body).
type HttpServerRequest struct {
	Path string
	Body string
}

func (r HttpServerRequest) ToString() string {
	return fmt.Sprintf("HttpServerRequest(path=%s, body=%s)", r.Path, r.Body)
}

func (r HttpServerRequest) String() string { return r.ToString() }

func (r HttpServerRequest) Key() string {
	return "HttpServerRequest|" + KeyOf(r.Path) + "|" + KeyOf(r.Body)
}

// HttpServerResponse matches Julay HttpServerResponse (body, code).
type HttpServerResponse struct {
	Body string
	Code int
}

func (r HttpServerResponse) ToString() string {
	return fmt.Sprintf("HttpServerResponse(body=%s, code=%d)", r.Body, r.Code)
}

func (r HttpServerResponse) String() string { return r.ToString() }

func (r HttpServerResponse) Key() string {
	return "HttpServerResponse|" + KeyOf(r.Body) + "|" + KeyOf(r.Code)
}

// HttpClientRequest matches Julay HttpClientRequest (url, method, body).
type HttpClientRequest struct {
	URL    string
	Method string
	Body   string
}

func (r HttpClientRequest) ToString() string {
	return fmt.Sprintf("HttpClientRequest(url=%s, method=%s, body=%s)", r.URL, r.Method, r.Body)
}

func (r HttpClientRequest) String() string { return r.ToString() }

func (r HttpClientRequest) Key() string {
	return "HttpClientRequest|" + KeyOf(r.URL) + "|" + KeyOf(r.Method) + "|" + KeyOf(r.Body)
}

// HttpClientResponse matches Julay HttpClientResponse (body, code).
type HttpClientResponse struct {
	Body string
	Code int
}

func (r HttpClientResponse) ToString() string {
	return fmt.Sprintf("HttpClientResponse(body=%s, code=%d)", r.Body, r.Code)
}

func (r HttpClientResponse) String() string { return r.ToString() }

func (r HttpClientResponse) Key() string {
	return "HttpClientResponse|" + KeyOf(r.Body) + "|" + KeyOf(r.Code)
}

// HttpPathFromUriPath strips a leading "/" so http://a.com/x/y yields path "x/y".
func HttpPathFromUriPath(uriPath string) string {
	return strings.TrimPrefix(uriPath, "/")
}

// ServeHttp starts an HTTP server on port that invokes handler for each request.
// net/http already runs each request in its own goroutine. Blocks forever
// (or until ListenAndServe fails, in which case it Fail()s).
func ServeHttp(port int, handler func(HttpServerRequest) HttpServerResponse) {
	mux := http.NewServeMux()
	mux.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		bodyBytes, err := io.ReadAll(r.Body)
		if err != nil {
			http.Error(w, err.Error(), http.StatusInternalServerError)
			return
		}
		_ = r.Body.Close()
		req := HttpServerRequest{
			Path: HttpPathFromUriPath(r.URL.Path),
			Body: string(bodyBytes),
		}
		resp := handler(req)
		w.WriteHeader(resp.Code)
		_, _ = w.Write([]byte(resp.Body))
	})
	addr := fmt.Sprintf(":%d", port)
	if err := http.ListenAndServe(addr, mux); err != nil {
		Fail(fmt.Sprintf("ServeHttp: %v", err))
	}
}

// DoHttpRequest performs one HTTP client call (Julay doHttpRequest).
// On transport error it returns body "" and code 0 (same as the Kotlin runtime).
func DoHttpRequest(request HttpClientRequest) HttpClientResponse {
	method := strings.ToUpper(request.Method)
	var bodyReader io.Reader
	switch method {
	case http.MethodGet, http.MethodHead:
		bodyReader = nil
	default:
		bodyReader = strings.NewReader(request.Body)
	}
	req, err := http.NewRequest(method, request.URL, bodyReader)
	if err != nil {
		return HttpClientResponse{Body: "", Code: 0}
	}
	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return HttpClientResponse{Body: "", Code: 0}
	}
	defer resp.Body.Close()
	b, err := io.ReadAll(resp.Body)
	if err != nil {
		return HttpClientResponse{Body: "", Code: 0}
	}
	return HttpClientResponse{Body: string(b), Code: resp.StatusCode}
}
