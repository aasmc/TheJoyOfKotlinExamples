package ru.aasmc.bookexamples.chapter02

import java.io.File

fun fileLinesSequence(fileName: String, action: (String) -> Unit) {
    File(fileName)
        .inputStream()
        .use { stream ->
            stream.bufferedReader()
                .lineSequence()
                .forEach(action)
        }
}

fun fileForEachLine(fileName: String, action: (String) -> Unit) {
    File(fileName)
        .forEachLine(action = action)
}

fun fileUseLines(fileName: String, action: (String) -> Unit) {
    File(fileName)
        .useLines {
            it.forEach(action)
        }
}