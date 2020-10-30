package compare

import (
	"github.com/clarechu/algorithms/code/testdata"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestSelectionSort(t *testing.T) {
	data := testdata.GetCompare()
	assert.False(t, isSorted(data))
	selectSort(data)
	assert.True(t, isSorted(data))
}
