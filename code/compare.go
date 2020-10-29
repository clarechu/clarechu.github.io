package compare

import (
	"fmt"
	"github.com/clarechu/algorithms/code/models"
)

func show(comparable []*models.Comparable) {
	for i := 0; i < len(comparable); i++ {
		fmt.Println(comparable[i])
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
