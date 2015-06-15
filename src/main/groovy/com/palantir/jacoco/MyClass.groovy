package com.palantir.jacoco

class MyClass {
    static void mymethod(Map m, Closure c) {
        println(m)
        c.run()
    }
}
