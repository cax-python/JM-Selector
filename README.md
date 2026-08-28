# JM-Selector

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

一个 [JMComic 检索 → DeepSeek 识图筛选 → 按编号下载] 的命令行小工具

你告诉它你的口味，它会自动翻漫画、让 DeepSeek **看图判断**每本是否符合你的偏好，最后把**符合的编号**列出来，你用编号就能下载

---

## 命令

```bash
java -jar JM-Selector-v1.0.0.jar <子命令> [参数]
```

| 子命令 | 作用           |
|---|--------------|
| `filter` | 让 AI 帮你筛漫画   |
| `download` | 按编号下载本子（不并行） |

### 例 1：AI 帮你找喜欢的漫画

```bash
java -jar target/java-select-1.0-SNAPSHOT.jar filter --search "纯爱" --preference "我喜欢画风精致、剧情向、不要纯肉" --limit 10
```

- 搜 纯爱 → 让 AI 逐本看图 → 输出每本通过率  → 列出推荐编号

### 例 2：下载 AI 推荐的

```bash
java -jar target/java-select-1.0-SNAPSHOT.jar download --id-file results.json --path ./my-comics
```

- 每个本子下到 `./my-comics/<编号>/` 子文件夹

---

## 工作原理

```
搜索/范围/编号 → 候选本子池 → 采样内页 → 下载解密 → base64
  → 智能分批 → DeepSeek 逐张看图(是/否) → 算通过率
  → ≥ pass-ratio 入选 → 输出编号 + 存库 → 下载
```

- 不使用封面，只抽内页
- 通过率 = 判定"符合"的图数 / 抽样图数
- 已筛过的（同编号+同偏好）记在 SQLite，下次直接复用

---

## 参数

### 必填 and 连接

| 参数 | 含义 |
|---|---|
| `API_KEY` | DeepSeek 密钥 |
| `MODEL` | 模型，默认 `deepseek-v4-flash-vision-exp` |

### 候选来源（至少一个）

| 参数 | 含义                    |
|---|-----------------------|
| `--search` | 关键词，可多个               |
| `--search-max-pages` | 每个关键词最多翻几页，0=不限       |
| `--search-max-results` | 搜索最多收几条，0=不限          |
| `--range` | ID 范围 比如：10000-20000 |
| `--ids` | 精确 ID 列表，逗号分隔         |

### 偏好 and 破甲

| 参数 | 含义 |
|---|---|
| `--preference` | **必填**，你的口味 |
| `--preference-file` | 从文件读偏好（仅当 `--preference` 为空时） |
| `--jailbreak` | 破甲提示词|
| `--jailbreak-file` | 从文件读破甲（仅当 `--jailbreak` 为空时） |

### 采样方式

| 参数 | 含义 |
|---|---|
| `--image-level` | `full`/`medium`/`deep`/`auto` |
| `--image-n` | 配合 `deep`，每章抽 1/n |
| `--chapter-strategy` | `all`/`first`/`first-last`/`pick`/`fraction`/`auto` |
| `--chapter-pick` | 配合 `pick`，挑 k 章 |
| `--chapter-n` | 配合 `fraction`，抽 1/n 章 |
#### image-level
 - full - 采样全部图片
 - medium - 等距抽6张图片
 - deep - 等距抽取n张图片（n值自定义）
 - auto - 自动挡
#### chapter-strategy
 - all - 所有章节
 - first - 第一章
 - first-last - 第一章和最后一张
 - pick - 等距k章节（k值自定义）
 - fraction - 抽取1/n章（n值自定义）
 - auto - 自动挡


### 判定 and 收集

| 参数 | 含义 |
|---|---|
| `--pass-ratio` | 宽容度阈值，0-1，默认 0.6 |
| `--limit` | 本次最多新筛 N 个（历史命中自动顺延，不占额），默认 100，0=不限 |
| `--batch-max-images` | 单请求最大图数，默认 30 |
| `--batch-max-bytes` | 单请求 base64 字节上限，默认 44MiB |

### 持久化 and 输出

| 参数 | 含义                            |
|---|-------------------------------|
| `--db` | SQLite 历史库路径，默认 `./filter.db` |
| `--recheck` | 强制重扫                          |
| `--output` | 结果写成 JSON 文件                  |
| `--dry-run` | 只列候选本子，不调用ds                  |
| `--verbose` | 详细日志                          |


---

## config.json

所有参数都能写进 `config.json`（`config.json` 即是字段参考模板）。优先级：**命令行 > config.json > 内置默认**

```json
{
    "API_KEY": "sk-xxx",
    "MODEL": "deepseek-v4-flash-vision-exp",
    "SEARCH": ["纯爱"],
    "PREFERENCE": "我喜欢画风精致、剧情向",
    "IMAGE_LEVEL": "auto",
    "LIMIT": 100
}
```

---

## 注意

- mvn构建信息为 Java 25
- DeepSeek API Key（余额不足会报错：`Insufficient Balance`）

## 说明

- 下载/筛选会联网访问 JMComic；其服务器偶发 MySQL 故障，处理方法为搜索/取本子各 3 次
- 本工具用于个人研究/整理，请遵守当地法律法规与平台条款

Master-Spark

---
## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.