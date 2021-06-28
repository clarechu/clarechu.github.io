---
title: 如何快速安装zsh
date: 2021-03-12 14:16:56
tags:
- linux
---

## Linux 安装并配置zsh

1.1 安装zsh

```bash
$ sudo apt-get install -y zsh
```

1.2 安装oh-my-zsh

```bash
$ sh -c "$(curl -fsSL https://raw.github.com/ohmyzsh/ohmyzsh/master/tools/install.sh)"
```

1.3 安装powerline font字体库

```bash

$ sudo apt-get install fonts-powerline
```

1.4 打开zsh配置文件 ~/.zshrc，修改主题为agnoster

```bash
1 # Set name of the theme to load --- if set to "random", it will
2 # load a random theme each time oh-my-zsh is loaded, in which case,
3 # to know which specific one was loaded, run: echo $RANDOM_THEME
4 # See https://github.com/ohmyzsh/ohmyzsh/wiki/Themes
5 ZSH_THEME="agnoster"
```
