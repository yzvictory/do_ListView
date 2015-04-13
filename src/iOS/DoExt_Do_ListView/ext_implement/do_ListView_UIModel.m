//
//  TYPEID_Model.m
//  DoExt_UI
//
//  Created by @userName on @time.
//  Copyright (c) 2015年 DoExt. All rights reserved.
//

#import "do_ListView_UIModel.h"
#import "doProperty.h"
#import "doJsonValue.h"
#import "doIListData.h"
#import "do_ListView_UIView.h"

@implementation do_ListView_UIModel

#pragma mark - 注册属性（--属性定义--）
/*
[self RegistProperty:[[doProperty alloc]init:@"属性名" :属性类型 :@"默认值" : BOOL:是否支持代码修改属性]];
 */
-(void)OnInit
{
    [super OnInit];
    
    //注册属性
    //属性声明
    [self RegistProperty:[[doProperty alloc]init:@"cellTemplates" :String :@"" :YES]];
    [self RegistProperty:[[doProperty alloc]init:@"headerView" :String :@"" :YES]];
    [self RegistProperty:[[doProperty alloc]init:@"isShowbar" :Bool :@"" :YES]];
    [self RegistProperty:[[doProperty alloc]init:@"selectedColor" :String :@"" :YES]];
}
-(void) SetModelData:(NSMutableDictionary*) _bindParas :(id) _jsonObject
{
    if([_jsonObject conformsToProtocol:@protocol(doIListData)]){
        do_ListView_UIView* view = (do_ListView_UIView*) self.CurrentUIModuleView;
        [view SetModelData:_jsonObject];
    }
    else
        [super SetModelData:_bindParas :_jsonObject ];    
}
@end
