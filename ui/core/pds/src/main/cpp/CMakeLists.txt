cmake_minimum_required(VERSION 3.18.1)

project("pds_node_bridge")

# Add Node.js headers
include_directories(
        ${CMAKE_SOURCE_DIR}/../../../libs/include/node
)

# Create the JNI bridge library
add_library(
        pds_node_bridge
        SHARED
        node_bridge.cpp
)

# Link against Node.js library and Android log
target_link_libraries(
        pds_node_bridge
        ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libnode.so
        log
)
