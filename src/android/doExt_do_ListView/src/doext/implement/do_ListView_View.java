package doext.implement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import core.DoServiceContainer;
import core.helper.DoScriptEngineHelper;
import core.helper.DoTextHelper;
import core.helper.DoUIModuleHelper;
import core.helper.jsonparse.DoJsonNode;
import core.helper.jsonparse.DoJsonValue;
import core.interfaces.DoIListData;
import core.interfaces.DoIPage;
import core.interfaces.DoIScriptEngine;
import core.interfaces.DoIUIModuleView;
import core.object.DoInvokeResult;
import core.object.DoMultitonModule;
import core.object.DoSourceFile;
import core.object.DoUIContainer;
import core.object.DoUIModule;
import doext.define.do_ListView_IMethod;
import doext.define.do_ListView_MAbstract;

/**
 * 自定义扩展UIView组件实现类，此类必须继承相应VIEW类，并实现DoIUIModuleView,Do_ListView_IMethod接口；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.model.getUniqueKey());
 */
public class do_ListView_View extends LinearLayout implements DoIUIModuleView, do_ListView_IMethod, android.widget.AdapterView.OnItemClickListener, android.widget.AdapterView.OnItemLongClickListener {

	/**
	 * 每个UIview都会引用一个具体的model实例；
	 */
	private do_ListView_MAbstract model;
	protected MyAdapter myAdapter;
	private ListView listview;
	// ///////////////////
	private static final int PULL_TO_REFRESH = 0; // 下拉刷新
	private static final int RELEASE_TO_REFRESH = 1; // 松开后刷新
	private static final int REFRESHING = 2; // 加载中...
	private static final int PULL_DOWN_STATE = 3; // 刷新完成

	private View mHeaderView;
	private int mHeaderViewHeight;
	private int mLastMotionX, mLastMotionY;
	private int mHeaderState;
	private int mPullState;
	private boolean supportHeaderRefresh;

	// ///////////////////

	public do_ListView_View(Context context) {
		super(context);
		this.setOrientation(VERTICAL);
		listview = new ListView(context);
		myAdapter = new MyAdapter();
	}

	/**
	 * 初始化加载view准备,_doUIModule是对应当前UIView的model实例
	 */
	@Override
	public void loadView(DoUIModule _doUIModule) throws Exception {
		this.model = (do_ListView_MAbstract) _doUIModule;
		listview.setOnItemClickListener(this);
		listview.setOnItemLongClickListener(this);

		String _headerViewPath = this.model.getHeaderView();
		if (_headerViewPath != null && !"".equals(_headerViewPath.trim())) {
			try {
				DoIPage _doPage = this.model.getCurrentPage();
				DoSourceFile _uiFile = _doPage.getCurrentApp().getSourceFS().getSourceByFileName(_headerViewPath);
				if (_uiFile != null) {

					DoUIContainer _rootUIContainer = new DoUIContainer(_doPage);
					_rootUIContainer.loadFromFile(_uiFile, null, null);
					_rootUIContainer.loadDefalutScriptFile(_headerViewPath);
					DoUIModule _model = _rootUIContainer.getRootView();
					View _headerView = (View) _model.getCurrentUIModuleView();
					// 设置headerView 的 宽高
					_headerView.setLayoutParams(new LayoutParams((int) _model.getRealWidth(), (int) _model.getRealHeight()));
					addHeaderView(_headerView);
					this.supportHeaderRefresh = true;
				} else {
					this.supportHeaderRefresh = false;
					DoServiceContainer.getLogEngine().writeDebug("试图打开一个无效的页面文件:" + _headerViewPath);
				}
			} catch (Exception _err) {
				DoServiceContainer.getLogEngine().writeError("DoListView  headerView \n", _err);
			}
		}
		listview.setBackgroundColor(Color.TRANSPARENT);
		this.addView((View) listview, new LinearLayout.LayoutParams(-1, -1));
	}

	private void addHeaderView(View _mHeaderView) {
		// header view
		this.mHeaderView = _mHeaderView;
		// header layout
		DoUIModuleHelper.measureView(mHeaderView);
		mHeaderViewHeight = mHeaderView.getMeasuredHeight();
		LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, mHeaderViewHeight);
		// 设置topMargin的值为负的header View高度,即将其隐藏在最上方
		params.topMargin = -(mHeaderViewHeight);
		addView(mHeaderView, params);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent e) {
		int y = (int) e.getRawY();
		int x = (int) e.getRawX();
		switch (e.getAction()) {
		case MotionEvent.ACTION_DOWN:
			// 首先拦截down事件,记录y坐标
			mLastMotionY = y;
			mLastMotionX = x;
			break;
		case MotionEvent.ACTION_MOVE:
			// deltaY > 0 是向下运动,< 0是向上运动
			int deltaX = x - mLastMotionX;
			int deltaY = y - mLastMotionY;
			boolean isRefresh = isRefreshViewScroll(deltaX, deltaY);
			// 一旦底层View收到touch的action后调用这个方法那么父层View就不会再调用onInterceptTouchEvent了，也无法截获以后的action
			getParent().requestDisallowInterceptTouchEvent(isRefresh);
			if (isRefresh) {
				return true;
			}
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			break;
		}
		return false;
	}

	/**
	 * 是否应该到了父View,即PullToRefreshView滑动
	 * 
	 * @param deltaY
	 *            , deltaY > 0 是向下运动,< 0是向上运动
	 * @return
	 */
	private boolean isRefreshViewScroll(int deltaX, int deltaY) {
		if (mHeaderState == REFRESHING) {
			return false;
		}
		// 对于ListView和GridView
		if (listview != null) {
			// 子view(ListView or GridView)滑动到最顶端
			int angle = (int) (Math.atan2(Math.abs(deltaY), Math.abs(deltaX)) * 100);
			if (angle > 60) {
				if (deltaY > 0 && supportHeaderRefresh) {

					View child = listview.getChildAt(0);
					if (child == null) {
						// 如果mAdapterView中没有数据,不拦截
						mPullState = PULL_DOWN_STATE;
						return true;
					}
					if (listview.getFirstVisiblePosition() == 0 && child.getTop() == 0) {
						mPullState = PULL_DOWN_STATE;
						return true;
					}
					int top = child.getTop();
					int padding = listview.getPaddingTop();
					if (listview.getFirstVisiblePosition() == 0 && Math.abs(top - padding) <= 8) {// 这里之前用3可以判断,但现在不行,还没找到原因
						mPullState = PULL_DOWN_STATE;
						return true;
					}

				}
			}
		}
		return false;
	}

	/*
	 * 如果在onInterceptTouchEvent()方法中没有拦截(即onInterceptTouchEvent()方法中 return
	 * false)则由PullToRefreshView 的子View来处理;否则由下面的方法来处理(即由PullToRefreshView自己来处理)
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		int y = (int) event.getRawY();
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			// onInterceptTouchEvent已经记录
			// mLastMotionY = y;
			break;
		case MotionEvent.ACTION_MOVE:
			int deltaY = y - mLastMotionY;
			if (mPullState == PULL_DOWN_STATE) {// 执行下拉
				if (supportHeaderRefresh)
					headerPrepareToRefresh(deltaY);
				// setHeaderPadding(-mHeaderViewHeight);
			}
			mLastMotionY = y;
			break;
		case MotionEvent.ACTION_UP:
		case MotionEvent.ACTION_CANCEL:
			int topMargin = getHeaderTopMargin();
			if (mPullState == PULL_DOWN_STATE) {
				if (topMargin >= 0) {
					// 开始刷新
					if (supportHeaderRefresh)
						headerRefreshing();
				} else {
					if (supportHeaderRefresh)
						// 还没有执行刷新，重新隐藏
						setHeaderTopMargin(-mHeaderViewHeight);
				}
			}
			break;
		}
		return false;
	}

	/**
	 * 获取当前header view 的topMargin
	 * 
	 */
	private int getHeaderTopMargin() {
		LayoutParams params = (LayoutParams) mHeaderView.getLayoutParams();
		return params.topMargin;
	}

	/**
	 * header refreshing
	 * 
	 */
	private void headerRefreshing() {
		mHeaderState = REFRESHING;
		setHeaderTopMargin(0);
		doPullRefresh(mHeaderState, 0);
	}

	/**
	 * 设置header view 的topMargin的值
	 * 
	 * @param topMargin
	 *            ，为0时，说明header view 刚好完全显示出来； 为-mHeaderViewHeight时，说明完全隐藏了
	 */
	private void setHeaderTopMargin(int topMargin) {
		LayoutParams params = (LayoutParams) mHeaderView.getLayoutParams();
		params.topMargin = topMargin;
		mHeaderView.setLayoutParams(params);
		invalidate();
	}

	/**
	 * header view 完成更新后恢复初始状态
	 * 
	 */
	public void onHeaderRefreshComplete() {
		setHeaderTopMargin(-mHeaderViewHeight);
		mHeaderState = PULL_TO_REFRESH;
	}

	/**
	 * header 准备刷新,手指移动过程,还没有释放
	 * 
	 * @param deltaY
	 *            ,手指滑动的距离
	 */
	private void headerPrepareToRefresh(int deltaY) {
		int newTopMargin = changingHeaderViewTopMargin(deltaY);
		// 当header view的topMargin>=0时，说明已经完全显示出来了,修改header view 的提示状态
		if (newTopMargin >= 0 && mHeaderState != RELEASE_TO_REFRESH) {
			mHeaderState = RELEASE_TO_REFRESH;
		} else if (newTopMargin < 0 && newTopMargin > -mHeaderViewHeight) {// 拖动时没有释放
			mHeaderState = PULL_TO_REFRESH;
		}
		doPullRefresh(mHeaderState, newTopMargin);
	}

	/**
	 * 修改Header view top margin的值
	 * 
	 * @param deltaY
	 */
	private int changingHeaderViewTopMargin(int deltaY) {
		LayoutParams params = (LayoutParams) mHeaderView.getLayoutParams();
		float newTopMargin = params.topMargin + deltaY * 0.3f;
		// 这里对上拉做一下限制,因为当前上拉后然后不释放手指直接下拉,会把下拉刷新给触发了
		// 表示如果是在上拉后一段距离,然后直接下拉
//		if (deltaY > 0 && mPullState == PULL_UP_STATE && Math.abs(params.topMargin) <= mHeaderViewHeight) {
//			return params.topMargin;
//		}
		// 同样地,对下拉做一下限制,避免出现跟上拉操作时一样的bug
		if (deltaY < 0 && mPullState == PULL_DOWN_STATE && Math.abs(params.topMargin) >= mHeaderViewHeight) {
			return params.topMargin;
		}
		params.topMargin = (int) newTopMargin;
		mHeaderView.setLayoutParams(params);
		invalidate();
		return params.topMargin;
	}

	/**
	 * 动态修改属性值时会被调用，方法返回值为true表示赋值有效，并执行onPropertiesChanged，否则不进行赋值；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public boolean onPropertiesChanging(Map<String, String> _changedValues) {
		if (_changedValues.containsKey("templates")) {
			String value = _changedValues.get("templates");
			if ("".equals(value)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 属性赋值成功后被调用，可以根据组件定义相关属性值修改UIView可视化操作；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public void onPropertiesChanged(Map<String, String> _changedValues) {
		DoUIModuleHelper.handleBasicViewProperChanged(this.model, _changedValues);
		if (_changedValues.containsKey("herderView")) {

		}
		if (_changedValues.containsKey("isShowbar")) {
			boolean _isShowbar = DoTextHelper.strToBool(_changedValues.get("isShowbar"), true);
			listview.setVerticalScrollBarEnabled(_isShowbar);
		}
		if (_changedValues.containsKey("selectedColor")) {
			try {
				String _bgColor = this.model.getPropertyValue("bgColor");
				String _selectedColor = _changedValues.get("selectedColor");
				Drawable normal = new ColorDrawable(DoUIModuleHelper.getColorFromString(_bgColor, Color.WHITE));
				Drawable selected = new ColorDrawable(DoUIModuleHelper.getColorFromString(_selectedColor, Color.WHITE));
				Drawable pressed = new ColorDrawable(DoUIModuleHelper.getColorFromString(_selectedColor, Color.WHITE));
				listview.setSelector(getBg(normal, selected, pressed));
			} catch (Exception _err) {
				DoServiceContainer.getLogEngine().writeError("do_ListView selectedColor \n\t", _err);
			}
		}

		if (_changedValues.containsKey("templates")) {
			initViewTemplate(_changedValues.get("templates"));
			listview.setAdapter(myAdapter);
		}
	}

	/**
	 * 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V）
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public boolean invokeSyncMethod(String _methodName, DoJsonNode _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if ("rebound".equals(_methodName)) {
			rebound(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("bindItems".equals(_methodName)) {
			bindItems(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("refreshItems".equals(_methodName)) {
			refreshItems(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		return false;
	}

	/**
	 * 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用， 可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V）
	 * @_scriptEngine 当前page JS上下文环境
	 * @_callbackFuncName 回调函数名 #如何执行异步方法回调？可以通过如下方法：
	 *                    _scriptEngine.callback(_callbackFuncName,
	 *                    _invokeResult);
	 *                    参数解释：@_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象；
	 *                    获取DoInvokeResult对象方式new
	 *                    DoInvokeResult(this.model.getUniqueKey());
	 */
	@Override
	public boolean invokeAsyncMethod(String _methodName, DoJsonNode _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
		// ...do something
		return false;
	}

	private void refreshItems(DoJsonNode _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) {
		myAdapter.notifyDataSetChanged();
	}

	private void bindItems(DoJsonNode _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		String _address = _dictParas.getOneText("data", "");
		if (_address == null || _address.length() <= 0)
			throw new Exception("doListView 未指定相关的listview data参数！");
		DoMultitonModule _multitonModule = DoScriptEngineHelper.parseMultitonModule(_scriptEngine, _address);
		if (_multitonModule == null)
			throw new Exception("doListView data参数无效！");
		if (_multitonModule instanceof DoIListData) {
			DoIListData _data = (DoIListData) _multitonModule;
			myAdapter.bindData(_data);
		}
	}

	/**
	 * 释放资源处理，前端JS脚本调用closePage或执行removeui时会被调用；
	 */
	@Override
	public void onDispose() {
		// ...do something
	}

	/**
	 * 重绘组件，构造组件时由系统框架自动调用；
	 * 或者由前端JS脚本调用组件onRedraw方法时被调用（注：通常是需要动态改变组件（X、Y、Width、Height）属性时手动调用）
	 */
	@Override
	public void onRedraw() {
		this.setLayoutParams(DoUIModuleHelper.getLayoutParams(this.model));
	}

	private void initViewTemplate(String data) {
		try {
			myAdapter.initTemplates(data.split(","));
		} catch (Exception e) {
			DoServiceContainer.getLogEngine().writeError("解析cell属性错误： \t", e);
		}
	}

	private class MyAdapter extends BaseAdapter {
		private Map<String, String> viewTemplates = new HashMap<String, String>();
		private List<String> cellTemplates = new ArrayList<String>();
		private Map<Integer, Integer> datasPositionMap = new HashMap<Integer, Integer>();
		private DoIListData data;

		public void bindData(DoIListData _listData) {
			this.data = _listData;
		}

		public void initTemplates(String[] templates) throws Exception {
			cellTemplates.clear();
			for (String templateUi : templates) {
				if (templateUi != null && !templateUi.equals("")) {
					DoSourceFile _sourceFile = model.getCurrentPage().getCurrentApp().getSourceFS().getSourceByFileName(templateUi);
					if (_sourceFile != null) {
						viewTemplates.put(templateUi, _sourceFile.getTxtContent());
						cellTemplates.add(templateUi);
					} else {
						throw new Exception("试图使用一个无效的页面文件:" + templateUi);
					}
				}
			}
		}

		@Override
		public void notifyDataSetChanged() {
			int _size = data.getCount();
			for (int i = 0; i < _size; i++) {
				DoJsonValue childData = (DoJsonValue) data.getData(i);
				try {
					Integer index = DoTextHelper.strToInt(childData.getNode().getOneText("template", "0"), 0);
					datasPositionMap.put(i, index);
				} catch (Exception e) {
					DoServiceContainer.getLogEngine().writeError("解析data数据错误： \t", e);
				}
			}
			super.notifyDataSetChanged();
		}

		@Override
		public int getCount() {
			if (data == null) {
				return 0;
			}
			return data.getCount();
		}

		@Override
		public Object getItem(int position) {
			return data.getData(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public int getItemViewType(int position) {
			return datasPositionMap.get(position);
		}

		@Override
		public int getViewTypeCount() {
			return cellTemplates.size();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			DoJsonValue childData = (DoJsonValue) data.getData(position);
			try {
				DoIUIModuleView _doIUIModuleView = null;
				int _index = DoTextHelper.strToInt(childData.getNode().getOneText("template", "0"), 0);
				String templateUI = cellTemplates.get(_index);
				if (convertView == null) {
					String content = viewTemplates.get(templateUI);
					DoUIContainer _doUIContainer = new DoUIContainer(model.getCurrentPage());
					_doUIContainer.loadFromContent(content, null, null);
					_doUIContainer.loadDefalutScriptFile(templateUI);// @zhuozy效率问题，listview第一屏可能要加载多次模版、脚本，需改进需求设计；
					_doIUIModuleView = _doUIContainer.getRootView().getCurrentUIModuleView();
				} else {
					_doIUIModuleView = (DoIUIModuleView) convertView;
				}
				if (_doIUIModuleView != null) {
					_doIUIModuleView.getModel().setModelData(childData);

					View _childView = (View) _doIUIModuleView;
					// 设置headerView 的 宽高
					_childView.setLayoutParams(new AbsListView.LayoutParams((int) _doIUIModuleView.getModel().getRealWidth(), (int) _doIUIModuleView.getModel().getRealHeight()));
					return _childView;
				}
			} catch (Exception e) {
				DoServiceContainer.getLogEngine().writeError("解析data数据错误： \t", e);
			}
			return null;
		}

	}

	/**
	 * 获取当前model实例
	 */
	@Override
	public DoUIModule getModel() {
		return model;
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		doListView_LongTouch(position);
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		doListView_Touch(position);
	}

	private void doListView_Touch(int position) {
		DoInvokeResult _invokeResult = new DoInvokeResult(this.model.getUniqueKey());
		_invokeResult.setResultInteger(position);
		this.model.getEventCenter().fireEvent("touch", _invokeResult);
	}

	private void doListView_LongTouch(int position) {
		DoInvokeResult _invokeResult = new DoInvokeResult(this.model.getUniqueKey());
		_invokeResult.setResultInteger(position);
		this.model.getEventCenter().fireEvent("longTouch", _invokeResult);
	}

	private StateListDrawable getBg(Drawable normal, Drawable selected, Drawable pressed) {
		StateListDrawable bg = new StateListDrawable();
		bg.addState(View.PRESSED_ENABLED_STATE_SET, pressed);
		bg.addState(View.ENABLED_FOCUSED_STATE_SET, selected);
		bg.addState(View.ENABLED_STATE_SET, normal);
		bg.addState(View.FOCUSED_STATE_SET, selected);
		bg.addState(View.EMPTY_STATE_SET, normal);
		return bg;
	}

	private void rebound(DoJsonNode _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) {
		onHeaderRefreshComplete();
	}

	private void doPullRefresh(int mHeaderState, int newTopMargin) {
		DoInvokeResult _invokeResult = new DoInvokeResult(this.model.getUniqueKey());
		try {
			DoJsonNode _node = new DoJsonNode();
			_node.setOneInteger("state", mHeaderState);
			_node.setOneText("y", (newTopMargin / this.model.getYZoom()) + "");
			_invokeResult.setResultNode(_node);
			this.model.getEventCenter().fireEvent("pull", _invokeResult);
		} catch (Exception _err) {
			DoServiceContainer.getLogEngine().writeError("DoListView pull \n", _err);
		}
	}
}
