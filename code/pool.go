package compare

import (
	"errors"
	"fmt"
	"sync"
	"time"
)

type CloseInterface interface {
	Close() error
}

type instance func() (CloseInterface, error)

type PoolInterface interface {
	//Get 获取连接池数据
	Get() (CloseInterface, error)
	//Shutdown 关闭所有连接池
	Shutdown() error
	// Set 开新的 factory
	Set() error
	// ReleaseNum 获取当前线程池的数量
	GetPoolNum() int32
	//Release 添加一个连接
	Release(closer CloseInterface) error
	//Close 关闭当前连接
	Close(closer CloseInterface) error
}

var (
	ErrInvalidConfig = errors.New("invalid pool config")
	ErrPoolClosed    = errors.New("pool closed")
)

type Pool struct {
	sync.Mutex
	pool chan CloseInterface
	//maxOpen 最大连接池数
	maxOpen int32
	//currOpen 当前连接池数量
	//最小连接数
	minOpen int32
	//是否已经关闭
	closed  bool
	numOpen int32
	//maxLifetime 存活时间
	maxLifetime time.Time
	instance    instance
}

func (p *Pool) Get() (CloseInterface, error) {
	if p.closed {
		return nil, ErrPoolClosed
	}
	var closer CloseInterface
	if len(p.pool) > 1 {
		closer = <-p.pool
	} else {
		return nil, fmt.Errorf("pool len == 0")
	}
	return closer, nil
}

func (p *Pool) Put() {
	panic("implement me")
}

func (p *Pool) Set() error {
	p.Lock()
	defer p.Unlock()
	if p.closed {
		return ErrPoolClosed
	}
	if p.maxOpen == p.numOpen {
		return errors.New("poll to max curr number ")
	}
	closer, err := p.instance()
	if err != nil {
		return err
	}
	p.numOpen++
	p.pool <- closer

	return nil
}

//Shutdown 关闭连接池，释放所有资源
func (p *Pool) Shutdown() error {
	if p.closed {
		return ErrPoolClosed
	}
	p.Lock()
	for closer := range p.pool {
		closer.Close()
		p.numOpen--
	}
	close(p.pool)
	p.closed = true
	p.Unlock()
	return nil
}

func (p *Pool) Release(closer CloseInterface) error {
	if p.closed {
		return ErrPoolClosed
	}
	p.Lock()
	p.pool <- closer
	p.Unlock()
	return nil
}

func (p *Pool) GetPoolNum() int32 {
	return p.numOpen
}

func (p *Pool) Close(closer CloseInterface) error {
	p.Lock()
	closer.Close()
	p.numOpen--
	p.Unlock()
	return nil
}

func NewPool(minOpen, maxOpen int32, i instance) (PoolInterface, error) {
	if maxOpen <= 0 || minOpen > maxOpen {
		return nil, ErrInvalidConfig
	}
	p := &Pool{
		maxOpen:     maxOpen,
		minOpen:     minOpen,
		maxLifetime: time.Now(),
		instance:    i,
		pool:        make(chan CloseInterface, maxOpen),
	}

	for j := int32(0); j < minOpen; j++ {
		closer, err := i()
		if err != nil {
			continue
		}
		p.numOpen++
		p.pool <- closer
	}
	return p, nil
}
