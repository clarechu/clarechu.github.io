package compare

import (
	"github.com/clarechu/algorithms/code/testdata"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestQuick(t *testing.T) {
	data := testdata.GetCompare()
	assert.False(t, isSorted(data))
	assert.True(t, isSorted(quickSort(data)))
}

func TestSort(t *testing.T)  {
	data := testdata.GetCompare()
	assert.False(t, isSorted(data))
	quickSort(data)
}