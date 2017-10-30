/*
 * Copyright (c) 2014-2017 Afero, Inc. All rights reserved.
 */

package io.afero.aferolab.helper;


import java.util.Stack;

public class BackStack<T extends BackStack.Stackable> {

    public interface Stackable {
        boolean onBackPressed();
    }


    private final Stack<T> mStack = new Stack<>();

    public BackStack() {
    }

    public void push(T s) {
        mStack.push(s);
    }

    public T pop() {
        return mStack.isEmpty() ? null : mStack.pop();
    }

    public T remove(T s) {
        return mStack.remove(s) ? s : null;
    }

    public void clear() {
        mStack.clear();
    }

    public T onBackPressed() {
        if (!(mStack.empty() || mStack.peek().onBackPressed())) {
            return mStack.pop();
        }

        return null;
    }
}
