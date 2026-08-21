# Star Fantasy Refined Storage Addon

一个面向 Minecraft Forge 1.20.1 与 Refined Storage 1.12.x 的独立扩展。

本模组只在 RS 合成终端侧边提供网络工作站入口。每个合成终端拥有 7 个专用工作台槽位：把已兼容的工作台拖入槽位后，槽位左侧会出现打开按钮。槽位内容会随方块终端或无线/便携终端持久保存，不再遍历 RS 仓库寻找工作台；网络操作仍由服务端校验并执行。

JEI 是可选依赖。安装 JEI 后，可以在 RS 合成终端界面把切石、锻造和铁砧配方直接传给已配置的网络工作站；首次批量摆放和自动补货会同时使用 RS 网络与玩家背包中的材料。材料只在玩家点击 JEI 传输时由服务端检查，未安装 JEI 时仍可用侧边按钮打开工作站。

安装 Goety 2.5.56.5 或更高版本后，RS 合成终端还会提供黑暗铁砧入口，并保留 Goety 自身的黑暗铁砧菜单与规则。

可选兼容 Transmog 1.3.0、Quality Equipment 1.6 与 TerraCurio 0.2.0，分别增加幻化台、品质重铸台和工匠作坊入口。网络幻化台复用 Transmog 的幻化规则，但不需要紫水晶充能；工匠作坊配方支持从 JEI 直接跳转并批量填入材料。

安装 Disenchanting 2.2.3 或更高版本后会增加网络祛魔台。打开时会从 RS 网络（不足时也会检查玩家背包）取出并尽量装满书本，书本耗尽后自动补货；祛魔物品和剩余书本只保存在当前网络菜单中，关闭时统一退回 RS，不写入世界方块实体。

安装 Iron's Spells 'n Spellbooks 3.16.3 或更高版本后会增加法术抄写台、奥术铁砧与卷轴撰写台。卷轴撰写台打开时会从 RS 网络和玩家背包尝试装满纸张，纸张耗尽后自动补货，关闭时将剩余输入退回 RS。

安装 Timeless & Classics Guns: Zero 1.1.7 或更高版本后会增加枪械工作台、弹药工作台与配件工作台入口。TACZ 工作台会在自己的界面中合并统计 RS 网络和玩家背包材料；制作仍由 TACZ 原配方与工作台过滤规则校验，成功后从两处材料源中统一扣除。

本项目是独立实现，不包含第三方模组的反编译代码。项目源码使用 MIT 许可证；Refined Storage 本身不包含在发布 JAR 中。

## 本地构建

将合法取得的 `refinedstorage-1.12.4.jar` 放入 `libs/`。如需编译对应兼容，另将 Goety、Transmog、Quality Equipment、TerraCurio、Disenchanting、Iron's Spells 'n Spellbooks 与 TACZ 的兼容版本 JAR 放入 `libs/`，然后从项目根目录运行：

```powershell
gradle build
```
