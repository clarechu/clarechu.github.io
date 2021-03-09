package compare

import "github.com/clarechu/algorithms/source/code/models"

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
