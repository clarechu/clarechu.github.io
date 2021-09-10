package compare

import (
	"fmt"
	"testing"
	"time"
)

func TestTokenBucket(t *testing.T) {
	tb := NewTokenBucket(10, 1*time.Second)
	for i := 0; i < 1000; i++ {
		if i%50 == 0 {
			time.Sleep(3 * time.Second)
			fmt.Println("=========")
		}

		go func() {
			fmt.Println(tb.Check())
		}()
	}
	time.Sleep(time.Second * 100)
}
