using doCore.Helper;
using doCore.Helper.JsonParse;
using doCore.Interface;
using doCore.Object;
using do_ListView.extdefine;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;
using System.IO;
using Windows.Storage;
using Windows.Storage.Streams;
using Windows.UI;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Media;
using Windows.UI.Xaml.Media.Imaging;
using Windows.UI.Text;
using doCore;

namespace do_ListView.extimplement
{
    /// <summary>
    /// 自定义扩展UIView组件实现类，此类必须继承相应控件类或UserControl类，并实现doIUIModuleView,@TYPEID_IMethod接口；
    /// #如何调用组件自定义事件？可以通过如下方法触发事件：
    /// this.model.EventCenter.fireEvent(_messageName, jsonResult);
    /// 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象；
    /// 获取doInvokeResult对象方式new doInvokeResult(model.UniqueKey);
    /// </summary>
    public class do_ListView_View : ListView, doIUIModuleView, do_ListView_IMethod
    {
        /// <summary>
        /// 每个UIview都会引用一个具体的model实例；
        /// </summary>
        private do_ListView_MAbstract model;
        Dictionary<string, string> DicTemplates = new Dictionary<string, string>();
        doIListData listdata;
        string selectedColor = "";
        public do_ListView_View()
        {

        }
        /// <summary>
        /// 初始化加载view准备,_doUIModule是对应当前UIView的model实例
        /// </summary>
        /// <param name="_doComponentUI"></param>
        public void LoadView(doUIModule _doUIModule)
        {
            this.model = (do_ListView_MAbstract)_doUIModule;
            this.HorizontalAlignment = Windows.UI.Xaml.HorizontalAlignment.Left;
            this.VerticalAlignment = Windows.UI.Xaml.VerticalAlignment.Top;
        }

        public doUIModule GetModel()
        {
            return this.model;
        }

        /// <summary>
        /// 动态修改属性值时会被调用，方法返回值为true表示赋值有效，并执行OnPropertiesChanged，否则不进行赋值；
        /// </summary>
        /// <param name="_changedValues">属性集（key名称、value值）</param>
        /// <returns></returns>
        public bool OnPropertiesChanging(Dictionary<string, string> _changedValues)
        {
            return true;
        }
        /// <summary>
        /// 属性赋值成功后被调用，可以根据组件定义相关属性值修改UIView可视化操作；
        /// </summary>
        /// <param name="_changedValues">属性集（key名称、value值）</param>
        public void OnPropertiesChanged(Dictionary<string, string> _changedValues)
        {
            doUIModuleHelper.HandleBasicViewProperChanged(this.model, _changedValues);
            if (_changedValues.ContainsKey("cellTemplates"))
            {
                GetTemplateGroup(_changedValues["cellTemplates"]);
            }
            if (_changedValues.ContainsKey("selectedColor"))
            {
                selectedColor = _changedValues["selectedColor"];
            }
        }
        /// <summary>
        /// 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
        /// </summary>
        /// <param name="_methodName">方法名称</param>
        /// <param name="_dictParas">参数（K,V）</param>
        /// <param name="_scriptEngine">当前Page JS上下文环境对象</param>
        /// <param name="_invokeResult">用于返回方法结果对象</param>
        /// <returns></returns>
        public bool InvokeSyncMethod(string _methodName, doJsonNode _dictParas, doIScriptEngine _scriptEngine, doInvokeResult _invokeResult)
        {
            return false;
        }
        /// <summary>
        /// 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用，
        /// 可以根据_methodName调用相应的接口实现方法；#如何执行异步方法回调？可以通过如下方法：
        /// _scriptEngine.callback(_callbackFuncName, _invokeResult);
        /// 参数解释：@_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象；
        /// 获取doInvokeResult对象方式new doInvokeResult(model.UniqueKey);
        /// </summary>
        /// <param name="_methodName">方法名称</param>
        /// <param name="_dictParas">参数（K,V）</param>
        /// <param name="_scriptEngine">当前page JS上下文环境</param>
        /// <param name="_callbackFuncName">回调函数名</param>
        /// <returns></returns>
        public bool InvokeAsyncMethod(string _methodName, doJsonNode _dictParas, doIScriptEngine _scriptEngine, string _callbackFuncName)
        {
            doInvokeResult _invokeResult = _scriptEngine.CreateInvokeResult(this.model.UniqueKey);

            return false;
        }
        /// <summary>
        /// 重绘组件，构造组件时由系统框架自动调用；
        /// 或者由前端JS脚本调用组件onRedraw方法时被调用（注：通常是需要动态改变组件（X、Y、Width、Height）属性时手动调用）
        /// </summary>
        public void OnRedraw()
        {
            var tp = doUIModuleHelper.GetThickness(this.model);
            this.Margin = tp.Item1;
            this.Width = tp.Item2;
            this.Height = tp.Item3;
        }
        /// <summary>
        /// 释放资源处理，前端JS脚本调用closePage或执行removeui时会被调用；
        /// </summary>
        public void OnDispose()
        {

        }

        void lvi_LostFocus(object sender, RoutedEventArgs e)
        {
            ListViewItem listitem = sender as ListViewItem;
            listitem.Background = new SolidColorBrush(Colors.Transparent);
        }
        void lvi_Tapped(object sender, Windows.UI.Xaml.Input.TappedRoutedEventArgs e)
        {
            ListViewItem listitem = sender as ListViewItem;
            listitem.Background = doUIModuleHelper.GetColorFromString(selectedColor, new SolidColorBrush(Colors.Transparent));

            doInvokeResult _invokeResult = new doInvokeResult(this.model.UniqueKey);
            this.model.EventCenter.FireEvent("touch", _invokeResult);
        }
        void lvi_Holding(object sender, Windows.UI.Xaml.Input.HoldingRoutedEventArgs e)
        {
            doInvokeResult _invokeResult = new doInvokeResult(this.model.UniqueKey);
            this.model.EventCenter.FireEvent("longtouch", _invokeResult);
        }

        public void setModelData(object _obj)
        {
            if (_obj == null)
                return;
            if (_obj is doIListData)
            {
                listdata = _obj as doIListData;
                bind();
            }

        }
        private void bind()
        {
            this.Items.Clear();
            List<doJsonValue> ja = new List<doJsonValue>();
            for (int i = 0; i < listdata.getCount(); i++)
            {
                ja.Add((listdata.getData(i) as doJsonValue));
            }
            SetListItems(ja);
        }
        private void GetTemplateGroup(string templates)
        {
            string[] temps = templates.Split(',');
            foreach (var item in temps)
            {
                if (item.Length > 0 && item != null)
                {
                    doSourceFile _sourceFile = model.CurrentPage.CurrentApp.SourceFS.GetSourceByFileName(item);
                    string tempcontent = _sourceFile.TxtContent();
                    DicTemplates.Add(item, tempcontent);
                }
            }
        }
        private string GetTempContent(int template)
        {
            return DicTemplates.ElementAt(template).Value;
        }
        private async void SetListItems(List<doJsonValue> ja)
        {
            foreach (var item in ja)
            {
                doIUIModuleView _doIUIModuleView = null;
                int viewtemplate = item.GetNode().GetOneInteger("cellTemplate", 0);
                string viewtemplatetemp = "";
                string tempcontent = GetTempContent(viewtemplate);
                doUIContainer _doUIContainer = new doUIContainer(model.CurrentPage);
                _doUIContainer.loadFromContent(tempcontent, null, null);
                if (viewtemplate == 0)
                {
                    viewtemplatetemp = DicTemplates.First().Key;
                }
                else
                {
                    viewtemplatetemp = DicTemplates.ElementAt(viewtemplate).Key;
                }
                _doUIContainer.loadDefalutScriptFile(viewtemplatetemp);

                _doIUIModuleView = _doUIContainer.RootView.CurrentComponentUIView;
                doUIContainer doUIContainer = _doIUIModuleView.GetModel().CurrentUIContainer;

                _doIUIModuleView = _doUIContainer.RootView.CurrentComponentUIView;

                if (_doIUIModuleView != null)
                {
                    await _doIUIModuleView.GetModel().SetModelData(null, item);
                }

                FrameworkElement fe = _doIUIModuleView as FrameworkElement;
                ListViewItem lvi = new ListViewItem();
                lvi.Tapped += lvi_Tapped;
                lvi.Holding += lvi_Holding;
                lvi.LostFocus += lvi_LostFocus;
                lvi.Content = fe;
                this.Items.Add(lvi);
            }
        }
    }
}
