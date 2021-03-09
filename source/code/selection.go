package compare

import "github.com/clarechu/algorithms/source/code/models"

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
