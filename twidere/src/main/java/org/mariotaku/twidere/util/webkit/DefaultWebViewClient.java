/*
 * 				Twidere - Twitter client for Android
 * 
 *  Copyright (C) 2012-2014 Mariotaku Lee <mariotaku.lee@gmail.com>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mariotaku.twidere.util.webkit;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.mariotaku.twidere.util.Utils;


public class DefaultWebViewClient extends WebViewClient {

    private final Activity mActivity;

    public DefaultWebViewClient(final Activity activity) {
        mActivity = activity;
    }

    @Override
    public boolean shouldOverrideUrlLoading(final WebView view, final String url) {
        try {
            mActivity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (final ActivityNotFoundException e) {
            Utils.INSTANCE.showErrorMessage(mActivity, null, e, false);
        }
        return true;
    }

    public Activity getActivity() {
        return mActivity;
    }
}
