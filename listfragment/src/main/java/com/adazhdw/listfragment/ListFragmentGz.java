package com.adazhdw.listfragment;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.adazhdw.listfragment.base.BaseFragment;
import com.adazhdw.listfragment.layout.InterceptFrameLayoutEx;
import com.scwang.smartrefresh.header.MaterialHeader;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.constant.RefreshState;
import com.scwang.smartrefresh.layout.listener.OnLoadMoreListener;

import java.util.ArrayList;
import java.util.List;

public abstract class ListFragmentGz<M, VH extends RecyclerView.ViewHolder, A extends RecyclerView.Adapter<VH>> extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener, IThemeColor {
    private List<M> mList = new ArrayList<>();
    private boolean mLoading = false;
    private SwipeRefreshLayout mSwipe;
    private SmartRefreshLayout mSmartRefreshLayout;
    private MaterialHeader mFooterEx;
    private A mListAdapter = onAdapter();
    private RecyclerView mListView;
    protected Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mEventRunnable = new Runnable() {
        @Override
        public void run() {
            mSmartRefreshLayout.setEnableLoadMore(!mSwipe.isRefreshing());//根据mSwipe的isRefreshing状态来判断事件是否要禁止mSmartRefreshLayout可用
            mSwipe.setEnabled(mSmartRefreshLayout.getState() != RefreshState.ReleaseToLoad &&
                    mSmartRefreshLayout.getState() != RefreshState.LoadReleased &&
                    mSmartRefreshLayout.getState() != RefreshState.Loading);  //根据mSmartRefreshLayout的Load状态来判断事件是否要禁止mSwipe可用
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        InterceptFrameLayoutEx rootLayout = view.findViewById(R.id.rootLayout);
        mSwipe = view.findViewById(R.id.swipe);
        mSwipe.setOnRefreshListener(this);
        mSmartRefreshLayout = view.findViewById(R.id.smartRefreshLayout);
        mFooterEx = view.findViewById(R.id.mMaterialHeader);
        mListView = view.findViewById(R.id.listRecyclerView);

        onListHeader(mSwipe);
        onListFooter(mFooterEx);
        mListView.setItemAnimator(onItemAnimator());
        mListView.setLayoutManager(onLayoutManager());
        mListView.setAdapter(mListAdapter);

        mSmartRefreshLayout.setEnableRefresh(false);
        mSmartRefreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                nextPage();
            }
        });

        rootLayout.setOnMotionEventListener(new InterceptFrameLayoutEx.OnMotionEventListener() {
            @Override
            public void OnSlipAction(MotionEvent event) {
                mEventRunnable.run();
                mHandler.post(mEventRunnable);
            }
        });

        mSwipe.setOnRefreshListener(this);
        customizeView(getContext(), view.<ViewGroup>findViewById(R.id.rooContentFl));
        refresh();
    }

    public final void refresh() {
        if (mSwipe != null) {
            mSwipe.setRefreshing(true);
            onRefresh();
        }
    }

    /**
     * 返回值不要太小，尽量避免一屏高度可以显示一页数据的情况。
     */
    protected int pageSize() {
        return 10;
    }

    protected int pageStartAt() {
        return 0;
    }

    protected void customizeView(Context context, ViewGroup rooContentFl) {
    }

    protected RecyclerView.LayoutManager onLayoutManager() {
        return new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false);
    }

    protected void addItemDecoration(@NonNull RecyclerView.ItemDecoration decor) {
        if (mListView != null) {
            mListView.addItemDecoration(decor);
        }
    }

    protected RecyclerView.ItemAnimator onItemAnimator() {
        return new DefaultItemAnimator();
    }

    @NonNull

    protected abstract A onAdapter();

    protected abstract void onNextPage(int page, LoadCallback callback);

    protected int getListSize() {
        return mList.size();
    }

    protected M getListItem(int position) {
        return mList.get(position);
    }

    protected List<M> getList() {
        return new ArrayList<>(mList);
    }

    protected int getCurrPage() {
        return currPage;
    }

    protected String noDataTip() {
        return getString(R.string.no_more_data_text);
    }

    @Override
    public final void onRefresh() {
        nextPage();
    }

    private int currPage = 0;

    private void nextPage() {
        if (mLoading)
            return;
        mLoading = true;

        final boolean refresh = mSwipe.isRefreshing();
        currPage = pageStartAt() + (refresh ? 0 : currPage);
        if (refresh) {
            mSmartRefreshLayout.setEnableLoadMore(false);
        } else {
            mSwipe.setEnabled(false);
        }
        onNextPage(currPage, new LoadCallback() {
            @Override
            public void onResult() {
                if (refresh) {
                    mList.clear();
                    mListAdapter.notifyDataSetChanged();
                }
                mLoading = false;
                mSwipe.setEnabled(true);
                mSwipe.setRefreshing(false);
                mSmartRefreshLayout.setEnableLoadMore(true);
                mSmartRefreshLayout.finishLoadMore();
            }

            @Override
            public void onLoad(List<M> list) {
                if (!list.isEmpty()) {
                    currPage++;
                    int start = mList.size();
                    mList.addAll(list);
                    mListAdapter.notifyItemRangeInserted(start, mList.size());
                } else {
                    Toast.makeText(getContext(), noDataTip(), Toast.LENGTH_SHORT).show();
                }
                Log.d(TAG, "currPage--------------" + currPage);
            }
        });
    }

    public abstract class LoadCallback {
        /**
         * 必须调用这个方法来结束加载过程。
         */
        public abstract void onResult();

        /**
         * 调用这个方法代表成功获取指定页面的数据。
         * 失败时不需要调用。
         * 这个方法的调用必须在{@link #onResult()}后面，且中间不能插入对{@link ListFragmentGz#nextPage()}的调用
         */
        public abstract void onLoad(List<M> list);
    }

}