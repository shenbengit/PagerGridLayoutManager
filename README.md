# PagerGridLayoutManager
基于RecyclerView实现网格分页布局——PagerGridLayoutManager
## 运行效果
|滑动方向|设置行数列数|滚动到指定位置|
|:---:|:---:|:---:|
|<img src="https://github.com/shenbengit/PagerGridLayoutManager/blob/master/screenshots/%E6%BB%91%E5%8A%A8%E6%96%B9%E5%90%91.gif" alt="滑动方向" width="250px">|<img src="https://github.com/shenbengit/PagerGridLayoutManager/blob/master/screenshots/%E8%AE%BE%E7%BD%AE%E8%A1%8C%E6%95%B0%E5%88%97%E6%95%B0.gif" alt="设置行数列数" width="250px">|<img src="https://github.com/shenbengit/PagerGridLayoutManager/blob/master/screenshots/%E6%BB%9A%E5%8A%A8%E5%88%B0%E6%8C%87%E5%AE%9A%E4%BD%8D%E7%BD%AE.gif" alt="滚动到指定位置" width="250px">

|滚动到指定页|其他操作|
|:---:|:---:|
|<img src="https://github.com/shenbengit/PagerGridLayoutManager/blob/master/screenshots/%E6%BB%9A%E5%8A%A8%E5%88%B0%E6%8C%87%E5%AE%9A%E9%A1%B5.gif" alt="滚动到指定页" width="250px">|<img src="https://github.com/shenbengit/PagerGridLayoutManager/blob/master/screenshots/%E5%85%B6%E4%BB%96%E6%93%8D%E4%BD%9C.gif" alt="其他操作" width="250px">|


## 功能特点
- 复用机制和视图回收
- 支持scrollToPosition()和smoothScrollToPosition()
- 兼容输入法弹出导致onLayoutChildren()方法重新调用的问题
- 支持scrollBar
- 状态恢复

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
## 快速使用
### 注意事项
> 1、RecyclerView的**宽高**必须指定，match_parent或者例如100dp等。 (RecyclerView's width and height must be exactly. )    
> 2、item布局的**宽高**必须是match_parent。(item layout must use match_parent.)    
> 3、在ViewPager中使用是正常的，ViewPager已经处理好了滑动冲突。    
> 4、在ViewPager2中使用存在滑动冲突，ViewPager2未做滑动冲突处理，需自行实现，如需使用要自行处理冲突。这个后续加入。。。

### 使用PagerGridLayoutManager
```java
//是否开启调试日志
PagerGridLayoutManager.setDebug(BuildConfig.DEBUG);

PagerGridLayoutManager layoutManager = new PagerGridLayoutManager(/*rows*/3, /*columns*/ 3, /*PagerGridLayoutManager.VERTICAL*/PagerGridLayoutManager.HORIZONTAL);
recyclerView.setLayoutManager(layoutManager);

//设置监听
layoutManager.setPagerChangedListener(new PagerGridLayoutManager.PagerChangedListener() {
    /**
     * 页数回调
     * 仅会在页数变化时回调
     * @param pagerCount 页数，从1开始
     */
    @Override
    public void onPagerCountChanged(int pagerCount) {
        Log.w(TAG, "onPagerCountChanged-pagerCount:" + pagerCount);
    }

    /**
     * 选中的页面下标
     * 从0开始
     * @param prePagerIndex     上次的页码，当{{@link PagerGridLayoutManager#getItemCount()}}为0时，为-1，{{@link PagerGridLayoutManager#NO_ITEM}}
     * @param currentPagerIndex 当前的页码，当{{@link PagerGridLayoutManager#getItemCount()}}为0时，为-1，{{@link PagerGridLayoutManager#NO_ITEM}}
     */
    @Override
    public void onPagerIndexSelected(int prePagerIndex, int currentPagerIndex) {
        Log.w(TAG, "onPagerIndexSelected-prePagerIndex " + prePagerIndex + ",currentPagerIndex:" + currentPagerIndex);
    }
});
//设置滑动方向，注意：水平和垂直排列顺序不一致。
layoutManager.setOrientation(/*PagerGridLayoutManager.HORIZONTAL*/PagerGridLayoutManager.VERTICAL);
//设置行数
layoutManager.setRows(2);
//设置列数
layoutManager.setColumns(2);

//滚动到指定位置，注意：这个方法只会滚动到目标位置所在的页。
recyclerView.scrollToPosition(10);
//平滑滚动到指定位置，注意：这个方法只会滚动到目标位置所在的页。
recyclerView.smoothScrollToPosition(10);

//滚动到指定页
layoutManager.scrollToPagerIndex(3);
//平滑滚动到指定页，注意：如果滚动的页与当前页超过3，避免长时间滚动，会先直接滚动到就近的附近，再做平滑滚动
layoutManager.smoothScrollToPagerIndex(6);
//滚动到上一页
layoutManager.scrollToPrePager();
//滚动到下一页
layoutManager.scrollToNextPager();
//平滑滚动到上一页
layoutManager.smoothScrollToPrePager();
//平滑滚动到下一页
layoutManager.smoothScrollToNextPager();

```

### proguard-rules.pro
> 此库不需要额外混淆

如果此库对您有帮助，欢迎star!

# [License](https://github.com/shenbengit/PagerGridLayoutManager/blob/master/LICENSE)
