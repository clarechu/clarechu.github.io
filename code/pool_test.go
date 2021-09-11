package compare

import (
	"fmt"
	"github.com/stretchr/testify/assert"
	"testing"
	"time"
)

var testCases = struct {
}{}

type Demo struct {
	q   chan interface{}
	num int
}

func NewDemo() *Demo {
	return &Demo{
		q: make(chan interface{}, 1024),
	}
}

func (d *Demo) Factory() (CloseInterface, error) {
	d.num = d.num + 1
	n := d.num
	go func() {
		for {
			select {
			case i := <-d.q:
				fmt.Printf("number %d,i --> :%+v \n", n, i)
			}
		}
	}()
	return d, nil
}

func (d *Demo) Send(i interface{}) {
	d.q <- i
}

func (d *Demo) Close() error {
	close(d.q)
	return nil
}

func TestPool(t *testing.T) {
	demo := NewDemo()
	_, err := NewPool(10, 100, demo.Factory)
	go func() {
		for i := 0; i < 1000; i++ {
			time.Sleep(1 * time.Millisecond)
			demo.Send(i)
		}
	}()
	time.Sleep(3 * time.Second)
	assert.Equal(t, nil, err)
}
