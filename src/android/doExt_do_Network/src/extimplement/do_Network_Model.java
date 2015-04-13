package extimplement;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.apache.http.conn.util.InetAddressUtils;

import android.content.Context;
import android.telephony.TelephonyManager;
import core.DoServiceContainer;
import core.helper.DoSingletonModuleHelper;
import core.helper.jsonparse.DoJsonNode;
import core.interfaces.DoIScriptEngine;
import core.object.DoInvokeResult;
import core.object.DoSingletonModule;
import extapp.NetWorkChangedListener;
import extapp.do_Network_App;
import extdefine.do_Network_IMethod;

/**
 * 自定义扩展SM组件Model实现，继承DoSingletonModule抽象类，并实现do_Network_IMethod接口方法；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.getUniqueKey());
 */
public class do_Network_Model extends DoSingletonModule implements do_Network_IMethod, NetWorkChangedListener {

	public do_Network_Model() throws Exception {
		super();
		do_Network_App.getInstance().setNetWorkChangedListener(this);
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
		if ("getStatus".equals(_methodName)) { // 获取应用ID
			this.getStatus(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("getIP".equals(_methodName)) { // 获取应用ID
			this.getIP(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("getOperators".equals(_methodName)) { // 获取应用ID
			this.getOperators(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}

		return super.invokeSyncMethod(_methodName, _dictParas, _scriptEngine, _invokeResult);
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
	 *                    DoInvokeResult(this.getUniqueKey());
	 */
	@Override
	public boolean invokeAsyncMethod(String _methodName, DoJsonNode _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) throws Exception {
		// ...do something
		return super.invokeAsyncMethod(_methodName, _dictParas, _scriptEngine, _callbackFuncName);
	}

	/**
	 * 获取移动终端ip地址；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void getIP(DoJsonNode _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		_invokeResult.setResultText(getLocalIpAddress());
	}

	private String getLocalIpAddress() {
		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface intf = en.nextElement();
				for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
					InetAddress inetAddress = enumIpAddr.nextElement();
					if (!inetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(inetAddress.getHostAddress())) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException e) {
			e.printStackTrace();
		}

		return "";
	}

	/**
	 * 需要添加到权限 <uses-permission
	 * android:name="android.permission.ACCESS_NETWORK_STATE"/> <uses-permission
	 * android:name="android.permission.ACCESS_WIFI_STATE"/> <uses-permission
	 * android:name="android.permission.CHANGE_WIFI_STATE"/> <uses-permission
	 * android:name="android.permission.WAKE_LOCK" /> <uses-permission
	 * android:name="android.permission.INTERNET" />
	 */

	/**
	 * 获取设备的运营商；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void getOperators(DoJsonNode _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		_invokeResult.setResultText(getProvidersName(DoServiceContainer.getPageViewFactory().getAppContext()));
	}

	/**
	 * 获取手机服务商信息 需要加入权限<uses-permission
	 * android:name="android.permission.READ_PHONE_STATE"/>
	 */
	public String getProvidersName(Context context) {
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String providersName = "";
		// 返回唯一的用户ID;就是这张卡的编号神马的
		String IMSI = telephonyManager.getSubscriberId();
		// IMSI号前面3位460是国家，紧接着后面2位00 02是中国移动，01是中国联通，03是中国电信。
		if (IMSI.startsWith("46000") || IMSI.startsWith("46002")) {
			providersName = "中国移动";
		} else if (IMSI.startsWith("46001")) {
			providersName = "中国联通";
		} else if (IMSI.startsWith("46003")) {
			providersName = "中国电信";
		}
		return providersName;
	}

	/**
	 * 获取当前网络状态；
	 * 
	 * @_dictParas 参数（K,V），可以通过此对象提供相关方法来获取参数值（Key：为参数名称）；
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public void getStatus(DoJsonNode _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		String netWorkType = DoSingletonModuleHelper.getAPNType(DoServiceContainer.getPageViewFactory().getAppContext());
		_invokeResult.setResultText(netWorkType);
	}

	@Override
	public void changed(String networkType) {
		DoInvokeResult _invokeResult = new DoInvokeResult(this.getUniqueKey());
		_invokeResult.setResultText(networkType);
		this.getEventCenter().fireEvent("changed", _invokeResult);
	}
}