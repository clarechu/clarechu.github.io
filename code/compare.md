---
title: Compare
---

# Compare

比较大小的基本方法

```go
package compare

import (
	"fmt"
	"github.com/clarechu/algorithms/code/models"
)

func show(comparable []*models.Comparable) {
	for i := 0; i < len(comparable); i++ {
		fmt.Println(comparable[i].Value)
	}
}

//isSorted 查看当前数组是否按照顺序排序
func isSorted(comparable []*models.Comparable) bool {
	for i := 1; i < len(comparable); i++ {
		if less(comparable[i], comparable[i-1]) {
			return false
		}
	}
	return true
}

//less v > w
func less(v, w *models.Comparable) bool {
	return v.Value < w.Value
}

//exchange
func exchange(comparable []*models.Comparable, i, j int) {
	c := comparable[i]
	comparable[i] = comparable[j]
	comparable[j] = c
}

```

选择排序

```go

func selectSort(a []*models.Comparable) {
	n := len(a)
	for i := 0; i < n; i++ {
		min := i
		for j := i + 1; j < n; j++ {
			if less(a[j], a[min]) {
				min = j
			}
		}
		exchange(a, min, i)
	}
}

```

插入排序


```go

func insertionSort(a []*models.Comparable) {
	n := len(a)
	for i := 0; i < n; i++ {
		for j := i; j > 0 && less(a[j], a[j-1]); j-- {
			exchange(a, j, j-1)
		}
	}
}

```

归并排序

```go

//Merge Sort 归并排序
func mergeSort(a []*models.Comparable) {
	l := len(a)
	mSort(a, 0, l-1)
}

func mSort(a []*models.Comparable, lo, hi int) {
	if hi <= lo {
		return
	}
	mid := lo + (hi-lo)/2
	mSort(a, lo, mid)
	mSort(a, mid+1, hi)
	merge(a, lo, mid, hi)
}

//merge 合并数组
// lo 初始位置
// min 中位
// hi 高位
func merge(a []*models.Comparable, lo, mid, hi int) {
	i, j := lo, mid+1
	d := make([]*models.Comparable, 0)
	for _, k := range a {
		d = append(d, k)
	}
	for k := lo; k <= hi; k++ {
		// 左边取完了 取右边的
		if i > mid {
			a[k] = d[j]
			j++
			// 右边取完了 取左边的
		} else if j > hi {
			a[k] = d[i]
			i++

		} else if less(d[j], d[i]) {
			a[k] = d[j]
			j++
		} else {
			a[k] = d[i]
			i++
		}
	}
}

```


快速排序



```go
func quick(a []*models.Comparable, lo, hi int) {
	if lo >= hi {
		return
	}
	k := a[lo]
	i, j := lo, hi
	for {
		for ; i < j; j-- {
			if less(a[j], k) {
				break
			}
		}
		if i < j {
			a[i] = a[j]
			i++
		}
		for ; i < j; i++ {
			if less(k, a[i]) {
				break
			}
		}
		if i < j {
			a[j] = a[i]
			j--
		}
		if i >= j {
			break
		}
	}
	a[i] = k
	quick(a, lo, i-1)
	quick(a, i+1, hi)
}
```