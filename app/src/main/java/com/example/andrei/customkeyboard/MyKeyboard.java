package com.example.andrei.customkeyboard;


import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.media.AudioManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import java.security.Key;

public class MyKeyboard extends InputMethodService
            implements KeyboardView.OnKeyboardActionListener{

    StringBuilder stringBuilder = new StringBuilder();
    DatabaseHelper db;
    private KeyboardView kv;
    private Keyboard keyboard;
    private Keyboard keyboardNumbers;
    private Keyboard keyboardSpecial;
    private Keyboard keyboardDefault;
    private Keyboard keyboardExtra;
    private Keyboard maori;
    private Language defaultLanguage;
    private Language currentLanguage;
    private boolean caps = false;
    private boolean vowel = false;

    //burmese special logic handling
    private char longAChar = 0;
    private boolean isNg = false;
    private boolean isW = false;

    //add each language here
    enum Language {
        burmese,
        maori
    }

    @Override
    public View onCreateInputView() {
        kv = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard, null);
        db.getInstance(getApplicationContext());

        //default language gets initialised to burmese upon initialisation
        currentLanguage = Language.maori;
        changeLanguage();


        kv.setPreviewEnabled(false);
        kv.setOnKeyboardActionListener(this);
        return kv;
    }

    //set the four keyboard types for each language here
    public void changeLanguage() {

        //remove this and replace with language menu when feasible
        if (currentLanguage == Language.maori)
            currentLanguage = Language.burmese;
        else
            currentLanguage = Language.maori;

        switch (currentLanguage) {
            case burmese:
                keyboardNumbers = new Keyboard(this, R.xml.burmese_numbers);
                keyboardSpecial = new Keyboard(this, R.xml.burmese_vowels);
                keyboardDefault = new Keyboard(this, R.xml.burmese_consonants);
                keyboardExtra = new Keyboard(this, R.xml.special);
                vowel = false;
                break;
            case maori:
                keyboardNumbers = new Keyboard(this, R.xml.maori_numbers);
                keyboardDefault = new Keyboard(this, R.xml.maori_default);

                break;

        }

        kv.setKeyboard(keyboardDefault);

    }




    @Override
    //I am putting common buttons here, so shift, backspace, enter etc. also trigger language changes
    //this is called by buttons that have android:codes
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();

        playClick(primaryCode);
        switch (primaryCode){
            case Keyboard.KEYCODE_DELETE:
                ic.deleteSurroundingText(1, 0);
                if (currentLanguage == Language.burmese)
                    myKeyboardSwap("bs");
                break;
            case Keyboard.KEYCODE_SHIFT:
                caps = !caps;
                keyboard.setShifted(caps);
                kv.invalidateAllKeys();
                break;
            case Keyboard.KEYCODE_DONE:
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                break;
            //case 510: //medial puts the input and doesn't change stat
              //  db.getInstance(getApplicationContext()).insertData_words(String.valueOf(primaryCode));
               // ic.commitText(String.valueOf(primaryCode), 1);
            default:
                char code = (char) primaryCode;
                if(Character.isLetter(code) && caps) {
                    code = Character.toUpperCase(code);
                }

                if(primaryCode==500 || primaryCode==-101 ||  primaryCode==502 || primaryCode==505 || primaryCode==506 || primaryCode==521) {

                    if (primaryCode == 502)
                        kv.setKeyboard(keyboardNumbers);

                    if (primaryCode == 505) {
                        kv.setKeyboard(keyboardDefault);
                        if (currentLanguage == Language.burmese)
                            vowel = false;
                    }

                    if (primaryCode == 506)
                        kv.setKeyboard(keyboardExtra);

                    if (primaryCode == 521)
                        changeLanguage();

                    kv.setPreviewEnabled(false);
                    kv.setOnKeyboardActionListener(this);


                }
                else {
                    //weird characters like from popup keyboard
                    CharSequence text = String.valueOf(code);
                    db.getInstance(getApplicationContext()).insertData_words(String.valueOf(code));
                    if (currentLanguage == Language.burmese)
                        text = myProcessing(String.valueOf(code));
                    ic.commitText(text, 1);
                    if (primaryCode == 32)
                        myKeyboardSwap("sp");
                    else
                        myKeyboardSwap(text);
                    //not even sure this code executes
                    /*db.getInstance(getApplicationContext()).insertData_words(String.valueOf(code));
                    ic.commitText(String.valueOf(code), 1);
                    //String s = String.valueOf(primaryCode);
                    //ic.commitText(s, 1);
                    //might be better way to do this, e.g. handle space bar separate
                    if (primaryCode == 32 && !vowel) // dont swap keyboards if a space bar and consonant
                        vowel = false;
                    else if (primaryCode == 4156 || primaryCode == 4155 || primaryCode == 4158)
                        vowel = true;
                    else
                        vowelConsonantSwap();*/
                }
        }
    }


    @Override
    //here it will handle actual characters typed to the line rather than controls, trying to remove language specific logic from it
    //this is called for buttons that have android:keyOutputText
    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();

        if (currentLanguage == Language.burmese)
            text = myProcessing(text);

        ic.commitText(text,0);

        if (currentLanguage == Language.burmese) {
                myKeyboardSwap(text);
        }

    }

    //push as much Burmese logic down here, or yeet into a separate class a later date
    public CharSequence myProcessing(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();

        //process possible characters so flag doesn't get sucked up immediately

        //turn off long A for stacking character or W
        if (text.equals("\u1039") || text.equals("\u103E"))
            longAChar = 0;

        //process long A if a long A triggering character was pressed earlier
        if (longAChar != 0 && vowel) {
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
                ic.deleteSurroundingText(1, 0);
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
            kv.setKeyboard(keyboardSpecial);
        else
            kv.setKeyboard(keyboardDefault);

        kv.setPreviewEnabled(false);
        kv.setOnKeyboardActionListener(this);
    }

    @Override
    public void swipeLeft() {

    }

    @Override
    public void swipeRight() {

    }

    @Override
    public void swipeDown() {

    }

    @Override
    public void swipeUp() {

    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    //I'm not even sure if this does anything, consider yeeting
    public void onPress(int primaryCode) {
        if(primaryCode == -101)
        {
            InputMethodManager imeManager = (InputMethodManager) getApplicationContext().getSystemService(INPUT_METHOD_SERVICE);
            imeManager.showInputMethodPicker();
        }

        if (primaryCode==500 || primaryCode==-101 || primaryCode==32 || primaryCode==-5 || primaryCode==-4 || primaryCode==505 || primaryCode==502) {

        } else {
            kv.setPreviewEnabled(false);
        }
    }

    @Override
    public void onRelease(int primaryCode) {
        kv.setPreviewEnabled(false);
    }

    //sound, consider yeeting
    public void playClick(int keyCode){
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        switch (keyCode) {
            case 32:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD);
                break;
            case 10:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD);
                break;
            case Keyboard.KEYCODE_DELETE:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD);
                break;
            default:
                am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD);
        }
    }

    //1) the ng+stack needs a rework, so \u1004 + \u1039 → \u1004 + \u103A\u1039, GR: code I think is correct but not working as expected
    //2) w and h still need to be inverted, GR: I think this is working, not sure what success looks like
    //3) finals located on the consonant page should not trigger the vowel page, GR: I think this works, may need to confirm what the finals are
    //4) backspace needs to be made repeatable, GR: it is, just doens't appear to work as intended
    //5) related: if you type a 2 code-point vowel, deleting it takes 2 backspaces, which means we'll be on the wrong page, so that's a thing
    // GR: really unsure how to fix this, but it is easy to navigate back to consonants if stuck?
    //6) also the special vowel symbols on the အ key should not move to the vowel page either, stupid code didn't handle this before, GR: doesn't now
    //7) a-rasing being turned off and, also a-raising even in stacking syllables, possibly the same, GR: fixed
    //8) when we press the ှ key, it should turn off the a-raising state, GR: fixed

}
