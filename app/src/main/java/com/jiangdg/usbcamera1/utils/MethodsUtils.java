package com.jiangdg.usbcamera1.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * desc: 公共类
 * author:  yt
 * <p>
 * creat:  2018/12/6 10:50
 */

public class MethodsUtils {

    public static String stringFilter2(String str, String pach) throws PatternSyntaxException {
        //只允许数字和汉字和字母
        String regEx = pach;
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        return m.replaceAll("").trim();
    }


    /**
     * 将系统表情转化为字符串
     *
     * @param s
     * @return
     */
    public static String getString(String s) {
        int length = s.length();
        String context = "";
        //循环遍历字符串，将字符串拆分为一个一个字符
        for (int i = 0; i < length; i++) {
            char codePoint = s.charAt(i);
            //判断字符是否是emoji表情的字符
            if (isEmojiCharacter(codePoint)) {
                //如果是将以大括号括起来
                String emoji = "{" + Integer.toHexString(codePoint) + "}";
                context = context + emoji;
                continue;
            }
            context = context + codePoint;
        }
        return context;
    }

    /**
     * 将系统表情转化为字符串
     *
     * @param s
     * @return
     */
    public static boolean havaEmoji(String s) {
        int length = s.length();
        String context = "";
        //循环遍历字符串，将字符串拆分为一个一个字符
        for (int i = 0; i < length; i++) {
            char codePoint = s.charAt(i);
            //判断字符是否是emoji表情的字符
            if (isEmojiCharacter(codePoint)) {
                //如果是就返回
                return true;
            }
        }
        return false;
    }

    private static char[] codePoints = {'☔', '☕', '✈', '⛪', '⛲', '✂', '⛵', '⛽', '⛺', '⚽',
            '⚾', '⛳', '⛄', '⛽', '⚓', '⌚', '✒', '✏', '✍', '☁', '⚡', '⏰', '☎', '⏳', '⌛', '⛎', '♈', '♉', '♊', '♋', '♌', '♍', '♎',
            '♏', '♐', '♑', '♒', '♓', '✴', '㊙', '㊗',
            '▶', '⏩', '⏪', '◀', '➡', '⬆', '⬇', '↗', '↘', '↙', '↖', '⬅', '©', '®', '™', '⬛', '⬜', '♠', '♣', '♥', 'Ⓜ', '⛔', '‼', '⁉', '〽', '♻', '❇', '⃣',
            'Ⓜ', '⛔', '⁉', '‼', '〽', '❎', '✅', '⏫', '⏬', '↕', '↔', '↪', '↩', '⤵', 'ℹ', '✔', '〰', '➖', '➗', '✖', '➰', '⚪', '☑', '⚫', '▫', '◾', '◻',
            '◽', '✉', '☀', '⛅', '❄', '✨', '⭐', '�',
            '✊', '☝', '✋', '✌', '✋', '☝', '❌', '⭕', '♨', '❗', '❕', '❓', '❔', '⚠', '✳', '➿', '♿', '♦'};

    /**
     * 是否包含表情
     *
     * @param codePoint
     * @return 如果不包含 返回false,包含 则返回true
     */

    public static boolean isEmojiCharacter(char codePoint) {
        if (!((codePoint == 0x0) || (codePoint == 0x9) || (codePoint == 0xA)
                || (codePoint == 0xD) || ((codePoint >= 0x20) && (codePoint <= 0xD7FF))
                || ((codePoint >= 0xE000) && (codePoint <= 0xFFFD)) || ((codePoint >= 0x10000) && (codePoint <= 0x10FFFF)))) {
            return true;
        }
        for (char c : codePoints) {
            if (c == codePoint) {
                return true;
            }
        }
        return false;
    }


}