//
// Created by magnus on 10.01.20.
//

#ifndef LIBAUMS_LOG_H
#define LIBAUMS_LOG_H

#include <android/log.h>

// https://stackoverflow.com/questions/19375984/define-macro-to-log-error-with-file-and-line-in-android
#define TP_STR_HELPER(x) #x
#define TP_STR(x) TP_STR_HELPER(x)

#define LOG_D(tag, fmt, ...) __android_log_print(ANDROID_LOG_DEBUG, tag, "%s:%s " fmt "\n", __PRETTY_FUNCTION__, TP_STR(__LINE__), ##__VA_ARGS__)
#define LOG_I(tag, fmt, ...) __android_log_print(ANDROID_LOG_INFO, tag, "%s:%s " fmt "\n", __PRETTY_FUNCTION__, TP_STR(__LINE__), ##__VA_ARGS__)
#define LOG_E(tag, fmt, ...) __android_log_print(ANDROID_LOG_ERROR, tag, "%s:%s " fmt "\n", __PRETTY_FUNCTION__, TP_STR(__LINE__), ##__VA_ARGS__)
#define LOG_W(tag, fmt, ...) __android_log_print(ANDROID_LOG_WARN, tag, "%s:%s " fmt "\n", __PRETTY_FUNCTION__, TP_STR(__LINE__), ##__VA_ARGS__)

#endif //LIBAUMS_LOG_H
