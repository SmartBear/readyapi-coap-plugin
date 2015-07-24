package com.smartbear.coapsupport;

import ch.ethz.inf.vs.californium.coap.CoAP;

import java.util.Arrays;

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

    public static boolean areArraysEqual(byte[] arr1, byte[] arr2, boolean dontDistinctNilAndEmpty) {
        if(!dontDistinctNilAndEmpty) return Arrays.equals(arr1, arr2);
        if(arr1 == null || arr1.length == 0) return arr2 == null || arr2.length == 0;
        return Arrays.equals(arr1, arr2);
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

    public static String responseCodeToText(int code){
        switch (code){
            case 201: return "Created";
            case 202: return "Deleted";
            case 203: return "Valid";
            case 204: return "Changed";
            case 205: return "Content";
            case 231: return "Continue";
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 402: return "Bad Option";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 406: return "Not Acceptable";
            case 408: return "Request Entity Incomplete";
            case 412: return "Precondition Failed";
            case 413: return "Request Entity Too Large";
            case 415: return "Unsupported Content-Format";
            case 500: return "Internal Server Error";
            case 501: return "Not Implemented";
            case 502: return "Bad Gateway";
            case 503: return "Service Unavailable";
            case 504: return "Gateway Timeout";
            case 505: return "Proxying Not Supported";
        }
        return "";
    }


}
