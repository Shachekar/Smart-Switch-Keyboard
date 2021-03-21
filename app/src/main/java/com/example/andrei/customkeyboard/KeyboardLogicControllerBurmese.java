package com.example.andrei.customkeyboard;

import android.inputmethodservice.Keyboard;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

public class KeyboardLogicControllerBurmese extends KeyboardLogicController {

    //import user settings for this language here

    //burmese special logic handling
    private boolean vowel = false;
    private char longAChar = 0;
    private char bottomChar = 0;
    private boolean isNg = false;
    private CharSequence hCharSeq = "";
    private boolean isLongAStacking = false;
    private boolean isStacking = false;
    private boolean isNgStack = false;


    public KeyboardLogicControllerBurmese() {

        //keyboards should always have index 0 for default, index 1 for numbers, index 2 for special characters
        keyboardNames = new String[]{"burmese_consonants", "burmese_numbers", "burmese_special", "burmese_vowels"};

    }

    //edit these three to suit the particular button presses for these languages
    public void processBackspace() {

    }

    public void handleSpace() {

    }

    public void handleShift() {

    }

    public void toExtraKeyboard() {
        //actual extra which in this case is vowels
        nextKeyboardNum = 3;
    }

    public void toSpecialKeyboard() {
        //random keys
        nextKeyboardNum = 2;
    }

    public void toDefaultKeyboard() {

        nextKeyboardNum = 0;
    }

    public void toNumKeyboard() {

        nextKeyboardNum = 1;
    }




    //for internal use only, these are edited to modify local variables nextKeyboardNum and nextTextOutput

    public void changeToNextKeyboard(CharSequence text) {
// <!-- android:id="@id/keyboard_burmese_consonants" -->
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
            nextKeyboardNum = 3;
        else if (text.equals("num"))
            nextKeyboardNum = 1;
        else if (text.equals("xtra"))
            nextKeyboardNum = 2;
        else
            nextKeyboardNum = 0;

    }

    public void processNextTextOutput(CharSequence text) {
            int numToDelete = 0;
            //in most instances is the same as typed text, will get overwritten if needed
            CharSequence processedText = text;
            //turn off flags if required

            //turn off long A if on the vowel page after stacking (this should not be triggered until after second consonant of stack), does not trigger for long a top character of stack
            if (isStacking && vowel && !isLongAStacking) {
                longAChar = 0;
                isStacking = false;
            }
            //turn off long A for W, consider yeeting, as the w + a key does not trigger long a
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
                    processedText = longAChar + "\u1039" + bottomChar + replaceLongA(text);;

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
                processedText = replaceLongA(text);
                longAChar = 0;
            }

            //stacking character for ng, incoroporated into
            if (isNg) {
                if (text.equals("\u1039")) {
                    processedText = "\u103A\u1039";
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

                    processedText = charactersBeforeH + "\u103D" + "\u103E" + charactersAfterW;
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
            CharacterProcessingReturnType n = new CharacterProcessingReturnType(processedText, numToDelete);
            nextTextOutput = n;

            changeToNextKeyboard(text);
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


    //put all the additional language specific logical rules here




}
