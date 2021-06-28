---
title: golang 中的读写锁与互斥锁、自旋锁
date: 2021-06-28 14:43:55
tags:
- golang
---

## 读写锁(RWMutex)

读写锁是针对于读写操作的互斥锁。它与普通的互斥锁最大的不同就是，
它可以分别针对读操作和写操作进行锁定和解锁操作。
读写锁遵循的访问控制规则与互斥锁有所不同。
在读写锁管辖的范围内，它允许任意个读操作的同时进行。
但是，在同一时刻，它只允许有一个写操作在进行。
并且，在某一个写操作被进行的过程中，读操作的进行也是不被允许的。
也就是说，读写锁控制下的多个写操作之间都是互斥的，并且写操作与读操作之间也都是互斥的。
但是，多个读操作之间却不存在互斥关系。

换句话说：

1、 读锁: 所有的 goroutine 都可以同时读, 但不允许写。

2、 写锁: 写锁 只允许一个goroutine 写, 其他的goroutine 不允许读也不允许写

```go
func (rw *RWMutex) Lock       //写锁定
func (rw *RWMutex) Unlock     //写解锁
func (rw *RWMutex) RLock      //读锁定
func (rw *RWMutex) RUnlock    //读解锁
```

## 互斥锁(Mutex)

使用互斥锁（Mutex，全称 mutual exclusion）是为了来保护一个资源不会因为并发操作而引起冲突导致数据不准确。

```go
package main

import (
	"fmt"
	"sync"
)

func add(count *int, wg *sync.WaitGroup) {
	for i := 0; i < 1000; i++ {
		*count = *count + 1
	}
	wg.Done()
}

func main() {
	var wg sync.WaitGroup
	count := 0
	wg.Add(3)
	go add(&count, &wg)
	go add(&count, &wg)
	go add(&count, &wg)

	wg.Wait()
	fmt.Println("count 的值为：", count)
}
```
