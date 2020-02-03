package com.example

import com.example.Transient

class Dependent {
    static void doSth() {
        Transient.doSth()
    }
}
