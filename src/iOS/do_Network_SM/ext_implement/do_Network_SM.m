//
//  do_Network_SM.m
//  DoExt_SM
//
//  Created by @userName on @time.
//  Copyright (c) 2015年 DoExt. All rights reserved.
//

#import "do_Network_SM.h"
#import "doReachability.h"

#import <UIKit/UIKit.h>

#import "doScriptEngineHelper.h"
#import "doIScriptEngine.h"
#import "doInvokeResult.h"

#import <CoreTelephony/CTCarrier.h>
#import <CoreTelephony/CTTelephonyNetworkInfo.h>

#import <ifaddrs.h>
#import <arpa/inet.h>

#import "doEventCenter.h"



@implementation do_Network_SM
{
    doReachability *_hostReach;
}
#pragma mark -
#pragma mark - 同步异步方法的实现

static do_Network_SM      * theInstance = nil;
+(do_Network_SM *)Instance
{
    if( theInstance == nil )
    {
        theInstance = [[do_Network_SM alloc]init];
    }
    
    return theInstance;
}
- (instancetype)init
{
    self = [super init];
    if (self) {
        [self startObserve];
        
    }
    return self;
}
-(void)startObserve
{
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(reachabilityChanged:)
                                                 name: kReachabilityChangedNotification
                                               object: nil];
    _hostReach = [doReachability reachabilityWithHostName:@"www.baidu.com"] ;
    [_hostReach startNotifier];
}
- (void)reachabilityChanged:(NSNotification *)note {
    doReachability* curReach = [note object];
    NetworkStatus status = [curReach currentReachabilityStatus];
    
    NSString *changedStatus = @"";
    switch (status) {
        case NotReachable:
            changedStatus = @"None";
            break;
        case ReachableVia2G:
            changedStatus = @"2G";
            break;
        case ReachableVia3G:
            changedStatus = @"3G";
            break;
        case ReachableViaWiFi:
            changedStatus = @"WIFI";
        default:
            changedStatus = @"UnKnown";;
            break;
    }
    doInvokeResult  *invoke = [[doInvokeResult alloc] init];
    [invoke SetResultText:changedStatus];
    [self.EventCenter FireEvent:@"changed" :invoke];
    
}

//同步
 - (void)getIP:(NSArray *)parms
 {

     
     doInvokeResult *_invokeResult = [parms objectAtIndex:2];
     //自己的代码实现
     
     
     NSString *address = @"error";
     struct ifaddrs *interfaces = NULL;
     struct ifaddrs *temp_addr = NULL;
     int success = 0;
     
     // retrieve the current interfaces - returns 0 on success
     success = getifaddrs(&interfaces);
     if (success == 0) {
         // Loop through linked list of interfaces
         temp_addr = interfaces;
         while (temp_addr != NULL) {
             if( temp_addr->ifa_addr->sa_family == AF_INET) {
                 // Check if interface is en0 which is the wifi connection on the iPhone
                 if ([[NSString stringWithUTF8String:temp_addr->ifa_name] isEqualToString:@"en0"]) {
                     // Get NSString from C String
                     address = [NSString stringWithUTF8String:inet_ntoa(((struct sockaddr_in *)temp_addr->ifa_addr)->sin_addr)];
                 }
             }
             
             temp_addr = temp_addr->ifa_next;
         }
     }
     
     // Free memory
     freeifaddrs(interfaces);
     [_invokeResult SetResultText:address];
     
 }

//获取设备的运营商
 - (void)getOperators:(NSArray *)parms
 {
     doInvokeResult *_invokeResult = [parms objectAtIndex:2];
     //自己的代码实现
     
     //首先判断是否插入了sim卡
     
     BOOL isSimCardAvailable = YES;
     
     CTTelephonyNetworkInfo* info = [[CTTelephonyNetworkInfo alloc] init];
     
     CTCarrier* carrier = info.subscriberCellularProvider;
     
     if(carrier.mobileNetworkCode == nil || [carrier.mobileNetworkCode isEqualToString:@""])
         
     {
         
         isSimCardAvailable = NO;
        [_invokeResult SetResultText:@"未插入sim卡"];
         
     }
     
     else
     {
         NSString *communicationType = [NSString stringWithFormat:@"%@",[carrier carrierName]];
         
         [_invokeResult SetResultText:communicationType];
     }
 }
//获取网络状态
 - (void)getStatus:(NSArray *)parms
 {
     //自己的代码实现
     doInvokeResult *_invokeResult = [parms objectAtIndex:2];
     NSArray *children = [[[[UIApplication sharedApplication]  valueForKeyPath:@"statusBar"]valueForKeyPath:@"foregroundView"]subviews];
     NSString *state = [[NSString alloc]init];
     int netType = 0;
     //获取到网络返回码
     for (id child in children) {
         if ([child isKindOfClass:NSClassFromString(@"UIStatusBarDataNetworkItemView")]) {
             //获取到状态栏
             netType = [[child valueForKeyPath:@"dataNetworkType"]intValue];
             
             switch (netType) {
                 case 0:
                     state = @"无网络";
                     //无网模式
                     break;
                 case 1:
                     state = @"2G";
                     break;
                 case 2:
                     state = @"3G";
                     break;
                 case 3:
                     state = @"4G";
                     break;
                 case 5:
                 {
                     state = @"wifi网络";
                 }
                     break;
                 default:
                 {
                     state = @"未识别网络";
                     break;
                 }
                
             }
             if(state == nil || state.length <= 0)
             {
                 state = @"没有网络连接";
             }
             [_invokeResult SetResultText:state];
         }
     }
 }
//异步

@end