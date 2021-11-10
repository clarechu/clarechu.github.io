---

title: java-序列化 
date: 2021-09-22 22:28:03 
tags:
- java
---

## 其他方法优先于java序列化

    java反序列化是一个明显存在的风险，它不仅被应用直接广泛使用，也被java子系统RMI(远程方法调用)、JMX(Java管理扩展)和JMS等大量的间接被使用。
    将不被信任的流进行反序列化，可能导致远程代码执行，拒绝服务，以及一系列其他的攻击。即使应用本身没有做错任何事情，也可能被攻击。

    下面举一个例子， 在下面的例子中只要引入serializable， 就可以轻易地展开一次拒绝服务的攻击。

```java
static byte[] bomb() {
    Set<Object> root = new HashSet<>();
    Set<Object> s1 = root;
    Set<Object> s2 = new HashSet<>();
    for (int i = 0; i< 100; i++) {
        Set<Object> t1 = new HashSet<>();
        Set<Object> t2 = new HashSet<>();
        t1.add("foo");
        s1.add(t1);
        s2.add(t1);
        s1 = t1;
        s2 = t2;
    }
    return serialize(root);
}
```

每当反序列化一个不信任的字节流时，自己就需要试着去攻击它。`避免序列化攻击的最佳方式是永远不要序列化任何东西`。主要原因 
1. 如果系统是机遇java序列化的 ，那么就无法迁移到别的平台或者别的语言的结构化数据表示法。
2. java序列化存在很高的风险.

我们应该使用一种支持跨平台的数据结构例如： json 和protobuf。


## 谨慎地实现serializable 接口

实现了serializable 接口主要有以下代价:

1. 如果想要这个类允许被序列化，那么非常简单 只需要在类上声明 implements serializable即可。但是这样做就会导致 一旦这个类被发布，
就大大降低了"改变这个类的实现"的灵活性，如果实现了serializable，那么他的所有的字节流编码就本城了它的导出API的一部分。如果接受了
默认的反序列化形式，那么这个类中的私有的实例都变成导出的API的一部分。这个不符合"最低限度地访问域"

2. 实现了serializable 接口之后增加了出现BUG 和安全漏洞爹可能性。
3. 随着类发行新的版本，相关的测试负担也会增加。

以上几处 我们应该为了继承而设计类 我们应该尽可能少地去实现serializable 接口，用户的接口也应该少的继承serializable 接口。

## 考虑使用自定义的序列化形式

如果一个对象的物理表示法等同于它的逻辑内容，可能就适合于使用默认的序列化形式。例如：

```java
public class Name implements Serializable {
    private final String lastName;
    
    private final String firstName;
    
    private final String middleName;
}
```

即使你确定了默认的序列化形式是适合的，通常还必须提供一个readObject 方法以保证约束关系和安全性。

当一个对象的物理表示法与它的逻辑数据内容有实质性的区别时，使用默认序列形式会有以下4个缺点：

1. 它使这个类的导出API 永远地束缚在该类的内部表示法上。
2. 他会消耗过多的空间
3. 他会消耗过多的时间
4. 他会引起堆栈溢出



