//
//  TYPEID_UI.h
//  DoExt_UI
//
//  Created by @userName on @time.
//  Copyright (c) 2015年 DoExt. All rights reserved.
//

#import <UIKit/UIKit.h>

@protocol do_ListView_IView <NSObject>

@required
//属性方法
- (void)change_templates:(NSString *)newValue;
- (void)change_headerView:(NSString *)newValue;
- (void)change_isShowbar:(NSString *)newValue;
- (void)change_selectedColor:(NSString *)newValue;

//同步或异步方法
@end
