package compare

import "github.com/clarechu/algorithms/source/code/models"

func insertionSort(a []*models.Comparable) {
	n := len(a)
	for i := 0; i < n; i++ {
		for j := i; j > 0 && less(a[j], a[j-1]); j-- {
			exchange(a, j, j-1)
		}
	}
}
