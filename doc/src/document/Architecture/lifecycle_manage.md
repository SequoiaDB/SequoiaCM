
SequoiaCM 基于调度服务提供了生命周期管理功能，可以依据用户配置的规则，使对象在不同存储类别间定时迁移和清理，实现对象存储的智能分层。

##阶段标签##

阶段标签用于定义站点的存储类别，SequoiaCM 提供了三个内置阶段标签，具体说明如下：

- Hot：存储热数据阶段的对象，即频繁访问的对象。
- Warm：存储温数据阶段的对象，即访问频率较低的对象。
- Cold：存储冷数据阶段的对象，即长时间未访问的对象。

>**Note:**
>
> 用户也可根据业务需求自定义阶段标签。

##数据流##

数据流用于定义对象的迁移和清理规则，包含起始阶段标签、目标阶段标签、触发条件等。将数据流与工作区关联后，系统将依据所定义的规则生成后台调度任务，实现对象的定时迁移和清理操作。

以数据流 1、数据流 2 分别关联不同的工作区为例，数据流 1 定义的对象迁移规则为 Hot 站点迁移至 Warm 站点、数据流 2 为 Warm 站点迁移至 Cold 站点，对应工作区的关联示意图如下：

![数据流][lifecycle_apply]

则工作区 1 的对象迁移示意图如下：

![对象迁移][lifecycle_arc]

##使用##
相关 OM 管理操作可参考 [阶段标签管理][stage_tag] 和 [数据流管理][transition]

[lifecycle_arc]:Architecture/lifecycle_arc.png
[lifecycle_apply]:Architecture/lifecycle_apply.png
[stage_tag]:Om/Operation/LifeCycle/stage_tag.md
[transition]:Om/Operation/LifeCycle/transition.md