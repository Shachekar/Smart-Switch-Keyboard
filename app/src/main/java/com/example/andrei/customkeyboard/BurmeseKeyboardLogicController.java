package com.example.andrei.customkeyboard;

import android.inputmethodservice.Keyboard;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

public class BurmeseKeyboardLogicController extends KeyboardLogicController {

    private boolean vowel = false;


    //import user settings for this language here

    //burmese special logic handling
    private char longAChar = 0;
    private boolean isNg = false;
    private boolean isW = false;
    private boolean isStackingLongA = false;

    //burmese extra keyboards
    private Keyboard consonantKeyboard;
    private Keyboard vowelKeyboard;
    //private Keyboard extraKeyboard;
    private Keyboard numbersKeyboard;


    public BurmeseKeyboardLogicController() {
        //super(consonantKeyboard);
        //okay so this is where it all fails

        //consonantKeyboard = new Keyboard(this, R.xml.burmese_consonants);
        //vowelKeyboard = new Keyboard(this, R.xml.burmese_vowels);
        //extraKeyboard = new Keyboard(this, R.xml.burmese_);
        //numbersKeyboard = new Keyboard(this, R.xml.burmese_numbers);

        //currentKeyboard = consonantKeyboard;
        //defaultKeyboard = consonantKeyboard;


    }

    //put all the main methods here

    public void handleBackspace() {

    }

    public void handleShift() {

    }

    //push as much Burmese logic down here, or yeet into a separate class a later date
    public CharSequence myProcessing(CharSequence text) {
        //InputConnection ic = getCurrentInputConnection();

        //process possible characters so flag doesn't get sucked up immediately

        //turn off long A for W or stacking character)
        if (/*text.equals("\u1039") ||*/ text.equals("\u103E"))
            longAChar = 0;

        //stacking character for what should be long A, catches this before other long A
        if (longAChar != 0  && text.equals("\u1039"))
            isStackingLongA = true;

        //process long A if a long A triggering character was pressed earlier
        else if (longAChar != 0 && vowel) {
            if (text.equals("\u102C"))
                text = "\u102B";
            else if (text.equals("\u1031\u102C"))
                text = "\u1031\u102B";

                //ွာ is always ွာ
                // else if (text.equals("\u103D\u102C"))
                //    text = "\u103D\u102B";
            else if (text.equals("\u1031\u102C\u103A"))
                text = "\u1031\u102B\u103A";
            longAChar = 0;
        }
        else if (isStackingLongA)
            text = "\u1004\u103A\u1039" + longAChar;



        //stacking character for ng
        if (isNg) {
            if (text.equals("\u1039"))
                text = "\u1039\u103A";
            isNg = false;
        }
        //swap order wh
        if (isW) {
            if (text.equals("\u103D")) {
                //delete a character
                //ic.deleteSurroundingText(1, 0);
                text = "\u103D\u103E";
                isW = false;
            }
        }

        if (text.length() == 1) {
            //catch long A character need
            if (myIsLongAChar(text.charAt(0)))
                longAChar = text.charAt(0);
            //catch Ng
            if (text.charAt(0) == '\u1004') {
                isNg = true;
            }

            //catch W
            if (text.charAt(0) == '\u103E')
                isW = true;
        }



        return text;
    }


    public boolean myIsLongAChar(char code) {
        String longAChars = "ခဂငဎဒဓပဝ";
        return longAChars.contains(String.valueOf(code));
    }

    public boolean mySpecialSymbols(CharSequence text) {
        String specialChars = "\u1021\u1027\u1023\u1024\u1025\u1026\u1029\u102A";
        return specialChars.contains(String.valueOf(text));
    }

    public boolean myIsMedial(CharSequence text) {
        return (text.equals("\u103C\u103E") || text.equals("\u103C") || text.equals("\u103B\u103E")
                || text.equals("\u103B") || text.equals("\u103E"));


    }

    public boolean myToneMarkers(CharSequence text) {
        return (text.equals("\u1009\u103A") || text.equals("\u1009\u1039") || text.equals("\u1036") || text.equals("\u1037") || text.equals("\u1038"));
    }

    public void myKeyboardSwap(CharSequence text) {

        //change the vowel status to what it should be post typing

        if ((myToneMarkers(text) && vowel) || mySpecialSymbols(text))
            vowel = false;
        else if (text.equals("bs"))
            vowel = !vowel;
        else if (text.equals("sp") && vowel)
            vowel = true;
        else if (myIsMedial(text) || !vowel)
            vowel = true;
        else
            vowel = false;


        if (vowel)
            currentKeyboard = vowelKeyboard;
        else
            currentKeyboard = consonantKeyboard;

        //kv.setPreviewEnabled(false);
        //kv.setOnKeyboardActionListener(this);
    }

}
