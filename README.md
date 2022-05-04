# PagerGridLayoutManager
基于RecyclerView实现网格分页布局——PagerGridLayoutManager
## 运行效果
|滑动方向|设置行数列数|滚动到指定位置|
|:---:|:---:|:---:|
|<img src="https://github.com/shenbengit/PagerGridLayoutManager/blob/master/screenshots/%E6%BB%91%E5%8A%A8%E6%96%B9%E5%90%91.gif" alt="滑动方向" width="250px">|<img src="https://github.com/shenbengit/PagerGridLayoutManager/blob/master/screenshots/%E8%AE%BE%E7%BD%AE%E8%A1%8C%E6%95%B0%E5%88%97%E6%95%B0.gif" alt="设置行数列数" width="250px">|<img src="https://github.com/shenbengit/PagerGridLayoutManager/blob/master/screenshots/%E6%BB%9A%E5%8A%A8%E5%88%B0%E6%8C%87%E5%AE%9A%E4%BD%8D%E7%BD%AE.gif" alt="滚动到指定位置" width="250px">

|滚动到指定页|其他操作|
|:---:|:---:|
|<img src="https://github.com/shenbengit/PagerGridLayoutManager/blob/master/screenshots/%E6%BB%9A%E5%8A%A8%E5%88%B0%E6%8C%87%E5%AE%9A%E9%A1%B5.gif" alt="滚动到指定页" width="250px">|<img src="https://github.com/shenbengit/PagerGridLayoutManager/blob/master/screenshots/%E5%85%B6%E4%BB%96%E6%93%8D%E4%BD%9C.gif" alt="其他操作" width="250px">|

|ViewPager中使用|ViewPager2中使用|
|:---:|:---:|
|<img src="https://github.com/shenbengit/PagerGridLayoutManager/blob/master/screenshots/ViewPager%E4%B8%AD%E4%BD%BF%E7%94%A8.gif" alt="ViewPager中使用" width="250px">|<img src="https://github.com/shenbengit/PagerGridLayoutManager/blob/master/screenshots/ViewPager2%E4%B8%AD%E4%BD%BF%E7%94%A8.gif" alt="ViewPager2中使用" width="250px">|


## 功能特点
- 水平垂直分页滑动
- 复用机制和视图回收
- 支持scrollToPosition()和smoothScrollToPosition()
- 兼容输入法弹出导致onLayoutChildren()方法重新调用的问题
- 支持scrollBar
- 状态恢复
- 滑动冲突处理
- 支持clipToPadding=false
- 支持Reverse Layout，兼容RTL

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
[![](https://jitpack.io/v/shenbengit/PagerGridLayoutManager.svg)](https://jitpack.io/#shenbengit/PagerGridLayoutManager) [change log](https://github.com/shenbengit/PagerGridLayoutManager/blob/master/CHANGE%20LOG.md)
> 从v1.1.0版本开始，水平滑动排列方式改为先从左到右，再从上到下；若您需要水平滑动排列方式改为先从上到下，再从左到右的方式，请查看分支[1.0.x](https://github.com/shenbengit/PagerGridLayoutManager/tree/1.0.x)；        
> 在您引入项目的build.gradle中添加：    
```gradle
dependencies {
    //水平排列方式：先从左到右，再从上到下。最新版本。
    //Horizontal arrangement: from left to right, then from top to bottom. Latest version.
    implementation 'com.github.shenbengit:PagerGridLayoutManager:Tag'
    //或者 or 
    //水平排列方式：先从上到下，再从左到右。最终版本。
    //Horizontal arrangement: from top to bottom, then from left to right. Final version.
    //弃用，Deprecated.
    implementation 'com.github.shenbengit:PagerGridLayoutManager:1.0.6'
}
```
## 快速使用
### 注意事项
> 1、RecyclerView的**宽高**必须指定，match_parent或者例如100dp等。 (RecyclerView's width and height must be **exactly**. )    
> 2、Item布局的**宽高**必须是**match_parent**。(item layout's width and height must use **match_parent**.)        
> 3、在ViewPager中使用是正常的，ViewPager已经处理好了滑动冲突。    
> 4、在ViewPager2中使用存在滑动冲突，ViewPager2未做滑动冲突处理，**本库已经处理滑动冲突**，若不满足您的需求可自行处理。

### 使用PagerGridLayoutManager
```java
//是否开启调试日志
PagerGridLayoutManager.setDebug(BuildConfig.DEBUG);

PagerGridLayoutManager layoutManager = new PagerGridLayoutManager(
        /*rows*/3, 
        /*columns*/ 3, 
        /*PagerGridLayoutManager.VERTICAL*/PagerGridLayoutManager.HORIZONTAL, 
        /*reverseLayout*/ false
);
/*
是否启用处理滑动冲突滑动冲突，default: true；若不需要库中自带的处理方式，则置为false，自行处理。
setHandlingSlidingConflictsEnabled() 必须要在{@link RecyclerView#setLayoutManager(RecyclerView.LayoutManager)} 之前调用，否则无效
you must call this method before {@link RecyclerView#setLayoutManager(RecyclerView.LayoutManager)}
*/
layoutManager.setHandlingSlidingConflictsEnabled(true);

recyclerView.setLayoutManager(layoutManager);

//设置监听
layoutManager.setPagerChangedListener(new PagerGridLayoutManager.PagerChangedListener() {
    /**
     * 页数回调
     * 仅会在页数变化时回调
     * @param pagerCount 页数，从1开始，为0时说明无数据
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
//设置滑动方向
layoutManager.setOrientation(/*PagerGridLayoutManager.HORIZONTAL*/ PagerGridLayoutManager.VERTICAL);
/*
是否反向布局，自动兼容RTL；
注意：水平方向反向是排列顺序和滑动放向都反向，垂直方向仅排列顺序反向；
Whether the layout is reversed, automatically compatible with RTL;
Note: The horizontal reverse is the reverse of the arrangement order and the sliding direction, and the vertical direction is only the reverse of the arrangement order;
*/
layoutManager.setReverseLayout(/*true*/ false);
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
//平滑滚动到指定页，注意：如果滚动的页与当前页超过3，避免长时间滚动，会先直接滚动到就近的页，再做平滑滚动
layoutManager.smoothScrollToPagerIndex(6);
//滚动到上一页
layoutManager.scrollToPrePager();
//滚动到下一页
layoutManager.scrollToNextPager();
//平滑滚动到上一页
layoutManager.smoothScrollToPrePager();
//平滑滚动到下一页
layoutManager.smoothScrollToNextPager();
//设置滑动每像素需要花费的时间，不可过小，不然可能会出现划过再回退的情况。默认值：70
layoutManager.setMillisecondPreInch(70);
//设置最大滚动时间，如果您想此值无效，请使用{@link Integer#MAX_VALUE}。默认值：200 ms
layoutManager.setMaxScrollOnFlingDuration(200);

```

### proguard-rules.pro
> 此库不需要额外混淆

如果此库对您有用，欢迎给个**star**！

## 作者其他的开源项目
- Android端WebRTC一些扩展方法：[WebRTCExtension](https://github.com/shenbengit/WebRTCExtension)
- 基于Netty封装UDP收发工具：[UdpNetty](https://github.com/shenbengit/UdpNetty)
- Android端基于JavaCV实现人脸检测功能：[JavaCV-FaceDetect](https://github.com/shenbengit/JavaCV-FaceDetect)
- 使用Kotlin搭建Android MVVM快速开发框架：[MVVMKit](https://github.com/shenbengit/MVVMKit)

# [License](https://github.com/shenbengit/PagerGridLayoutManager/blob/master/LICENSE)
