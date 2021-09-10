package compare

//令牌桶算法 主要用于限流中

import (
	"sync"
	"sync/atomic"
	"time"
)

//TokenBucket
type TokenBucket struct {
	//次数限制
	total uint32
	//当前次数
	curr uint32
	//时长
	cTime uint64
	//一次调用时间
	startTime uint64
}

var mutex sync.Mutex

func NewTokenBucket(total uint32, cTime time.Duration) *TokenBucket {
	now := time.Now()
	return &TokenBucket{
		total:     total,
		curr:      0,
		startTime: uint64(now.UnixNano()),
		cTime:     uint64(cTime.Nanoseconds()),
	}
}

//Check 判断是否限流
func (tb *TokenBucket) Check() bool {
	now := uint64(time.Now().UnixNano())
	/*	//初始化
		mutex.Lock()
		if tb.startTime == 0 {
			defer mutex.Unlock()
			tb.startTime = now
			tb.curr++
			return false
		}
		mutex.Unlock()*/
	max := atomic.LoadUint64(&tb.cTime)
	mutex.Lock()
	timer := now - tb.startTime
	if max < timer {
		tb.reset(now)
		mutex.Unlock()
		return false
	}
	mutex.Unlock()
	if atomic.LoadUint32(&tb.curr) >= atomic.LoadUint32(&tb.total) {
		return true
	}
	atomic.AddUint32(&tb.curr, 1)
	return false
}

func (tb *TokenBucket) Update(total uint32, cTime time.Duration) {
	mutex.Lock()
	defer mutex.Unlock()
	now := time.Now()
	tb.total = total
	tb.curr = 0
	tb.startTime = uint64(now.UnixNano())
	tb.cTime = uint64(cTime.Nanoseconds())
}

//reset 对时间进行重制
func (tb *TokenBucket) reset(now uint64) {
	tb.curr = 1
	tb.startTime = now
}
