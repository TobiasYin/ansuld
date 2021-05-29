# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)

project("SubProcess")

link_directories(${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -D ANDROID_RUNTIME")
set(CMAKE_CXX_FLAGS  "${CMAKE_CXX_FLAGS} -D ANDROID_RUNTIME")

find_library( # Sets the name of the path variable.
        log-lib
        log)


add_library(
        create-sub-process-lib
        SHARED
        ${CMAKE_SOURCE_DIR}/cpp/log.h
        ${CMAKE_SOURCE_DIR}/cpp/create_sub_process.h
        ${CMAKE_SOURCE_DIR}/cpp/create_sub_process.cpp
        ${CMAKE_SOURCE_DIR}/jni/org_minal_minal_tools_ProcessUtil.h
        ${CMAKE_SOURCE_DIR}/jni/org_minal_minal_tools_ProcessUtil.cpp
)

target_link_libraries(
        create-sub-process-lib
        ${log-lib}
)


# vnc libraries
# Requred to enable SIMD support
#if (CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
#    set(CMAKE_ASM_FLAGS "${CMAKE_ASM_FLAGS} --target=aarch64-linux-android${ANDROID_VERSION}")
#elseif (CMAKE_ANDROID_ARCH_ABI MATCHES "^armeabi.*")  # armeabi-v7a || armeabi-v6 || armeabi
#    set(CMAKE_ASM_FLAGS "${CMAKE_ASM_FLAGS} --target=arm-linux-androideabi${ANDROID_VERSION}")
#endif ()

set(OUTLIB_SRC "../outerlib")
# build libJPEG
set(BUILD_SHARED_LIBS OFF)
set(libjpeg_src_DIR ${OUTLIB_SRC}/libjpeg-turbo)
set(libjpeg_build_DIR ${CMAKE_BINARY_DIR}/libjpeg)
# adds a source subdir with specifying a build output dir
add_subdirectory(${libjpeg_src_DIR} ${libjpeg_build_DIR})
# set these variables so FindJPEG can find the library
set(JPEG_LIBRARY ${libjpeg_build_DIR}/libturbojpeg.a)
set(JPEG_INCLUDE_DIR ${libjpeg_src_DIR})
# set include directories so dependent code can find the headers
include_directories(
        ${libjpeg_src_DIR}
        ${libjpeg_build_DIR}
)


# build LibVNCClient
set(BUILD_SHARED_LIBS OFF)
set(libvnc_src_DIR ${OUTLIB_SRC}/libvncserver)
set(libvnc_build_DIR ${CMAKE_BINARY_DIR}/libvnc)
# adds a source subdir with specifying a build output dir
add_subdirectory(${libvnc_src_DIR} ${libvnc_build_DIR})
# need to add the build dir to include dirs as well because of generated rfbconfig.h
include_directories(
        ${libvnc_src_DIR}
        ${libvnc_build_DIR}
)

# build VNCConn
add_library(vncconn SHARED ${CMAKE_SOURCE_DIR}/cpp/vncconn.c)
target_link_libraries(vncconn
        log
        vncclient)

# build VNCCanvas
add_library(vnccanvas SHARED ${CMAKE_SOURCE_DIR}/cpp/vnccanvas.c)
target_link_libraries(vnccanvas
        log
        GLESv1_CM)
