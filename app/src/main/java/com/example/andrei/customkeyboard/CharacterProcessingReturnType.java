package com.example.andrei.customkeyboard;

public class CharacterProcessingReturnType {

        CharSequence text;
        int numToDelete;

        //should only need to be constructed, this class literally exists to return two sets of data in one return type...
        public CharacterProcessingReturnType(CharSequence t, int dNum) {
            text = t;
            numToDelete = dNum;
        }

}
