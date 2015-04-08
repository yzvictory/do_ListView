package extimplement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import core.DoServiceContainer;
import core.helper.DoScriptEngineHelper;
import core.helper.DoTextHelper;
import core.helper.DoUIModuleHelper;
import core.helper.jsonparse.DoJsonNode;
import core.helper.jsonparse.DoJsonValue;
import core.interfaces.DoIListData;
import core.interfaces.DoIScriptEngine;
import core.interfaces.DoIUIModuleView;
import core.object.DoInvokeResult;
import core.object.DoMultitonModule;
import core.object.DoSourceFile;
import core.object.DoUIContainer;
import core.object.DoUIModule;
import extdefine.do_ListView_IMethod;
import extdefine.do_ListView_MAbstract;

/**
 * 自定义扩展UIView组件实现类，此类必须继承相应VIEW类，并实现DoIUIModuleView,Do_ListView_IMethod接口；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.model.getUniqueKey());
 */
public class do_ListView_View extends ListView implements DoIUIModuleView, do_ListView_IMethod, android.widget.AdapterView.OnItemClickListener, android.widget.AdapterView.OnItemLongClickListener {

	/**
	 * 每个UIview都会引用一个具体的model实例；
	 */
	private do_ListView_MAbstract model;

	protected MyAdapter myAdapter;

	public do_ListView_View(Context context) {
		super(context);
		myAdapter = new MyAdapter();
	}

	/**
	 * 初始化加载view准备,_doUIModule是对应当前UIView的model实例
	 */
	@Override
	public void loadView(DoUIModule _doUIModule) throws Exception {
		this.model = (do_ListView_MAbstract) _doUIModule;
		this.setOnItemClickListener(this);
		this.setOnItemLongClickListener(this);
	}

	/**
	 * 动态修改属性值时会被调用，方法返回值为true表示赋值有效，并执行onPropertiesChanged，否则不进行赋值；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public boolean onPropertiesChanging(Map<String, String> _changedValues) {
		if (_changedValues.containsKey("cellTemplates")) {
			String value = _changedValues.get("cellTemplates");
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
			this.setVerticalScrollBarEnabled(_isShowbar);
		}
		if (_changedValues.containsKey("selectedColor")) {
			try {
				String _bgColor = this.model.getPropertyValue("bgColor");
				String _selectedColor = _changedValues.get("selectedColor");
				Drawable normal = new ColorDrawable(DoUIModuleHelper.getColorFromString(_bgColor, Color.WHITE));
				Drawable selected = new ColorDrawable(DoUIModuleHelper.getColorFromString(_selectedColor, Color.WHITE));
				Drawable pressed = new ColorDrawable(DoUIModuleHelper.getColorFromString(_selectedColor, Color.WHITE));
				this.setSelector(getBg(normal, selected, pressed));
			} catch (Exception _err) {
				DoServiceContainer.getLogEngine().writeError("do_ListView selectedColor \n\t", _err);
			}
		}

		if (_changedValues.containsKey("cellTemplates")) {
			initViewTemplate(_changedValues.get("cellTemplates"));
			this.setAdapter(myAdapter);
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
		if ("bindData".equals(_methodName)) {
			bindData(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("refresh".equals(_methodName)) {
			refresh(_dictParas, _scriptEngine, _invokeResult);
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

	@Override
	public void bindData(DoJsonNode _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		String _address = _dictParas.getOneText("data", "");
		if (_address == null || _address.length() <= 0)
			throw new Exception("未指定相关的DataModel参数！");
		DoMultitonModule _multitonModule = DoScriptEngineHelper.parseMultitonModule(_scriptEngine, _address);
		if (_multitonModule == null || !(_multitonModule instanceof DoIListData))
			throw new Exception("model参数无效!");
		DoIListData _listData = (DoIListData) _multitonModule;
		myAdapter.bindData(_listData);
	}

	@Override
	public void refresh(DoJsonNode _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		myAdapter.notifyDataSetChanged();
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
					Integer index = DoTextHelper.strToInt(childData.getNode().getOneText("cellTemplate", "0"), 0);
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
				int _index = DoTextHelper.strToInt(childData.getNode().getOneText("cellTemplate", "0"), 0);
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
					_doIUIModuleView.getModel().setModelData(null, childData);
					return (View) _doIUIModuleView;
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
}
