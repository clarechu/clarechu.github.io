package compare

import (
	"github.com/clarechu/algorithms/source/code/models"
	"math/rand"
)

func quickSort(a []*models.Comparable) []*models.Comparable {
	a = shuffle(a)
	quick(a, 0, len(a)-1)
	return a
}

//快排
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

/** Returns a random shuffling of the array. */
func shuffle(a []*models.Comparable) []*models.Comparable {
	nums := make([]*models.Comparable, len(a))
	copy(nums, a)
	rand.Shuffle(len(nums), func(i int, j int) {
		nums[i], nums[j] = nums[j], nums[i]
	})
	return nums
}
