//
//  do_Network_SM.h
//  DoExt_SM
//
//  Created by @userName on @time.
//  Copyright (c) 2015年 DoExt. All rights reserved.
//

#import "do_Network_ISM.h"
#import "doSingletonModule.h"
@interface do_Network_SM : doSingletonModule<do_Network_ISM>

+ (do_Network_SM*)Instance;

@end