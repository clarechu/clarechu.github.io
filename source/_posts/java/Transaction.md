---

title: 分布式事务
date: 2021-11-26 22:28:03
tags:
- java
---


在讲分布式事务之前, 我们先了解一下什么是事务, 在程序中事务有什么作用。

### 事务

数据库事务的四大特性(ACID)

(1) 原子性(Atomicity)

原子性是指事务包含的所有操作要么全部成功，要么全部失败回滚，
这和前面两篇博客介绍事务的功能是一样的概念，
因此事务的操作如果成功就必须要完全应用到数据库，如果操作失败则不能对数据库有任何影响。

⑵ 一致性（Consistency）

一致性是指事务必须使数据库从一个一致性状态变换到另一个一致性状态，
也就是说一个事务执行之前和执行之后都必须处于一致性状态。

拿转账来说，假设用户A和用户B两者的钱加起来一共是5000，
那么不管A和B之间如何转账，转几次账，事务结束后两个用户的钱相加起来应该还得是5000，
这就是事务的一致性。

⑶ 隔离性（Isolation）

隔离性是当多个用户并发访问数据库时，比如操作同一张表时，
数据库为每一个用户开启的事务，不能被其他事务的操作所干扰，多个并发事务之间要相互隔离。

即要达到这么一种效果：对于任意两个并发的事务T1和T2，
在事务T1看来，T2要么在T1开始之前就已经结束，要么在T1结束之后才开始，
这样每个事务都感觉不到有其他事务在并发地执行。

关于事务的隔离性数据库提供了多种隔离级别，稍后会介绍到。

⑷ 持久性（Durability）

持久性是指一个事务一旦被提交了，那么对数据库中的数据的改变就是永久性的，
即便是在数据库系统遇到故障的情况下也不会丢失提交事务的操作。

例如我们在使用JDBC操作数据库时，在提交事务方法后，提示用户事务操作完成，
当我们程序执行完成直到看到提示后，就可以认定事务以及正确提交，即使这时候数据库出现了问题，
也必须要将我们的事务完全执行完成，否则就会造成我们看到提示事务处理完毕，
但是数据库因为故障而没有执行事务的重大错误。

---


## 分布式事务 

分布式事务是指事务的参与者、支持事务的服务器、
资源服务器以及事务管理器分别位于不同的分布式系统的不同节点之上。
例如在大型电商系统中，下单接口通常会扣减库存、减去优惠、生成订单 id, 
而订单服务与库存、优惠、订单 id 都是不同的服务，下单接口的成功与否，
不仅取决于本地的 db 操作，而且依赖第三方系统的结果，
这时候分布式事务就保证这些操作要么全部成功，要么全部失败。
本质上来说，分布式事务就是为了保证不同数据库的数据一致性。


---

## 分布式事务的解决方案


AT，XA，TCC，Saga

### AT
这是Seata的一大特色，AT对业务代码完全无侵入性，使用非常简单，
改造成本低。我们只需要关注自己的业务SQL，Seata会通过分析我们业务SQL，反向生成回滚数据

AT 包含两个阶段

```bash
  一阶段,所有参与事务的分支,本地事务Commit 业务数据和回滚日志(undoLog)
  二阶段,事务协调者根据所有分支的情况,决定本次全局事务是Commit 还是 Rollback(二阶段是完全异步)
```

---

## XA
也是我们常说的二阶段提交，XA要求数据库本身提供对规范和协议的支持。
XA用起来的话，也是对业务代码无侵入性的。

其他三种模式，都是属于补偿型，无法保证全局一致性。
啥意思呢，例如刚刚说的AT模式，我们是可能读到这一次分布式事务的中间状态，而XA模式不会。

补偿型 

事务处理机制构建在 事务资源（数据库）之上（要么在中间件层面，要么在应用层面）,
事务资源 本身对分布式事务是无感知的,这也就导致了补偿型事务无法做到真正的 全局一致性.
比如，一条库存记录，处在 补偿型 事务处理过程中，由 100 扣减为 50。此时,
仓库管理员连接数据库，查询统计库存，就看到当前的 50。之后,
事务因为意外回滚，库存会被补偿回滚为 100。显然，仓库管理员查询统计到的 50 就是脏数据。
如果是XA的话，中间态数据库存 50 由数据库本身保证,
不会被仓库管理员读到（当然隔离级别需要 读已提交 以上）
但是全局一致性带来的结果就是数据的锁定（AT模式也是存在全局锁的,
但是隔离级别无法保证，后边我们会详细说）,例如全局事务中有一条update语句,
其他事务想要更新同一条数据的话,只能等待全局事务结束


## TCC

TCC 模式同样包含三个阶段

```bash
一阶段 (Try): 所有参与分布式事务的分支,对业务资源进行检查和预留
二阶段 (Confirm): 所有分支的Try全部成功后,执行业务提交
二阶段 (Cancel): 取消Try阶段预留的业务资源
```

对比AT或者XA模式来说，TCC模式需要我们自己抽象并实现Try，Confirm，Cancel三个接口，
编码量会大一些，但是由于事务的每一个阶段都由开发人员自行实现。
而且相较于AT模式来说，减少了SQL解析的过程，也没有全局锁的限制,
所以TCC模式的性能是优于AT 、XA模式。

## SAGA

Saga 是长事务解决方案，每个参与者需要实现事务的正向操作和补偿操作。
当参与者正向操作执行失败时，回滚本地事务的同时，
会调用上一阶段的补偿操作，在业务失败时最终会使事务回到初始状态.

```bash
一阶段(reduce): commit 提交事务,或者说执行当前任务
二阶段(compensateReduce): 回滚当前业务
```

Saga与TCC类似，同样没有全局锁。由于相比缺少锁定资源这一步，
在某些适合的场景，Saga要比TCC实现起来更简单。
由于Saga和TCC都需要我们手动编码实现，所以在开发时我们需要参考一些设计上的规范，
由于不是本文重点，这里就不多说了，可以参考分布式事务 `Seata` 及其三种模式详解
在我们了解完四种分布式事务的原理之后，我们回到本文重点AT模式


## 结合seata

tcc事务 

申明一个全局事务 `@GlobalTransactional`

```java
    @GetMapping(value = "/tc")
    @GlobalTransactional
    public Object tc(@RequestParam("from") String accountNo, @RequestParam("to") String toAccountNo, @RequestParam("amount") double amount) {
        Boolean first = firstTccAction.prepare(null, accountNo, amount);
        secondTccAction.prepare(null, toAccountNo, amount);

        return null;
    }
```

声明 tcc 的try confirm cancel, 使用 `@LocalTCC`标签 开启一个本地的tcc 事务

```java
@LocalTCC
public interface FirstTccAction {

    @TwoPhaseBusinessAction(name = "FirstTccAction", commitMethod = "commit", rollbackMethod = "rollback")
    public Boolean prepare(BusinessActionContext businessActionContext, @BusinessActionContextParameter(paramName = "accountNo") String accountNo, @BusinessActionContextParameter(paramName = "amount") double amount);

    public boolean commit(BusinessActionContext businessActionContext);

    public Boolean rollback(BusinessActionContext businessActionContext);
}



@LocalTCC
public interface SecondTccAction {
    @TwoPhaseBusinessAction(name = "SecondTccAction", commitMethod = "commit", rollbackMethod = "rollback")
    public Boolean prepare(BusinessActionContext businessActionContext, @BusinessActionContextParameter(paramName = "accountNo") String accountNo, @BusinessActionContextParameter(paramName = "amount") double amount);

    public Boolean commit(BusinessActionContext businessActionContext);

    public Boolean rollback(BusinessActionContext businessActionContext);
}

```