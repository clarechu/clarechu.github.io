package compare

import (
	"github.com/clarechu/algorithms/source/code/testdata"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestMergeSort(t *testing.T) {
	data := testdata.GetCompare()
	assert.False(t, isSorted(data))
	mergeSort(data)
	assert.True(t, isSorted(data))
}
