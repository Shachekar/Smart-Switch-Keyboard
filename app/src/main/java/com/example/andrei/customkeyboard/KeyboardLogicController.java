package com.example.andrei.customkeyboard;

import android.inputmethodservice.Keyboard;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;


public abstract class KeyboardLogicController {

    //might not need
    private Keyboard defaultKeyboard;
    public Keyboard currentKeyboard;
    public CharacterProcessingReturnType returnType;

    public void handleBackspace() {

    }

    public void handleSpace() {

    }

    public void handleShift() {

    }

    public CharacterProcessingReturnType processText() {

        return returnType;
    }

    public void getNextKeyboard() /*return type keyboard? or just string for name */ {

    }




}
