package com.smartbear.coapsupport;

public class Utils {
    public static boolean areStringsEqual(String s1, String s2, boolean caseInsensitive, boolean dontDistinctNullAndEmpty){
        if(dontDistinctNullAndEmpty) {
            if (s1 == null || s1.length() == 0) return s2 == null || s2.length() == 0;
        }
        return areStringsEqual(s1, s2, caseInsensitive);
    }

    public static boolean areStringsEqual(String s1, String s2, boolean caseInsensitive){
        if(s1 == null) return s2 == null;
        if(caseInsensitive) return s1.equalsIgnoreCase(s2); else return s1.equals(s2);
    }
    public static boolean areStringsEqual(String s1, String s2){
        return areStringsEqual(s1, s2, false);
    }

    public static String bytesToHexString(byte[] buf){
        final String decimals = "0123456789ABCDEF";
        if(buf == null) return null;
        char[] r = new char[buf.length * 2];
        for(int i = 0; i < buf.length; ++i){
            r[i * 2] = decimals.charAt((buf[i] & 0xf0) >> 4);
            r[i * 2 + 1] = decimals.charAt(buf[i] & 0x0f);
        }
        return new String(r);
    }

    public static byte[] hexStringToBytes(String str){
        if(str == null) return null;
        if(str.length() % 2 != 0) throw new IllegalArgumentException();
        byte[] result = new byte[str.length() / 2];
        try {
            for(int i = 0; i < result.length; ++i){
                result[i] = (byte)Short.parseShort(str.substring(i * 2, i * 2 + 2), 16);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException();
        }
        return result;
    }

}
