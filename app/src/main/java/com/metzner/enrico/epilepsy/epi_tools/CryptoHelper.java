package com.metzner.enrico.epilepsy.epi_tools;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Random;

public abstract class CryptoHelper {

    public static final int DECRYPT = 1;
    public static final int ENCRYPT = 2;

    public static final String checkString =
            "1023User0123User9876User4514User2421User8192User2351User6154User0001User6518User4515User4444User3214User8246User1793User0456User";

    private static char[] alphabet = {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            ' ', ',', ':', '#', '/', 'ä', 'ö', 'ü', '_', '|',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
            'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
            'u', 'v', 'w', 'x', 'y', 'z', '(', ')', '[', ']',
            '.', '*', '+', '-', 'A', 'B', 'C', 'D', 'E', 'F',
            'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
            'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'Ä', 'Ö', 'Ü', '!', '?', '^', '&', '%'
    };

    @NonNull
    public static String trimToAlphabet(@NonNull String _inString) {
        StringBuilder alphaString = new StringBuilder();
        int alphaLength = alphabet.length;
        for(char c: _inString.toCharArray()) {
            boolean isInArray = false;
            for(char a: alphabet) {
                if(c==a) {
                    isInArray = true;
                    break;
                }
            }
            if(isInArray) alphaString.append(c);
        }
        return alphaString.toString();
    }
    @NonNull
    public static String createNewKey() {
        Random cRand = new Random();
        int aLength = alphabet.length;
        int cipherLength = 41 + 2*cRand.nextInt(11);
        StringBuilder cipher = new StringBuilder();
        for(int c=0; c<cipherLength; c++) {
            int charID = cRand.nextInt(aLength);
            cipher.append(alphabet[charID]);
        }
        return cipher.toString();
    }
    @NonNull
    public static String crypt(@NonNull String _src, @NonNull String _key, int de_en_crypt) {
        int aLength = alphabet.length;
        StringBuilder dst = new StringBuilder();
        int ed = (de_en_crypt==DECRYPT ? -1 : (de_en_crypt==ENCRYPT ? 1 : 0));
        int srcLength = _src.length();
        int keyLength = _key.length();
        for(int c=0; c<srcLength; c++) {
            char s = _src.charAt(c);
            int si = 0;
            while(si<aLength && alphabet[si]!=s) si++;
            char k = _key.charAt(c%keyLength);
            int ki = 0;
            while(ki<aLength && alphabet[ki]!=k) ki++;
            int ni = si + ed*ki + 10*aLength;
            dst.append(alphabet[ni%aLength]);
        }
        return dst.toString();
    }
}
