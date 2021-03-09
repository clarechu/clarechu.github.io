package compare

import (
	"github.com/clarechu/algorithms/code/testdata"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestInsertion(t *testing.T) {
	data := testdata.GetCompare()
	assert.False(t, isSorted(data))
	insertionSort(data)
	assert.True(t, isSorted(data))
}
