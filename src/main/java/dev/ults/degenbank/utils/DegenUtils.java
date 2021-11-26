package dev.ults.degenbank.utils;

import java.text.DecimalFormat;

public class DegenUtils {

    public static DecimalFormat getBalanceFormat() {
        return new DecimalFormat("#,###,###,##0");
    }

    public static String getFormattedBalance(long balance) {
        return getBalanceFormat().format(balance);
    }

    public static String getDisplayBalance(long balance) {
        return getFormattedBalance(balance) + " DGN";
    }

}
