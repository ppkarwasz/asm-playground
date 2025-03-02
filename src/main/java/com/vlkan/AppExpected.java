package com.vlkan;

import static com.vlkan.Log4j.*;

public class AppExpected {

    public static void main(String[] args) {
        System.out.println("should log at line 9");
        LOCATION_REF.get().init("AppExpected.java", "com.vlkan.AppExpected", "main", 9);
        log();
        System.out.println("nothing to see here");
        System.out.println("should log at line 12");
        LOCATION_REF.get().init("AppExpected.java", "com.vlkan.AppExpected", "main", 12);
        log();
        f();
    }

    private static void f() {
        System.out.println("adding some indirection");
        System.out.println("should log at line 19");
        LOCATION_REF.get().init("AppExpected.java", "com.vlkan.AppExpected", "main", 19);
        log();
    }

}
