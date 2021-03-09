package testdata

import (
	"github.com/clarechu/algorithms/source/code/models"
)

var data []*models.Comparable
var data1 []*models.Comparable
var data2 []*models.Comparable
var data3 []*models.Comparable

func init() {
	data = []*models.Comparable{
		{
			Value: 1,
		},
		{
			Value: 2,
		},
		{
			Value: 4,
		},
		{
			Value: 3,
		},
		{
			Value: 7,
		},
		{
			Value: 11,
		},
		{
			Value: 23,
		},
		{
			Value: 45,
		},
	}

	data1 = []*models.Comparable{
		{
			Value: 1,
		},
		{
			Value: 2,
		},
		{
			Value: 3,
		},
		{
			Value: 4,
		},
		{
			Value: 7,
		},
	}
}

func GetCompare() []*models.Comparable {
	return data
}

func GetCompare1() []*models.Comparable {
	return data1
}
