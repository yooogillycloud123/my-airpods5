cmake_minimum_required(VERSION 3.22.1)
project(aabridge_starter C)

add_executable(libaabridge.so
        route_starter.c)

target_compile_options(libaabridge.so PRIVATE
        -O2
        -fvisibility=hidden)

target_link_options(libaabridge.so PRIVATE
        -Wl,--gc-sections
        -Wl,--strip-all)
