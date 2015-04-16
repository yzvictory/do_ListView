package doext.implement;

import java.util.Map;

import core.helper.jsonparse.DoJsonValue;
import core.interfaces.DoIListData;
import doext.define.do_ListView_MAbstract;

/**
 * 自定义扩展组件Model实现，继承Do_ListView_MAbstract抽象类；
 * 
 */
public class do_ListView_Model extends do_ListView_MAbstract {

	public do_ListView_Model() throws Exception {
		super();
	}

	@Override
	public void setModelData(Map<String, DoJsonValue> _bindParas, Object _obj) throws Exception {
		if (_obj instanceof DoIListData) {
			do_ListView_View _view = (do_ListView_View) this.getCurrentUIModuleView();
			_view.setModelData(_obj);
		} else {
			super.setModelData(_bindParas, _obj);
		}
	}

}
