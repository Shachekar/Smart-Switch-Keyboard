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



//this module should primarily include the use of the keyboard functions and passing them to the respective language controller
//it should rarely be edited once the languages are abstracted out
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


    //burmese special logic handling
    private boolean vowel = false;
    private char longAChar = 0;
    private char bottomChar = 0;
    private boolean isNg = false;
    private CharSequence hCharSeq = "";
    private boolean isLongAStacking = false;
    private boolean isStacking = false;
    private boolean isNgStack = false;

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

        //TODO: set default here


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
                //put a whole bunch of code in here to reset values like longAChar etc.
                //it is acceptable to go back to about 8 characters (one syllable)
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
                    CharSequence text;
                    CharacterProcessingReturnType thisReturn;
                    db.getInstance(getApplicationContext()).insertData_words(String.valueOf(code));
                    if (currentLanguage == Language.burmese) {
                        thisReturn = myProcessing(String.valueOf(code));
                        text = thisReturn.text;
                    }
                    else
                        text = String.valueOf(code);
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
        CharacterProcessingReturnType thisReturn;
        CharSequence processedText;
        int toDelete = 0;

        //the return type has the processed text (defaults to unprocessed where needed) and the number of characters that should be deleted (default 0)
        if (currentLanguage == Language.burmese) {
            thisReturn = myProcessing(text);
            processedText = thisReturn.text;
            toDelete = thisReturn.numToDelete;
        }
        else
            processedText = text;

        //delete and replace text
        ic.deleteSurroundingText(toDelete,0);
        ic.commitText(processedText,0);

        if (currentLanguage == Language.burmese) {
                myKeyboardSwap(text);
        }

    }



    //push as much Burmese logic down here, or yeet into a separate class a later date
    public CharacterProcessingReturnType myProcessing(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        CharacterProcessingReturnType returnValue;
        int numToDelete = 0;
        //turn off flags if required

        //turn off long A if on the vowel page after stacking (this should not be triggered until after second consonant of stack), does not trigger for long a top character of stack
        if (isStacking && vowel && !isLongAStacking) {
            longAChar = 0;
            isStacking = false;
        }
        //turn off long A for W
        if (text.charAt(0) == '\u103D')
            longAChar = 0;

        //stacking character for what should be long A, catches this before regular long A
        if (longAChar != 0  && text.equals("\u1039")) {
            isLongAStacking = true;
            isStacking = false;
        }


        //process long A for a stack if a long A triggering character was pressed earlier before the more recent (bottom) consonant
        else if (isLongAStacking) {
            //make sure ng does not trigger a long A when it is stacked
            if (longAChar == 'င') {
                isLongAStacking = false;

                //for an ng stack, the long A character of the stack is the current character
                if (myIsLongAChar(text.charAt(0)))
                    longAChar = text.charAt(0);
                else
                    longAChar = 0;

                //clear the flags
                bottomChar = 0;
                longAChar = 0;
            }

            //stack where the top character (typed first) should trigger a long A
            else if (text.toString().contains("\u102C") && bottomChar != 0) {
                //ensure long A for the back text
                text = longAChar + "\u1039" + bottomChar + replaceLongA(text);;

                //ic.deleteSurroundingText(3, 0);
                numToDelete = 3;
                isLongAStacking = false;
                longAChar = 0;
                bottomChar = 0;
            }

            //capture the text at the bottom of the stack to rebuild later
            else if (bottomChar == 0)
                bottomChar = text.charAt(0);

            //reset if none of the key considerations are met
            else {
                isLongAStacking = false;
                bottomChar = 0;
                longAChar = 0;
            }
        }

        //make into a process longA routine?
        //this is the default long a replacement routine with no stacking etc.
        if (longAChar != 0 && vowel && !isLongAStacking && !isStacking) {
                text = replaceLongA(text);
            longAChar = 0;
        }

        //stacking character for ng, incoroporated into
        if (isNg) {
            if (text.equals("\u1039")) {
                text = "\u103A\u1039";
                isNgStack = true;
            }
            isNg = false;
        }
        //swap order wh
        if (hCharSeq.length() > 0) {
            //w is always the first character in a W key press regardless of length
            if (text.charAt(0) == '\u103D') {

                //delete the number of characters in the hString
                //ic.deleteSurroundingText(hCharSeq.length(), 0);
                numToDelete = hCharSeq.length();
                CharSequence charactersAfterW;
                CharSequence charactersBeforeH;

                if (text.length() >= 3)
                    charactersAfterW = text.subSequence(1,text.length()-1);
                else if (text.length() == 2)
                    charactersAfterW = Character.toString(text.charAt(1));
                else //w only, no others
                    charactersAfterW = "";

                if (hCharSeq.length() == 2)
                    charactersBeforeH = Character.toString(hCharSeq.charAt(0));
                else //h only, no others
                    charactersBeforeH = "";

                text = charactersBeforeH + "\u103D" + "\u103E" + charactersAfterW;
            }
            hCharSeq = "";
        }

        if (text.length() == 1) {
            //catch long A character need
            if (myIsLongAChar(text.charAt(0)))
                longAChar = text.charAt(0);
            //catch Ng
            if (text.charAt(0) == '\u1004') {
                isNg = true;
            }
            //catch general stacking
            if (text.charAt(0) == '\u1039' && !isLongAStacking) {
                isStacking = true;
            }
        }

        //H is always the last character in a sequence that contains H, save the characters for rebuilding in inverted W H sequence
        if (text.charAt(text.length()-1) == '\u103E')
                hCharSeq = text;

        returnValue = new CharacterProcessingReturnType(text, numToDelete);

        return returnValue;
    }

    public CharSequence replaceLongA(CharSequence text) {
        if (text.equals("\u102C"))
            text = "\u102B";
        else if (text.equals("\u1031\u102C"))
            text = "\u1031\u102B";

            //ွာ is generally preferred as  ွာ
            // else if (text.equals("\u103D\u102C"))
            //    text = "\u103D\u102B";
        else if (text.equals("\u1031\u102C\u103A"))
            text = "\u1031\u102B\u103A";

        return text;
    }

    public boolean myIsLongAChar(char code) {
        String longAChars = "ခဂငဎဒဓပဝ";
        return longAChars.contains(String.valueOf(code));
    }

    public boolean mySpecialSymbols(CharSequence text) {
        String specialChars = "\u1021\u1027\u1023\u1024\u1025\u1026\u1029\u102A\u1009\u102C\u1009\u1039";
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

        if ((myToneMarkers(text) && !vowel) || mySpecialSymbols(text))
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


}
