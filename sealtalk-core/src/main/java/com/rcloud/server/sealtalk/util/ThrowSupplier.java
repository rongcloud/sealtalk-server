package com.rcloud.server.sealtalk.util;

@FunctionalInterface
public interface ThrowSupplier<T> {


    T get() throws Throwable;

}
