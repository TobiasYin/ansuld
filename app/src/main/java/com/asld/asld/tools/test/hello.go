package main

import (
	"bytes"
	"fmt"
	"net/http"
)

func main() {
	fmt.Println("Start to run go process")
	client := &http.Client{}
	buf := bytes.Buffer{}
	buf.WriteString("hello world from go process!")
	req, err := http.NewRequest("GET", "http://192.168.2.190:8090/go_hello", &buf);
	if err != nil {
		panic(err)
	}
	_, err = client.Do(req)
	if err != nil {
		panic(err)
	}
	fmt.Println("success request!")
}
