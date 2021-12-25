package com.shencoder.demo.adapter;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chad.library.adapter.base.BaseMultiItemQuickAdapter;
import com.chad.library.adapter.base.entity.MultiItemEntity;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.shencoder.demo.R;
import com.shencoder.demo.bean.NestedRecyclerViewBean;
import com.shencoder.demo.bean.TestBean;
import com.shencoder.pagergridlayoutmanager.PagerGridLayoutManager;

/**
 * @author ShenBen
 * @date 2021/12/25 13:28
 * @email 714081644@qq.com
 */
public class NestedRecyclerViewAdapter extends BaseMultiItemQuickAdapter<MultiItemEntity, BaseViewHolder> {
    public static final int TYPE_NORMAL = 1;
    public static final int TYPE_RECYCLER_VIEW = 2;

    public NestedRecyclerViewAdapter() {
        addItemType(TYPE_NORMAL, R.layout.item_nested_normal);
        addItemType(TYPE_RECYCLER_VIEW, R.layout.item_nested_recycler_view);
    }

    @Override
    protected void convert(@NonNull BaseViewHolder holder, MultiItemEntity multiItemEntity) {
        switch (holder.getItemViewType()) {
            case TYPE_NORMAL: {
                TestBean bean = (TestBean) multiItemEntity;
                holder.setText(R.id.tvItem, bean.getName());
            }
            break;
            case TYPE_RECYCLER_VIEW: {
                NestedRecyclerViewBean bean = (NestedRecyclerViewBean) multiItemEntity;
                RecyclerView rv = holder.getView(R.id.rv);
                RecyclerView.Adapter<?> adapter = rv.getAdapter();
                RecyclerView.LayoutManager layoutManager = rv.getLayoutManager();
                if (adapter == null) {
                    adapter = new GlideAdapter();
                    rv.setAdapter(adapter);
                }
                if (layoutManager == null) {
                    layoutManager = new PagerGridLayoutManager(2, 3, PagerGridLayoutManager.HORIZONTAL);
                    rv.setLayoutManager(layoutManager);
                }

                ((GlideAdapter) adapter).setList(bean.getList());
            }
            break;
        }
    }
}
