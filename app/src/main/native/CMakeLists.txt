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


