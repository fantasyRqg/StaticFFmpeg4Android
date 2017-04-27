cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -frtti -fexceptions -Wall")

set(EXTERNAL_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../external)

find_package(PkgConfig)


#add_library(libswscale STATIC IMPORTED )
#add_library(libavutil STATIC IMPORTED )
#add_library(libavcodec STATIC IMPORTED )
#add_library(libavfilter STATIC IMPORTED )
#add_library(libavformat STATIC IMPORTED )
#add_library(libpostproc STATIC IMPORTED )
#add_library(libswresample STATIC IMPORTED )


set(PKG_CONFIG_USE_CMAKE_PREFIX_PATH ${EXTERNAL_DIR})

pkg_check_modules(LIBSWSCALE REQUIRED libswscale)
pkg_check_modules(LIBAVUTIL REQUIRED libavutil)
#pkg_search_module(LIBAVCODEC REQUIRED libavcodec)
#pkg_search_module(LIBAVFILTER REQUIRED libavfilter)
#pkg_search_module(LIBAVFORMAT REQUIRED libavformat)
#pkg_search_module(LIBPOSTPROC REQUIRED libpostproc)
#pkg_search_module(LIBSwRESAMPLE REQUIRED libswresample)


if(${ANDROID_ABI} STREQUAL "armeabi-v7a")
    include_directories(${EXTERNAL_DIR}/FFmpeg/armeabi-v7a/include)

#    set_property(TARGET libswscale PROPERTY IMPORTED_LOCATION ${EXTERNAL_DIR}/FFmpeg/armeabi-v7a/lib/libswscale.a)
#    set_property(TARGET libavutil PROPERTY IMPORTED_LOCATION ${EXTERNAL_DIR}/FFmpeg/armeabi-v7a/lib/libavutil.a)
#    set_property(TARGET libavutil PROPERTY IMPORTED_LOCATION ${EXTERNAL_DIR}/FFmpeg/armeabi-v7a/lib/libavcodec.a)
#   set_property(TARGET libavutil PROPERTY IMPORTED_LOCATION ${EXTERNAL_DIR}/FFmpeg/armeabi-v7a/lib/libavfilter.a)
#    set_property(TARGET libavutil PROPERTY IMPORTED_LOCATION ${EXTERNAL_DIR}/FFmpeg/armeabi-v7a/lib/libavformat.a)
#    set_property(TARGET libavutil PROPERTY IMPORTED_LOCATION ${EXTERNAL_DIR}/FFmpeg/armeabi-v7a/lib/libpostproc.a)
#    set_property(TARGET libavutil PROPERTY IMPORTED_LOCATION ${EXTERNAL_DIR}/FFmpeg/armeabi-v7a/lib/libswresample.a)


endif()

#ANDROID_NDK_ABI_NAME
get_cmake_property(_variableNames VARIABLES)
foreach (_variableName ${_variableNames})
    message(STATUS "vvvvvvv : ${_variableName}=${${_variableName}}")
endforeach()



find_library(
        LOG_LIB
        log)

add_library(session SHARED
        SessionCombine.cpp
        )

target_link_libraries(  session
                        ${LOG_LIB}

                        ${EXTERNAL_DIR}/FFmpeg/armeabi-v7a/lib/libswscale.a
                        ${EXTERNAL_DIR}/FFmpeg/armeabi-v7a/lib/libavutil.a
                        ${EXTERNAL_DIR}/FFmpeg/armeabi-v7a/lib/libavcodec.a
                        ${EXTERNAL_DIR}/FFmpeg/armeabi-v7a/lib/libavfilter.a
                        ${EXTERNAL_DIR}/FFmpeg/armeabi-v7a/lib/libavformat.a
                        ${EXTERNAL_DIR}/FFmpeg/armeabi-v7a/lib/libpostproc.a
                        ${EXTERNAL_DIR}/FFmpeg/armeabi-v7a/lib/libswresample.a
                      )


