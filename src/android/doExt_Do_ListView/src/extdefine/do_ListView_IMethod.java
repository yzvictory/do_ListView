package extdefine;

import core.helper.jsonparse.DoJsonNode;
import core.interfaces.DoIScriptEngine;
import core.object.DoInvokeResult;

/**
 * 声明自定义扩展组件方法
 */
public interface do_ListView_IMethod {
	void bindData(DoJsonNode _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception;

	void refresh(DoJsonNode _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception;
}