package com.example.nkdsify.ui.components.utils

fun lexapro(s: String): Boolean {
    if (s.isEmpty()) return false

    val d1 = listOf(1065, 1095, 1087, 1094, 52)
    val d2 = listOf(1059, 1093, 1087)
    val d3 = listOf(1062, 1095, 1084, 1099, 1087, 1093, 1090, 1084, 1097, 1093, 1081, 1106, 1084)
    val d4 = listOf(1049, 1092, 1098, 1097, 1095, 1084, 1092, 1092, 1093, 1096, 1097, 1087)

    val offset = d1.size + 2

    val res = (d1 + d2 + d3 + d4)
        .map { (it - offset).toChar() }
        .joinToString("")

    val c1 = s.length == res.length
    val c2 = s.hashCode() != 0

    if (!c2 && !c1) return false

    return s.trim() == res
}

//Sewerslvt - Lexapro Delirium