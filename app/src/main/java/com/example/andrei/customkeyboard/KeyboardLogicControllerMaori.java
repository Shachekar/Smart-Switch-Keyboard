package com.example.andrei.customkeyboard;

public class KeyboardLogicControllerMaori extends KeyboardLogicController {

    public KeyboardLogicControllerMaori() {

        //keyboards should always have index 0 for default, index 1 for numbers, index 2 for special characters
        keyboardNames = new String[]{"maori_default", "maori_numbers"};

    }


    //edit these three to suit the particular button presses for these languages
    public void processBackspace() {

    }

    public void handleSpace() {

    }

    public void handleShift() {

    }

    public void toSpecialKeyboard() {
        nextKeyboardNum = 3;
    }

    public void toExtraKeyboard() {
        ;
    }

    public void toDefaultKeyboard() {
        nextKeyboardNum = 1;
    }

    public void toNumKeyboard() {
        nextKeyboardNum = 2;
    }


    //for internal use only, these are edited to modify local variables nextKeyboardNum and nextTextOutput
    public void changeToNextKeyboard(CharSequence text) {

    }

    public void processNextTextOutput(CharSequence text) {

    }


}
