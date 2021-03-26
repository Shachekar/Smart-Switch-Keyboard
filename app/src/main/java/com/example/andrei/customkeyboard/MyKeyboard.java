package com.example.andrei.customkeyboard;


import android.content.res.Resources;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.media.AudioManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;


import java.util.ArrayList;


//this module should primarily include the use of the keyboard functions and passing them to the respective language controller
//it should rarely be edited once the languages are abstracted out
public class MyKeyboard extends InputMethodService
            implements KeyboardView.OnKeyboardActionListener{

    StringBuilder stringBuilder = new StringBuilder();
    DatabaseHelper db;
    private KeyboardView kv;
    private Language defaultLanguage;
    private Language currentLanguage;
    private KeyboardLogicController currentKeyboardLogicController;
    private ArrayList<Keyboard> keyboardsForSelectedLanguage;

    private boolean caps = false;

    Keyboard keyboardNumbers;
    Keyboard keyboardDefault;
    Keyboard keyboardSpecial;
    Keyboard keyboardExtra;


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
        if (currentLanguage == Language.maori) {
            currentLanguage = Language.burmese;
            currentKeyboardLogicController = new KeyboardLogicControllerBurmese();
        }
        else {
            currentLanguage = Language.maori;
            currentKeyboardLogicController = new KeyboardLogicControllerMaori();
        }
        //for (int k = 0; k < currentKeyboardLogicController.getNumberOfKeyboards(); k++) {
        //    Keyboard newKeyboard = new Keyboard(burmese_constants.xml));
        //}
        switch (currentLanguage) {
        //do not yeet too fast, I need this :(
            case burmese:
                keyboardNumbers = new Keyboard(this, R.xml.burmese_numbers);
                keyboardSpecial = new Keyboard(this, R.xml.special);
                keyboardDefault = new Keyboard(this, R.xml.burmese_consonants);
                keyboardExtra = new Keyboard(this, R.xml.burmese_vowels);

                break;
            case maori:
                keyboardNumbers = new Keyboard(this, R.xml.maori_numbers);
                keyboardDefault = new Keyboard(this, R.xml.maori_default);

                break;

        }
        //just change to array index 0
        kv.setKeyboard(keyboardDefault);

    }

    public void changeKeyboard(Integer keyboardIndex) {

        //kv.setKeyboard(keyboardsForSelectedLanguage.get(keyboardIndex));
        switch (keyboardIndex) {
            case 0:
                kv.setKeyboard(keyboardDefault);
                break;
            case 1:
                kv.setKeyboard(keyboardNumbers);
                break;
            case 2:
                //special characters such as ?!#)*@
                kv.setKeyboard(keyboardSpecial);
            case 3:
                //vowels in Burmese, anything "extra"
                kv.setKeyboard(keyboardExtra);
                break;

        }

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
                    //changeKeyboard(currentKeyboardLogicController.getNextKeyboard("bs"));
                //put a whole bunch of code in here to reset values like longAChar etc.
                //it is acceptable to go back to about 8 characters (one syllable)
                break;
            case Keyboard.KEYCODE_SHIFT:
                caps = !caps;
                //keyboard.setShifted(caps);
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

                    if (primaryCode == 502) //numbers
                        currentKeyboardLogicController.toNumKeyboard();

                    if (primaryCode == 505) //return to default keyboard
                        currentKeyboardLogicController.toDefaultKeyboard();

                    if (primaryCode == 506) //special symbols2
                        currentKeyboardLogicController.toSpecialKeyboard();

                    if (primaryCode == 507) //special symbols1
                        currentKeyboardLogicController.toExtraKeyboard();

                    if (primaryCode == 521) //change language
                        changeLanguage();

                    changeKeyboard(currentKeyboardLogicController.getKeyboardIndex());
                    kv.setPreviewEnabled(false);
                    kv.setOnKeyboardActionListener(this);


                }
                else {
                    //weird characters like from popup keyboard
                    CharSequence text;
                    CharacterProcessingReturnType thisReturn;
                    db.getInstance(getApplicationContext()).insertData_words(String.valueOf(code));
                    thisReturn = currentKeyboardLogicController.processText(String.valueOf(code));
                    text = thisReturn.text;

                    ic.commitText(text, 1);
                    if (primaryCode == 32)
                        currentKeyboardLogicController.handleSpace();


                    //else
                    //    changeKeyboard(currentKeyboardLogicController.getNextKeyboard(text));
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

        kv.setPreviewEnabled(false);
        kv.setOnKeyboardActionListener(this);
    }


    @Override
    //here it will handle actual characters typed to the line rather than controls, trying to remove language specific logic from it
    //this is called for buttons that have android:keyOutputText
    //still not sure what the difference is, can I have two different on long press handlers, one for text,
    //other for keycode
    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        CharacterProcessingReturnType thisReturn;
        CharSequence processedText;
        int toDelete = 0;

        //the return type has the processed text (defaults to unprocessed where needed) and the number of characters that should be deleted (default 0)
        if (currentLanguage == Language.burmese) {
            thisReturn = currentKeyboardLogicController.processText(text);
            processedText = thisReturn.text;
            toDelete = thisReturn.numToDelete;
        }
        else
            processedText = text;

        //delete and replace text
        ic.deleteSurroundingText(toDelete,0);
        ic.commitText(processedText,0);

        if (currentLanguage == Language.burmese) {
            changeKeyboard(currentKeyboardLogicController.getKeyboardIndex());

        }

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

//    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
//        return super.onKeyLongPress(keyCode, event);
//        CharacterProcessingReturnType longKey;
//        longKey = currentKeyboardLogicController.handleLongPress(keyCode);

//        return super.onKeyLongPress(keyCode, event);
//    }

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
