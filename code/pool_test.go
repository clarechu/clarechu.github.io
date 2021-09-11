package compare

import (
	"context"
	"fmt"
	"github.com/stretchr/testify/assert"
	"sync"
	"testing"
	"time"
)

var testCases = struct {
}{}

type Demo struct {
	q   chan interface{}
	num int
}

type Close struct {
	closer context.CancelFunc
	num    int
}

func NewDemo() *Demo {
	return &Demo{
		q: make(chan interface{}, 1024),
	}
}

func (d *Demo) Factory() (CloseInterface, error) {
	ctx, cancel := context.WithCancel(context.TODO())
	d.num = d.num + 1
	n := d.num
	go func() {
		for {
			select {
			case i := <-d.q:
				fmt.Printf("number %d,i --> :%+v \n", n, i)
			case <-ctx.Done():
				fmt.Printf("%d -->is close ... \n", n)
				return
			}
		}
	}()
	return &Close{
		closer: cancel,
		num:    n,
	}, nil
}

func (d *Demo) Send(i interface{}) {
	d.q <- i
}

func (c *Close) Close() error {
	fmt.Printf("close to %d \n", c.num)
	c.closer()
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

var once sync.Once

func TestPoolClose(t *testing.T) {
	demo := NewDemo()
	pool, err := NewPool(10, 100, demo.Factory)
	go func() {
		for i := 0; i < 1000; i++ {
			if i > 900 {
				once.Do(func() {
					for j := 0; j < 8; j++ {
						closer, err := pool.Get()
						assert.Equal(t, nil, err)
						pool.Close(closer)
					}
				})
			}
			time.Sleep(1 * time.Millisecond)
			demo.Send(i)
		}
	}()
	time.Sleep(3 * time.Second)
	assert.Equal(t, nil, err)
}
