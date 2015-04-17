using doCore.Object;
using do_ListView.extdefine;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using doCore.Helper.JsonParse;
using doCore.Interface;

namespace do_ListView.extimplement
{
    /// <summary>
    /// 自定义扩展组件Model实现，继承@TYPEID_MAbstract抽象类；
    /// </summary>
    public class do_ListView_Model : do_ListView_MAbstract
    {
        public do_ListView_Model()
            : base()
        {
        }
        public override void OnInit()
        {
            base.OnInit();
            this.RegistProperty(new doProperty("selectedColor", PropertyDataType.String, "", false));
            this.RegistProperty(new doProperty("cellTemplates", PropertyDataType.String, "", false));
            this.RegistProperty(new doProperty("herderView", PropertyDataType.String, "", false));
            this.RegistProperty(new doProperty("isShowbar", PropertyDataType.Bool, "", true));
        }
        public override async Task<bool> InvokeAsyncMethod(string _methodName, doCore.Helper.JsonParse.doJsonNode _dictParas, doCore.Interface.doIScriptEngine _scriptEngine, string _callbackFuncName)
        {
            if (await base.InvokeAsyncMethod(_methodName, _dictParas, _scriptEngine, _callbackFuncName)) return true;
            return false;
        }
        public override bool InvokeSyncMethod(string _methodName, doCore.Helper.JsonParse.doJsonNode _dictParas, doCore.Interface.doIScriptEngine _scriptEngine, doCore.Object.doInvokeResult _invokeResult)
        {
            if (base.InvokeSyncMethod(_methodName, _dictParas, _scriptEngine, _invokeResult)) return true;
            return false;
        }
        public override async Task SetModelData(Dictionary<string, doJsonValue> _bindParas, object _obj)
        {
            if (_obj is doIListData)
            {
                do_ListView_View _view = (do_ListView_View)this.CurrentComponentUIView;
                _view.setModelData(_obj);
            }
            else
            {
                await base.SetModelData(_bindParas, _obj);
            }
        }
    }
}
