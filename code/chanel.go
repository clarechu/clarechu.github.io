package compare

import (
	"fmt"
	"sync"
	"time"
)

func cc() {
	a := make(chan int)
	go func() {
		time.Sleep(1 * time.Second)
		a <- 1
		close(a)
	}()

	if a == nil {
		fmt.Println("haode")
		return
	}
	b := <-a
	fmt.Println("sleep !!!")
	time.Sleep(3 * time.Second)
	c := make(chan int, 0)
	c <- b

}

func cc1() {
	a := make(chan int)
	mu := sync.WaitGroup{}
	mu.Add(2)
	go func() {
		defer close(a)
		time.Sleep(10 * time.Second)
		a <- 1
		mu.Done()
	}()
	go func() {
		ticker := time.NewTicker(1 * time.Second)
		select {
		case cc := <-a:
			fmt.Println("xx", cc)
		case <-ticker.C:
			fmt.Println("ticker")
		}
		mu.Done()
	}()
	mu.Wait()
	fmt.Println("complete !!!")
}
