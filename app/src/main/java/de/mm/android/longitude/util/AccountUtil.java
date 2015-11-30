package de.mm.android.longitude.util;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

import com.google.android.gms.auth.GoogleAuthUtil;

public class AccountUtil {
	private static String[] getAccountNames(Context context) {
		Account[] accounts = AccountManager.get(context).getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
		String[] names = new String[accounts.length];
		for (int i = 0; i < names.length; i++) {
			names[i] = accounts[i].name;
		}
		return names;
	}

	public static boolean isAccountValid(Context context, String user) {
		String[] accounts = getAccountNames(context);
		for (String acc : accounts) {
            if (acc.equals(user)) {
				return true;
			}
		}
		return false;
	}

	private AccountUtil() {}

}