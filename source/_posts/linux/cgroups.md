---
title: cgroups Linux控制组
date: 2021-03-10 18:06:35
tags:
- linux
- kernal
---

实话实说,某些软件应用程序可能需要控制或限制-至少出于稳定性和某种程度上的安全性考虑。
错误或错误代码常常会破坏整个计算机,并可能破坏整个生态系统。
幸运的是,有一种方法可以检查那些相同的应用程序。
控制组（cgroups）是一项内核功能,可以限制,说明和隔离一个或多个进程的CPU,内存,磁盘I / O和网络使用情况。

cgroups框架提供以下内容：

资源限制： 可以将组配置为不超过指定的内存限制或使用的处理器数量不超过期望的数量,或者限制为特定的外围设备。
优先级： 可以将一个或多个组配置为利用更少或更多的CPU或磁盘 I/O 吞吐量。
监控： 监视和衡量组的资源使用情况。
控制： 可以冻结或停止并重新启动一组进程。

一个`cgroup`可以包含一个或多个绑定到同一组限制的进程。这些组也可以是分层的,这意味着子组继承了对其父组管理的限制。

Linux内核提供对cgroup技术的一系列控制器或子系统的访问。控制器负责将特定类型的系统资源分配给一组一个或多个进程。
例如,memory控制器是在cpuacct监视CPU使用率时限制内存使用率的。

您可以直接和间接（使用LXC,libvirt或Docker）访问和管理cgroup,在此我将首先通过sysfs和libcgroups库来介绍和管理cgroup 。
要遵循此处的示例,您首先需要安装必要的软件包。在Red Hat Enterprise Linux或CentOS上,在命令行上键入以下内容：

```

blkio：设置限制每个块设备的输入输出控制。例如:磁盘,光盘以及usb等等。
cpu：使用调度程序为cgroup任务提供cpu的访问。
cpuacct：产生cgroup任务的cpu资源报告。
cpuset：如果是多核心的cpu,这个子系统会为cgroup任务分配单独的cpu和内存。
devices：允许或拒绝cgroup任务对设备的访问。
freezer：暂停和恢复cgroup任务。
memory：设置每个cgroup的内存限制以及产生内存资源报告。
net_cls：标记每个网络包以供cgroup方便使用。
ns：命名空间子系统。
perf_event：增加了对每group的监测跟踪的能力,即可以监测属于某个特定的group的所有线程以及运行在特定CPU上的线程。

```

## cgroups 对内存的限制

### 手动方式
安装了正确的软件包后,您可以直接通过sysfs层次结构配置cgroup。例如,要foo在memory子系统下创建一个名为cgroup 的目录,请在/ sys / fs / cgroup / memory中创建一个名为foo的目录：

```bash
$ sudo mkdir /sys/fs/cgroup/memory/foo
```

默认情况下,每个新创建的cgroup都将继承对系统整个内存池的访问权限。对于某些应用程序,主要是那些继续分配更多内存但拒绝释放已经分配的内存的应用程序,可能不是一个好主意。要将应用程序限制在合理的范围内,您需要更新 memory.limit_in_bytes文件。

将在cgroup下运行的任何内容的内存限制foo为50MB：

```bash
$ echo 50000000 | sudo tee /sys/fs/cgroup/memory/foo/memory.limit_in_bytes
```

验证设置：


```bash
$ sudo cat memory.limit_in_bytes
50003968
```

请注意,回读的值始终是内核页面大小的倍数（即4096字节或4KB）。该值是最小的可分配内存大小。

启动应用程序：

```bash
$ sh ~/test.sh &
```

使用其进程ID（PID）,将应用程序移动到控制器foo下的 cgroup memory：

```bash
$ echo 2845 > /sys/fs/cgroup/memory/foo/cgroup.procs
```

使用相同的PID编号,列出正在运行的进程并验证其是否在所需的cgroup中运行：

```bash
$ ps -o cgroup 2845
CGROUP
8:memory:/foo,1:name=systemd:/user.slice/user-0.slice/session-4.scope
```

您还可以通过读取所需的文件来监视该cgroup当前正在使用的内容。在这种情况下,您将需要查看由您的进程（和产生的子进程）分配的内存量：

```bash
$ cat /sys/fs/cgroup/memory/foo/memory.usage_in_bytes
253952
```

### 当我改变limit 

现在,让我们重新创建相同的场景,但不要将cgroup限制 foo为50MB内存,而是将其限制为500个字节：


```bash
$ echo 500 | sudo tee /sys/fs/cgroup/memory/foo/memory.limit_in_bytes
```

注意：如果一项任务超出其定义的限制,内核将进行干预,在某些情况下,将终止该任务。

同样,当您读回该值时,该值将始终是内核页面大小的倍数。因此,尽管将其设置为500字节,但实际上设置为4 KB：

```bash
$ cat /sys/fs/cgroup/memory/foo/memory.limit_in_bytes
4096
```

启动应用程序,将其移至cgroup并监视系统日志：

```bash

$ sudo tail -f /var/log/messages

Oct 14 10:22:40 localhost kernel: sh invoked oom-killer:
↪gfp_mask=0xd0, order=0, oom_score_adj=0
Oct 14 10:22:40 localhost kernel: sh cpuset=/ mems_allowed=0
Oct 14 10:22:40 localhost kernel: CPU: 0 PID: 2687 Comm:
↪sh Tainted: G
OE  ------------   3.10.0-327.36.3.el7.x86_64 #1
Oct 14 10:22:40 localhost kernel: Hardware name: innotek GmbH
VirtualBox/VirtualBox, BIOS VirtualBox 12/01/2006
Oct 14 10:22:40 localhost kernel: ffff880036ea5c00
↪0000000093314010 ffff88000002bcd0 ffffffff81636431
Oct 14 10:22:40 localhost kernel: ffff88000002bd60
↪ffffffff816313cc 01018800000000d0 ffff88000002bd68
Oct 14 10:22:40 localhost kernel: ffffffffbc35e040
↪fffeefff00000000 0000000000000001 ffff880036ea6103
Oct 14 10:22:40 localhost kernel: Call Trace:
Oct 14 10:22:40 localhost kernel: [<ffffffff81636431>]
↪dump_stack+0x19/0x1b
Oct 14 10:22:40 localhost kernel: [<ffffffff816313cc>]
↪dump_header+0x8e/0x214
Oct 14 10:22:40 localhost kernel: [<ffffffff8116d21e>]
↪oom_kill_process+0x24e/0x3b0
Oct 14 10:22:40 localhost kernel: [<ffffffff81088e4e>] ?
↪has_capability_noaudit+0x1e/0x30
Oct 14 10:22:40 localhost kernel: [<ffffffff811d4285>]
↪mem_cgroup_oom_synchronize+0x575/0x5a0
Oct 14 10:22:40 localhost kernel: [<ffffffff811d3650>] ?
↪mem_cgroup_charge_common+0xc0/0xc0
Oct 14 10:22:40 localhost kernel: [<ffffffff8116da94>]
↪pagefault_out_of_memory+0x14/0x90
Oct 14 10:22:40 localhost kernel: [<ffffffff8162f815>]
↪mm_fault_error+0x68/0x12b
Oct 14 10:22:40 localhost kernel: [<ffffffff816422d2>]
↪__do_page_fault+0x3e2/0x450
Oct 14 10:22:40 localhost kernel: [<ffffffff81642363>]
↪do_page_fault+0x23/0x80
Oct 14 10:22:40 localhost kernel: [<ffffffff8163e648>]
↪page_fault+0x28/0x30
Oct 14 10:22:40 localhost kernel: Task in /foo killed as
↪a result of limit of /foo
Oct 14 10:22:40 localhost kernel: memory: usage 4kB, limit
↪4kB, failcnt 8
Oct 14 10:22:40 localhost kernel: memory+swap: usage 4kB,
↪limit 9007199254740991kB, failcnt 0
Oct 14 10:22:40 localhost kernel: kmem: usage 0kB, limit
↪9007199254740991kB, failcnt 0
Oct 14 10:22:40 localhost kernel: Memory cgroup stats for /foo:
↪cache:0KB rss:4KB rss_huge:0KB mapped_file:0KB swap:0KB
↪inactive_anon:0KB active_anon:0KB inactive_file:0KB
↪active_file:0KB unevictable:0KB
Oct 14 10:22:40 localhost kernel: [ pid ]   uid  tgid total_vm
↪rss nr_ptes swapents oom_score_adj name
Oct 14 10:22:40 localhost kernel: [ 2687]     0  2687    28281
↪347     12        0             0 sh
Oct 14 10:22:40 localhost kernel: [ 2702]     0  2702    28281
↪50    7        0             0 sh
Oct 14 10:22:40 localhost kernel: Memory cgroup out of memory:
↪Kill process 2687 (sh) score 0 or sacrifice child
Oct 14 10:22:40 localhost kernel: Killed process 2702 (sh)
↪total-vm:113124kB, anon-rss:200kB, file-rss:0kB
Oct 14 10:22:41 localhost kernel: sh invoked oom-killer:
↪gfp_mask=0xd0, order=0, oom_score_adj=0
[ ... ]
```

请注意,一旦应用程序达到4KB的限制,内核的“内存不足杀手”（或oom-killer）就会介入。它终止了该应用程序,并且不再运行。您可以通过键入以下内容进行验证：

```bash

$ ps -o cgroup 2687
CGROUP

```

## 使用`libcgroup`

libcgroup软件包中 提供的管理实用程序简化了此处描述的许多早期步骤。例如,使用cgcreate二进制文件的单个命令调用将负责创建sysfs条目和文件的过程。

要创建在 子系统foo下命名的组memory,请键入以下内容：

```bash
$ sudo cgcreate -g memory:foo
```

注意：libcgroup提供了一种用于管理控制组中的任务的机制。

使用与以前相同的方法,您可以开始设置阈值：

```bash
$ echo 50000000 | sudo tee
↪/sys/fs/cgroup/memory/foo/memory.limit_in_bytes
```

验证新配置的设置：

```bash
$ sudo cat memory.limit_in_bytes
50003968
```

foo使用 cgexec二进制文件 在cgroup中运行应用程序：

```bash
$ sudo cgexec -g memory:foo ~/test.sh
```

使用其PID编号,验证应用程序正在cgroup中并在已定义的子系统（memory）下运行：

```bash
$  ps -o cgroup 2945
CGROUP
6:memory:/foo,1:name=systemd:/user.slice/user-0.slice/
↪session-1.scope
```

如果您的应用程序不再运行,并且您想要清理并删除cgroup,则可以使用cgdelete二进制文件来完成。要从控制器foo 下面删除组memory,请键入：

```bash
$ sudo cgdelete memory:foo
```

持久群体
您还可以通过一个简单的配置文件和启动服务来完成上述所有操作。您可以在/etc/cgconfig.conf文件中定义所有cgroup名称和属性。以下内容为该组添加了一些属性foo：

```bash

$ cat /etc/cgconfig.conf
#
#  Copyright IBM Corporation. 2007
#
#  Authors:     Balbir Singh <balbir@linux.vnet.ibm.com>
#  This program is free software; you can redistribute it
#  and/or modify it under the terms of version 2.1 of the GNU
#  Lesser General Public License as published by the Free
#  Software Foundation.
#
#  This program is distributed in the hope that it would be
#  useful, but WITHOUT ANY WARRANTY; without even the implied
#  warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
#  PURPOSE.
#
#
# By default, we expect systemd mounts everything on boot,
# so there is not much to do.
# See man cgconfig.conf for further details, how to create
# groups on system boot using this file.

group foo {
cpu {
cpu.shares = 100;
}
memory {
memory.limit_in_bytes = 5000000;
}
}
```

这些cpu.shares选项定义组的CPU优先级。默认情况下,所有组都继承1,024个份额或100％的CPU时间。通过将此值降低到较为保守的程度（例如100）,该组将被限制为大约CPU时间的10％。

如前所述,在cgroup中运行的进程也可以限制为它可以访问的CPU（核心）数量。将以下部分添加到相同的cgconfig.conf文件中,并在所需的组名称下：

```bash

cpuset {
cpuset.cpus="0-5";
}

```

有了这个限制,此cgroup会将应用程序绑定到核心0到5-也就是说,它将仅看到系统上的前六个CPU核心。

接下来,您需要使用该cgconfig服务加载此配置。首先,启用cgconfig以在系统启动时加载以上配置：

```bash
$ sudo systemctl enable cgconfig
Create symlink from /etc/systemd/system/sysinit.target.wants/
↪cgconfig.service
to /usr/lib/systemd/system/cgconfig.service.
```

现在,启动cgconfig服务并手动加载相同的配置文件（或者您可以跳过此步骤并重新引导系统）：

```bash
$ sudo systemctl start cgconfig
```

将应用程序启动到cgroup中,foo并将其绑定到您的 memory和cpu 限制：

```bash
$ sudo cgexec -g memory,cpu,cpuset:foo ~/test.sh &
```

除了将应用程序启动到预定义的cgroup中之外,其余所有内容将在系统重新引导后继续存在。但是,您可以通过定义依赖于cgconfig 服务的启动初始化脚本来启动该应用程序,从而自动执行该过程 。

