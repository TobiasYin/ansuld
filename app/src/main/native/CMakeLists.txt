# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.10.2)

# Declares and names the project.

project("SubProcess")

# if you use prebuild native so
link_directories(${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -D ANDROID_RUNTIME")
set(CMAKE_CXX_FLAGS  "${CMAKE_CXX_FLAGS} -D ANDROID_RUNTIME")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
        create-sub-process-lib

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        cpp/log.h
        cpp/create_sub_process.h
        cpp/create_sub_process.cpp
        jni/com_asld_asld_tools_ProcessUtil.h
        jni/com_asld_asld_tools_ProcessUtil.cpp
        )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.


target_link_libraries( # Specifies the target library.
        create-sub-process-lib

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib}
        )

# vnc libraries
# Requred to enable SIMD support
if (CMAKE_ANDROID_ARCH_ABI STREQUAL "arm64-v8a")
    set(CMAKE_ASM_FLAGS "${CMAKE_ASM_FLAGS} --target=aarch64-linux-android${ANDROID_VERSION}")
elseif (CMAKE_ANDROID_ARCH_ABI MATCHES "^armeabi.*")  # armeabi-v7a || armeabi-v6 || armeabi
    set(CMAKE_ASM_FLAGS "${CMAKE_ASM_FLAGS} --target=arm-linux-androideabi${ANDROID_VERSION}")
endif ()
# build libJPEG
set(BUILD_SHARED_LIBS OFF)
set(libjpeg_src_DIR ../../../../native-lib/libjpeg-turbo)
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
set(libvnc_src_DIR ../../../../native-lib/libvncserver)
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

