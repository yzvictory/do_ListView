//
//  TYPEID_View.m
//  DoExt_UI
//
//  Created by @userName on @time.
//  Copyright (c) 2015年 DoExt. All rights reserved.
//

#import "do_ListView_UIView.h"

#import "doUIModuleHelper.h"
#import "doScriptEngineHelper.h"
#import "doInvokeResult.h"
#import "doIPage.h"
#import "doISourceFS.h"
#import "doUIContainer.h"

@implementation do_ListView_UIView
{
    NSMutableDictionary *_cellTemplatesDics;
    NSMutableArray *_cellTemplatesArray;
    NSMutableArray* modulesArray;
    UIColor *_selectColor;
    doUIModule *_headViewModel;
    id<doIListData> _dataArrays;
    BOOL _isRefreshing;
}

#pragma mark - doIUIModuleView协议方法（必须）
//引用Model对象
- (void) LoadView: (doUIModule *) _doUIModule
{
    _model = (typeof(_model)) _doUIModule;
    self.separatorStyle = UITableViewCellSeparatorStyleNone;
    _cellTemplatesDics = [[NSMutableDictionary alloc]init];
    modulesArray = [[NSMutableArray alloc]init];
    _cellTemplatesArray = [[NSMutableArray alloc]init];
    self.delegate = self;
    self.dataSource = self;

}
//销毁所有的全局对象
- (void) OnDispose
{
    _model = nil;
    //自定义的全局属性
    [(doModule*)_dataArrays Dispose];
    for(doModule* module in [_cellTemplatesDics allValues]){
        [module Dispose];
    }
    [_cellTemplatesDics removeAllObjects];
    _cellTemplatesDics = nil;
    for(int i =0;i<modulesArray.count;i++){
        [(doModule*) modulesArray[i] Dispose];
    }
    [modulesArray removeAllObjects];
    modulesArray = nil;
    [_cellTemplatesArray removeAllObjects];
    _cellTemplatesArray = nil;
    [_headViewModel Dispose];
    _headViewModel = nil;
}
//实现布局
- (void) OnRedraw
{
    //重新调整视图的x,y,w,h
    [doUIModuleHelper OnRedraw:_model];
    
    //实现布局相关的修改
    [_headViewModel.CurrentUIModuleView OnRedraw];
    UIView *headView = (UIView *)_headViewModel.CurrentUIModuleView;
    CGFloat realW = self.frame.size.width;
    CGFloat realH = realW/_headViewModel.RealWidth*_headViewModel.RealHeight;
    headView.frame = CGRectMake(0, -realH, realW, realH);
}

#pragma mark - TYPEID_IView协议方法（必须）
#pragma mark - Changed_属性
/*
 如果在Model及父类中注册过 "属性"，可用这种方法获取
 NSString *属性名 = [(doUIModule *)_model GetPropertyValue:@"属性名"];
 
 获取属性最初的默认值
 NSString *属性名 = [(doUIModule *)_model GetProperty:@"属性名"].DefaultValue;
 */
- (void)change_selectedColor:(NSString *)newValue
{
    UIColor *defulatCol = [doUIModuleHelper GetColorFromString:[_model GetProperty:@"selectedColor"].DefaultValue :[UIColor whiteColor]];
    _selectColor = [doUIModuleHelper GetColorFromString:newValue :defulatCol];
}
- (void)change_cellTemplates:(NSString *)newValue
{
    NSArray *arrays = [newValue componentsSeparatedByString:@","];
    [_cellTemplatesDics removeAllObjects];
    for(int i=0;i<arrays.count;i++)
    {
        NSString *modelStr = arrays[i];
        if(modelStr != nil && ![modelStr isEqualToString:@""])
        {
            doSourceFile *source = [[[_model.CurrentPage CurrentApp] SourceFS] GetSourceByFileName:modelStr];
            if(!source)
                [NSException raise:@"listview" format:@"试图使用无效的页面文件",nil];
            doUIContainer* _container = [[doUIContainer alloc] init:_model.CurrentPage];
            
            [_container LoadFromFile:source:nil:nil];
            doUIModule* _insertViewModel = _container.RootView;
            if (_insertViewModel == nil) {
                [NSException raise:@"listview" format:@"创建view失败",nil];
            }
            _cellTemplatesDics[modelStr] = _insertViewModel;
            [_cellTemplatesArray addObject:modelStr];
        }
    }
}
- (void)change_headerView:(NSString *)newValue
{
    id<doIPage> pageModel = _model.CurrentPage;
    doSourceFile *fileName = [pageModel.CurrentApp.SourceFS GetSourceByFileName:newValue];
    if(!fileName)
    {
        [NSException raise:@"listview" format:@"无效的headView:%@",newValue,nil];
        return;
    }
    doUIContainer *container = [[doUIContainer alloc] init:pageModel];
    [container LoadFromFile:fileName:nil:nil];
    _headViewModel = container.RootView;
    if (_headViewModel == nil)
    {
        [NSException raise:@"listview" format:@"创建view失败",nil];
        return;
    }
    UIView *insertView = (UIView*)_headViewModel.CurrentUIModuleView;
    if (insertView == nil)
    {
        [NSException raise:@"listview" format:@"创建view失败"];
        return;
    }
    [self addSubview:insertView];
    [container LoadDefalutScriptFile:newValue];
    //const CGFloat *color = CGColorGetComponents([_headView.backgroundColor CGColor]);
    //self.backgroundColor = [UIColor colorWithRed:color[0]/255 green:color[1]/255 blue:color[3]/255 alpha:color[4]/255];
}
- (void)change_isShowbar:(NSString *)newValue
{
    NSLog(@"change_isShowbar待实现......");
}
#pragma mark -
#pragma mark - 同步异步方法的实现

#pragma mark - private methed

- (void)fireEvent:(int)state :(CGFloat)y
{
    doJsonNode *node = [[doJsonNode alloc] init];
    [node SetOneInteger:@"state" :state];
    [node SetOneText:@"y" :[NSString stringWithFormat:@"%f",y]];
    doInvokeResult* _invokeResult = [[doInvokeResult alloc]init:_model.UniqueKey];
    [_invokeResult SetResultNode:node];
    [_model.EventCenter FireEvent:@"pull":_invokeResult];
}

-(void) SetModelData:(id<doIListData>) _jsonObject
{
    if(_dataArrays!= _jsonObject)
        _dataArrays = _jsonObject;
    [self reloadData];
}
#pragma mark - tableView sourcedelegate
- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section
{
    return [_dataArrays GetCount];
}

- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath
{
    doJsonValue *jsonValue = [_dataArrays GetData:(int)indexPath.row];
    doJsonNode *dataNode = [jsonValue GetNode];
    int cellIndex = [dataNode GetOneInteger:@"cellTemplate" :0];
    NSString* indentify = _cellTemplatesArray[cellIndex];
    doUIModule *showCellMode;
    
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:indentify];
    if(cell == nil)
    {
        cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleDefault reuseIdentifier:indentify];
        cell.selectedBackgroundView.backgroundColor = _selectColor;
        UILongPressGestureRecognizer *longPress = [[UILongPressGestureRecognizer alloc] initWithTarget:self action:@selector(longPress:)];
        [cell addGestureRecognizer:longPress];
         doSourceFile *source = [[[_model.CurrentPage CurrentApp] SourceFS] GetSourceByFileName:indentify];
        id<doIPage> pageModel = _model.CurrentPage;
        doUIContainer *container = [[doUIContainer alloc] init:pageModel];
        [container LoadFromFile:source:nil:nil];
        showCellMode = container.RootView;
        [container LoadDefalutScriptFile:indentify];
        UIView *insertView = (UIView*)showCellMode.CurrentUIModuleView;
        id<doIUIModuleView> modelView = showCellMode.CurrentUIModuleView;
        [modelView OnRedraw];
        [[cell contentView] addSubview:insertView];
        [modulesArray addObject:showCellMode];
    }
    else
    {
        showCellMode = [(id<doIUIModuleView>)[cell.contentView.subviews objectAtIndex:0] GetModel];
    }
    [showCellMode SetModelData:nil :jsonValue];
    return cell;
}
- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath
{
    [tableView deselectRowAtIndexPath:indexPath animated:YES];
    NSLog(@"%s",__func__);
    
    doInvokeResult* _invokeResult = [[doInvokeResult alloc]init:_model.UniqueKey];
    [_invokeResult SetResultInteger:(int)indexPath.row];
    [_model.EventCenter FireEvent:@"touch":_invokeResult];
}

#pragma mark - tableView delegate
- (CGFloat)tableView:(UITableView *)tableView heightForRowAtIndexPath:(NSIndexPath *)indexPath
{
    doJsonValue *jsonValue = [_dataArrays GetData:(int)indexPath.row];
    doJsonNode *dataNode = [jsonValue GetNode];
    int cellIndex = [dataNode GetOneInteger:@"cellTemplate" :0];
    NSString* indentify = _cellTemplatesArray[cellIndex];
    doUIModule*  model = _cellTemplatesDics[indentify];
    [model SetModelData:nil :jsonValue ];
    [model.CurrentUIModuleView OnRedraw];
    return ((UIView*)model.CurrentUIModuleView).frame.size.height;
}

#pragma mark - All GestureRecognizer Method
- (void)longPress:(UILongPressGestureRecognizer *)longPress
{
    if(longPress.state == UIGestureRecognizerStateBegan)
    {
        NSLog(@"%s",__func__);
        UITableViewCell *cell = (UITableViewCell *)[longPress view];
        NSIndexPath *indexPath = [self indexPathForCell:cell];
        doInvokeResult* _invokeResult = [[doInvokeResult alloc]init:_model.UniqueKey];
        [_invokeResult SetResultInteger:(int)indexPath.row];
        [_model.EventCenter FireEvent:@"longTouch":_invokeResult];
    }
}
-(void)tableView:(UITableView*)tableView  willDisplayCell:(UITableViewCell*) cell forRowAtIndexPath:(NSIndexPath*)indexPath
{
    [cell setBackgroundColor:[UIColor clearColor]];
}

#pragma mark - scrollView delegate
- (void)scrollViewDidScroll:(UIScrollView *)scrollView
{
    UIView *_headView = (UIView *)_headViewModel.CurrentUIModuleView;
    if(_headView && !_isRefreshing)
    {
        if(scrollView.contentOffset.y >= _headView.frame.size.height*(-1))
            [self fireEvent:0 :scrollView.contentOffset.y];
        else
            [self fireEvent:1 :scrollView.contentOffset.y];
    }
    doInvokeResult* _invokeResult = [[doInvokeResult alloc]init:_model.UniqueKey];
    [_model.EventCenter FireEvent:@"didScroll":_invokeResult];
}
- (void)scrollViewDidEndDecelerating:(UIScrollView *)scrollView
{
    UIView *_headView = (UIView *)_headViewModel.CurrentUIModuleView;
    if(scrollView.contentOffset.y < _headView.frame.size.height*(-1) && !_isRefreshing && _headView)
    {
        [self fireEvent:2 :scrollView.contentOffset.y];
        self.contentInset = UIEdgeInsetsMake(_headView.frame.size.height, 0, 0, 0);
        _isRefreshing = YES;
    }
    doInvokeResult* _invokeResult = [[doInvokeResult alloc]init:_model.UniqueKey];
    [_model.EventCenter FireEvent:@"endScroll":_invokeResult];
}

#pragma mark - doIUIModuleView协议方法（必须）<大部分情况不需修改>
- (BOOL) OnPropertiesChanging: (NSMutableDictionary *) _changedValues
{
    //属性改变时,返回NO，将不会执行Changed方法
    return YES;
}
- (void) OnPropertiesChanged: (NSMutableDictionary*) _changedValues
{
    //_model的属性进行修改，同时调用self的对应的属性方法，修改视图
    [doUIModuleHelper HandleViewProperChanged: self :_model : _changedValues ];
}
- (BOOL) InvokeSyncMethod: (NSString *) _methodName : (doJsonNode *)_dicParas :(id<doIScriptEngine>)_scriptEngine : (doInvokeResult *) _invokeResult
{
    //同步消息
    return [doScriptEngineHelper InvokeSyncSelector:self : _methodName :_dicParas :_scriptEngine :_invokeResult];
}
- (BOOL) InvokeAsyncMethod: (NSString *) _methodName : (doJsonNode *) _dicParas :(id<doIScriptEngine>) _scriptEngine : (NSString *) _callbackFuncName
{
    //异步消息
    return [doScriptEngineHelper InvokeASyncSelector:self : _methodName :_dicParas :_scriptEngine: _callbackFuncName];
}
- (doUIModule *) GetModel
{
    //获取model对象
    return _model;
}

@end
