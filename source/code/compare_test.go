package compare

import (
	"github.com/clarechu/algorithms/source/code/testdata"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestIsSort(t *testing.T) {
	data := testdata.GetCompare()
	assert.False(t, isSorted(data))
	data1 := testdata.GetCompare1()
	assert.True(t, isSorted(data1))
}

func TestShow(t *testing.T) {
	show(testdata.GetCompare())
}

func TestExchange(t *testing.T) {
	data := testdata.GetCompare()
	i := 1
	j := 2
	id := data[i]
	jd := data[j]
	exchange(data, i, j)
	assert.Equal(t, data[j], id)
	assert.Equal(t, data[i], jd)
}
