# 贴图目录规范

## 目标
统一多贴图资源的组织方式，避免根目录堆积，便于后续维护。

## 规则
1. 单贴图资源：
   - 可直接放在对应分类目录下（如 `items/`、`units/`、`blocks/`）。
2. 多贴图资源（动画帧、单位多状态图、同一对象多变体）：
   - 必须放入“与对象同名”的子目录。
   - 示例：`items/magic-stone/`、`units/skyLaser/`。
3. 文件名保持原有加载名不变：
   - 例如 `magic-stone1.png ~ magic-stone10.png`、`skyLaser-left-attack.png`。
   - Mindustry 按区域名加载，文件名不变即可保证功能不受影响。

## 已应用
- `items/magic-stone/`
- `units/skyLaser/`
