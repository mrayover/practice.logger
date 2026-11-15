cmake_minimum_required(VERSION 3.22.1)
project(composer_soundfont)
add_library(composer_soundfont SHARED composer_soundfont.cpp)
target_compile_options(composer_soundfont PRIVATE -O2 -Wall -Wextra)
