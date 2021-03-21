package com.example.andrei.customkeyboard;


public abstract class KeyboardLogicController {

    //might not need
    //private Keyboard defaultKeyboard;

    Integer nextKeyboardNum;
    CharacterProcessingReturnType nextTextOutput;
    String[] keyboardNames;
    Integer numToBackspace;


    // the following do not need to be edited, they work the same for all languages
    public Integer getNumberOfKeyboards() {
        return keyboardNames.length;

    }

    public String getKeyboardNameOfIndex(Integer index) {
        return keyboardNames[index];
    }


    public CharacterProcessingReturnType processText(CharSequence text) {

        processNextTextOutput(text);

        return nextTextOutput;
    }

    public Integer getKeyboardIndex() /*return type keyboard? or just string for name */ {
        //change to next keyboard should only be called by the logic controller when needed
        //changeToNextKeyboard(text);
        return nextKeyboardNum;
    }

    public Integer handleBackspace() {

        processBackspace();
        return numToBackspace;
    }

    //edit these three to suit the particular button presses for these languages
    public void processBackspace() {

    }

    public void handleSpace() {

    }

    public void handleShift() {

    }

    public CharacterProcessingReturnType handleLongPress (int keyCode) {
        int i;
        if (keyCode == 0)
            i = 1;

        return nextTextOutput;

    }


    //for internal use only, these are edited to modify local variables nextKeyboardNum and nextTextOutput
    public void changeToNextKeyboard(CharSequence text) {

    }

    public void processNextTextOutput(CharSequence text) {

    }

    public abstract void toDefaultKeyboard();

    public abstract void toExtraKeyboard();

    public abstract void toSpecialKeyboard();

    public abstract void toNumKeyboard();
}
