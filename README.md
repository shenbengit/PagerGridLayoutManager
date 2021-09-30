# PagerGridLayoutManager
基于RecyclerView实现网格分页布局——PagerGridLayoutManager
# 运行效果
|滑动方向|设置行数列数|滚动到指定位置|
|:---:|:---:|:---:|
|<img src="https://github.com/shenbengit/PagerGridLayoutManager/blob/master/screenshots/%E6%BB%91%E5%8A%A8%E6%96%B9%E5%90%91.gif" alt="滑动方向" width="250px">|<img src="https://github.com/shenbengit/PagerGridLayoutManager/blob/master/screenshots/%E8%AE%BE%E7%BD%AE%E8%A1%8C%E6%95%B0%E5%88%97%E6%95%B0.gif" alt="设置行数列数" width="250px">|<img src="https://github.com/shenbengit/PagerGridLayoutManager/blob/master/screenshots/%E6%BB%9A%E5%8A%A8%E5%88%B0%E6%8C%87%E5%AE%9A%E4%BD%8D%E7%BD%AE.gif" alt="滚动到指定位置" width="250px">

|滚动到指定页|其他操作|
|:---:|:---:|
|<img src="https://github.com/shenbengit/PagerGridLayoutManager/blob/master/screenshots/%E6%BB%9A%E5%8A%A8%E5%88%B0%E6%8C%87%E5%AE%9A%E9%A1%B5.gif" alt="滚动到指定页" width="250px">|<img src="https://github.com/shenbengit/PagerGridLayoutManager/blob/master/screenshots/%E5%85%B6%E4%BB%96%E6%93%8D%E4%BD%9C.gif" alt="其他操作" width="250px">|


# 功能特点
- 复用机制和视图回收
- 支持scrollToPosition和smoothScrollToPosition
- 兼容输入法弹出导致onLayoutChildren()方法重新调用的问题
- 支持scrollBar

## 引入
### 将JitPack存储库添加到您的项目中(项目根目录下build.gradle文件)
```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
### 添加依赖
[![](https://jitpack.io/v/shenbengit/PagerGridLayoutManager.svg)](https://jitpack.io/#shenbengit/PagerGridLayoutManager)
> 在您引入项目的build.gradle中添加
```gradle
dependencies {
    implementation 'com.github.shenbengit:PagerGridLayoutManager:Tag'
}
```
