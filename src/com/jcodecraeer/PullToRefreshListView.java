package com.jcodecraeer;
import com.jcodecraeer.pulltorefreshlistview.R;

import android.content.Context;
import android.util.AttributeSet;
 
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Scroller;
import android.widget.TextView;

public class PullToRefreshListView extends ListView implements OnScrollListener {
	
    private final static String TAG = "PullToRefreshListView";  
    private float mLastY = -1;
 
	private boolean mPullRefreshing = false;  
	private boolean  mPullLoading = false; 
    private final static int OFFSET_RADIO = 3; 
	private final static int SCROLL_DURATION = 400; // scroll back duration
	private boolean isAllLoaded;
	
    private LayoutInflater inflater;  
  
    private PullListViewHeader mHeaderView;
	private int mHeaderViewHeight; // header view's height
 
    private LinearLayout mFootView; 
    private TextView listFootMore;
    private ProgressBar listFootProgress;
   
    private int firstItemIndex;  
    private Scroller mScroller;  
    public OnRefreshListener refreshListener;  

    public PullToRefreshListView(Context context, AttributeSet attrs) {  
        super(context, attrs);  
        init(context);  
    }  
    
    public PullToRefreshListView(Context context, AttributeSet attrs, int defStyle) {  
        super(context, attrs, defStyle);  
        init(context);  
    }
    
    public void setIsAllLoaded(boolean  f){
    	isAllLoaded = f; 
    }
    
    private void init(Context context) {   
    	mScroller = new Scroller(context, new DecelerateInterpolator());
 
		// init header view
		mHeaderView = new PullListViewHeader(context);
		addHeaderView(mHeaderView);
        
		// init header height
		mHeaderView.getViewTreeObserver().addOnGlobalLayoutListener(
				new OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						RelativeLayout headerViewContent = (RelativeLayout) mHeaderView
								.findViewById(R.id.listview_header_content);
						mHeaderViewHeight = headerViewContent.getHeight();
						getViewTreeObserver()
								.removeGlobalOnLayoutListener(this);
					}
				});
		inflater = LayoutInflater.from(context);  
        mFootView = (LinearLayout) inflater.inflate(R.layout.listview_footer, null);
		listFootMore = (TextView) mFootView.findViewById(R.id.listview_foot_more);
		listFootProgress = (ProgressBar) mFootView.findViewById(R.id.listview_foot_progress);
		
		this.addFooterView(mFootView);
		
        setOnScrollListener(this); 
    }  
  
    public void onScroll(AbsListView view, int firstVisiableItem, int visibleItemCount,  int totalItemCount) {  
        firstItemIndex = firstVisiableItem; 
    }  
  
    public void onScrollStateChanged(AbsListView view, int scrollState) {  
		if (this.getAdapter().getCount() == 0)
			return;
		boolean scrollEnd = false;
		try {
			if (view.getPositionForView(mFootView) == view
					.getLastVisiblePosition())
				scrollEnd = true;
		} catch (Exception e) {
			scrollEnd = false;
		}
		
		if (scrollEnd && !isAllLoaded && !mPullLoading) {
			listFootMore.setText("加载中...");
			listFootProgress.setVisibility(View.VISIBLE);
			this.setSelection(this.getBottom());//继续滚动onrefresh的高度 
	        if (refreshListener != null) { 
	            refreshListener.onLoadingMore();  
	        }  
	        mPullLoading = true;
		}
    }  
  
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (mLastY == -1) {
			mLastY = ev.getRawY();
		}

		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			mLastY = ev.getRawY();
			break;
		case MotionEvent.ACTION_MOVE:
			final float deltaY = ev.getRawY() - mLastY;
			mLastY = ev.getRawY();
			if (getFirstVisiblePosition() == 0
					&& (mHeaderView.getVisiableHeight() > 0 || deltaY > 0)) {
				// the first item is showing, header has shown or pull down.
				updateHeaderHeight(deltaY / OFFSET_RADIO);
			}  
			break;
		default:
			mLastY = -1; // reset
			if (getFirstVisiblePosition() == 0) {
				// invoke refresh
				if (mHeaderView.getVisiableHeight() > mHeaderViewHeight) {
					mPullRefreshing = true;
					mHeaderView.setState(PullListViewHeader.STATE_REFRESHING);
			        if (refreshListener != null) {  
			            refreshListener.onRefresh();  
			        } 
				} 
				resetHeaderHeight();
			}  
			break;
		}
		return super.onTouchEvent(ev);
	}  
   
	@Override
	public void computeScroll() {
		if (mScroller.computeScrollOffset()) { 
		    mHeaderView.setVisiableHeight(mScroller.getCurrY());
			postInvalidate();
		}
		super.computeScroll();
	}
  
    public void setOnRefreshListener(OnRefreshListener refreshListener) {  
        this.refreshListener = refreshListener;  
    }  
  
    public interface OnRefreshListener {  
        public void onRefresh(); 
        public void onLoadingMore();  
    }  
  
    public void onRefreshComplete(String time) {  
    	mHeaderView.setRefreshTime(time); 
		if (mPullRefreshing == true) {
			mPullRefreshing = false;
			resetHeaderHeight();
		}	 
    }
    
    public void onLoadingMoreComplete(boolean isAllLoaded) { 
    	this.isAllLoaded = isAllLoaded;
    	if(isAllLoaded){
    		listFootMore.setText("加载完成");
    	} else {
    		listFootMore.setText("");
    	}
		listFootProgress.setVisibility(View.GONE); 
		mPullLoading = false;
    }
   
	private void updateHeaderHeight(float delta) {
		mHeaderView.setVisiableHeight((int) delta
				+ mHeaderView.getVisiableHeight());
		if (!mPullRefreshing) { // 未处于刷新状态，更新箭头
			if (mHeaderView.getVisiableHeight() > mHeaderViewHeight) {
				mHeaderView.setState(PullListViewHeader.STATE_RELEASE_To_REFRESH);
			} else {
				mHeaderView.setState(PullListViewHeader.STATE_NORMAL);
			}
		}
		setSelection(0); // scroll to top each time
	}
	/**
	 * reset header view's height.
	 */
	private void resetHeaderHeight() {
		int height = mHeaderView.getVisiableHeight();
		if (height == 0) // not visible.
			return;
		// refreshing and header isn't shown fully. do nothing.
		if (mPullRefreshing && height <= mHeaderViewHeight) {
			return;
		}
		int finalHeight = 0; // default: scroll back to dismiss header.
		// is refreshing, just scroll back to show all the header.
		if (mPullRefreshing && height > mHeaderViewHeight) {
			finalHeight = mHeaderViewHeight;
		}
		mScroller.startScroll(0, height, 0, finalHeight - height,
				SCROLL_DURATION);
		// trigger computeScroll
		invalidate();
	} 
	
	
    /**
     * 以下是工具函数
     * *
     */
	public static int toInt(String str, int defValue) {
		try{
			return Integer.parseInt(str);
		}catch(Exception e){}
		return defValue;
	}
 
	public static int toInt(Object obj) {
		if(obj==null) return 0;
		return toInt(obj.toString(),0);
	}    
    
}